package com.readverse.content.dto;

import jakarta.validation.constraints.NotBlank;

public record CategorySaveRequest(
        Long parentId,
        @NotBlank(message = "分类名称不能为空") String name,
        @NotBlank(message = "分类编码不能为空") String code,
        Integer sort,
        String icon,
        Integer status) {
}
