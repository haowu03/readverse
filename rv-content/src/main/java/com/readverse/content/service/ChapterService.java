package com.readverse.content.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.content.dto.ChapterSaveRequest;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.entity.ContentChapter;
import com.readverse.content.entity.ReaderEntitlement;
import com.readverse.content.mapper.ContentBookMapper;
import com.readverse.content.mapper.ContentChapterMapper;
import com.readverse.content.mapper.ReaderEntitlementMapper;
import com.readverse.content.vo.ChapterDetailVO;
import com.readverse.content.vo.ChapterSummaryVO;
import com.readverse.framework.cache.BloomFilterService;
import com.readverse.framework.cache.CacheProperties;
import com.readverse.framework.cache.TwoLevelCacheService;
import com.readverse.framework.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ContentChapterMapper chapterMapper;
    private final ContentBookMapper bookMapper;
    private final ReaderEntitlementMapper entitlementMapper;
    private final BookService bookService;
    private final TwoLevelCacheService cache;
    private final BloomFilterService bloom;
    private final CacheProperties cacheProperties;

    @EventListener(ApplicationReadyEvent.class)
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 4 * * ?")
    public void warmBloom() {
        try {
            List<Long> ids = chapterMapper.selectList(Wrappers.<ContentChapter>lambdaQuery()
                            .select(ContentChapter::getId))
                    .stream().map(ContentChapter::getId).toList();
            bloom.init(ids);
        } catch (Exception e) {
            log.warn("Bloom warmup skipped: {}", e.getMessage());
        }
    }

    public ChapterDetailVO detail(Long id) {
        if (!bloom.mightContain(id)) {
            throw new BizException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        String key = "rv:chapter:" + id;
        ChapterDetailVO cached = cache.get(key, ChapterDetailVO.class);
        if (cached != null) {
            return new ChapterDetailVO(cached.id(), cached.bookId(), cached.bookTitle(), cached.chapterNo(),
                    cached.title(), cached.content(), cached.isVip(), cached.wordCount(),
                    cached.prevChapterId(), cached.nextChapterId(), true);
        }
        String nullKey = "rv:chapter:null:" + id;
        if (cache.get(nullKey, String.class) != null) {
            throw new BizException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        ContentChapter chapter = chapterMapper.selectById(id);
        if (chapter == null || !"PUBLISHED".equals(chapter.getStatus())) {
            cache.put(nullKey, "1", cacheProperties.getNullTtlSeconds());
            throw new BizException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        if (Boolean.TRUE.equals(chapter.getIsVip()) && !canReadVip(chapter)) {
            throw new BizException(ErrorCode.CHAPTER_LOCKED);
        }
        ContentBook book = bookMapper.selectById(chapter.getBookId());
        ContentChapter prev = chapterMapper.selectOne(Wrappers.<ContentChapter>lambdaQuery()
                .eq(ContentChapter::getBookId, chapter.getBookId())
                .eq(ContentChapter::getStatus, "PUBLISHED")
                .lt(ContentChapter::getChapterNo, chapter.getChapterNo())
                .orderByDesc(ContentChapter::getChapterNo)
                .last("limit 1"));
        ContentChapter next = chapterMapper.selectOne(Wrappers.<ContentChapter>lambdaQuery()
                .eq(ContentChapter::getBookId, chapter.getBookId())
                .eq(ContentChapter::getStatus, "PUBLISHED")
                .gt(ContentChapter::getChapterNo, chapter.getChapterNo())
                .orderByAsc(ContentChapter::getChapterNo)
                .last("limit 1"));
        ChapterDetailVO vo = new ChapterDetailVO(chapter.getId(), chapter.getBookId(),
                book == null ? "" : book.getTitle(), chapter.getChapterNo(), chapter.getTitle(),
                chapter.getContent(), chapter.getIsVip(), chapter.getWordCount(),
                prev == null ? null : prev.getId(), next == null ? null : next.getId(), false);
        cache.put(key, vo, cacheProperties.getChapterTtlSeconds());
        return vo;
    }

    private boolean canReadVip(ContentChapter chapter) {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            return false;
        }
        Long chapterEntitlement = entitlementMapper.selectCount(Wrappers.<ReaderEntitlement>lambdaQuery()
                .eq(ReaderEntitlement::getUserId, userId)
                .eq(ReaderEntitlement::getTargetType, "CHAPTER")
                .eq(ReaderEntitlement::getTargetId, chapter.getId())
                .eq(ReaderEntitlement::getStatus, 1)
                .and(q -> q.isNull(ReaderEntitlement::getExpireTime)
                        .or().gt(ReaderEntitlement::getExpireTime, LocalDateTime.now())));
        if (chapterEntitlement != null && chapterEntitlement > 0) {
            return true;
        }
        Long bookEntitlement = entitlementMapper.selectCount(Wrappers.<ReaderEntitlement>lambdaQuery()
                .eq(ReaderEntitlement::getUserId, userId)
                .eq(ReaderEntitlement::getTargetType, "BOOK")
                .eq(ReaderEntitlement::getTargetId, chapter.getBookId())
                .eq(ReaderEntitlement::getStatus, 1)
                .and(q -> q.isNull(ReaderEntitlement::getExpireTime)
                        .or().gt(ReaderEntitlement::getExpireTime, LocalDateTime.now())));
        return bookEntitlement != null && bookEntitlement > 0;
    }

    public List<ContentChapter> adminList(Long bookId) {
        return chapterMapper.selectList(Wrappers.<ContentChapter>lambdaQuery()
                .eq(bookId != null, ContentChapter::getBookId, bookId)
                .orderByAsc(ContentChapter::getChapterNo));
    }

    @Transactional
    public ContentChapter create(ChapterSaveRequest request) {
        ContentBook book = bookMapper.selectById(request.bookId());
        if (book == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "书籍不存在");
        }
        Long exists = chapterMapper.selectCount(Wrappers.<ContentChapter>lambdaQuery()
                .eq(ContentChapter::getBookId, request.bookId())
                .eq(ContentChapter::getChapterNo, request.chapterNo()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "章节号已存在");
        }
        ContentChapter chapter = new ContentChapter();
        apply(chapter, request);
        chapter.setStatus("PUBLISHED");
        chapter.setPublishTime(LocalDateTime.now());
        chapter.setDeleted(0);
        chapterMapper.insert(chapter);
        bloom.add(chapter.getId());
        bookService.refreshMeta(chapter.getBookId());
        return chapter;
    }

    @Transactional
    public ContentChapter update(Long id, ChapterSaveRequest request) {
        ContentChapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        Long exists = chapterMapper.selectCount(Wrappers.<ContentChapter>lambdaQuery()
                .eq(ContentChapter::getBookId, request.bookId())
                .eq(ContentChapter::getChapterNo, request.chapterNo())
                .ne(ContentChapter::getId, id));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "章节号已存在");
        }
        apply(chapter, request);
        chapterMapper.updateById(chapter);
        bloom.add(chapter.getId());
        cache.evict("rv:chapter:" + id);
        cache.evict("rv:chapter:null:" + id);
        bookService.refreshMeta(chapter.getBookId());
        return chapter;
    }

    @Transactional
    public void delete(Long id) {
        ContentChapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            return;
        }
        chapterMapper.deleteById(id);
        cache.evict("rv:chapter:" + id);
        cache.evict("rv:chapter:null:" + id);
        bookService.refreshMeta(chapter.getBookId());
    }

    @Transactional
    public void publish(Long id) {
        ContentChapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        chapter.setStatus("PUBLISHED");
        chapter.setPublishTime(LocalDateTime.now());
        chapterMapper.updateById(chapter);
        bloom.add(id);
        cache.evict("rv:chapter:" + id);
        bookService.refreshMeta(chapter.getBookId());
    }

    private void apply(ContentChapter chapter, ChapterSaveRequest request) {
        chapter.setBookId(request.bookId());
        chapter.setChapterNo(request.chapterNo());
        chapter.setTitle(request.title());
        chapter.setContent(request.content());
        chapter.setWordCount(request.content() == null ? 0 : request.content().replaceAll("\\s", "").length());
        chapter.setIsVip(Boolean.TRUE.equals(request.isVip()));
        chapter.setStatus("PUBLISHED");
    }

    public List<ChapterSummaryVO> summaries(Long bookId) {
        return adminList(bookId).stream()
                .map(c -> new ChapterSummaryVO(c.getId(), c.getChapterNo(), c.getTitle(), c.getIsVip(),
                        c.getWordCount(), c.getPublishTime()))
                .toList();
    }

    public ContentChapter getEntity(Long id) {
        ContentChapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "章节不存在");
        }
        return chapter;
    }

    public boolean exists(Long id) {
        return id != null && chapterMapper.selectById(id) != null;
    }

    public List<Long> allIds() {
        return chapterMapper.selectList(Wrappers.<ContentChapter>lambdaQuery().select(ContentChapter::getId))
                .stream().map(ContentChapter::getId).filter(Objects::nonNull).toList();
    }
}
