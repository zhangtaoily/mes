package com.xinao.mes.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参
 */
@Data
public class LoginDTO {

    @NotBlank(message = "登录名不能为空")
    private String loginName;

    @NotBlank(message = "密码不能为空")
    private String password;
}
