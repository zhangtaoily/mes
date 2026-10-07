package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 计划工艺单-工艺项（编制工艺单结果，从技术/车间工艺库带出快照） */
@Data
@TableName("plan_craft_craft")
public class PlanCraftCraft {

    /** 引用来源：技术工艺库 */
    public static final int REF_TECH = 1;
    /** 引用来源：车间工艺库 */
    public static final int REF_WORKSHOP = 2;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划工艺单ID */
    private Long planCraftId;
    /** 工序路线ID */
    private Long routeId;
    /** 工序ID */
    private Long processId;
    /** 工序名称 */
    private String processName;
    /** 工艺参数名称 */
    private String paramName;
    /** 工艺参数值 */
    private String paramValue;
    /** 参数下限 */
    private String lowerLimit;
    /** 参数上限 */
    private String upperLimit;
    /** 单位 */
    private String unit;
    /** 引用来源(1技术工艺库/2车间工艺库) */
    private Integer refSource;
    /** 引用工艺方案号 */
    private String refSchemeNo;
    /** 排序号 */
    private Integer sortNo;
    /** 创建人 */
    private String createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
}
