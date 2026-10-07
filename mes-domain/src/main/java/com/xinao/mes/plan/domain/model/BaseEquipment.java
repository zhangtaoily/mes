package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/** 设备信息（机台） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_equipment")
public class BaseEquipment extends BaseEntity {

    /** 所属车间ID */
    private Long workshopId;
    /** 设备类型ID */
    private Long eqTypeId;
    /** 机型：VTS-08/SINCRO B 306/CSN等 */
    private String machineModel;
    /** 机台名称：110倍捻12/F9粗纱1/F10并线1 */
    private String machineNo;
    /** 小时产能 */
    private BigDecimal hourCapacity;
    /** 状态：1启用 0停用 */
    private Integer status;
}
