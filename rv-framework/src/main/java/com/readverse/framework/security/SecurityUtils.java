package com.readverse.framework.security;

import com.readverse.common.exception.BizException;
import com.readverse.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static LoginUser getLoginUser() {
        LoginUser loginUser = getLoginUserOrNull();
        if (loginUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long getUserIdOrNull() {
        LoginUser loginUser = getLoginUserOrNull();
        return loginUser == null ? null : loginUser.getId();
    }

    public static Long requireUserId() {
        return getLoginUser().getId();
    }

    public static boolean isAuthenticated() {
        return getLoginUserOrNull() != null;
    }
}
