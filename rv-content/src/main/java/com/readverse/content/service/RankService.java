package com.readverse.content.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.mapper.ContentBookMapper;
import com.readverse.content.vo.BookCardVO;
import com.readverse.content.vo.RankItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String TOTAL_KEY = "rv:rank:click:total";
    private static final String DAILY_PREFIX = "rv:rank:click:";

    private final StringRedisTemplate redis;
    private final ContentBookMapper bookMapper;
    private final BookAssembler assembler;

    @Value("${rv.redis.enabled:true}")
    private boolean redisEnabled;

    public void recordClick(Long bookId) {
        if (bookId == null) {
            return;
        }
        String dailyKey = DAILY_PREFIX + LocalDate.now().format(DAY);
        try {
            redis.opsForZSet().incrementScore(dailyKey, String.valueOf(bookId), 1);
            redis.opsForZSet().incrementScore(TOTAL_KEY, String.valueOf(bookId), 1);
            redis.expire(dailyKey, Duration.ofDays(40));
        } catch (Exception e) {
            log.debug("Record rank click failed: {}", e.getMessage());
        }
    }

    public List<RankItemVO> top(String type, int limit) {
        int size = Math.min(50, Math.max(1, limit));
        String normalized = type == null ? "all" : type.toLowerCase();
        if (redisEnabled) {
            try {
                String key = rankKey(normalized);
                if (key != null) {
                    return fromZset(key, size);
                }
            } catch (Exception e) {
                log.warn("Read rank from Redis failed, fallback to DB: {}", e.getMessage());
            }
        }
        List<ContentBook> books = bookMapper.selectList(Wrappers.<ContentBook>lambdaQuery()
                .eq(ContentBook::getStatus, "ON_SHELF")
                .orderByDesc(ContentBook::getVisitCount)
                .last("limit " + size));
        List<RankItemVO> result = new ArrayList<>();
        int rank = 1;
        for (ContentBook book : books) {
            result.add(new RankItemVO(rank++, assembler.toCard(book), book.getVisitCount() == null ? 0 : book.getVisitCount()));
        }
        return result;
    }

    private String rankKey(String type) {
        if ("all".equals(type)) {
            return TOTAL_KEY;
        }
        int days = "7d".equals(type) ? 7 : "30d".equals(type) ? 30 : 0;
        if (days == 0) {
            return TOTAL_KEY;
        }
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            keys.add(DAILY_PREFIX + LocalDate.now().minusDays(i).format(DAY));
        }
        String dest = "rv:rank:" + type + ":" + LocalDate.now().format(DAY);
        if (Boolean.TRUE.equals(redis.hasKey(dest))) {
            return dest;
        }
        String first = keys.remove(0);
        if (keys.isEmpty()) {
            return first;
        }
        redis.opsForZSet().unionAndStore(first, keys, dest);
        redis.expire(dest, Duration.ofMinutes(5));
        return dest;
    }

    private List<RankItemVO> fromZset(String key, int size) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet().reverseRangeWithScores(key, 0, size - 1);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        List<Long> ids = tuples.stream().map(t -> Long.valueOf(t.getValue())).toList();
        Map<Long, ContentBook> bookMap = new HashMap<>();
        List<ContentBook> books = bookMapper.selectBatchIds(ids);
        books.forEach(book -> bookMap.put(book.getId(), book));
        List<RankItemVO> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            ContentBook book = bookMap.get(Long.valueOf(tuple.getValue()));
            if (book != null) {
                result.add(new RankItemVO(rank++, assembler.toCard(book),
                        tuple.getScore() == null ? 0 : tuple.getScore()));
            }
        }
        return result;
    }

    public List<Long> topIds(String type, int limit) {
        return top(type, limit).stream().map(item -> item.book().id()).toList();
    }
}
