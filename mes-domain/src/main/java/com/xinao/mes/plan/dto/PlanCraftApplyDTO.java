package com.xinao.mes.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 计划工艺单-编制工艺单（工艺项引用带出）入参 */
@Data
public class PlanCraftApplyDTO {

    @NotNull(message = "工艺单不能为空")
    private Long planCraftId;
    /** 目标工序行（null=全部工序一键带出） */
    private Long routeId;
    /** 引用来源：1技术工艺库 2车间工艺库 */
    @NotNull(message = "引用来源不能为空")
    private Integer refSource;
    /** 方案id（技术库=base_craft.id / 车间库=base_craft_workshop.id） */
    @NotNull(message = "工艺方案不能为空")
    private Long schemeId;
    /** 机型（车间工艺库按 工序+机型 匹配） */
    private String machineModel;
}
