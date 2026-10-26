package com.readverse.system.vo;

import java.time.LocalDateTime;

public record NotifyVO(
        Long id,
        String type,
        String title,
        String content,
        Long bizId,
        Boolean isRead,
        LocalDateTime createTime) {
}
