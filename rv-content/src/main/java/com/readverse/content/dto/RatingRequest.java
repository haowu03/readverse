package com.readverse.content.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RatingRequest(
        @NotNull(message = "评分不能为空") @Min(value = 1, message = "评分最低 1 星") @Max(value = 5, message = "评分最高 5 星") Integer score) {
}
