package com.xinao.mes.plan.vo;

import com.xinao.mes.engine.flow.vo.ButtonVO;
import com.xinao.mes.plan.domain.model.PlanCraft;
import com.xinao.mes.plan.domain.model.PlanCraftCraft;
import com.xinao.mes.plan.domain.model.PlanCraftMaterial;
import com.xinao.mes.plan.domain.model.PlanCraftProd;
import com.xinao.mes.plan.domain.model.PlanCraftRawQc;
import com.xinao.mes.plan.domain.model.PlanCraftRoute;
import lombok.Data;

import java.util.List;

/** 计划工艺单详情（7Tab 聚合视图 + 当前状态可用按钮） */
@Data
public class CraftDetailVO {

    private PlanCraft craft;
    private List<PlanCraftProd> prods;
    private List<PlanCraftMaterial> materials;
    private List<PlanCraftRawQc> rawQcs;
    private List<RouteVO> routes;
    /** 当前状态可用操作（flow 引擎按角色过滤） */
    private List<ButtonVO> buttons;

    /** 工序行（含编制的工艺项与进度） */
    @Data
    public static class RouteVO {
        private PlanCraftRoute route;
        private String processName;
        private String equipmentNo;
        private List<PlanCraftCraft> crafts;
        /** 该工序是否已编制（有工艺项） */
        private boolean crafted;
    }
}
