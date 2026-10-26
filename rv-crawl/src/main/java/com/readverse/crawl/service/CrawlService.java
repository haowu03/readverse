package com.readverse.crawl.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.common.result.PageResult;
import com.readverse.content.entity.ContentAuthor;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.entity.ContentCategory;
import com.readverse.content.entity.ContentChapter;
import com.readverse.content.mapper.ContentAuthorMapper;
import com.readverse.content.mapper.ContentBookMapper;
import com.readverse.content.mapper.ContentCategoryMapper;
import com.readverse.content.mapper.ContentChapterMapper;
import com.readverse.content.service.BookService;
import com.readverse.crawl.adapter.MockSourceAdapter;
import com.readverse.crawl.dto.CrawlSourceRequest;
import com.readverse.crawl.entity.CrawlBookMap;
import com.readverse.crawl.entity.CrawlSource;
import com.readverse.crawl.entity.CrawlTask;
import com.readverse.crawl.entity.SysJobLog;
import com.readverse.crawl.mapper.CrawlBookMapMapper;
import com.readverse.crawl.mapper.CrawlSourceMapper;
import com.readverse.crawl.mapper.CrawlTaskMapper;
import com.readverse.crawl.mapper.SysJobLogMapper;
import com.readverse.framework.cache.BloomFilterService;
import com.readverse.framework.cache.TwoLevelCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlService {

    private final CrawlSourceMapper sourceMapper;
    private final CrawlTaskMapper taskMapper;
    private final CrawlBookMapMapper mapMapper;
    private final SysJobLogMapper jobLogMapper;
    private final ContentBookMapper bookMapper;
    private final ContentChapterMapper chapterMapper;
    private final ContentAuthorMapper authorMapper;
    private final ContentCategoryMapper categoryMapper;
    private final BookService bookService;
    private final MockSourceAdapter adapter;
    private final BloomFilterService bloom;
    private final TwoLevelCacheService cache;

    public List<CrawlSource> sources() {
        return sourceMapper.selectList(Wrappers.<CrawlSource>lambdaQuery().orderByDesc(CrawlSource::getCreateTime));
    }

    @Transactional
    public CrawlSource createSource(CrawlSourceRequest request) {
        Long exists = sourceMapper.selectCount(Wrappers.<CrawlSource>lambdaQuery().eq(CrawlSource::getName, request.name()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "书源名称已存在");
        }
        CrawlSource source = new CrawlSource();
        source.setName(request.name());
        source.setSourceType(request.sourceType() == null ? "MOCK" : request.sourceType());
        source.setBaseUrl(request.baseUrl());
        source.setConfigJson(request.configJson());
        source.setStatus(request.status() == null ? 1 : request.status());
        sourceMapper.insert(source);
        return source;
    }

    @Transactional
    public CrawlSource updateSource(Long id, CrawlSourceRequest request) {
        CrawlSource source = sourceMapper.selectById(id);
        if (source == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "书源不存在");
        }
        source.setName(request.name());
        if (request.sourceType() != null) {
            source.setSourceType(request.sourceType());
        }
        source.setBaseUrl(request.baseUrl());
        source.setConfigJson(request.configJson());
        if (request.status() != null) {
            source.setStatus(request.status());
        }
        sourceMapper.updateById(source);
        return source;
    }

    @Transactional
    public void deleteSource(Long id) {
        sourceMapper.deleteById(id);
    }

    public int testSource(Long id) {
        getSource(id);
        return adapter.fetchBooks().size();
    }

    public PageResult<CrawlTask> tasks(long current, long size, String state) {
        Page<CrawlTask> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        IPage<CrawlTask> result = taskMapper.selectPage(page, Wrappers.<CrawlTask>lambdaQuery()
                .eq(state != null && !state.isBlank(), CrawlTask::getState, state)
                .orderByDesc(CrawlTask::getCreateTime));
        return PageResult.of(result);
    }

    public PageResult<SysJobLog> jobLogs(long current, long size) {
        Page<SysJobLog> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        IPage<SysJobLog> result = jobLogMapper.selectPage(page, Wrappers.<SysJobLog>lambdaQuery()
                .orderByDesc(SysJobLog::getStartTime));
        return PageResult.of(result);
    }

    @Transactional
    public CrawlTask trigger(Long sourceId, String type) {
        CrawlSource source = getSource(sourceId);
        if (source.getStatus() == null || source.getStatus() != 1) {
            throw new BizException("书源已停用");
        }
        return run(source, type == null ? "FULL" : type, null);
    }

    @Transactional
    public CrawlTask retry(Long taskId) {
        CrawlTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if ("RUNNING".equals(task.getState())) {
            throw new BizException("任务正在执行中");
        }
        task.setRetryTimes((task.getRetryTimes() == null ? 0 : task.getRetryTimes()) + 1);
        CrawlSource source = getSource(task.getSourceId());
        return run(source, task.getTaskType(), task.getBookId());
    }

    private CrawlSource getSource(Long id) {
        CrawlSource source = sourceMapper.selectById(id);
        if (source == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "书源不存在");
        }
        return source;
    }

    private CrawlTask run(CrawlSource source, String type, Long bookId) {
        CrawlTask task = new CrawlTask();
        task.setSourceId(source.getId());
        task.setTaskType(type);
        task.setBookId(bookId);
        task.setState("RUNNING");
        task.setRetryTimes(0);
        task.setMaxRetry(3);
        task.setStartTime(LocalDateTime.now());
        taskMapper.insert(task);

        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobName(type + "SyncJob");
        jobLog.setState("RUNNING");
        jobLog.setStartTime(LocalDateTime.now());
        jobLog.setSuccessCount(0);
        jobLog.setFailCount(0);
        jobLogMapper.insert(jobLog);

        int success = 0;
        int fail = 0;
        try {
            if ("CHAPTER".equals(type)) {
                success = syncChaptersForBook(source, bookId);
            } else {
                for (MockSourceAdapter.MockBook mockBook : adapter.fetchBooks()) {
                    try {
                        upsertBook(source, mockBook);
                        success++;
                    } catch (Exception e) {
                        fail++;
                        log.warn("Sync book {} failed: {}", mockBook.externalId(), e.getMessage());
                    }
                }
                source.setLastSyncTime(LocalDateTime.now());
                sourceMapper.updateById(source);
            }
            task.setState(fail == 0 ? "SUCCESS" : "FAILED");
            task.setMessage("success=" + success + ", fail=" + fail);
            jobLog.setState(fail == 0 ? "SUCCESS" : "FAILED");
        } catch (Exception e) {
            task.setState("FAILED");
            task.setMessage(e.getMessage());
            jobLog.setState("FAILED");
            jobLog.setMessage(e.getMessage());
            log.error("Crawl task failed", e);
        } finally {
            LocalDateTime end = LocalDateTime.now();
            task.setEndTime(end);
            taskMapper.updateById(task);
            jobLog.setEndTime(end);
            jobLog.setCostMs(java.time.Duration.between(jobLog.getStartTime(), end).toMillis());
            jobLog.setSuccessCount(success);
            jobLog.setFailCount(fail);
            logMapperUpdate(jobLog);
        }
        return task;
    }

    private void logMapperUpdate(SysJobLog log) {
        jobLogMapper.updateById(log);
    }

    private int syncChaptersForBook(CrawlSource source, Long bookId) {
        if (bookId == null) {
            throw new BizException("章节同步必须指定 bookId");
        }
        CrawlBookMap map = mapMapper.selectOne(Wrappers.<CrawlBookMap>lambdaQuery()
                .eq(CrawlBookMap::getSourceId, source.getId())
                .eq(CrawlBookMap::getBookId, bookId));
        if (map == null) {
            throw new BizException("该书籍不在当前书源映射中");
        }
        MockSourceAdapter.MockBook mockBook = adapter.fetchBooks().stream()
                .filter(b -> b.externalId().equals(map.getExternalBookId()))
                .findFirst().orElseThrow(() -> new BizException("示例源中找不到对应书籍"));
        int count = 0;
        for (MockSourceAdapter.MockChapter chapter : mockBook.chapters()) {
            upsertChapter(bookId, chapter);
            count++;
        }
        bookService.refreshMeta(bookId);
        return count;
    }

    private void upsertBook(CrawlSource source, MockSourceAdapter.MockBook mockBook) {
        ContentCategory category = categoryMapper.selectOne(Wrappers.<ContentCategory>lambdaQuery()
                .eq(ContentCategory::getCode, mockBook.categoryCode()));
        if (category == null) {
            category = new ContentCategory();
            category.setParentId(0L);
            category.setName(mockBook.categoryCode());
            category.setCode(mockBook.categoryCode());
            category.setSort(0);
            category.setStatus(1);
            categoryMapper.insert(category);
        }
        ContentAuthor author = authorMapper.selectOne(Wrappers.<ContentAuthor>lambdaQuery()
                .eq(ContentAuthor::getName, mockBook.author()));
        if (author == null) {
            author = new ContentAuthor();
            author.setName(mockBook.author());
            author.setIntro("ReadVerse 示例作者");
            author.setStatus("APPROVED");
            author.setBookCount(0);
            author.setDeleted(0);
            authorMapper.insert(author);
        }

        CrawlBookMap map = mapMapper.selectOne(Wrappers.<CrawlBookMap>lambdaQuery()
                .eq(CrawlBookMap::getSourceId, source.getId())
                .eq(CrawlBookMap::getExternalBookId, mockBook.externalId()));
        ContentBook book = map == null ? null : bookMapper.selectById(map.getBookId());
        if (book == null) {
            book = bookMapper.selectOne(Wrappers.<ContentBook>lambdaQuery()
                    .eq(ContentBook::getTitle, mockBook.title())
                    .eq(ContentBook::getAuthorId, author.getId())
                    .last("limit 1"));
        }
        boolean isNew = book == null;
        if (book == null) {
            book = new ContentBook();
            book.setTitle(mockBook.title());
            book.setSubtitle("ReadVerse 示例书源");
            book.setAuthorId(author.getId());
            book.setCategoryId(category.getId());
            book.setCoverUrl("https://picsum.photos/seed/readverse-" + mockBook.externalId() + "/300/420");
            book.setIntro(mockBook.intro());
            book.setStatus("ON_SHELF");
            book.setScore(BigDecimal.valueOf(4.2));
            book.setRatingCount(0);
            book.setVisitCount(0L);
            book.setWordCount(0);
            book.setChapterCount(0);
            book.setIsVip(false);
            book.setPublishTime(LocalDateTime.now());
            book.setDeleted(0);
            bookMapper.insert(book);
            book.setCoverUrl("/api/portal/books/" + book.getId() + "/cover");
            bookMapper.updateById(book);
        } else {
            book.setCategoryId(category.getId());
            book.setIntro(mockBook.intro());
            book.setStatus("ON_SHELF");
            book.setCoverUrl("/api/portal/books/" + book.getId() + "/cover");
            bookMapper.updateById(book);
        }

        if (map == null) {
            map = new CrawlBookMap();
            map.setSourceId(source.getId());
            map.setExternalBookId(mockBook.externalId());
            map.setBookId(book.getId());
            map.setLastSyncTime(LocalDateTime.now());
            mapMapper.insert(map);
        } else {
            map.setLastSyncTime(LocalDateTime.now());
            mapMapper.updateById(map);
        }

        for (MockSourceAdapter.MockChapter chapter : mockBook.chapters()) {
            upsertChapter(book.getId(), chapter);
        }
        bookService.refreshMeta(book.getId());
        if (isNew) {
            log.info("Crawl created book {} ({})", book.getId(), book.getTitle());
        }
    }

    private void upsertChapter(Long bookId, MockSourceAdapter.MockChapter mockChapter) {
        ContentChapter chapter = chapterMapper.selectOne(Wrappers.<ContentChapter>lambdaQuery()
                .eq(ContentChapter::getBookId, bookId)
                .eq(ContentChapter::getChapterNo, mockChapter.chapterNo()));
        if (chapter == null) {
            chapter = new ContentChapter();
            chapter.setBookId(bookId);
            chapter.setChapterNo(mockChapter.chapterNo());
            chapter.setTitle(mockChapter.title());
            chapter.setContent(mockChapter.content());
            chapter.setWordCount(mockChapter.content().replaceAll("\\s", "").length());
            chapter.setIsVip(false);
            chapter.setStatus("PUBLISHED");
            chapter.setPublishTime(LocalDateTime.now());
            chapter.setDeleted(0);
            chapterMapper.insert(chapter);
        } else {
            chapter.setTitle(mockChapter.title());
            chapter.setContent(mockChapter.content());
            chapter.setWordCount(mockChapter.content().replaceAll("\\s", "").length());
            chapter.setStatus("PUBLISHED");
            chapterMapper.updateById(chapter);
            cache.evict("rv:chapter:" + chapter.getId());
        }
        bloom.add(chapter.getId());
    }

    public long pendingCount() {
        Long count = taskMapper.selectCount(Wrappers.<CrawlTask>lambdaQuery()
                .in(CrawlTask::getState, "PENDING", "RUNNING", "RETRYING"));
        return count == null ? 0 : count;
    }

    public double successRate() {
        Long total = taskMapper.selectCount(null);
        if (total == null || total == 0) {
            return 0;
        }
        Long success = taskMapper.selectCount(Wrappers.<CrawlTask>lambdaQuery().eq(CrawlTask::getState, "SUCCESS"));
        return success == null ? 0 : (success * 100.0 / total);
    }
}
