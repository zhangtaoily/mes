package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 包装任务（聚合根：含倒毛包装任务，task_variant 区分；了批/缸号了批为标志位批量操作） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pk_task")
public class PkTask extends BaseEntity {

    /** 任务编号 PS20260929092 */
    private String taskNo;
    /** 普通/委外/倒毛 */
    private String taskVariant;
    /** 生产批号 */
    private String batchNo;
    /** 生产订单号 */
    private String k3OrderNo;
    /** 缸号 */
    private String dyeVatNo;
    /** 入库批号 */
    private String warehouseBatchNo;
    /** 生产车间 */
    private Long workshopId;
    /** 包装车间（质检五组） */
    private Long packWorkshopId;
    /** 产品名称 */
    private String productName;
    /** 英文名称 */
    private String nameEn;
    /** 原料 */
    private String rawMaterial;
    /** 色号 */
    private String colorNo;
    /** 产品规格 1/72 NM */
    private String productSpec;
    /** 客户名称 */
    private String customerName;
    /** 销售部门 */
    private String saleDept;
    /** 销售员 */
    private String saleUser;
    /** 包装要求：内销纸箱(标准)/内胆纸箱(实重) */
    private String packRequire;
    /** 包装规格（base_pack_material） */
    private Long packSpecId;
    /** 箱单份数 */
    private Integer boxListCopies;
    /** 交期 */
    private LocalDate deliveryDate;
    /** 毛重 */
    private BigDecimal grossWeight;
    /** 打印模板 */
    private Long templateId;
    /** 交货方式：简装交货/绞纱交货 */
    private String deliveryMode;
    /** 计重方式：标重交货/实磅交货 */
    private String weightMode;
    /** 是否绞纱 */
    private Integer isHank;
    /** 是否混纤/混托 */
    private Integer isBlended;
    /** 客户订单号 */
    private String customerOrderNo;
    /** 客户批号 */
    private String customerBatchNo;
    /** 筒子数/绞数 */
    private Integer tubeCount;
    /** 纸管重量 */
    private BigDecimal paperTubeWeight;
    /** 纸箱重量 */
    private BigDecimal cartonWeight;
    /** 薄膜袋重量 */
    private BigDecimal filmBagWeight;
    /** 公定回潮% */
    private BigDecimal moistureStandard;
    /** 实测回潮% */
    private BigDecimal moistureActual;
    /** 去毛重量 */
    private BigDecimal woolRemoveWeight;
    /** 每箱重量（倒毛） */
    private BigDecimal boxWeightPer;
    /** 箱号类型（倒毛） */
    private String boxNoType;
    /** 箱型号（倒毛） */
    private String boxModel;
    /** 做前10块箱重量（倒毛） */
    private BigDecimal weightFront10;
    /** 做后10块箱重量（倒毛） */
    private BigDecimal weightBack10;
    /** 温度（倒毛） */
    private BigDecimal temperature;
    /** 每管重量（倒毛） */
    private BigDecimal perTubeWeight;
    /** 任务了批 */
    private Integer isFinished;
    /** 了批时间 */
    private LocalDateTime finishTime;
    /** 缸号了批 */
    private Integer vatFinished;
    /** 缸号了批时间 */
    private LocalDateTime vatFinishTime;
    /** 备注 */
    private String remark;
}
