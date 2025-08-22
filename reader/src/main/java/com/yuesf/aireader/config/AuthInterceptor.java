package com.yuesf.aireader.config;

import com.yuesf.aireader.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        System.out.println("拦截器处理请求: " + method + " " + uri);
        
        // 公开接口直接放行
        if (uri.startsWith("/v1/auth/")
                || uri.startsWith("/v1/health")
                || "/v1/reports".equals(uri)
                || ("GET".equals(method) && uri.startsWith("/v1/images/"))) {
            System.out.println("公开接口，直接放行");
            return true;
        }
        
        String auth = request.getHeader("Authorization");
        System.out.println("Authorization头: " + (auth != null ? auth.substring(0, Math.min(20, auth.length())) + "..." : "null"));
        
        if (auth != null && auth.startsWith("Bearer ")) {
            var user = authService.verifyToken(auth.substring(7));
            if (user != null) {
                System.out.println("Token验证成功，用户: " + user.getUsername());
                return true;
            } else {
                System.out.println("Token验证失败");
            }
        } else {
            System.err.println("缺少Authorization头");
        }
        
        System.err.println("请求被拒绝，返回401");
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未授权\"}");
        return false;
    }
}


