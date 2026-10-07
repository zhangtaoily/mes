package com.xinao.mes.qc.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 质检方案（车间质检方案：方案编号/名称/车间/试样数，启用停用） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("base_qc_scheme")
public class BaseQcScheme extends BaseEntity {

    /** 方案编号 */
    private String schemeCode;
    /** 方案名称 */
    private String schemeName;
    /** 车间ID */
    private Long workshopId;
    /** 试样数（矩阵列数 1..n） */
    private Integer sampleCount;
    /** 1启用 0停用 */
    private Integer status;
}
