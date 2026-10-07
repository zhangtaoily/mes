package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 生产任务-订单明细 */
@Data
@TableName("prod_task_order")
public class ProdTaskOrder {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;
    /** 纺纱批号 */
    private String spinBatchNo;
    /** 产品名称 */
    private String productName;
    /** 原料 */
    private String rawMaterial;
    /** 股数/支数 */
    private String plyCount;
    /** 原计划量 */
    private BigDecimal originPlanQty;
    /** 计划量 */
    private BigDecimal planQty;
    /** 投料量 */
    private BigDecimal feedQty;
    /** 产出量 */
    private BigDecimal outputQty;
    /** 单位 */
    private String unit;
}
