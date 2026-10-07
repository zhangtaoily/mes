package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 生产任务-用料汇总 */
@Data
@TableName("prod_task_material")
public class ProdTaskMaterial {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;
    /** 原料批号 */
    private String rawBatchNo;
    /** 原料名称 */
    private String rawName;
    /** 计划投料量 */
    private BigDecimal planFeedQty;
    /** 车间用料量 */
    private BigDecimal workshopQty;
    /** 已投料量 */
    private BigDecimal fedQty;
    /** 单位 */
    private String unit;
}
