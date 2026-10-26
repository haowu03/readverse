package com.readverse.crawl.dto;

import jakarta.validation.constraints.NotBlank;

public record CrawlSourceRequest(
        @NotBlank(message = "书源名称不能为空") String name,
        String sourceType,
        String baseUrl,
        String configJson,
        Integer status) {
}
