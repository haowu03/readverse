package com.readverse.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "旧密码不能为空") String oldPassword,
        @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 32, message = "密码长度需在 6-32 之间") String newPassword) {
}
