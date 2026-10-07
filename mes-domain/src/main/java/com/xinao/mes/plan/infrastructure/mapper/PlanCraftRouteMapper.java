package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.PlanCraftRoute;
import org.apache.ibatis.annotations.Mapper;

/** 计划工艺单-工序路线 mapper */
@Mapper
public interface PlanCraftRouteMapper extends BaseMapper<PlanCraftRoute> {
}
