package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.BaseEquipment;
import org.apache.ibatis.annotations.Mapper;

/** 设备（机台） mapper */
@Mapper
public interface BaseEquipmentMapper extends BaseMapper<BaseEquipment> {
}
