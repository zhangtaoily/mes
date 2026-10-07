package com.xinao.mes.plan.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 排程-任务明细新增面板行（生效任务 + 实时剩余可排，排满前端置灰） */
@Data
public class TaskRemainingVO {

    private Long taskId;
    private String taskNo;
    private String batchNo;
    private String productNo;
    private String productName;
    private String spec;
    private String rawDetail;
    private BigDecimal planQty;
    private BigDecimal scheduledQty;
    /** 剩余可排 = 计划产量 - 已排产量 */
    private BigDecimal remaining;
    private String unit;
}
