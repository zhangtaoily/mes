package com.xinao.mes.plan.dto;

import com.xinao.mes.plan.domain.model.PlanCraft;
import com.xinao.mes.plan.domain.model.PlanCraftMaterial;
import com.xinao.mes.plan.domain.model.PlanCraftProd;
import com.xinao.mes.plan.domain.model.PlanCraftRawQc;
import lombok.Data;

import java.util.List;

/** 计划工艺单保存入参（主信息 + 生产信息/用料汇总/原料检验三个Tab） */
@Data
public class CraftSaveDTO {

    /** 主信息（id=null 新增，否则更新） */
    private PlanCraft craft;
    private List<PlanCraftProd> prods;
    private List<PlanCraftMaterial> materials;
    private List<PlanCraftRawQc> rawQcs;
}
