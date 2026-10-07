package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.PlanCraftProd;
import org.apache.ibatis.annotations.Mapper;

/** 计划工艺单-生产信息 mapper */
@Mapper
public interface PlanCraftProdMapper extends BaseMapper<PlanCraftProd> {
}
