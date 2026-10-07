package com.xinao.mes.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 组织机构（sys_org）：集团 → 职能部门/事业部 → 车间 → 班组
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_org")
public class SysOrg extends BaseEntity {

    /** 父组织ID，顶级为 0 */
    private Long parentId;
    /** 组织名称 */
    private String orgName;
    /** 类型:1公司 2部门 3车间 */
    private Integer orgType;
    /** 排序号 */
    private Integer sortNo;
    /** 1启用 0停用 */
    private Integer status;
}
