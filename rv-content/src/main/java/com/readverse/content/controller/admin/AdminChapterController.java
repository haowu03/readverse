package com.readverse.content.controller.admin;

import com.readverse.common.result.R;
import com.readverse.content.dto.ChapterSaveRequest;
import com.readverse.content.entity.ContentChapter;
import com.readverse.content.service.ChapterService;
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
@RequestMapping("/api/admin/chapters")
@RequiredArgsConstructor
public class AdminChapterController {

    private final ChapterService chapterService;

    @GetMapping
    @PreAuthorize("hasAuthority('content:chapter:list')")
    public R<List<ContentChapter>> list(@RequestParam(required = false) Long bookId) {
        return R.ok(chapterService.adminList(bookId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('content:chapter:list')")
    public R<ContentChapter> detail(@PathVariable Long id) {
        return R.ok(chapterService.getEntity(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('content:chapter:add')")
    public R<ContentChapter> create(@Valid @RequestBody ChapterSaveRequest request) {
        return R.ok(chapterService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('content:chapter:edit')")
    public R<ContentChapter> update(@PathVariable Long id, @Valid @RequestBody ChapterSaveRequest request) {
        return R.ok(chapterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('content:chapter:delete')")
    public R<Void> delete(@PathVariable Long id) {
        chapterService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('content:chapter:publish')")
    public R<Void> publish(@PathVariable Long id) {
        chapterService.publish(id);
        return R.ok();
    }
}
