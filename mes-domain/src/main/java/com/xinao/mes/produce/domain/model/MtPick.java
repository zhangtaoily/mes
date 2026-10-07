package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 领退料确认（车间对K3/仓库推送单据的确认；领料JDSC1L/退料JDSC1T 走编码引擎） */
@Data
@TableName("mt_pick")
public class MtPick {

    public static final int TYPE_PICK = 1;
    public static final int TYPE_RETURN = 2;

    public static final String STATUS_WAIT = "待确认";
    public static final String STATUS_CONFIRMED = "已确认";

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;
    /** 批号 */
    private String batchNo;
    /** 领退料单编码:领料JDSC1L.../退料JDSC1T... */
    private String pickNo;
    /** 单据类型：1领料单 2退料单 */
    private Integer pickType;
    /** 车间 */
    private Long workshopId;
    /** 生产任务号 */
    private String taskNo;
    /** 领退料时间 */
    private LocalDateTime pickTime;
    /** 领退料人(确认后回填) */
    private String picker;
    /** 仓库 */
    private String warehouse;
    /** 库位 */
    private String location;
    /** 仓管 */
    private String warehouseKeeper;
    /** 产品编码 */
    private String productNo;
    /** 产品名称 */
    private String productName;
    /** 物料号 */
    private String materialNo;
    /** 物料名称 */
    private String materialName;
    /** 物料规格 */
    private String materialSpec;
    /** 辅助属性 */
    private String auxAttr;
    /** 产品英文名 */
    private String productEn;
    /** 色号 */
    private String colorNo;
    /** 单位 */
    private String unit;
    /** 数量 */
    private BigDecimal qty;
    /** 领退料日期 */
    private LocalDate pickDate;
    /** 单据状态：待确认/已确认 */
    private String docStatus;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
