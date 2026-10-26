package com.readverse.content.vo;

public record ChapterDetailVO(
        Long id,
        Long bookId,
        String bookTitle,
        Integer chapterNo,
        String title,
        String content,
        Boolean isVip,
        Integer wordCount,
        Long prevChapterId,
        Long nextChapterId,
        Boolean cached) {
}
