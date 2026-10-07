package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 排程-用料汇总 */
@Data
@TableName("prod_schedule_material")
public class ProdScheduleMaterial {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 排程ID */
    private Long scheduleId;
    /** 原料批号 */
    private String rawBatchNo;
    /** 原料名称 */
    private String rawName;
    /** 计划用量 */
    private BigDecimal planQty;
    /** 实投数量 */
    private BigDecimal actualQty;
    /** 单位 */
    private String unit;
}
