package com.xinao.mes.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 筒子称重产量表（按日+入库批次：箱数/筒数/五列重量合计） */
@Data
public class TubeWeightOutputVO {

    private LocalDate boxDate;
    private String warehouseBatchNo;
    private String packBatchNo;
    private String productSpec;
    /** 箱数 */
    private Integer boxCount;
    /** 筒子数合计 */
    private Integer tubeCount;
    private BigDecimal grossWeight;
    private BigDecimal tareWeight;
    private BigDecimal netWeight;
    private BigDecimal publicWeight;
}
