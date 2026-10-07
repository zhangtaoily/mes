package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** WMS 入库记录（包装→组托→WMS 入库链路查询；M5 插件接入后自动写入） */
@Data
@TableName("pk_wms_in")
public class PkWmsIn {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 入库批次 */
    private String warehouseBatchNo;
    /** 生产批号 */
    private String batchNo;
    /** 缸号 */
    private String dyeVatNo;
    /** 箱子编码 */
    private String boxCode;
    /** 托盘码 */
    private String palletCode;
    /** 公重 */
    private BigDecimal publicWeight;
    /** 毛重 */
    private BigDecimal grossWeight;
    /** 去皮重量 */
    private BigDecimal tareWeight;
    /** 净重 */
    private BigDecimal netWeight;
    /** 入库时间 */
    private LocalDateTime inTime;
    /** 同步状态 */
    private String wmsStatus;
    /** 创建时间 */
    private LocalDateTime createTime;
}
