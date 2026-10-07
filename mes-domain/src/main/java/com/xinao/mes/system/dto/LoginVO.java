package com.xinao.mes.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * 登录返回：token + 用户身份 + 权限集（前端按钮可见性按 perms 渲染）
 */
@Data
@Builder
public class LoginVO {

    private String token;
    private Long userId;
    private String userName;
    private String empNo;
    private String orgId;
    /** 角色编码集合 */
    private Set<String> roles;
    /** 按钮权限集合（超管为 ["*"]） */
    private Set<String> perms;
}
