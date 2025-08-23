package com.yuesf.aireader.controller;

import com.yuesf.aireader.dto.ApiResponse;
import com.yuesf.aireader.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供登录和JWT令牌管理功能
 */
@Slf4j
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     * POST /auth/login
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody LoginRequest request) {
        try {
            log.info("用户登录请求: {}", request.getUsername());
            
            // 这里应该验证用户名和密码，暂时使用硬编码验证
            if ("admin".equals(request.getUsername()) && "admin123".equals(request.getPassword())) {
                // 生成JWT令牌
                String token = jwtUtil.generateToken("1", request.getUsername(), "ADMIN");
                
                Map<String, String> result = new HashMap<>();
                result.put("token", token);
                result.put("username", request.getUsername());
                result.put("role", "ADMIN");
                
                log.info("用户 {} 登录成功", request.getUsername());
                return ApiResponse.success(result);
            } else {
                log.warn("用户 {} 登录失败，密码错误", request.getUsername());
                return ApiResponse.error(401, "用户名或密码错误");
            }
            
        } catch (Exception e) {
            log.error("登录处理失败", e);
            return ApiResponse.error(500, "服务器内部错误");
        }
    }

    /**
     * 验证令牌
     * POST /auth/verify
     */
    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verifyToken(@RequestBody TokenRequest request) {
        try {
            if (jwtUtil.isTokenValid(request.getToken())) {
                String userId = jwtUtil.getUserIdFromToken(request.getToken());
                String username = jwtUtil.getUsernameFromToken(request.getToken());
                String role = jwtUtil.getRoleFromToken(request.getToken());
                
                Map<String, Object> result = new HashMap<>();
                result.put("valid", true);
                result.put("userId", userId);
                result.put("username", username);
                result.put("role", role);
                
                return ApiResponse.success(result);
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("valid", false);
                return ApiResponse.success(result);
            }
        } catch (Exception e) {
            log.error("令牌验证失败", e);
            return ApiResponse.error(500, "服务器内部错误");
        }
    }

    /**
     * 登录请求DTO
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 令牌请求DTO
     */
    public static class TokenRequest {
        private String token;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}


