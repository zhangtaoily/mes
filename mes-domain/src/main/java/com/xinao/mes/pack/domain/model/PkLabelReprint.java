package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 箱贴补打（单据编号 FG...；按任务/单据查询，含箱子编码/序号/重量组/托盘码） */
@Data
@TableName("pk_label_reprint")
public class PkLabelReprint {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单据编号 FG20260929082 */
    private String docNo;
    /** 包装任务ID（pk_task.id） */
    private Long pkTaskId;
    /** 生产批号 */
    private String batchNo;
    /** 生产订单号 */
    private String k3OrderNo;
    /** 缸号 */
    private String dyeVatNo;
    /** 入库批号 */
    private String warehouseBatchNo;
    /** 产品名称 */
    private String productName;
    /** 英文名称 */
    private String nameEn;
    /** 产品规格 */
    private String productSpec;
    /** 客户名称 */
    private String customerName;
    /** 单据日期 */
    private LocalDate docDate;
    /** 业务类型 */
    private String bizType;
    /** 原料说明 */
    private String rawDesc;
    /** 客户批号 */
    private String customerBatchNo;
    /** 确认状态 */
    private String confirmStatus;
    /** 箱子编码 */
    private String boxCode;
    /** 箱子序号 */
    private Integer boxSeq;
    /** 公重 */
    private BigDecimal publicWeight;
    /** 净重 */
    private BigDecimal netWeight;
    /** 毛重 */
    private BigDecimal grossWeight;
    /** 去皮重量 */
    private BigDecimal tareWeight;
    /** 包装规格 */
    private String packSpec;
    /** 托盘码 */
    private String palletCode;
    /** 打印时间 */
    private LocalDateTime printTime;

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
