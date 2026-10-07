package com.xinao.mes.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色（sys_role）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色名 */
    private String roleName;
    /** 角色编码，如 ROLE_ADMIN */
    private String roleKey;
    /** 数据权限范围 */
    private Integer dataScope;
    /** 1启用 0停用 */
    private Integer status;
    /** 备注 */
    private String remark;
}
