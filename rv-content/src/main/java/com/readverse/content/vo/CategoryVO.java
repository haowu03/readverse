package com.readverse.content.vo;

import java.util.List;

public record CategoryVO(
        Long id,
        Long parentId,
        String name,
        String code,
        Integer sort,
        String icon,
        List<CategoryVO> children) {
}
