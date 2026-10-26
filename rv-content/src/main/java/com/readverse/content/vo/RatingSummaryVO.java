package com.readverse.content.vo;

public record RatingSummaryVO(
        double average,
        int count,
        Integer myScore) {
}
