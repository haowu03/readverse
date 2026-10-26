package com.readverse.content.vo;

import java.time.LocalDateTime;

public record CommentVO(
        Long id,
        Long userId,
        String nickname,
        String avatar,
        String content,
        Integer likeCount,
        LocalDateTime createTime) {
}
