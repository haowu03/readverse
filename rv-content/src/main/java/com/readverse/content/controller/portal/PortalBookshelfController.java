package com.readverse.content.controller.portal;

import com.readverse.common.result.R;
import com.readverse.content.dto.BookshelfRequest;
import com.readverse.content.service.ReadingService;
import com.readverse.content.vo.BookshelfItemVO;
import com.readverse.framework.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portal/bookshelf")
@RequiredArgsConstructor
public class PortalBookshelfController {

    private final ReadingService readingService;

    @GetMapping
    public R<List<BookshelfItemVO>> list() {
        return R.ok(readingService.bookshelf(SecurityUtils.requireUserId()));
    }

    @PostMapping
    public R<Void> add(@Valid @RequestBody BookshelfRequest request) {
        readingService.add(SecurityUtils.requireUserId(), request.bookId());
        return R.ok();
    }

    @DeleteMapping("/{bookId}")
    public R<Void> remove(@PathVariable Long bookId) {
        readingService.remove(SecurityUtils.requireUserId(), bookId);
        return R.ok();
    }
}
