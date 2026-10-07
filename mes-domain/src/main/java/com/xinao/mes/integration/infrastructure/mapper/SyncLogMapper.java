package com.xinao.mes.integration.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinao.mes.integration.domain.model.SyncLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SyncLogMapper extends BaseMapper<SyncLog> {
}
