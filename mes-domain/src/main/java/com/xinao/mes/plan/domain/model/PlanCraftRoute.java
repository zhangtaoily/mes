package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 计划工艺单-工序路线 */
@Data
@TableName("plan_craft_route")
public class PlanCraftRoute {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划工艺单ID */
    private Long planCraftId;
    /** 排序 */
    private Integer seq;
    /** 类型 */
    private Integer lineType;
    /** 工序ID */
    private Long processId;
    /** 是否投料 */
    private Integer isFeed;
    /** 是否报产 */
    private Integer isReport;
    /** 是否上下料 */
    private Integer isLoad;
    /** 线边仓 */
    private String lineSideWarehouse;
    /** 物料单位 */
    private String materialUnit;
    /** 生产单位 */
    private String prodUnit;
    /** 计划产量 */
    private BigDecimal planQty;
    /** 排程量 */
    private BigDecimal scheduledQty;
    /** 产出量 */
    private BigDecimal outputQty;
    /** 车间ID */
    private Long workshopId;
    /** 是否了批 */
    private Integer isFinished;
    /** 工序标识ID */
    private Long processFlagId;
    /** 机型ID */
    private Long equipmentId;
    /** 创建人 */
    private String createBy;
    /** 创建时间 */
    private java.time.LocalDateTime createTime;
}
