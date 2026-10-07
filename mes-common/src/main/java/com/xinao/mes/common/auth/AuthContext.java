package com.xinao.mes.common.auth;

/**
 * 登录用户 ThreadLocal 上下文
 */
public final class AuthContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** @return 当前登录用户；未登录返回 null */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** @return 当前登录用户ID */
    public static Long getUserId() {
        LoginUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
