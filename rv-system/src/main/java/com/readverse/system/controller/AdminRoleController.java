package com.readverse.system.controller;

import com.readverse.common.result.R;
import com.readverse.system.dto.RolePermissionsRequest;
import com.readverse.system.dto.RoleSaveRequest;
import com.readverse.system.service.SysRoleService;
import com.readverse.system.vo.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final SysRoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<RoleVO>> list() {
        return R.ok(roleService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public R<RoleVO> create(@Valid @RequestBody RoleSaveRequest request) {
        return R.ok(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<RoleVO> update(@PathVariable Long id, @Valid @RequestBody RoleSaveRequest request) {
        return R.ok(roleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('system:role:assign')")
    public R<Void> assignPermissions(@PathVariable Long id, @Valid @RequestBody RolePermissionsRequest request) {
        roleService.assignPermissions(id, request.permissionIds());
        return R.ok();
    }
}
