package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 生产任务-工艺配置（引用技术/车间工艺库带出快照） */
@Data
@TableName("prod_task_craft")
public class ProdTaskCraft {

    /** 引用来源：技术工艺库 */
    public static final int REF_TECH = 1;
    /** 引用来源：车间工艺库 */
    public static final int REF_WORKSHOP = 2;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;
    /** 工序 */
    private String processName;
    /** 工艺项 */
    private String paramName;
    /** 工艺值 */
    private String paramValue;
    /** 下限 */
    private String lowerLimit;
    /** 上限 */
    private String upperLimit;
    /** 单位 */
    private String unit;
    /** 引用来源（1技术工艺库 2车间工艺库） */
    private Integer refSource;
    /** 引用方案编号（如 TJ.../CJ...） */
    private String refSchemeNo;
}
