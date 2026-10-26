package com.readverse.system.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RolePermissionsRequest(@NotNull(message = "permissionIds 不能为空") List<Long> permissionIds) {
}
