package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 车间能耗（st_energy，无逻辑删除列，物理删除；唯一键 period_month+workshop_id） */
@Data
@TableName("st_energy")
public class StEnergy implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 期间(月) */
    private String periodMonth;
    /** 车间 */
    private Long workshopId;
    /** 用水(吨) */
    private BigDecimal waterUsage;
    /** 用电(KWh) */
    private BigDecimal powerUsage;
    /** 用气(m³) */
    private BigDecimal gasUsage;
    /** 编制中/已提交/审核通过/已撤回 */
    private String docStatus;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
