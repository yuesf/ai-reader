package com.yuesf.aireader.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限验证注解
 * 标记需要JWT鉴权的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
    
    /**
     * 是否需要管理员权限
     * 默认false，表示只需要登录即可
     */
    boolean requireAdmin() default false;
    
    /**
     * 允许的角色列表
     * 空数组表示不限制角色
     */
    String[] allowedRoles() default {};
}
