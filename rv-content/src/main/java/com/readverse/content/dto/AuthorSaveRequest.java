package com.readverse.content.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorSaveRequest(
        @NotBlank(message = "作者名不能为空") String name,
        String avatar,
        String intro) {
}
