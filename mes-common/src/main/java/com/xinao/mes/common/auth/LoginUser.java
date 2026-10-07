package com.xinao.mes.common.auth;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * 当前登录用户上下文对象（JWT 解析 + DB 加载后的快照）
 */
@Data
public class LoginUser implements Serializable {

    private Long userId;
    private Long orgId;
    private String empNo;
    private String userName;
    private String loginName;

    /** 角色编码集合，如 ROLE_ADMIN */
    private Set<String> roles;
    /** 按钮权限集合，超管为 ["*"] */
    private Set<String> perms;

    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }
}
