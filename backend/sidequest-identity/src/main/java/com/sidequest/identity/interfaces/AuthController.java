package com.sidequest.identity.interfaces;

import com.sidequest.common.Result;
import com.sidequest.identity.application.UserService;
import com.sidequest.common.context.UserContext;
import com.sidequest.identity.interfaces.dto.LoginVO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/identity")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest request) {
        userService.register(request.getUsername(), request.getPassword(), request.getNickname());
        return Result.success("Registration successful");
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest request) {
        return Result.success(userService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return Result.error(401, "Unauthorized");
        }
        String token = authHeader.substring(7);
        userService.logout(token);
        return Result.success("Logout successful");
    }

    @PostMapping("/refresh-token")
    public Result<LoginVO> refreshToken(@RequestBody RefreshTokenRequest request) {
        return Result.success(userService.refreshToken(request.getToken()));
    }

    @PostMapping("/reset-password")
    @PreAuthorize("hasAuthority('USER_ROLE_ASSIGN')")
    public Result<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getUsername(), request.getNewPassword());
        return Result.success("Password reset");
    }

    @PostMapping("/change-password")
    public Result<String> changePassword(@RequestBody ChangePasswordRequest request) {
        String userId = UserContext.getUserId();
        if (userId == null) {
            return Result.error(401, "Unauthorized");
        }
        userService.updatePassword(Long.parseLong(userId), request.getOldPassword(), request.getNewPassword());
        return Result.success("Password changed");
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String nickname;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RefreshTokenRequest {
        private String token;
    }

    @Data
    public static class ResetPasswordRequest {
        private String username;
        private String newPassword;
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}

