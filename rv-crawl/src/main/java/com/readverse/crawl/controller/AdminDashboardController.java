package com.readverse.crawl.controller;

import com.readverse.common.result.R;
import com.readverse.content.service.StatsService;
import com.readverse.content.vo.DashboardSummaryVO;
import com.readverse.content.vo.TrendPointVO;
import com.readverse.crawl.service.CrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final StatsService statsService;
    private final CrawlService crawlService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('stats:view')")
    public R<DashboardSummaryVO> summary() {
        DashboardSummaryVO base = statsService.summary();
        return R.ok(new DashboardSummaryVO(base.books(), base.chapters(), base.users(), base.todayReads(),
                crawlService.pendingCount(), crawlService.successRate()));
    }

    @GetMapping("/book-trend")
    @PreAuthorize("hasAuthority('stats:view')")
    public R<List<TrendPointVO>> bookTrend(@RequestParam(defaultValue = "7") int days) {
        return R.ok(statsService.bookTrend(days));
    }

    @GetMapping("/read-activity")
    @PreAuthorize("hasAuthority('stats:view')")
    public R<List<TrendPointVO>> readActivity(@RequestParam(defaultValue = "7") int days) {
        return R.ok(statsService.readActivity(days));
    }
}
