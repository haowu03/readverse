package com.readverse.content.vo;

import java.util.List;

public record SignInMonthVO(
        String month,
        List<Integer> signedDays,
        int continuousDays,
        boolean signedToday) {
}
