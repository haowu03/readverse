package com.readverse.system.controller;

import com.readverse.common.result.R;
import com.readverse.system.dto.LoginRequest;
import com.readverse.system.dto.PasswordChangeRequest;
import com.readverse.system.dto.RefreshRequest;
import com.readverse.system.dto.RegisterRequest;
import com.readverse.system.service.AuthService;
import com.readverse.system.vo.LoginResponse;
import com.readverse.system.vo.UserInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @PostMapping("/register")
    public R<UserInfo> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return R.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
                          @RequestBody(required = false) RefreshRequest request) {
        String accessToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }
        authService.logout(accessToken, request == null ? null : request.refreshToken());
        return R.ok();
    }

    @GetMapping("/me")
    public R<UserInfo> me() {
        return R.ok(authService.currentUser());
    }

    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(request);
        return R.ok();
    }
}
