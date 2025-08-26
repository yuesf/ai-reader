package com.yuesf.aireader.interceptor;

import com.yuesf.aireader.annotation.RequireAuth;
import com.yuesf.aireader.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * 权限验证拦截器
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是方法处理器，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String uri = request.getRequestURI();
        String method = request.getMethod();
        // 公开接口直接放行
        if (uri.startsWith("/v1/auth/")
                || uri.startsWith("/health")
                || "/v1/mini/reports".equals(uri)
                || ("GET".equals(method) && uri.startsWith("/v1/images/"))) {
            System.out.println("公开接口，直接放行");
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查类和方法上的权限注解
        RequireAuth classAuth = handlerMethod.getBeanType().getAnnotation(RequireAuth.class);
        RequireAuth methodAuth = handlerMethod.getMethod().getAnnotation(RequireAuth.class);
        
        // 如果都没有权限注解，直接放行
        if (classAuth == null && methodAuth == null) {
            return true;
        }

        // 获取Authorization头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("请求缺少Authorization头: {}", request.getRequestURI());
            sendUnauthorizedResponse(response, "缺少认证令牌");
            return false;
        }

        // 提取JWT令牌
        String token = authHeader.substring(7);
        
        // 验证令牌
        if (!jwtUtil.isTokenValid(token)) {
            log.warn("无效的JWT令牌: {}", request.getRequestURI());
            sendUnauthorizedResponse(response, "无效的认证令牌");
            return false;
        }

        // 获取用户信息
        String userId = jwtUtil.getUserIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        // 将用户信息存储到请求属性中，供后续使用
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("userRole", role);

        // 检查权限要求
        RequireAuth auth = methodAuth != null ? methodAuth : classAuth;
        
        // 检查是否需要管理员权限
        if (auth.requireAdmin() && !"ADMIN".equals(role)) {
            log.warn("用户 {} 尝试访问需要管理员权限的接口: {}", username, request.getRequestURI());
            sendForbiddenResponse(response, "需要管理员权限");
            return false;
        }

        // 检查角色限制
        if (auth.allowedRoles().length > 0) {
            boolean hasAllowedRole = Arrays.stream(auth.allowedRoles())
                    .anyMatch(allowedRole -> allowedRole.equals(role));
            
            if (!hasAllowedRole) {
                log.warn("用户 {} 角色 {} 无权访问接口: {}", username, role, request.getRequestURI());
                sendForbiddenResponse(response, "权限不足");
                return false;
            }
        }

        log.debug("用户 {} 通过权限验证，访问接口: {}", username, request.getRequestURI());
        return true;
    }

    /**
     * 发送未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
            "{\"code\":401,\"message\":\"%s\",\"data\":null}", message
        ));
    }

    /**
     * 发送禁止访问响应
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
            "{\"code\":403,\"message\":\"%s\",\"data\":null}", message
        ));
    }
}
