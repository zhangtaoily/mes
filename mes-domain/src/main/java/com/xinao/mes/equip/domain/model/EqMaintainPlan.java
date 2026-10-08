package com.xinao.mes.equip.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 保养计划（周期：1日 2周 3月） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("eq_maintain_plan")
public class EqMaintainPlan extends BaseEntity {

    /** 计划编码 */
    private String planCode;
    /** 车间ID */
    private Long workshopId;
    /** 保养名称 */
    private String planName;
    /** 周期:1日/2周/3月 */
    private Integer cycleType;
    /** 周期值 */
    private Integer cycleValue;
    /** 备注 */
    private String remark;
}
