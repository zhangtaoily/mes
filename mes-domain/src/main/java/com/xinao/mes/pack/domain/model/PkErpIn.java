package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** ERP 入库记录（WMS 入库→ERP 入库链路查询；M5 K3 插件接入后自动写入） */
@Data
@TableName("pk_erp_in")
public class PkErpIn {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 入库批号 */
    private String warehouseBatchNo;
    /** 生产批号 */
    private String batchNo;
    /** 生产订单号 */
    private String k3OrderNo;
    /** 入库数量 */
    private BigDecimal inQty;
    /** 入库时间 */
    private LocalDateTime inTime;
    /** ERP同步状态 */
    private String erpStatus;
    /** 创建时间 */
    private LocalDateTime createTime;
}
