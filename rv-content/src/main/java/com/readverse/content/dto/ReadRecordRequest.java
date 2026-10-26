package com.readverse.content.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReadRecordRequest(
        @NotNull(message = "书籍不能为空") Long bookId,
        @NotNull(message = "章节不能为空") Long chapterId,
        Integer chapterNo,
        BigDecimal progress,
        Integer readSeconds) {
}
