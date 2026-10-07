package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.BaseProcess;
import org.apache.ibatis.annotations.Mapper;

/** 生产工序 mapper */
@Mapper
public interface BaseProcessMapper extends BaseMapper<BaseProcess> {
}
