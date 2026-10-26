package com.readverse.content.dto;

import jakarta.validation.constraints.NotNull;

public record BookshelfRequest(@NotNull(message = "书籍不能为空") Long bookId) {
}
