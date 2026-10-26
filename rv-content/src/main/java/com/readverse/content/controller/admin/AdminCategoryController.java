package com.readverse.content.controller.admin;

import com.readverse.common.result.R;
import com.readverse.content.dto.CategorySaveRequest;
import com.readverse.content.entity.ContentCategory;
import com.readverse.content.service.CategoryService;
import com.readverse.content.vo.CategoryVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAuthority('content:category:list')")
    public R<List<CategoryVO>> tree() {
        return R.ok(categoryService.tree());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('content:category:list')")
    public R<List<ContentCategory>> all() {
        return R.ok(categoryService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('content:category:add')")
    public R<ContentCategory> create(@Valid @RequestBody CategorySaveRequest request) {
        return R.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('content:category:edit')")
    public R<ContentCategory> update(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest request) {
        return R.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('content:category:delete')")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }
}
