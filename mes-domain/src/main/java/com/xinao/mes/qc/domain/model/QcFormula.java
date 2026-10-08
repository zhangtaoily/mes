package com.xinao.mes.qc.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 质检公式（qc_formula，无逻辑删除列，物理删除；JS引擎 $BQ 取值计算衍生指标） */
@Data
@TableName("qc_formula")
public class QcFormula implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公式名称 */
    private String formulaName;
    /** 工序 */
    private Long processId;
    /** 整精梳车间/复精梳车间/前纺/后纺 */
    private String workshopType;
    /** 公式类型 */
    private String formulaType;
    /** 目标指标（base_qc_indicator.id） */
    private Long targetIndicatorId;
    /** 影响的质检项(多选，逗号分隔) */
    private String affectIndicators;
    private Integer sortNo;
    /** JS公式脚本:var calculateBYL=function(){var zlObj=$BQ(质检项ID);...} */
    private String script;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
