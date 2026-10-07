package com.xinao.mes.qc.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 检验报告-指标值（方案矩阵纵表：行=指标×试样号）
 *
 * <p>value_str 录入检验值；is_over 越限标志（公式计算后按上下限判定，
 * 含 script 引擎算出的衍生指标——$BQ 结果同样参与越限判定）。
 */
@Data
@TableName("qc_report_item")
public class QcReportItem {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 检验报告ID（qc_report 外键） */
    private Long qcReportId;
    /** 指标 */
    private Long indicatorId;
    /** 试样号 1..n */
    private Integer sampleNo;
    /** 检验值（指标值纵表，字符串存数值） */
    private String valueStr;
    /** 是否越限（1越限 0正常） */
    private Integer isOver;
    /** 方案带出的标准/上限/下限（快照） */
    private String standardValue;
    /** 上限（方案带出快照） */
    private String upperLimit;
    /** 下限（方案带出快照） */
    private String lowerLimit;
}
