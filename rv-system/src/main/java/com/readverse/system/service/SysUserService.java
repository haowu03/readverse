package com.readverse.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.common.result.PageResult;
import com.readverse.system.dto.UserRoleRequest;
import com.readverse.system.entity.SysRole;
import com.readverse.system.entity.SysUser;
import com.readverse.system.entity.SysUserRole;
import com.readverse.system.mapper.SysRoleMapper;
import com.readverse.system.mapper.SysUserMapper;
import com.readverse.system.mapper.SysUserRoleMapper;
import com.readverse.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public PageResult<UserVO> page(long current, long size, String keyword, Integer status) {
        Page<SysUser> page = new Page<>(Math.max(1, current), Math.min(100, Math.max(1, size)));
        var query = Wrappers.<SysUser>lambdaQuery()
                .eq(status != null, SysUser::getStatus, status)
                .and(StringUtils.hasText(keyword), q -> q
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getNickname, keyword))
                .orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> result = userMapper.selectPage(page, query);
        List<UserVO> records = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void assignRoles(Long id, UserRoleRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        for (Long roleId : request.roleIds()) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(id);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    private UserVO toVO(SysUser user) {
        List<String> roles = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream().map(SysUserRole::getRoleId)
                .map(roleMapper::selectById)
                .filter(java.util.Objects::nonNull)
                .map(SysRole::getCode)
                .toList();
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(),
                user.getEmail(), user.getPhone(), user.getStatus(), user.getLastLoginTime(),
                user.getCreateTime(), roles);
    }
}
