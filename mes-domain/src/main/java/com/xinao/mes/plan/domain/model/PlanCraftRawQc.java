package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 计划工艺单-原料检验报告 */
@Data
@TableName("plan_craft_raw_qc")
public class PlanCraftRawQc {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划工艺单ID */
    private Long planCraftId;
    /** 取样批次 */
    private String sampleBatch;
    /** 取样产品 */
    private String sampleProduct;
    /** 备注 */
    private String remark;
    /** 条重 */
    private BigDecimal weightPerMeter;
    /** 不匀率 */
    private BigDecimal unevenRate;
    /** 毛粒50克 */
    @TableField("neps_50g")
    private BigDecimal neps50g;
    /** 杂质/毛屑不清 */
    private BigDecimal impurity;
    /** 回潮 */
    private BigDecimal moisture;
    /** 毛片 */
    private BigDecimal woolPiece;
    /** 异色毛 */
    private BigDecimal alienColor;
    /** 异纤 */
    private BigDecimal alienFiber;
    /** 大毛粒 */
    private BigDecimal nepsLarge;
    /** 中毛粒 */
    private BigDecimal nepsMid;
    /** 小毛粒 */
    private BigDecimal nepsSmall;
    /** 含油 */
    private BigDecimal oil;
    /** 草屑50克 */
    @TableField("grass_50g")
    private BigDecimal grass50g;
    /** 长度 */
    private BigDecimal length;
    /** 离散 */
    private BigDecimal dispersion;
}
