package com.readverse.content.controller.admin;

import com.readverse.common.result.PageResult;
import com.readverse.common.result.R;
import com.readverse.content.dto.AuditRequest;
import com.readverse.content.dto.BookSaveRequest;
import com.readverse.content.entity.ContentBook;
import com.readverse.content.service.BookService;
import com.readverse.content.vo.BookCardVO;
import com.readverse.content.vo.BookDetailVO;
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

@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookService bookService;

    @GetMapping
    @PreAuthorize("hasAuthority('content:book:list')")
    public R<PageResult<BookCardVO>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long categoryId) {
        return R.ok(bookService.adminPage(current, size, keyword, status, categoryId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('content:book:list')")
    public R<BookDetailVO> detail(@PathVariable Long id) {
        return R.ok(bookService.detail(id, false));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('content:book:add')")
    public R<ContentBook> create(@Valid @RequestBody BookSaveRequest request) {
        return R.ok(bookService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('content:book:edit')")
    public R<ContentBook> update(@PathVariable Long id, @Valid @RequestBody BookSaveRequest request) {
        return R.ok(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('content:book:delete')")
    public R<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('content:book:publish')")
    public R<Void> publish(@PathVariable Long id) {
        bookService.publish(id);
        return R.ok();
    }

    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasAuthority('content:book:publish')")
    public R<Void> unpublish(@PathVariable Long id) {
        bookService.unpublish(id);
        return R.ok();
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('content:book:audit')")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        bookService.audit(id, request.status());
        return R.ok();
    }
}
