package com.xinao.mes.plan.dto;

import com.xinao.mes.plan.domain.model.PlanCraftCraft;
import com.xinao.mes.plan.domain.model.PlanCraftRoute;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 计划工艺单-工序路线保存入参（整表替换，seq 按列表顺序重排） */
@Data
public class RouteSaveDTO {

    @NotNull(message = "工艺单不能为空")
    private Long planCraftId;
    private List<Item> routes;

    /** 工序行 + 该行编制的工艺项 */
    @Data
    public static class Item {
        private PlanCraftRoute route;
        private List<PlanCraftCraft> crafts;
    }
}
