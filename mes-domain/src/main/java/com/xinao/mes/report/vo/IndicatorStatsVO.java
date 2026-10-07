package com.xinao.mes.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 纱线物理指标/条干CV 统计行（指标聚合口径：报告数/均值/极值/越限数） */
@Data
public class IndicatorStatsVO {

    private Long indicatorId;
    private String indicatorName;
    private String unit;
    /** 报告数（批次维去重） */
    private Integer reportCount;
    /** 检验值数（试样维） */
    private Integer valueCount;
    private Integer overCount;
    private BigDecimal avgValue;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    /** 越限率%（Service 计算） */
    private BigDecimal overRate;
    /** 条干CV专项：批次/日期明细行用 */
    private String batchNo;
    private String inspectDate;
}
