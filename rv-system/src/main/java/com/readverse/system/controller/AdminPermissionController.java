package com.readverse.system.controller;

import com.readverse.common.result.R;
import com.readverse.system.service.SysPermissionService;
import com.readverse.system.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final SysPermissionService permissionService;

    @GetMapping("/permissions/tree")
    @PreAuthorize("hasAuthority('system:permission:list')")
    public R<List<MenuVO>> tree() {
        return R.ok(permissionService.allMenuTree());
    }

    @GetMapping("/menus")
    public R<List<MenuVO>> menus() {
        return R.ok(permissionService.menuTree());
    }
}
