package com.readverse.system.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UserRoleRequest(@NotNull(message = "roleIds 不能为空") List<Long> roleIds) {
}
