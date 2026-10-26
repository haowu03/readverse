package com.readverse.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.system.dto.RoleSaveRequest;
import com.readverse.system.entity.SysRole;
import com.readverse.system.entity.SysRolePermission;
import com.readverse.system.entity.SysUserRole;
import com.readverse.system.mapper.SysRoleMapper;
import com.readverse.system.mapper.SysRolePermissionMapper;
import com.readverse.system.mapper.SysUserRoleMapper;
import com.readverse.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    public List<RoleVO> list() {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getSort))
                .stream().map(this::toVO).toList();
    }

    @Transactional
    public RoleVO create(RoleSaveRequest request) {
        Long exists = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, request.code()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        role.setSort(request.sort() == null ? 0 : request.sort());
        role.setStatus(1);
        roleMapper.insert(role);
        if (request.permissionIds() != null) {
            replacePermissions(role.getId(), request.permissionIds());
        }
        return toVO(role);
    }

    @Transactional
    public RoleVO update(Long id, RoleSaveRequest request) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if ("ADMIN".equals(role.getCode()) && !"ADMIN".equals(request.code())) {
            throw new BizException("内置管理员角色编码不可修改");
        }
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        if (request.sort() != null) {
            role.setSort(request.sort());
        }
        roleMapper.updateById(role);
        if (request.permissionIds() != null) {
            replacePermissions(id, request.permissionIds());
        }
        return toVO(role);
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            return;
        }
        if ("ADMIN".equals(role.getCode())) {
            throw new BizException("内置管理员角色不可删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery().eq(SysRolePermission::getRoleId, id));
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, id));
    }

    @Transactional
    public void assignPermissions(Long id, List<Long> permissionIds) {
        replacePermissions(id, permissionIds);
    }

    private void replacePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(Wrappers.<SysRolePermission>lambdaQuery().eq(SysRolePermission::getRoleId, roleId));
        for (Long permissionId : permissionIds) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        }
    }

    private RoleVO toVO(SysRole role) {
        List<Long> permissionIds = rolePermissionMapper.selectList(Wrappers.<SysRolePermission>lambdaQuery()
                        .eq(SysRolePermission::getRoleId, role.getId()))
                .stream().map(SysRolePermission::getPermissionId).toList();
        return new RoleVO(role.getId(), role.getCode(), role.getName(), role.getDescription(),
                role.getStatus(), role.getSort(), permissionIds);
    }
}
