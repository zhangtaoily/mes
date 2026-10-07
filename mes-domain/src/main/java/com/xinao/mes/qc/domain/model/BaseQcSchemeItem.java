package com.xinao.mes.qc.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 质检方案-指标项（标准值/上限/下限，检验报告选方案后快照生成指标矩阵） */
@Data
@TableName("base_qc_scheme_item")
public class BaseQcSchemeItem {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 质检方案ID（base_qc_scheme 外键，方案矩阵行=指标项） */
    private Long schemeId;
    /** 指标ID（base_qc_indicator 外键） */
    private Long indicatorId;
    /** 标准值 */
    private String standardValue;
    /** 下限 */
    private String lowerLimit;
    /** 上限 */
    private String upperLimit;
    /** 排序号 */
    private Integer sortNo;
}
