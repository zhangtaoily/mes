package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.BaseProdLine;
import org.apache.ibatis.annotations.Mapper;

/** 车间产线 mapper */
@Mapper
public interface BaseProdLineMapper extends BaseMapper<BaseProdLine> {
}
