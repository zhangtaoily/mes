package com.xinao.mes.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 纺纱车间生产进度跟踪（原型首展示报表：计划产量/累计产出/完成率/预计交期/状态） */
@Data
public class ProgressTrackingVO {

    private Long id;
    private String taskNo;
    private String batchNo;
    private String productSpec;
    private String actualCount;
    private String workshopName;
    private BigDecimal planQty;
    /** 累计产出（末道报产工序 output_qty） */
    private BigDecimal outputQty;
    /** 完成率%（output/plan，Service 计算） */
    private BigDecimal completeRate;
    private LocalDate deliveryDate;
    /** 完成状态：已完成/逾期/进行中（Service 计算） */
    private String status;
    private Integer isFinished;
    private String execStatus;
    /** 排程条数 */
    private Integer scheduleCount;
}
