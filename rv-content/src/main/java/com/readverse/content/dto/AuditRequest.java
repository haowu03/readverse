package com.readverse.content.dto;

import jakarta.validation.constraints.NotBlank;

public record AuditRequest(
        @NotBlank(message = "审核状态不能为空") String status,
        String remark) {
}
