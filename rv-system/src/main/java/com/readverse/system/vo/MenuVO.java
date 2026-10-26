package com.readverse.system.vo;

import java.util.ArrayList;
import java.util.List;

public record MenuVO(
        Long id,
        Long parentId,
        String code,
        String name,
        String path,
        String component,
        String icon,
        Integer sort,
        List<MenuVO> children) {

    public MenuVO(Long id, Long parentId, String code, String name, String path,
                  String component, String icon, Integer sort) {
        this(id, parentId, code, name, path, component, icon, sort, new ArrayList<>());
    }
}
