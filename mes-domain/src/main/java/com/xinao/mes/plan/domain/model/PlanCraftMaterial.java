package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 计划工艺单-用料汇总 */
@Data
@TableName("plan_craft_material")
public class PlanCraftMaterial {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划工艺单ID */
    private Long planCraftId;
    /** 原料批号 */
    private String rawBatchNo;
    /** 原料名称 */
    private String rawName;
    /** 车间用料量 */
    private BigDecimal workshopQty;
    /** 配料量 */
    private BigDecimal mixQty;
    /** 投料量 */
    private BigDecimal feedQty;
    /** 单位 */
    private String unit;
}
