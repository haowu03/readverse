package com.readverse.content.vo;

import java.time.LocalDateTime;

public record ChapterSummaryVO(
        Long id,
        Integer chapterNo,
        String title,
        Boolean isVip,
        Integer wordCount,
        LocalDateTime publishTime) {
}
