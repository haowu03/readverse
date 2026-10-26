package com.readverse.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookSaveRequest(
        @NotBlank(message = "书名不能为空") String title,
        String subtitle,
        @NotNull(message = "作者不能为空") Long authorId,
        @NotNull(message = "分类不能为空") Long categoryId,
        String coverUrl,
        String intro,
        Boolean isVip,
        String status) {
}
