package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.ProdTaskMaterial;
import org.apache.ibatis.annotations.Mapper;

/** 生产任务-用料汇总 mapper */
@Mapper
public interface ProdTaskMaterialMapper extends BaseMapper<ProdTaskMaterial> {
}
