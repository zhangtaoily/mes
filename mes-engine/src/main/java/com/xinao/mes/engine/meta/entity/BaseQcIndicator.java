package com.xinao.mes.engine.meta.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质检指标（base_qc_indicator，meta 动态列的纵表数据源）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_qc_indicator")
public class BaseQcIndicator extends BaseEntity {

    private String indicatorCode;

    /** 每米重量/含油/回潮/长度/离散/短毛率/疵点/不匀率/强力 等 */
    private String indicatorName;

    private String unit;

    /** 1原始 2衍生(公式计算) */
    private Integer indicatorType;

    /** 衍生指标关联公式 */
    private Long formulaId;

    /** 1启用 0停用 */
    private Integer status;
}
