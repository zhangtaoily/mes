package com.xinao.mes.common.auth;

/**
 * 用户鉴权数据接口（SPI）
 *
 * <p>common 层只负责拦截与校验，用户/角色/权限数据由 domain 层（system 上下文）实现本接口提供，
 * 保持依赖方向 domain → common 单向。
 */
public interface UserAuthPort {

    /**
     * 按用户ID加载登录用户（含角色与权限集）
     *
     * @param userId 用户ID
     * @return 用户上下文；用户不存在或已停用时返回 null
     */
    LoginUser loadUserById(Long userId);
}
