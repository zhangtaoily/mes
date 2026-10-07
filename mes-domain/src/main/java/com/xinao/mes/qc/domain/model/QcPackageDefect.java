package com.xinao.mes.qc.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 包装质检-疵纱明细（正纱/AA BB纱/尾纱/辫子纱/松捻纱/紧捻纱/重纱/轻纱/断纱/多股纱/油纱/大小肚/乱纱/圈圈纱/其它） */
@Data
@TableName("qc_package_defect")
public class QcPackageDefect {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 包装质检报告ID（qc_package_report 外键） */
    private Long packageQcId;
    /** 疵纱分类 */
    private String defectType;
    /** 指标值 */
    private BigDecimal value;
    /** 备注 */
    private String remark;
}
