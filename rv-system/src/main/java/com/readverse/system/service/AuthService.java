package com.readverse.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import com.readverse.framework.security.JwtProperties;
import com.readverse.framework.security.JwtService;
import com.readverse.framework.security.LoginUser;
import com.readverse.framework.security.SecurityUtils;
import com.readverse.framework.security.TokenStore;
import com.readverse.system.dto.LoginRequest;
import com.readverse.system.dto.PasswordChangeRequest;
import com.readverse.system.dto.RegisterRequest;
import com.readverse.system.entity.SysPermission;
import com.readverse.system.entity.SysRole;
import com.readverse.system.entity.SysRolePermission;
import com.readverse.system.entity.SysUser;
import com.readverse.system.entity.SysUserRole;
import com.readverse.system.mapper.SysPermissionMapper;
import com.readverse.system.mapper.SysRoleMapper;
import com.readverse.system.mapper.SysRolePermissionMapper;
import com.readverse.system.mapper.SysUserMapper;
import com.readverse.system.mapper.SysUserRoleMapper;
import com.readverse.system.vo.LoginResponse;
import com.readverse.system.vo.UserInfo;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenStore tokenStore;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已被禁用");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        LoginUser loginUser = buildLoginUser(user);
        JwtService.TokenPair pair = jwtService.createTokens(user.getId(), user.getUsername());
        tokenStore.save(pair.accessJti(), loginUser, jwtProperties.getAccessTtlSeconds());
        tokenStore.save(pair.refreshJti(), loginUser, jwtProperties.getRefreshTtlSeconds());
        return new LoginResponse(pair.accessToken(), pair.refreshToken(), pair.expiresIn(), toUserInfo(loginUser));
    }

    @Transactional
    public UserInfo register(RegisterRequest request) {
        Long exists = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.CONFLICT, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setStatus(1);
        user.setDeleted(0);
        userMapper.insert(user);

        SysRole readerRole = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getCode, "READER"));
        if (readerRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(readerRole.getId());
            userRoleMapper.insert(userRole);
        }
        return toUserInfo(buildLoginUser(user));
    }

    public LoginResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (Exception e) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!jwtService.isRefresh(claims)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        LoginUser loginUser = tokenStore.get(claims.getId());
        if (loginUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        tokenStore.remove(claims.getId());
        JwtService.TokenPair pair = jwtService.createTokens(loginUser.getId(), loginUser.getUsername());
        tokenStore.save(pair.accessJti(), loginUser, jwtProperties.getAccessTtlSeconds());
        tokenStore.save(pair.refreshJti(), loginUser, jwtProperties.getRefreshTtlSeconds());
        return new LoginResponse(pair.accessToken(), pair.refreshToken(), pair.expiresIn(), toUserInfo(loginUser));
    }

    public void logout(String accessToken, String refreshToken) {
        removeQuietly(accessToken, true);
        removeQuietly(refreshToken, false);
    }

    private void removeQuietly(String token, boolean access) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtService.parse(token);
            if (access && !jwtService.isAccess(claims)) {
                return;
            }
            if (!access && !jwtService.isRefresh(claims)) {
                return;
            }
            tokenStore.remove(claims.getId());
        } catch (Exception ignored) {
        }
    }

    public UserInfo currentUser() {
        return toUserInfo(SecurityUtils.getLoginUser());
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request) {
        Long userId = SecurityUtils.requireUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BizException("旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userMapper.updateById(user);
    }

    public LoginUser buildLoginUser(SysUser user) {
        List<Long> roleIds = userRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, user.getId()))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
        List<SysRole> roles = roleIds.isEmpty() ? List.of() : roleMapper.selectBatchIds(roleIds);
        Set<String> roleCodes = roles.stream().map(SysRole::getCode).collect(Collectors.toCollection(LinkedHashSet::new));

        List<Long> permissionIds = roleIds.isEmpty() ? List.of() : rolePermissionMapper.selectList(
                        Wrappers.<SysRolePermission>lambdaQuery().in(SysRolePermission::getRoleId, roleIds))
                .stream().map(SysRolePermission::getPermissionId).distinct().toList();
        List<SysPermission> permissions = permissionIds.isEmpty() ? List.of() : permissionMapper.selectBatchIds(permissionIds);
        Set<String> permissionCodes = permissions.stream()
                .map(SysPermission::getCode)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return LoginUser.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .roles(roleCodes)
                .permissions(permissionCodes)
                .build();
    }

    private UserInfo toUserInfo(LoginUser user) {
        return new UserInfo(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(),
                user.getRoles() == null ? Set.of() : user.getRoles(),
                user.getPermissions() == null ? Set.of() : user.getPermissions());
    }
}
