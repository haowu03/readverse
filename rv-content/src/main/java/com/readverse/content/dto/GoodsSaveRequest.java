package com.readverse.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoodsSaveRequest(
        @NotBlank(message = "商品名称不能为空") String name,
        @NotBlank(message = "商品类型不能为空") String type,
        @NotNull(message = "积分价格不能为空") Integer pointsPrice,
        @NotNull(message = "库存不能为空") Integer stock,
        Integer status,
        String configJson) {
}
