package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import org.apache.ibatis.annotations.Mapper;

/** 生产排程 mapper */
@Mapper
public interface ProdScheduleMapper extends BaseMapper<ProdSchedule> {
}
