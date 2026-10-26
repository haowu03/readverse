package com.readverse.system.vo;

import java.util.Set;

public record UserInfo(
        Long id,
        String username,
        String nickname,
        String avatar,
        Set<String> roles,
        Set<String> permissions) {
}
