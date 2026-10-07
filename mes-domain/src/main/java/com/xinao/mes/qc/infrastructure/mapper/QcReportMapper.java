package com.xinao.mes.qc.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.qc.domain.model.QcReport;
import org.apache.ibatis.annotations.Mapper;

/** QcReport mapper */
@Mapper
public interface QcReportMapper extends BaseMapper<QcReport> {
}
