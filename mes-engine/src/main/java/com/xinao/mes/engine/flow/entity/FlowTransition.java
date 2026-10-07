package com.xinao.mes.engine.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * flow 状态机迁移配置（flow_transition）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("flow_transition")
public class FlowTransition extends BaseEntity {

    /** 单据类型：plan_craft/prod_task/qc_report/pk_task */
    private String billType;

    /** 当前状态 */
    private String fromStatus;

    /** 目标状态 */
    private String toStatus;

    /** 动作编码：submit/withdraw/effect/finish/close/void/unvoid */
    private String action;

    /** 按钮名称 */
    private String actionName;

    /** 允许角色，逗号分隔，*=全部 */
    private String roleKeys;

    private Integer sortNo;

    /** 1启用 0停用 */
    private Integer status;
}
