package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.BaseCraftParam;
import org.apache.ibatis.annotations.Mapper;

/** 工艺参数 mapper（技术/车间工艺库共用） */
@Mapper
public interface BaseCraftParamMapper extends BaseMapper<BaseCraftParam> {
}
