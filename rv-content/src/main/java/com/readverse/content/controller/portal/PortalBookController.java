package com.readverse.content.controller.portal;

import com.readverse.common.result.PageResult;
import com.readverse.common.result.R;
import com.readverse.content.service.BookService;
import com.readverse.content.service.CategoryService;
import com.readverse.content.service.CommentService;
import com.readverse.content.service.RankService;
import com.readverse.content.service.RatingService;
import com.readverse.content.vo.BookCardVO;
import com.readverse.content.vo.BookDetailVO;
import com.readverse.content.vo.CategoryVO;
import com.readverse.content.vo.ChapterSummaryVO;
import com.readverse.content.vo.CommentVO;
import com.readverse.content.vo.HomeVO;
import com.readverse.content.vo.RankItemVO;
import com.readverse.content.vo.RatingSummaryVO;
import com.readverse.framework.ratelimit.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
public class PortalBookController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final RankService rankService;
    private final RatingService ratingService;
    private final CommentService commentService;

    @GetMapping("/home")
    public R<HomeVO> home() {
        return R.ok(bookService.home());
    }

    @GetMapping("/categories/tree")
    public R<List<CategoryVO>> categories() {
        return R.ok(categoryService.tree());
    }

    @GetMapping("/books")
    public R<PageResult<BookCardVO>> books(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "12") long size,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "update") String sort) {
        return R.ok(bookService.portalPage(current, size, categoryId, keyword, sort));
    }

    @GetMapping("/books/{id}")
    @RateLimit(key = "'book:' + #id", permits = 200, windowSeconds = 1, dimension = "IP")
    public R<BookDetailVO> detail(@PathVariable Long id) {
        return R.ok(bookService.portalDetail(id));
    }

    @GetMapping("/books/{id}/chapters")
    public R<List<ChapterSummaryVO>> chapters(@PathVariable Long id) {
        return R.ok(bookService.chapters(id));
    }

    @GetMapping("/search")
    public R<PageResult<BookCardVO>> search(@RequestParam String keyword,
                                            @RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "12") long size) {
        return R.ok(bookService.portalPage(current, size, null, keyword, "update"));
    }

    @GetMapping("/rankings")
    public R<List<RankItemVO>> rankings(@RequestParam(defaultValue = "all") String type,
                                        @RequestParam(defaultValue = "10") int limit) {
        return R.ok(rankService.top(type, limit));
    }

    @GetMapping("/ratings/{bookId}")
    public R<RatingSummaryVO> ratingSummary(@PathVariable Long bookId) {
        return R.ok(ratingService.summary(bookId));
    }

    @GetMapping("/comments")
    public R<PageResult<CommentVO>> comments(@RequestParam Long bookId,
                                             @RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size) {
        return R.ok(commentService.page(bookId, current, size));
    }
}
