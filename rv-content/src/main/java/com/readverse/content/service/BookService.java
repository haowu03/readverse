package com.readverse.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.common.result.PageResult;
import com.readverse.content.dto.BookSaveRequest;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.entity.ContentCategory;
import com.readverse.content.entity.ContentChapter;
import com.readverse.content.mapper.ContentBookMapper;
import com.readverse.content.mapper.ContentCategoryMapper;
import com.readverse.content.mapper.ContentChapterMapper;
import com.readverse.content.vo.BookCardVO;
import com.readverse.content.vo.BookDetailVO;
import com.readverse.content.vo.CategoryVO;
import com.readverse.content.vo.ChapterSummaryVO;
import com.readverse.content.vo.HomeVO;
import com.readverse.content.vo.RankItemVO;
import com.readverse.framework.cache.TwoLevelCacheService;
import com.readverse.framework.mq.DomainEvent;
import com.readverse.framework.mq.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookService {

    private final ContentBookMapper bookMapper;
    private final ContentChapterMapper chapterMapper;
    private final ContentCategoryMapper categoryMapper;
    private final AuthorService authorService;
    private final CategoryService categoryService;
    private final BookAssembler assembler;
    private final RankService rankService;
    private final TwoLevelCacheService cache;
    private final EventPublisher eventPublisher;

    public PageResult<BookCardVO> adminPage(long current, long size, String keyword, String status, Long categoryId) {
        Page<ContentBook> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        IPage<ContentBook> result = bookMapper.selectPage(page, Wrappers.<ContentBook>lambdaQuery()
                .like(StringUtils.hasText(keyword), ContentBook::getTitle, keyword)
                .eq(StringUtils.hasText(status), ContentBook::getStatus, status)
                .eq(categoryId != null, ContentBook::getCategoryId, categoryId)
                .orderByDesc(ContentBook::getUpdateTime));
        List<BookCardVO> records = result.getRecords().stream().map(assembler::toCard).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public PageResult<BookCardVO> portalPage(long current, long size, Long categoryId, String keyword, String sort) {
        Page<ContentBook> page = new Page<>(Math.max(1, current), Math.min(50, Math.max(1, size)));
        var query = Wrappers.<ContentBook>lambdaQuery()
                .eq(ContentBook::getStatus, "ON_SHELF")
                .eq(categoryId != null, ContentBook::getCategoryId, categoryId)
                .and(StringUtils.hasText(keyword), q -> q
                        .like(ContentBook::getTitle, keyword)
                        .or().like(ContentBook::getIntro, keyword))
                .orderByDesc(ContentBook::getUpdateTime);
        if ("new".equalsIgnoreCase(sort)) {
            query = query.orderByDesc(ContentBook::getCreateTime);
        } else if ("hot".equalsIgnoreCase(sort)) {
            query = query.orderByDesc(ContentBook::getVisitCount);
        } else if ("score".equalsIgnoreCase(sort)) {
            query = query.orderByDesc(ContentBook::getScore);
        }
        IPage<ContentBook> result = bookMapper.selectPage(page, query);
        List<BookCardVO> records = result.getRecords().stream().map(assembler::toCard).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public ContentBook getEntity(Long id) {
        ContentBook book = bookMapper.selectById(id);
        if (book == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "书籍不存在");
        }
        return book;
    }

    public BookDetailVO detail(Long id, boolean portalOnly) {
        String key = "rv:book:" + id;
        BookDetailVO cached = cache.get(key, BookDetailVO.class);
        if (cached != null) {
            return cached;
        }
        ContentBook book = bookMapper.selectById(id);
        if (book == null || (portalOnly && !"ON_SHELF".equals(book.getStatus()))) {
            throw new BizException(ErrorCode.NOT_FOUND, "书籍不存在或未上架");
        }
        BookDetailVO vo = assembler.toDetail(book);
        cache.put(key, vo, 300);
        return vo;
    }

    public BookDetailVO portalDetail(Long id) {
        BookDetailVO detail = detail(id, true);
        try {
            bookMapper.update(null, Wrappers.<ContentBook>lambdaUpdate()
                    .eq(ContentBook::getId, id)
                    .setSql("visit_count = visit_count + 1"));
        } catch (Exception ignored) {
        }
        rankService.recordClick(id);
        return detail;
    }

    public HomeVO home() {
        HomeVO cached = cache.get("rv:home", HomeVO.class);
        if (cached != null) {
            return cached;
        }
        List<CategoryVO> categories = categoryService.tree();
        List<BookCardVO> newBooks = bookMapper.selectList(Wrappers.<ContentBook>lambdaQuery()
                        .eq(ContentBook::getStatus, "ON_SHELF")
                        .orderByDesc(ContentBook::getCreateTime)
                        .last("limit 8"))
                .stream().map(assembler::toCard).toList();
        List<BookCardVO> hotBooks = bookMapper.selectList(Wrappers.<ContentBook>lambdaQuery()
                        .eq(ContentBook::getStatus, "ON_SHELF")
                        .orderByDesc(ContentBook::getVisitCount)
                        .last("limit 8"))
                .stream().map(assembler::toCard).toList();
        List<RankItemVO> rankings = rankService.top("all", 5);
        HomeVO vo = new HomeVO(categories, newBooks, hotBooks, rankings);
        cache.put("rv:home", vo, 60);
        return vo;
    }

    public List<ChapterSummaryVO> chapters(Long bookId) {
        String key = "rv:book:chapters:" + bookId;
        List<ChapterSummaryVO> cached = cache.get(key, new com.fasterxml.jackson.core.type.TypeReference<>() {
        });
        if (cached != null) {
            return cached;
        }
        ContentBook book = getEntity(bookId);
        List<ChapterSummaryVO> list = chapterMapper.selectList(Wrappers.<ContentChapter>lambdaQuery()
                        .eq(ContentChapter::getBookId, bookId)
                        .eq(ContentChapter::getStatus, "PUBLISHED")
                        .orderByAsc(ContentChapter::getChapterNo))
                .stream().map(c -> new ChapterSummaryVO(c.getId(), c.getChapterNo(), c.getTitle(), c.getIsVip(),
                        c.getWordCount(), c.getPublishTime()))
                .toList();
        cache.put(key, list, 300);
        return list;
    }

    @Transactional
    public ContentBook create(BookSaveRequest request) {
        ensureAuthorAndCategory(request);
        ContentBook book = new ContentBook();
        apply(book, request);
        book.setScore(BigDecimal.ZERO);
        book.setRatingCount(0);
        book.setVisitCount(0L);
        book.setWordCount(0);
        book.setChapterCount(0);
        book.setDeleted(0);
        if ("ON_SHELF".equals(book.getStatus())) {
            book.setPublishTime(LocalDateTime.now());
        }
        bookMapper.insert(book);
        authorService.refreshBookCount(book.getAuthorId());
        evict(book.getId());
        return book;
    }

    @Transactional
    public ContentBook update(Long id, BookSaveRequest request) {
        ContentBook book = getEntity(id);
        ensureAuthorAndCategory(request);
        Long oldAuthor = book.getAuthorId();
        apply(book, request);
        if ("ON_SHELF".equals(book.getStatus()) && book.getPublishTime() == null) {
            book.setPublishTime(LocalDateTime.now());
        }
        bookMapper.updateById(book);
        if (!book.getAuthorId().equals(oldAuthor)) {
            authorService.refreshBookCount(oldAuthor);
            authorService.refreshBookCount(book.getAuthorId());
        }
        evict(id);
        return book;
    }

    @Transactional
    public void delete(Long id) {
        ContentBook book = getEntity(id);
        chapterMapper.delete(Wrappers.<ContentChapter>lambdaQuery().eq(ContentChapter::getBookId, id));
        bookMapper.deleteById(id);
        authorService.refreshBookCount(book.getAuthorId());
        evict(id);
    }

    @Transactional
    public void publish(Long id) {
        ContentBook book = getEntity(id);
        book.setStatus("ON_SHELF");
        book.setPublishTime(LocalDateTime.now());
        bookMapper.updateById(book);
        evict(id);
    }

    @Transactional
    public void unpublish(Long id) {
        ContentBook book = getEntity(id);
        book.setStatus("OFF_SHELF");
        bookMapper.updateById(book);
        evict(id);
    }

    @Transactional
    public void audit(Long id, String status) {
        ContentBook book = getEntity(id);
        book.setStatus(status);
        if ("ON_SHELF".equals(status)) {
            book.setPublishTime(LocalDateTime.now());
        }
        bookMapper.updateById(book);
        evict(id);
        String eventType = "ON_SHELF".equals(status) ? "BOOK_AUDIT_PASSED" : "BOOK_AUDIT_REJECTED";
        eventPublisher.publish(DomainEvent.of(eventType, id,
                Map.of("bookId", id, "title", book.getTitle(), "status", status)));
    }

    public void refreshMeta(Long bookId) {
        ContentBook book = bookMapper.selectById(bookId);
        if (book == null) {
            return;
        }
        List<ContentChapter> chapters = chapterMapper.selectList(Wrappers.<ContentChapter>lambdaQuery()
                .eq(ContentChapter::getBookId, bookId)
                .eq(ContentChapter::getStatus, "PUBLISHED")
                .orderByAsc(ContentChapter::getChapterNo));
        int wordCount = chapters.stream().mapToInt(c -> c.getWordCount() == null ? 0 : c.getWordCount()).sum();
        book.setChapterCount(chapters.size());
        book.setWordCount(wordCount);
        if (!chapters.isEmpty()) {
            ContentChapter last = chapters.get(chapters.size() - 1);
            book.setLastChapterId(last.getId());
            book.setLastChapterTitle(last.getTitle());
        } else {
            book.setLastChapterId(null);
            book.setLastChapterTitle(null);
        }
        bookMapper.updateById(book);
        evict(bookId);
    }

    public void evict(Long bookId) {
        cache.evict("rv:book:" + bookId);
        cache.evict("rv:book:chapters:" + bookId);
        cache.evict("rv:home");
    }

    private void ensureAuthorAndCategory(BookSaveRequest request) {
        if (authorService.get(request.authorId()) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "作者不存在");
        }
        ContentCategory category = categoryMapper.selectById(request.categoryId());
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        }
    }

    private void apply(ContentBook book, BookSaveRequest request) {
        book.setTitle(request.title());
        book.setSubtitle(request.subtitle());
        book.setAuthorId(request.authorId());
        book.setCategoryId(request.categoryId());
        book.setCoverUrl(request.coverUrl());
        book.setIntro(request.intro());
        book.setIsVip(Boolean.TRUE.equals(request.isVip()));
        book.setStatus(StringUtils.hasText(request.status()) ? request.status() : "DRAFT");
    }
}
