package com.xinao.mes.common.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashSet;
import java.util.Set;

/**
 * 鉴权配置：注册认证/权限拦截器
 *
 * <p>白名单默认放行登录接口与公开接口，可通过 mes.jwt.exclude-paths 追加。
 */
@Configuration
@ConfigurationProperties(prefix = "mes.jwt")
public class WebAuthConfig implements WebMvcConfigurer {

    /** 默认白名单 */
    private static final Set<String> DEFAULT_EXCLUDES = Set.of(
            "/api/auth/login",
            "/api/hello",
            "/error"
    );

    private final JwtUtil jwtUtil;
    private final ObjectProvider<UserAuthPort> userAuthPortProvider;

    /** 追加白名单（Ant 风格） */
    private Set<String> excludePaths = new HashSet<>();

    public WebAuthConfig(JwtUtil jwtUtil, ObjectProvider<UserAuthPort> userAuthPortProvider) {
        this.jwtUtil = jwtUtil;
        this.userAuthPortProvider = userAuthPortProvider;
    }

    public void setExcludePaths(Set<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        Set<String> all = new HashSet<>(DEFAULT_EXCLUDES);
        all.addAll(excludePaths);
        UserAuthPort port = userAuthPortProvider.getIfAvailable();
        if (port == null) {
            throw new IllegalStateException("缺少 UserAuthPort 实现（应由 mes-domain 的 system 上下文提供）");
        }
        registry.addInterceptor(new AuthInterceptor(jwtUtil, port, all))
                .addPathPatterns("/**");
    }
}
