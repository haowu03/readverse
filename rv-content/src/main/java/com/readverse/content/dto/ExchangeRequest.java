package com.readverse.content.dto;

import jakarta.validation.constraints.NotNull;

public record ExchangeRequest(
        @NotNull(message = "商品不能为空") Long goodsId,
        Long targetId,
        String idempotencyKey) {
}
