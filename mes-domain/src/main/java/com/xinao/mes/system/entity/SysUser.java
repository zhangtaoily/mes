package com.xinao.mes.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户（sys_user）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 组织ID */
    private Long orgId;
    /** 工号 */
    private String empNo;
    /** 姓名 */
    private String userName;
    /** 登录名 */
    private String loginName;
    /** 密码（BCrypt，不外露） */
    @JsonIgnore
    private String password;
    /** 手机号 */
    private String phone;
    /** 1启用 0停用 */
    private Integer status;
}
