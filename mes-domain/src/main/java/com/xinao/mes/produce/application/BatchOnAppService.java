package com.xinao.mes.produce.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.event.MesEventPublisher;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.plan.domain.model.BaseBatchFlag;
import com.xinao.mes.plan.domain.model.BaseEquipment;
import com.xinao.mes.plan.domain.model.BaseProcess;
import com.xinao.mes.plan.domain.model.BaseTeam;
import com.xinao.mes.plan.domain.model.ProdMachineCard;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.infrastructure.mapper.BaseBatchFlagMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseEquipmentMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseProcessMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseTeamMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdMachineCardMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.produce.domain.event.BatchOnCreatedEvent;
import com.xinao.mes.produce.domain.model.RepBatchOn;
import com.xinao.mes.produce.domain.model.RepBatchOnCraft;
import com.xinao.mes.produce.domain.model.RepBatchOnFlag;
import com.xinao.mes.produce.dto.BatchOnQueryDTO;
import com.xinao.mes.produce.dto.BatchOnSaveDTO;
import com.xinao.mes.produce.infrastructure.mapper.RepBatchOnCraftMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepBatchOnFlagMapper;
import com.xinao.mes.produce.infrastructure.mapper.RepBatchOnMapper;
import com.xinao.mes.produce.vo.BatchOnDetailVO;
import com.xinao.mes.produce.vo.BatchOnListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 上批/下批汇报应用服务（聚合根 RepBatchOn；PDA 扫码与 PC 同源）
 *
 * <p>上批校验链：排程有效（撤回即逻辑删）→ 未撤回制造卡存在 → 同排程无活动上批
 * → 批号标识未被其他未了批批次占用（同批次多工序共用，跨批次互斥）。
 */
@Service
@RequiredArgsConstructor
public class BatchOnAppService {

    private final RepBatchOnMapper batchOnMapper;
    private final RepBatchOnFlagMapper flagMapper;
    private final RepBatchOnCraftMapper craftMapper;
    private final ProdScheduleMapper scheduleMapper;
    private final ProdTaskMapper taskMapper;
    private final ProdMachineCardMapper cardMapper;
    private final BaseProcessMapper processMapper;
    private final BaseEquipmentMapper equipmentMapper;
    private final BaseTeamMapper teamMapper;
    private final BaseBatchFlagMapper batchFlagMapper;
    private final CodeEngine codeEngine;
    private final MesEventPublisher publisher;

    public PageResult<BatchOnListVO> page(BatchOnQueryDTO query) {
        Page<RepBatchOn> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<RepBatchOn> result = batchOnMapper.selectPage(p, Wrappers.<RepBatchOn>lambdaQuery()
                .eq(query.getWorkshopId() != null, RepBatchOn::getWorkshopId, query.getWorkshopId())
                .like(StringUtils.isNotBlank(query.getBatchNo()), RepBatchOn::getBatchNo, query.getBatchNo())
                .like(StringUtils.isNotBlank(query.getBatchOnNo()), RepBatchOn::getBatchOnNo, query.getBatchOnNo())
                .eq(query.getIsBatchOff() != null, RepBatchOn::getIsBatchOff, query.getIsBatchOff())
                .orderByDesc(RepBatchOn::getId));
        List<BatchOnListVO> rows = result.getRecords().stream().map(this::toListVO).toList();
        return PageResult.of(result.getTotal(), rows);
    }

    public BatchOnDetailVO detail(Long id) {
        RepBatchOn on = require(id);
        BatchOnDetailVO vo = new BatchOnDetailVO();
        vo.setBatchOn(on);
        vo.setFlags(flagMapper.selectList(Wrappers.<RepBatchOnFlag>lambdaQuery()
                .eq(RepBatchOnFlag::getBatchOnId, id)));
        vo.setCrafts(craftMapper.selectList(Wrappers.<RepBatchOnCraft>lambdaQuery()
                .eq(RepBatchOnCraft::getBatchOnId, id)));
        vo.setScheduleNo(scheduleNo(on.getScheduleId()));
        vo.setProcessName(processName(on.getProcessId()));
        vo.setMachineNo(machineNo(on.getEquipmentId()));
        vo.setTeamName(teamName(on.getTeamId()));
        vo.setTaskNo(taskNoOf(on.getScheduleId()));
        return vo;
    }

