package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.ProdTaskOrder;
import org.apache.ibatis.annotations.Mapper;

/** 生产任务-订单明细 mapper */
@Mapper
public interface ProdTaskOrderMapper extends BaseMapper<ProdTaskOrder> {
}
