package com.readverse.crawl.job;

import com.readverse.crawl.entity.CrawlSource;
import com.readverse.crawl.service.CrawlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rv.scheduler.enabled", havingValue = "true")
public class CrawlScheduler {

    private final CrawlService crawlService;

    /**
     * Local fallback for XXL-Job fullSyncJob. Disabled by default; enable with rv.scheduler.enabled=true.
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void dailyFullSync() {
        for (CrawlSource source : crawlService.sources()) {
            if (source.getStatus() != null && source.getStatus() == 1) {
                try {
                    crawlService.trigger(source.getId(), "FULL");
                    log.info("Scheduled full sync finished for source {}", source.getName());
                } catch (Exception e) {
                    log.error("Scheduled full sync failed for source {}", source.getName(), e);
                }
            }
        }
    }
}
