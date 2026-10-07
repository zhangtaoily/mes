package com.xinao.mes.plugin.wms;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** WMS 入库报文（托级汇总 + 箱级明细，五列重量） */
@Data
public class WmsInboundMessage {

    /** 托盘号（幂等键） */
    private String palletCode;
    /** 入库批次 */
    private String warehouseBatchNo;
    /** 生产批号 */
    private String batchNo;
    /** 缸号 */
    private String dyeVatNo;
    /** 箱数 */
    private Integer boxCount;
    /** 标重 */
    private BigDecimal standardWeight;

    // 五列重量（托级汇总）
    private BigDecimal totalGrossWeight;
    private BigDecimal totalTareWeight;
    private BigDecimal totalNetWeight;
    private BigDecimal totalPublicWeight;

    /** 箱级明细 */
    private List<BoxItem> boxes;

    @Data
    public static class BoxItem {
        private String boxCode;
        private BigDecimal grossWeight;
        private BigDecimal tareWeight;
        private BigDecimal netWeight;
        private BigDecimal publicWeight;
    }
}
