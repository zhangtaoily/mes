package com.xinao.mes.qc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.qc.domain.model.BaseQcIndicator;
import org.apache.ibatis.annotations.Mapper;

/** 质检指标 mapper */
@Mapper
public interface BaseQcIndicatorMapper extends BaseMapper<BaseQcIndicator> {
}
