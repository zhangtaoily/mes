package com.xinao.mes.qc.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.qc.domain.model.QcSample;
import com.xinao.mes.qc.dto.QcSampleQueryDTO;
import com.xinao.mes.qc.infrastructure.mapper.QcSampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 抽样登记应用服务（对下批批次登记抽样，检验报告的取样来源） */
@Service
@RequiredArgsConstructor
public class QcSampleAppService {

    private final QcSampleMapper sampleMapper;

    public PageResult<QcSample> page(QcSampleQueryDTO query) {
        Page<QcSample> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<QcSample> result = sampleMapper.selectPage(p, Wrappers.<QcSample>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getBatchNo()), QcSample::getBatchNo, query.getBatchNo())
                .eq(query.getWorkshopId() != null, QcSample::getWorkshopId, query.getWorkshopId())
                .eq(query.getIsRegistered() != null, QcSample::getIsRegistered, query.getIsRegistered())
                .orderByDesc(QcSample::getReportTime));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 登记（批量置已登记 + 登记时间） */
    @Transactional
    public void register(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要登记的抽样记录");
        }
        for (Long id : ids) {
            QcSample sample = sampleMapper.selectById(id);
            if (sample == null) {
                throw new BizException("抽样记录不存在: " + id);
            }
            if (Integer.valueOf(1).equals(sample.getIsRegistered())) {
                continue;
            }
            sample.setIsRegistered(1);
            sample.setRegisterTime(LocalDateTime.now());
            sampleMapper.updateById(sample);
        }
    }
}
