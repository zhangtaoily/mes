package com.xinao.mes.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 生产任务-工艺配置（引用技术/车间工艺库带出）入参 */
@Data
public class TaskCraftApplyDTO {

    @NotNull(message = "任务不能为空")
    private Long taskId;
    /** 引用来源：1技术工艺库 2车间工艺库 */
    @NotNull(message = "引用来源不能为空")
    private Integer refSource;
    /** 方案id（技术库=base_craft.id / 车间库=base_craft_workshop.id） */
    @NotNull(message = "工艺方案不能为空")
    private Long schemeId;
    /** 当前工序（allProcesses=false 时必填） */
    private Long processId;
    /** 机型（车间工艺库按 工序+机型 匹配） */
    private String machineModel;
    /** true=全部工序批量引用 */
    private boolean allProcesses;
}
