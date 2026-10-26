package com.readverse.content.vo;

public record DashboardSummaryVO(
        long books,
        long chapters,
        long users,
        long todayReads,
        long pendingTasks,
        double crawlSuccessRate) {
}
