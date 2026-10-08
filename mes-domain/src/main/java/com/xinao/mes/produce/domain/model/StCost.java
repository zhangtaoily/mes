package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 车间生产费用（st_cost，无逻辑删除列，物理删除；唯一键 period_month+workshop_id） */
@Data
@TableName("st_cost")
public class StCost implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 期间(月) */
    private String periodMonth;
    /** 车间 */
    private Long workshopId;
    /** 用水量 */
    private BigDecimal waterUsage;
    /** 用水费用 */
    private BigDecimal waterCost;
    /** 用电量 */
    private BigDecimal powerUsage;
    /** 用电费用 */
    private BigDecimal powerCost;
    /** 机物料 */
    private BigDecimal machineMaterialCost;
    /** 包装费用 */
    private BigDecimal packCost;
    /** 月折旧 */
    private BigDecimal monthDepreciation;
    /** 加工费分摊 */
    private BigDecimal processCostShare;
    /** 工资 */
    private BigDecimal salary;
    /** 月度考核指标 */
    private BigDecimal monthKpi;
    /** 折算产量 */
    private BigDecimal convertOutput;
    /** 用毛率 */
    private BigDecimal woolRate;
    /** 编制中/已提交/审核通过/驳回 */
    private String docStatus;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
