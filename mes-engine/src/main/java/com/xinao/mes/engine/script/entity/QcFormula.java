package com.xinao.mes.engine.script.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 质检公式（qc_formula，JS 脚本存 DB，编辑即生效）
 */
@Data
@TableName("qc_formula")
public class QcFormula implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String formulaName;

    private Long processId;

    /** 整精梳车间/复精梳车间/前纺/后纺 */
    private String workshopType;

    private String formulaType;

    /** 目标指标 */
    private Long targetIndicatorId;

    /** 影响的质检项(多选) */
    private String affectIndicators;

    private Integer sortNo;

    /** JS 公式脚本：var calculate = function(){ var zl = $BQ(质检项ID); ... $SET("BYL", v); return v; }; calculate(); */
    private String script;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
