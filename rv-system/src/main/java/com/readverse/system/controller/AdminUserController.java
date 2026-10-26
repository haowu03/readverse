package com.readverse.system.controller;

import com.readverse.common.result.PageResult;
import com.readverse.common.result.R;
import com.readverse.system.dto.UserRoleRequest;
import com.readverse.system.dto.UserStatusRequest;
import com.readverse.system.service.SysUserService;
import com.readverse.system.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysUserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(userService.page(current, size, keyword, status));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        userService.updateStatus(id, request.status());
        return R.ok();
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody UserRoleRequest request) {
        userService.assignRoles(id, request);
        return R.ok();
    }
}
