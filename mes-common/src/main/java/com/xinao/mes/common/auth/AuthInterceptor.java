package com.xinao.mes.common.auth;

import com.xinao.mes.common.result.R;
import com.xinao.mes.common.result.ResultCode;
import com.xinao.mes.common.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

/**
 * 认证与按钮级权限拦截器
 *
 * <p>流程：白名单放行 → 解析 Bearer token → 加载用户（含角色/权限集）→ ThreadLocal → 校验 @RequiresPerm
 */
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserAuthPort userAuthPort;
    private final Set<String> excludePaths;

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 白名单直接放行
        String uri = request.getRequestURI();
        for (String pattern : excludePaths) {
            if (matcher.match(pattern, uri)) {
                return true;
            }
        }

        // 解析 token
        String token = resolveToken(request);
        Long userId = StringUtils.hasText(token) ? jwtUtil.parseToken(token) : null;
        if (userId == null) {
            return reject(response, ResultCode.UNAUTHORIZED);
        }

        // 加载用户（含最新角色权限，角色变更即时生效）
        LoginUser user = userAuthPort.loadUserById(userId);
        if (user == null) {
            return reject(response, ResultCode.UNAUTHORIZED);
        }
        AuthContext.set(user);

        // 按钮/接口级权限校验
        if (handler instanceof HandlerMethod hm) {
            RequiresPerm perm = hm.getMethodAnnotation(RequiresPerm.class);
            if (perm == null) {
                perm = hm.getBeanType().getAnnotation(RequiresPerm.class);
            }
            if (perm != null && !hasPerm(user, perm.value())) {
                return reject(response, ResultCode.FORBIDDEN);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean hasPerm(LoginUser user, String[] required) {
        Set<String> perms = user.getPerms();
        if (perms == null || perms.isEmpty()) {
            return false;
        }
        // 超管通配
        if (perms.contains("*") || user.isAdmin()) {
            return true;
        }
        return Arrays.stream(required).anyMatch(perms::contains);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private boolean reject(HttpServletResponse response, ResultCode code) throws Exception {
        response.setStatus(code.getCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(JsonUtil.toJson(R.fail(code)));
        return false;
    }
}
