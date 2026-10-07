package com.xinao.mes.qc.vo;

import com.xinao.mes.qc.domain.model.QcReport;
import lombok.Data;

import java.util.List;

/** 检验报告详情（主表 + 指标矩阵，含指标名/单位/越限标志） */
@Data
public class QcReportDetailVO {

    private QcReport report;

    private List<ItemVO> items;

    @Data
    public static class ItemVO {
        private Long indicatorId;
        private String indicatorCode;
        private String indicatorName;
        private String unit;
        /** 1原始 2衍生(公式计算) */
        private Integer indicatorType;
        private Integer sampleNo;
        private String valueStr;
        private Integer isOver;
        private String standardValue;
        private String upperLimit;
        private String lowerLimit;
    }
}
