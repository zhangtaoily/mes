package com.xinao.mes.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色新增/修改入参
 */
@Data
public class RoleSaveDTO {

    private Long id;

    @NotBlank(message = "角色名不能为空")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    private String roleKey;

    private Integer dataScope;
    private Integer status;
    private String remark;

    /** 已分配菜单/按钮ID集合（分配权限时使用） */
    private List<Long> menuIds;
}
