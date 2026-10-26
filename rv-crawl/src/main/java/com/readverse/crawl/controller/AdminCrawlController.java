package com.readverse.crawl.controller;

import com.readverse.common.result.PageResult;
import com.readverse.common.result.R;
import com.readverse.crawl.dto.CrawlSourceRequest;
import com.readverse.crawl.entity.CrawlSource;
import com.readverse.crawl.entity.CrawlTask;
import com.readverse.crawl.entity.SysJobLog;
import com.readverse.crawl.service.CrawlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/crawl")
@RequiredArgsConstructor
public class AdminCrawlController {

    private final CrawlService crawlService;

    @GetMapping("/sources")
    @PreAuthorize("hasAuthority('crawl:source:list')")
    public R<List<CrawlSource>> sources() {
        return R.ok(crawlService.sources());
    }

    @PostMapping("/sources")
    @PreAuthorize("hasAuthority('crawl:source:add')")
    public R<CrawlSource> createSource(@Valid @RequestBody CrawlSourceRequest request) {
        return R.ok(crawlService.createSource(request));
    }

    @PutMapping("/sources/{id}")
    @PreAuthorize("hasAuthority('crawl:source:edit')")
    public R<CrawlSource> updateSource(@PathVariable Long id, @Valid @RequestBody CrawlSourceRequest request) {
        return R.ok(crawlService.updateSource(id, request));
    }

    @DeleteMapping("/sources/{id}")
    @PreAuthorize("hasAuthority('crawl:source:delete')")
    public R<Void> deleteSource(@PathVariable Long id) {
        crawlService.deleteSource(id);
        return R.ok();
    }

    @PostMapping("/sources/{id}/test")
    @PreAuthorize("hasAuthority('crawl:source:test')")
    public R<Integer> testSource(@PathVariable Long id) {
        return R.ok(crawlService.testSource(id));
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('crawl:task:list')")
    public R<PageResult<CrawlTask>> tasks(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String state) {
        return R.ok(crawlService.tasks(current, size, state));
    }

    @PostMapping("/tasks/trigger")
    @PreAuthorize("hasAuthority('crawl:task:list')")
    public R<CrawlTask> trigger(@RequestParam Long sourceId,
                                @RequestParam(defaultValue = "FULL") String type) {
        return R.ok(crawlService.trigger(sourceId, type));
    }

    @PostMapping("/tasks/{id}/retry")
    @PreAuthorize("hasAuthority('crawl:task:retry')")
    public R<CrawlTask> retry(@PathVariable Long id) {
        return R.ok(crawlService.retry(id));
    }

    @GetMapping("/job-logs")
    @PreAuthorize("hasAuthority('crawl:task:list')")
    public R<PageResult<SysJobLog>> jobLogs(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        return R.ok(crawlService.jobLogs(current, size));
    }
}
