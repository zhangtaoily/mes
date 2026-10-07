package com.xinao.mes.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 客户端 IP 工具
 */
public final class IpUtil {

    private static final String[] HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    private IpUtil() {
    }

    public static String clientIp(HttpServletRequest request) {
        for (String header : HEADERS) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                int idx = ip.indexOf(',');
                return idx > 0 ? ip.substring(0, idx).trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }
}
