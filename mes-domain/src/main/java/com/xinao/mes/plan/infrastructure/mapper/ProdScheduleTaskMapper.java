package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.ProdScheduleTask;
import org.apache.ibatis.annotations.Mapper;

/** 排程-任务明细 mapper */
@Mapper
public interface ProdScheduleTaskMapper extends BaseMapper<ProdScheduleTask> {
}
