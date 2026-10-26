package com.readverse.content.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookDetailVO(
        Long id,
        String title,
        String subtitle,
        Long authorId,
        String authorName,
        Long categoryId,
        String categoryName,
        String coverUrl,
        String intro,
        String status,
        BigDecimal score,
        Integer ratingCount,
        Long visitCount,
        Integer wordCount,
        Integer chapterCount,
        Boolean isVip,
        LocalDateTime publishTime,
        String lastChapterTitle,
        LocalDateTime updateTime) {
}
