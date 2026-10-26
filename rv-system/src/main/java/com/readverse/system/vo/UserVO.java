package com.readverse.system.vo;

import java.time.LocalDateTime;
import java.util.List;

public record UserVO(
        Long id,
        String username,
        String nickname,
        String avatar,
        String email,
        String phone,
        Integer status,
        LocalDateTime lastLoginTime,
        LocalDateTime createTime,
        List<String> roles) {
}
