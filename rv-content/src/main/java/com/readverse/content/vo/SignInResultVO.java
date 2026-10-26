package com.readverse.content.vo;

public record SignInResultVO(
        boolean signed,
        int reward,
        int continuousDays,
        int balance) {
}
