package com.xinao.mes.produce.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.event.MesEventPublisher;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.produce.domain.event.OutputReportedEvent;
import com.xinao.mes.produce.domain.model.RepOutput;
import com.xinao.mes.produce.dto.OutputQueryDTO;
import com.xinao.mes.produce.infrastructure.mapper.RepOutputMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 产量汇报应用服务（逐班报产；发布事件累计任务工序/订单产出量，排程刷新实绩数据源）
 */
@Service
@RequiredArgsConstructor
public class OutputAppService {

    private final RepOutputMapper outputMapper;
    private final ProdScheduleMapper scheduleMapper;
    private final ProdTaskMapper taskMapper;
    private final CodeEngine codeEngine;
    private final MesEventPublisher publisher;

    public PageResult<RepOutput> page(OutputQueryDTO query) {
        Page<RepOutput> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<RepOutput> result = outputMapper.selectPage(p, Wrappers.<RepOutput>lambdaQuery()
                .like(StringUtils.isNotBlank(query.getBatchNo()), RepOutput::getBatchNo, query.getBatchNo())
                .like(StringUtils.isNotBlank(query.getOutputNo()), RepOutput::getOutputNo, query.getOutputNo())
                .eq(query.getTeamId() != null, RepOutput::getTeamId, query.getTeamId())
                .eq(query.getScheduleId() != null, RepOutput::getScheduleId, query.getScheduleId())
                .ge(query.getReportDateBegin() != null, RepOutput::getReportDate, query.getReportDateBegin())
                .le(query.getReportDateEnd() != null, RepOutput::getReportDate, query.getReportDateEnd())
                .orderByDesc(RepOutput::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 保存产量汇报（PC/PDA 同源），发布事件联动任务产出量 */
    @Transactional
    public Long save(RepOutput output) {
        if (output == null || output.getScheduleId() == null) {
            throw new BizException("请选择排程");
        }
        ProdSchedule schedule = scheduleMapper.selectById(output.getScheduleId());
        if (schedule == null) {
            throw new BizException("排程不存在或已撤回");
        }
        // 任务须生效（了批/已关闭任务不可再报产量，防止了批后产出量被污染）
        if (schedule.getTaskId() != null) {
            ProdTask task = taskMapper.selectById(schedule.getTaskId());
            if (task == null || !BillStatus.EFFECTIVE.equals(task.getDocStatus())) {
                throw new BizException("任务状态为[" + (task == null ? "未知" : task.getDocStatus()) + "]，仅生效任务可报产量");
            }
        }
        if (output.getOutputQty() == null || output.getOutputQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("汇报产量必须大于0");
        }
        output.setOutputNo(codeEngine.next("产量汇报", output.getBatchNo()));
        if (StringUtils.isBlank(output.getBatchNo())) {
            output.setBatchNo(schedule.getBatchNo());
        }
        output.setTaskId(schedule.getTaskId());
        if (output.getProcessId() == null) {
            output.setProcessId(schedule.getProcessId());
        }
        if (output.getEquipmentId() == null) {
            output.setEquipmentId(schedule.getEquipmentId());
        }
        if (output.getReportDate() == null) {
            output.setReportDate(LocalDate.now());
        }
        if (output.getIsWarehouse() == null) {
            output.setIsWarehouse(0);
        }
        outputMapper.insert(output);
        publisher.publish(new OutputReportedEvent(output.getId(), output.getOutputNo(),
                output.getScheduleId(), output.getTaskId(), output.getProcessId(), output.getOutputQty()));
        return output.getId();
    }

    @Transactional
    public void delete(Long id) {
        outputMapper.deleteById(id);
    }

    /** 排程关联的汇报明细（排程列表"报工回溯"数据源） */
    public List<RepOutput> listOfSchedule(Long scheduleId) {
        return outputMapper.selectList(Wrappers.<RepOutput>lambdaQuery()
                .eq(RepOutput::getScheduleId, scheduleId).orderByDesc(RepOutput::getId));
    }
}
