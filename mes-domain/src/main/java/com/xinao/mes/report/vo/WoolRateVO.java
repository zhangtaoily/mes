package com.xinao.mes.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 车间用毛率统计（月度：投料量/产出量 → 实际用毛率 vs 标准用毛率） */
@Data
public class WoolRateVO {

    private String periodMonth;
    private Long workshopId;
    private String workshopName;
    /** 投料量（投料完毕封账口径） */
    private BigDecimal feedQty;
    /** 产出量（后纺工序合计） */
    private BigDecimal outputQty;
    /** 回毛量 */
    private BigDecimal returnWoolQty;
    /** 实际用毛率% = feedQty / outputQty × 100（Service 计算） */
    private BigDecimal actualRate;
    /** 标准用毛率%（qc_wool_rate） */
    private BigDecimal standardRate;
    /** 差额（实际-标准，Service 计算） */
    private BigDecimal rateDiff;

    /** SQL 展平行（投料/产出/回毛三查询 + 标准值共用） */
    @Data
    public static class WoolRateRow {
        private String periodMonth;
        private Long workshopId;
        private BigDecimal feedQty;
        private BigDecimal outputQty;
        private BigDecimal returnWoolQty;
    }

    @Data
    public static class StandardRateRow {
        private String periodMonth;
        private Long workshopId;
        private BigDecimal standardRate;
        /** workshopNames 查询复用：车间名 */
        private String workshopName;
    }
}
