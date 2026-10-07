package com.xinao.mes.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 车间产品质量检验日报表（按日+车间+批次汇总，附指标矩阵） */
@Data
public class QcDailyVO {

    private LocalDate inspectDate;
    private Long workshopId;
    private String workshopName;
    private String batchNo;
    /** 报告数 */
    private Integer reportCount;
    /** 检验项数（含全部试样） */
    private Integer itemCount;
    /** 越限项数 */
    private Integer overCount;
    /** 取样数合计 */
    private BigDecimal sampleQty;
    /** 越限率%（overCount/itemCount，Service 计算） */
    private BigDecimal overRate;
    /** 指标矩阵（该日+车间范围内的指标聚合） */
    private List<IndicatorStatsVO> indicators;
}
