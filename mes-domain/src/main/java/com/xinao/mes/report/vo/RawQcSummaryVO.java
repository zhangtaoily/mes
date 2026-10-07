package com.xinao.mes.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 原料检验汇总（中心实验室：17 项原料指标台账，列即表列） */
@Data
public class RawQcSummaryVO {

    private Long id;
    private String craftNo;
    private String batchNo;
    private String sampleBatch;
    private String sampleProduct;
    private BigDecimal weightPerMeter;
    private BigDecimal unevenRate;
    private BigDecimal neps50g;
    private BigDecimal impurity;
    private BigDecimal moisture;
    private BigDecimal woolPiece;
    private BigDecimal alienColor;
    private BigDecimal alienFiber;
    private BigDecimal nepsLarge;
    private BigDecimal nepsMid;
    private BigDecimal nepsSmall;
    private BigDecimal oil;
    private BigDecimal grass50g;
    private BigDecimal length;
    private BigDecimal dispersion;
}
