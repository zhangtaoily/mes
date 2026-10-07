package com.xinao.mes.common.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按钮级权限注解
 *
 * <p>标注在 Controller 方法或类上，拦截器校验当前用户权限集包含指定权限串，
 * 超管（ROLE_ADMIN / perms 含 "*"）直接放行。权限串约定：模块:实体:操作，如 sys:user:add
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresPerm {

    /** 权限串，支持数组（任一命中即放行） */
    String[] value();
}
