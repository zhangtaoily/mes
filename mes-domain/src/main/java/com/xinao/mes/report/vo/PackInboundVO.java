package com.xinao.mes.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 包装批次入库汇总（按入库批次：托数/箱数/公重/入库状态） */
@Data
public class PackInboundVO {

    private String warehouseBatchNo;
    private String batchNo;
    private String dyeVatNo;
    /** 托数 */
    private Integer palletCount;
    /** 箱数 */
    private Integer boxCount;
    private BigDecimal publicWeight;
    private BigDecimal netWeight;
    private LocalDateTime firstInTime;
    private LocalDateTime lastInTime;
    private String wmsStatus;
}
