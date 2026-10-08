package com.xinao.mes.plan.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.plan.domain.model.BasePrintTemplate;
import org.apache.ibatis.annotations.Mapper;

/** 打印样式模板 mapper */
@Mapper
public interface BasePrintTemplateMapper extends BaseMapper<BasePrintTemplate> {
}
