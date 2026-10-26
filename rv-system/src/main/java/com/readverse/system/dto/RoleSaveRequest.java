package com.readverse.system.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record RoleSaveRequest(
        @NotBlank(message = "角色编码不能为空") String code,
        @NotBlank(message = "角色名称不能为空") String name,
        String description,
        Integer sort,
        List<Long> permissionIds) {
}
