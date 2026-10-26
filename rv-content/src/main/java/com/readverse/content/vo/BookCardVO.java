package com.readverse.content.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookCardVO(
        Long id,
        String title,
        String subtitle,
        String authorName,
        String categoryName,
        String coverUrl,
        String intro,
        String status,
        BigDecimal score,
        Integer ratingCount,
        Long visitCount,
        Integer chapterCount,
        Integer wordCount,
        Boolean isVip,
        String lastChapterTitle,
        LocalDateTime updateTime) {
}
