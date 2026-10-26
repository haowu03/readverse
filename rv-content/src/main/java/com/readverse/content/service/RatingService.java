package com.readverse.content.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.content.entity.BookRating;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.mapper.BookRatingMapper;
import com.readverse.content.mapper.ContentBookMapper;
import com.readverse.content.vo.RatingSummaryVO;
import com.readverse.framework.cache.TwoLevelCacheService;
import com.readverse.framework.lock.DistributedLock;
import com.readverse.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final BookRatingMapper ratingMapper;
    private final ContentBookMapper bookMapper;
    private final DistributedLock lock;
    private final TwoLevelCacheService cache;

    @Transactional
    public RatingSummaryVO rate(Long bookId, int score) {
        Long userId = SecurityUtils.requireUserId();
        ContentBook book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "书籍不存在");
        }
        BookRating rating = ratingMapper.selectOne(Wrappers.<BookRating>lambdaQuery()
                .eq(BookRating::getUserId, userId)
                .eq(BookRating::getBookId, bookId));
        if (rating == null) {
            rating = new BookRating();
            rating.setUserId(userId);
            rating.setBookId(bookId);
            rating.setScore(score);
            ratingMapper.insert(rating);
        } else {
            rating.setScore(score);
            ratingMapper.updateById(rating);
        }
        lock.execute("rating:" + bookId, 3, 10, () -> {
            recompute(bookId);
            return null;
        });
        return summary(bookId);
    }

    public RatingSummaryVO summary(Long bookId) {
        ContentBook book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "书籍不存在");
        }
        Long userId = SecurityUtils.getUserIdOrNull();
        Integer myScore = null;
        if (userId != null) {
            BookRating rating = ratingMapper.selectOne(Wrappers.<BookRating>lambdaQuery()
                    .eq(BookRating::getUserId, userId)
                    .eq(BookRating::getBookId, bookId));
            myScore = rating == null ? null : rating.getScore();
        }
        return new RatingSummaryVO(book.getScore() == null ? 0 : book.getScore().doubleValue(),
                book.getRatingCount() == null ? 0 : book.getRatingCount(), myScore);
    }

    public void recompute(Long bookId) {
        List<BookRating> ratings = ratingMapper.selectList(Wrappers.<BookRating>lambdaQuery()
                .eq(BookRating::getBookId, bookId));
        int count = ratings.size();
        double average = ratings.stream().mapToInt(BookRating::getScore).average().orElse(0);
        ContentBook update = new ContentBook();
        update.setId(bookId);
        update.setRatingCount(count);
        update.setScore(BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
        bookMapper.updateById(update);
        cache.evict("rv:book:" + bookId);
        cache.evict("rv:home");
    }
}
