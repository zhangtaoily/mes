package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 排程-任务明细（从生产任务带出，剩余可排校验与占用） */
@Data
@TableName("prod_schedule_task")
public class ProdScheduleTask {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 排程ID */
    private Long scheduleId;
    /** 生产任务ID（PI） */
    private Long taskId;
    /** 产品编号 */
    private String productNo;
    /** 产品名称 */
    private String productName;
    /** 产品编号（PNO） */
    private String productPno;
    /** 产品规格 */
    private String spec;
    /** 辅助属性 */
    private String auxAttr;
    /** 原料明细 */
    private String rawDetail;
    /** 排产工序 */
    private String processName;
    /** 计划产量 */
    private BigDecimal planQty;
    /** 任务已排产量快照（刷新排程时重算） */
    private BigDecimal scheduledQty;
    /** 排程数量（不变量：不可超 计划产量-已排产量） */
    private BigDecimal scheduleQty;
    /** 单位 */
    private String unit;
}
