package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 包装执行（聚合根：pk_box 箱子明细逐箱称重；汇总量随箱子增删实时重算） */
@Data
@TableName("pk_execute")
public class PkExecute {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 包装编号 PC2606929174 */
    private String executeNo;
    /** 包装任务ID（pk_task.id） */
    private Long pkTaskId;
    /** 包装批号 XJP2610456-12 */
    private String packBatchNo;
    /** 包装班组 */
    private Long teamId;
    /** 入库批号 */
    private String warehouseBatchNo;
    /** 缸号 */
    private String dyeVatNo;
    /** 包装名称 */
    private String packName;
    /** 产品名称 */
    private String productName;
    /** 英文名称 */
    private String nameEn;
    /** 原料 */
    private String rawMaterial;
    /** 色号 */
    private String colorNo;
    /** 产品规格 */
    private String productSpec;
    /** 包装要求 */
    private String packRequire;
    /** 筒子数 */
    private Integer tubeCount;
    /** 包装重量 */
    private BigDecimal packWeight;
    /** 标重 */
    private BigDecimal standardWeight;
    /** 公定回潮 */
    private BigDecimal moistureStandard;
    /** 实测回潮 */
    private BigDecimal moistureActual;
    /** 是否了包 */
    private Integer isPacked;
    /** 公重总和 */
    private BigDecimal sumPublicWeight;
    /** 毛重总和 */
    private BigDecimal sumGrossWeight;
    /** 净重总和 */
    private BigDecimal sumNetWeight;
    /** 筒子数总和 */
    private Integer sumTube;
    /** 包装箱数 */
    private Integer boxCount;

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新者 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
