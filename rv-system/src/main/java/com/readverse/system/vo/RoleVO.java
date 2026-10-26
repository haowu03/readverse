package com.readverse.system.vo;

import java.util.List;

public record RoleVO(
        Long id,
        String code,
        String name,
        String description,
        Integer status,
        Integer sort,
        List<Long> permissionIds) {
}
