package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.ProdScheduleMaterial;
import org.apache.ibatis.annotations.Mapper;

/** 排程-用料汇总 mapper */
@Mapper
public interface ProdScheduleMaterialMapper extends BaseMapper<ProdScheduleMaterial> {
}
