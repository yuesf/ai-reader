package com.yuesf.aireader.controller;

import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    public static class LoginRequest {
        @NotBlank
        public String username;
        @NotBlank
        public String password;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest req) {
        try {
            String token = authService.loginAndIssueToken(req.username, req.password);
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            return ApiResponse.success(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(401, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        }
    }

    @GetMapping("/verify")
    public ApiResponse<String> verify(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ApiResponse.error(401, "未授权");
        }
        String token = authorization.substring(7);
        var user = authService.verifyToken(token);
        if (user == null) {
            return ApiResponse.error(401, "Token无效或已过期");
        }
        return ApiResponse.success("ok");
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        // 基于JWT的无状态登出：前端删除本地token即可
        return ApiResponse.success("ok");
    }
}


