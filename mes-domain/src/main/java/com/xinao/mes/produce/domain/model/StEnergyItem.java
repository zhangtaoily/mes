package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 车间能耗-机台明细（st_energy_item，无审计列，物理删除） */
@Data
@TableName("st_energy_item")
public class StEnergyItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 能耗单ID */
    private Long energyId;
    /** 机台 */
    private Long equipmentId;
    private BigDecimal powerUsage;
    private BigDecimal waterUsage;
    private BigDecimal gasUsage;
}
