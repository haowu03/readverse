package com.readverse.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        Long bookId,
        Long chapterId,
        Long parentId,
        @NotBlank(message = "评论内容不能为空") @Size(max = 1000, message = "评论最多 1000 字") String content) {
}
