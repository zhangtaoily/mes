package com.xinao.mes.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户新增/修改入参
 */
@Data
public class UserSaveDTO {

    /** 修改时必传 */
    private Long id;

    private Long orgId;

    @NotBlank(message = "工号不能为空")
    private String empNo;

    @NotBlank(message = "姓名不能为空")
    private String userName;

    @NotBlank(message = "登录名不能为空")
    private String loginName;

    /** 新增必填，修改时不传则不变 */
    private String password;

    private String phone;

    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 关联角色ID集合 */
    private List<Long> roleIds;
}
