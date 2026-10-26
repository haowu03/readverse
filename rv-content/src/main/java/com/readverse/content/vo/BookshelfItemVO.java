package com.readverse.content.vo;

import java.time.LocalDateTime;

public record BookshelfItemVO(
        BookCardVO book,
        Long lastChapterId,
        Integer lastChapterNo,
        LocalDateTime updateTime) {
}
