package com.readverse.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChapterSaveRequest(
        @NotNull(message = "书籍不能为空") Long bookId,
        @NotNull(message = "章节号不能为空") Integer chapterNo,
        @NotBlank(message = "章节标题不能为空") String title,
        @NotBlank(message = "章节内容不能为空") String content,
        Boolean isVip,
        String status) {
}
