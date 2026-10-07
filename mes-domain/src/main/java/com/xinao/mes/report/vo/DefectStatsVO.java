package com.xinao.mes.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 疵纱统计表（包装质检 15 类疵纱：按入库批次行 + 类型合计） */
@Data
public class DefectStatsVO {

    private Long id;
    private String warehouseBatchNo;
    private String batchNo;
    /** 生产车间 */
    private String workshopName;
    /** 质检车间（如质检五组） */
    private String qcWorkshopName;
    private LocalDate docDate;
    private Integer firstGradeQty;
    private BigDecimal passRate;
    /** 疵纱分类明细（type → value） */
    private List<DefectRow> defects = new ArrayList<>();
    /** 疵纱合计 */
    private BigDecimal defectTotal;

    /** 接口返回体：批次台账 + 类型合计两个口径 */
    @Data
    public static class DefectSummary {
        /** 行=入库批次（含疵纱明细矩阵） */
        private List<DefectStatsVO> reports;
        /** 行=疵纱类型（全局数量合计） */
        private List<DefectRow> typeSummary;
    }

    /** SQL 展平行（package_qc_id × defect_type）与类型合计共用 */
    @Data
    public static class DefectRow {
        private Long id;
        private String warehouseBatchNo;
        private String batchNo;
        private String workshopName;
        private String qcWorkshopName;
        private LocalDate docDate;
        private Integer firstGradeQty;
        private BigDecimal passRate;
        private String defectType;
        private BigDecimal defectValue;
        /** 类型合计口径：记录条数 */
        private Integer recordCount;
    }
}