    /** 上批（PC/PDA 同源，source 区分） */
    @Transactional
    public Long onBatch(BatchOnSaveDTO dto) {
        RepBatchOn on = dto.getBatchOn();
        if (on == null || on.getScheduleId() == null) {
            throw new BizException("请选择排程");
        }
        ProdSchedule schedule = scheduleMapper.selectById(on.getScheduleId());
        if (schedule == null) {
            throw new BizException("排程不存在或已撤回，不可上批");
        }
        // 校验0：任务须生效（了批/已关闭任务不可再上批）
        if (schedule.getTaskId() != null) {
            ProdTask task = taskMapper.selectById(schedule.getTaskId());
            if (task == null || !BillStatus.EFFECTIVE.equals(task.getDocStatus())) {
                throw new BizException("任务状态为[" + (task == null ? "未知" : task.getDocStatus()) + "]，仅生效任务可上批");
            }
        }
        // 校验1：未撤回制造卡存在（打印制造卡是上机凭证）
        ProdMachineCard card = cardMapper.selectOne(Wrappers.<ProdMachineCard>lambdaQuery()
                .eq(ProdMachineCard::getScheduleId, schedule.getId())
                .isNull(ProdMachineCard::getWithdrawTime)
                .last("LIMIT 1"));
        if (card == null) {
            throw new BizException("排程[" + schedule.getScheduleNo() + "]未打印上机制造卡，不可上批");
        }
        // 校验2：同排程无活动上批
        Long active = batchOnMapper.selectCount(Wrappers.<RepBatchOn>lambdaQuery()
                .eq(RepBatchOn::getScheduleId, schedule.getId())
                .eq(RepBatchOn::getIsBatchOff, 0));
        if (active != null && active > 0) {
            throw new BizException("排程[" + schedule.getScheduleNo() + "]已上批未下批，请先下批");
        }
        // 校验3：批号标识占用（同批次多工序共用；跨批次互斥；了批释放）
        Long flagId = on.getBatchFlagId() != null ? on.getBatchFlagId() : schedule.getBatchFlagId();
        if (flagId != null) {
            Long occupied = batchOnMapper.selectCount(Wrappers.<RepBatchOn>lambdaQuery()
                    .eq(RepBatchOn::getBatchFlagId, flagId)
                    .eq(RepBatchOn::getIsFinished, 0)
                    .ne(on.getBatchNo() != null, RepBatchOn::getBatchNo, on.getBatchNo()));
            if (occupied != null && occupied > 0) {
                throw new BizException("批号标识[" + flagName(flagId) + "]已被其他未了批批次占用，请先了批释放");
            }
        }
        // 主表字段补齐（排程为准）
        if (StringUtils.isBlank(on.getBatchNo())) {
            on.setBatchNo(schedule.getBatchNo());
        }
        if (StringUtils.isBlank(on.getTraceBatchNo())) {
            on.setTraceBatchNo(schedule.getTraceBatchNo());
        }
        if (on.getProcessId() == null) {
            on.setProcessId(schedule.getProcessId());
        }
        if (flagId != null) {
            on.setBatchFlagId(flagId);
        }
        on.setEquipmentId(schedule.getEquipmentId());
        if (on.getWorkshopId() == null) {
            on.setWorkshopId(schedule.getWorkshopId());
        }
        on.setBatchOnNo(codeEngine.next("上批汇报", on.getBatchNo()));
        on.setIsBatchOff(0);
        on.setIsFinished(0);
        if (on.getBatchOnTime() == null) {
            on.setBatchOnTime(LocalDateTime.now());
        }
        if (on.getSource() == null) {
            on.setSource(RepBatchOn.SOURCE_PC);
        }
        batchOnMapper.insert(on);

        if (dto.getFlags() != null && !dto.getFlags().isEmpty()) {
            for (RepBatchOnFlag f : dto.getFlags()) {
                f.setId(null);
                f.setBatchOnId(on.getId());
                flagMapper.insert(f);
            }
        }
        if (dto.getCrafts() != null && !dto.getCrafts().isEmpty()) {
            for (RepBatchOnCraft c : dto.getCrafts()) {
                c.setId(null);
                c.setBatchOnId(on.getId());
                craftMapper.insert(c);
            }
        }
        publisher.publish(new BatchOnCreatedEvent(on.getId(), on.getBatchOnNo(), on.getScheduleId(),
                on.getBatchNo(), on.getBatchFlagId(), on.getTraceBatchNo()));
        return on.getId();
    }

