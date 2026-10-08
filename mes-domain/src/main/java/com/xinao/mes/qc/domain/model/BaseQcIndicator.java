package com.xinao.mes.qc.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 质检指标（每米重量/含油/回潮/不匀率等；衍生指标关联质检公式） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_qc_indicator")
public class BaseQcIndicator extends BaseEntity {

    /** 指标编码 */
    private String indicatorCode;
    /** 指标名称：每米重量/含油/回潮/长度/离散/短毛率/疵点/不匀率/毛粒50克/强力/毛羽 */
    private String indicatorName;
    /** 单位 */
    private String unit;
    /** 指标类型：1原始 2衍生（公式计算） */
    private Integer indicatorType;
    /** 衍生指标关联公式 */
    private Long formulaId;
    /** 状态：1启用 0停用 */
    private Integer status;
}
