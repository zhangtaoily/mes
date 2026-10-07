package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.FlowCardPrintLog;
import org.apache.ibatis.annotations.Mapper;

/** 工艺流转卡打印记录 mapper */
@Mapper
public interface FlowCardPrintLogMapper extends BaseMapper<FlowCardPrintLog> {
}