    /** 下批：活动上批 → 已下批（CAS，回填下批时间/人） */
    @Transactional
    public void offBatch(Long id) {
        RepBatchOn on = require(id);
        on.ensureOffable();
        if (batchOnMapper.update(null, Wrappers.<RepBatchOn>lambdaUpdate()
                .eq(RepBatchOn::getId, id).eq(RepBatchOn::getIsBatchOff, 0)
                .set(RepBatchOn::getIsBatchOff, 1)
                .set(RepBatchOn::getBatchOffTime, LocalDateTime.now())
                .set(RepBatchOn::getBatchOffUser, currentUser())) == 0) {
            throw new BizException("上批记录[" + on.getBatchOnNo() + "]已下批，请刷新后重试");
        }
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        batchOnMapper.deleteById(id);
        flagMapper.delete(Wrappers.<RepBatchOnFlag>lambdaQuery().eq(RepBatchOnFlag::getBatchOnId, id));
        craftMapper.delete(Wrappers.<RepBatchOnCraft>lambdaQuery().eq(RepBatchOnCraft::getBatchOnId, id));
    }

    // ---------- 内部 ----------

    private BatchOnListVO toListVO(RepBatchOn on) {
        BatchOnListVO vo = new BatchOnListVO();
        vo.setBatchOn(on);
        vo.setScheduleNo(scheduleNo(on.getScheduleId()));
        vo.setProcessName(processName(on.getProcessId()));
        vo.setMachineNo(machineNo(on.getEquipmentId()));
        vo.setTeamName(teamName(on.getTeamId()));
        vo.setFlagName(flagName(on.getBatchFlagId()));
        return vo;
    }

    private String scheduleNo(Long scheduleId) {
        if (scheduleId == null) {
            return null;
        }
        ProdSchedule s = scheduleMapper.selectById(scheduleId);
        return s == null ? null : s.getScheduleNo();
    }

    private String taskNoOf(Long scheduleId) {
        if (scheduleId == null) {
            return null;
        }
        ProdSchedule s = scheduleMapper.selectById(scheduleId);
        return s == null || s.getTaskId() == null ? null
                : String.valueOf(s.getTaskId());
    }

    private String processName(Long processId) {
        if (processId == null) {
            return null;
        }
        BaseProcess p = processMapper.selectById(processId);
        return p == null ? null : p.getProcessName();
    }

    private String machineNo(Long equipmentId) {
        if (equipmentId == null) {
            return null;
        }
        BaseEquipment e = equipmentMapper.selectById(equipmentId);
        return e == null ? null : e.getMachineNo();
    }

    private String teamName(Long teamId) {
        if (teamId == null) {
            return null;
        }
        BaseTeam t = teamMapper.selectById(teamId);
        return t == null ? null : t.getTeamName();
    }

    private String flagName(Long flagId) {
        if (flagId == null) {
            return null;
        }
        BaseBatchFlag f = batchFlagMapper.selectById(flagId);
        return f == null ? null : f.getFlagName();
    }

    private String currentUser() {
        var user = AuthContext.get();
        return user == null ? "system" : user.getUserName();
    }

    private RepBatchOn require(Long id) {
        RepBatchOn on = batchOnMapper.selectById(id);
        if (on == null) {
            throw new BizException("上批记录不存在: " + id);
        }
        return on;
    }
}
