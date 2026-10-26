package com.readverse.content.controller.portal;

import com.readverse.common.result.R;
import com.readverse.content.dto.CommentRequest;
import com.readverse.content.service.CommentService;
import com.readverse.content.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/comments")
@RequiredArgsConstructor
public class PortalCommentController {

    private final CommentService commentService;

    @PostMapping
    public R<CommentVO> create(@Valid @RequestBody CommentRequest request) {
        return R.ok(commentService.create(request));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/like")
    public R<Void> like(@PathVariable Long id) {
        commentService.like(id);
        return R.ok();
    }
}
