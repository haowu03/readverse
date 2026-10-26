package com.readverse.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.framework.security.LoginUser;
import com.readverse.framework.security.SecurityUtils;
import com.readverse.system.entity.SysPermission;
import com.readverse.system.mapper.SysPermissionMapper;
import com.readverse.system.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysPermissionService {

    private final SysPermissionMapper permissionMapper;

    public List<SysPermission> listAll() {
        return permissionMapper.selectList(Wrappers.<SysPermission>lambdaQuery()
                .orderByAsc(SysPermission::getSort).orderByAsc(SysPermission::getId));
    }

    public List<MenuVO> menuTree() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Set<String> owned = loginUser.getPermissions() == null ? Set.of() : loginUser.getPermissions();
        boolean admin = loginUser.getRoles() != null && loginUser.getRoles().contains("ADMIN");
        List<SysPermission> menus = listAll().stream()
                .filter(p -> "MENU".equalsIgnoreCase(p.getType()))
                .filter(p -> Boolean.TRUE.equals(p.getVisible()))
                .filter(p -> admin || owned.contains(p.getCode()))
                .sorted(Comparator.comparing(SysPermission::getSort).thenComparing(SysPermission::getId))
                .toList();
        return buildTree(menus);
    }

    public List<MenuVO> allMenuTree() {
        List<SysPermission> menus = listAll().stream()
                .filter(p -> "MENU".equalsIgnoreCase(p.getType()))
                .sorted(Comparator.comparing(SysPermission::getSort).thenComparing(SysPermission::getId))
                .toList();
        return buildTree(menus);
    }

    private List<MenuVO> buildTree(List<SysPermission> permissions) {
        Map<Long, MenuVO> nodes = new HashMap<>();
        List<MenuVO> roots = new ArrayList<>();
        for (SysPermission permission : permissions) {
            nodes.put(permission.getId(), new MenuVO(permission.getId(), permission.getParentId(),
                    permission.getCode(), permission.getName(), permission.getPath(), permission.getComponent(),
                    permission.getIcon(), permission.getSort(), new ArrayList<>()));
        }
        Set<Long> ids = new HashSet<>(nodes.keySet());
        for (SysPermission permission : permissions) {
            MenuVO node = nodes.get(permission.getId());
            if (permission.getParentId() == null || permission.getParentId() == 0 || !ids.contains(permission.getParentId())) {
                roots.add(node);
            } else {
                MenuVO parent = nodes.get(permission.getParentId());
                if (parent != null) {
                    parent.children().add(node);
                }
            }
        }
        return roots;
    }
}
