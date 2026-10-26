package com.readverse.content.controller.admin;

import com.readverse.common.result.PageResult;
import com.readverse.common.result.R;
import com.readverse.content.dto.AuditRequest;
import com.readverse.content.dto.AuthorSaveRequest;
import com.readverse.content.entity.ContentAuthor;
import com.readverse.content.service.AuthorService;
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
@RequestMapping("/api/admin/authors")
@RequiredArgsConstructor
public class AdminAuthorController {

    private final AuthorService authorService;

    @GetMapping
    @PreAuthorize("hasAuthority('content:author:list')")
    public R<PageResult<ContentAuthor>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String status) {
        return R.ok(authorService.page(current, size, keyword, status));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('content:author:list')")
    public R<List<ContentAuthor>> all() {
        return R.ok(authorService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('content:author:add')")
    public R<ContentAuthor> create(@Valid @RequestBody AuthorSaveRequest request) {
        return R.ok(authorService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('content:author:edit')")
    public R<ContentAuthor> update(@PathVariable Long id, @Valid @RequestBody AuthorSaveRequest request) {
        return R.ok(authorService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('content:author:delete')")
    public R<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('content:author:audit')")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        authorService.audit(id, request);
        return R.ok();
    }
}
