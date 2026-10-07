package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 班组（甲班/乙班/丙班(白班)等，现场汇报维度） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_team")
public class BaseTeam extends BaseEntity {

    /** 所属车间ID */
    private Long workshopId;
    /** 班组名称：甲班/乙班/丙班(白班) */
    private String teamName;
    /** 班组长用户ID */
    private Long leaderUserId;
    /** 状态：1启用 0停用 */
    private Integer status;
}
