package com.readverse.content.vo;

public record ExchangeResultVO(
        String orderNo,
        String couponCode,
        int pointsCost,
        int balance,
        String status) {
}
