package com.readverse.system.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull(message = "status 不能为空") Integer status) {
}
