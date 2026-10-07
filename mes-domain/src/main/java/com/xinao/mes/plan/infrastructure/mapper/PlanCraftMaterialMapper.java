package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.PlanCraftMaterial;
import org.apache.ibatis.annotations.Mapper;

/** 计划工艺单-用料汇总 mapper */
@Mapper
public interface PlanCraftMaterialMapper extends BaseMapper<PlanCraftMaterial> {
}
