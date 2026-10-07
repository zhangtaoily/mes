package com.xinao.mes.qc.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 检验报告统计（口径：已作废报告不计入统计，取消作废后恢复） */
@Data
public class QcStatisticsVO {

    /** 有效报告数（未作废未删） */
    private Long total;
    /** 已提交数 */
    private Long submitted;
    /** 已作废数（不计入 total） */
    private Long voided;
    /** 越限指标值个数 */
    private Long overLimitCount;
    /** 不匀率均值（衍生指标参与统计） */
    private BigDecimal avgByl;
}
