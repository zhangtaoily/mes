package com.xinao.mes.pack.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 包装箱子明细（逐箱称重）
 *
 * <p>M4 不变量——五列重量恒等式：毛重 = 去皮重量 + 净重（容差 0.01）；
 * 净→公重按回潮公式（qc_formula 外置脚本：公重 = 净重×(1+公定回潮)/(1+实测回潮)）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pk_box")
public class PkBox extends BaseEntity {

    /** 包装执行单ID（pk_execute.id） */
    private Long pkExecuteId;
    /** 箱子编码 */
    private String boxCode;
    /** 箱子状态 */
    private String boxStatus;
    /** 是否生成库存通知 */
    private Integer isStockNotice;
    /** 正序/箱序号 */
    private Integer seqNo;
    /** 颗数 */
    private Integer grainCount;
    /** 公重（回潮公式换算） */
    private BigDecimal publicWeight;
    /** 净重 */
    private BigDecimal netWeight;
    /** 毛重 */
    private BigDecimal grossWeight;
    /** 去皮重量（纸管+纸箱+薄膜袋） */
    private BigDecimal tareWeight;
    /** 包装规格 */
    private String packSpec;
    /** 扎绞/线颜色 */
    private String tieLineColor;
    /** 托盘码 */
    private String palletCode;
    /** 自动线/手动线 */
    private String lineType;
    /** 一体机编码 */
    private String machineCode;
    /** 上机/装箱人员 */
    private String packUser;
    /** 装箱时间 */
    private LocalDateTime boxTime;
    /** 实际筒数 */
    private Integer actualTube;
}
