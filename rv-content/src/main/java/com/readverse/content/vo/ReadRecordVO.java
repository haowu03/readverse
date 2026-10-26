package com.readverse.content.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReadRecordVO(
        Long bookId,
        Long chapterId,
        Integer chapterNo,
        BigDecimal progress,
        Integer readSeconds,
        LocalDateTime updateTime) {
}
