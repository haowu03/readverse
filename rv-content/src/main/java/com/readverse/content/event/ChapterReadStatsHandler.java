package com.readverse.content.event;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.content.entity.StatDailyBook;
import com.readverse.content.entity.StatDailyPlatform;
import com.readverse.content.mapper.StatDailyBookMapper;
import com.readverse.content.mapper.StatDailyPlatformMapper;
import com.readverse.framework.mq.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterReadStatsHandler {

    private final StatDailyBookMapper statDailyBookMapper;
    private final StatDailyPlatformMapper statDailyPlatformMapper;

    public void handle(DomainEvent event) {
        if (event == null || !"CHAPTER_READ".equals(event.type()) || event.bizId() == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        StatDailyBook daily = statDailyBookMapper.selectOne(Wrappers.<StatDailyBook>lambdaQuery()
                .eq(StatDailyBook::getStatDate, today)
                .eq(StatDailyBook::getBookId, event.bizId()));
        if (daily == null) {
            daily = new StatDailyBook();
            daily.setStatDate(today);
            daily.setBookId(event.bizId());
            daily.setReadCount(1);
            daily.setUniqueReaders(1);
            daily.setFavoriteCount(0);
            daily.setScoreAvg(BigDecimal.ZERO);
            statDailyBookMapper.insert(daily);
        } else {
            daily.setReadCount(daily.getReadCount() + 1);
            statDailyBookMapper.updateById(daily);
        }

        StatDailyPlatform platform = statDailyPlatformMapper.selectOne(Wrappers.<StatDailyPlatform>lambdaQuery()
                .eq(StatDailyPlatform::getStatDate, today));
        if (platform == null) {
            platform = new StatDailyPlatform();
            platform.setStatDate(today);
            platform.setNewUsers(0);
            platform.setActiveUsers(1);
            platform.setReadCount(1);
            platform.setNewBooks(0);
            platform.setCrawlSuccessRate(BigDecimal.ZERO);
            statDailyPlatformMapper.insert(platform);
        } else {
            platform.setReadCount(platform.getReadCount() + 1);
            statDailyPlatformMapper.updateById(platform);
        }
        log.debug("Stat event consumed for book {}", event.bizId());
    }
}
