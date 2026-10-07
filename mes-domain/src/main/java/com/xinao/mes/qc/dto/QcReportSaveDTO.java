package com.xinao.mes.qc.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 检验报告保存入参（主表 + 试样值；新建时按方案生成指标矩阵） */
@Data
public class QcReportSaveDTO {

    /** 空=新建 */
    private Long id;

    private Long workshopId;
    private Long prodLineId;
    /** 原料检/工序检/成品检 */
    private String qcType;
    /** 0车间 1中心实验室 */
    private Integer labType;
    private Long equipmentId;
    private Long teamId;
    private String batchNo;
    private String dyeVatNo;
    private String inspector;
    private LocalDate inspectDate;
    private BigDecimal sampleQty;
    private BigDecimal retainQty;
    /** 质检方案（新建时生成指标矩阵） */
    private Long schemeId;
    private String rawMaterial;
    private String spinCountDrop;
    private String spinCount;
    private String rawBatchNo;
    private BigDecimal mergeFeedQty;
    private String remark;

    /** 试样值（编辑时按 指标+试样号 定位更新 value_str） */
    private List<ItemVal> items;

    @Data
    public static class ItemVal {
        private Long indicatorId;
        private Integer sampleNo;
        private String valueStr;
    }
}
