package com.sidequest.identity.interfaces;

import com.sidequest.common.Result;
import com.sidequest.identity.application.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Result<String> login(@RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        return Result.success(token);
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
}

