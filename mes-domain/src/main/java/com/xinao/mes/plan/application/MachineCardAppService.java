package com.xinao.mes.plan.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.event.MesEventPublisher;
import com.xinao.mes.plan.domain.event.MachineCardWithdrawnEvent;
import com.xinao.mes.plan.domain.model.BaseEquipment;
import com.xinao.mes.plan.domain.model.BaseProcess;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.domain.model.FlowCardPrintLog;
import com.xinao.mes.plan.domain.model.ProdMachineCard;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdScheduleCraft;
import com.xinao.mes.plan.domain.model.ProdScheduleMaterial;
import com.xinao.mes.plan.domain.model.ProdScheduleTask;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.dto.CardQueryDTO;
import com.xinao.mes.plan.infrastructure.mapper.BaseEquipmentMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseProcessMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import com.xinao.mes.plan.infrastructure.mapper.FlowCardPrintLogMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdMachineCardMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleTaskMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.plan.vo.CardDetailVO;
import com.xinao.mes.plan.vo.CardListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 上机制造卡应用服务（A4 打印 + 二维码 + 补打留痕 + 撤回释放排程）
 *
 * <p>撤回语义（PRD 3.6）：收回已下发机台的制造卡，对应排程恢复可重排状态；
 * 撤回 = card 记录撤回时间 + 排程逻辑删 + 任务明细删除（释放占用）。
 */
@Service
@RequiredArgsConstructor
public class MachineCardAppService {

    private final ProdMachineCardMapper cardMapper;
    private final ProdScheduleMapper scheduleMapper;
    private final ProdScheduleCraftMapper scheduleCraftMapper;
    private final ProdScheduleMaterialMapper scheduleMaterialMapper;
    private final ProdScheduleTaskMapper scheduleTaskMapper;
    private final ProdTaskMapper taskMapper;
    private final FlowCardPrintLogMapper printLogMapper;
    private final BaseWorkshopMapper workshopMapper;
    private final BaseProcessMapper processMapper;
    private final BaseEquipmentMapper equipmentMapper;
    private final CodeEngine codeEngine;
    private final MesEventPublisher publisher;

    public PageResult<CardListVO> page(CardQueryDTO query) {
        Page<ProdMachineCard> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<ProdMachineCard> result = cardMapper.selectPage(p, Wrappers.<ProdMachineCard>lambdaQuery()
                .eq(StringUtils.isNotBlank(query.getStatus()), ProdMachineCard::getStatus, query.getStatus())
                .like(StringUtils.isNotBlank(query.getBatchNo()), ProdMachineCard::getBatchNo, query.getBatchNo())
                .like(StringUtils.isNotBlank(query.getCardNo()), ProdMachineCard::getCardNo, query.getCardNo())
                .orderByDesc(ProdMachineCard::getId));
        List<CardListVO> rows = result.getRecords().stream().map(this::toListVO).toList();
        return PageResult.of(result.getTotal(), rows);
    }

    public CardDetailVO detail(Long id) {
        ProdMachineCard card = requireCard(id);
        CardDetailVO vo = toDetailVO(card);
        ProdSchedule schedule = scheduleMapper.selectById(card.getScheduleId());
        if (schedule != null) {
            vo.setScheduleNo(schedule.getScheduleNo());
            vo.setWorkOrderNo(schedule.getWorkOrderNo());
        }
        vo.setPrintCount(printCount(id));
        return vo;
    }

    /** 打印上机制造卡：勾选排程 → 生成制造卡（快照车速/定长/定量/张力/并合根数）并发送打印 */
    @Transactional
    public List<Long> generate(List<Long> scheduleIds) {
        List<Long> cardIds = new ArrayList<>();
        for (Long scheduleId : scheduleIds) {
            ProdSchedule schedule = scheduleMapper.selectById(scheduleId);
            if (schedule == null) {
                throw new BizException("生产排程不存在或已撤回: " + scheduleId);
            }
            Long exist = cardMapper.selectCount(Wrappers.<ProdMachineCard>lambdaQuery()
                    .eq(ProdMachineCard::getScheduleId, scheduleId).isNull(ProdMachineCard::getWithdrawTime));
            if (exist != null && exist > 0) {
                continue; // 已生成过有效制造卡，跳过（勾选多条批量打印场景）
            }
            ProdTask task = taskMapper.selectById(schedule.getTaskId());
            List<ProdScheduleCraft> crafts = scheduleCraftMapper.selectList(
                    Wrappers.<ProdScheduleCraft>lambdaQuery()
                            .eq(ProdScheduleCraft::getScheduleId, scheduleId)
                            .orderByAsc(ProdScheduleCraft::getSortNo));

            ProdMachineCard card = new ProdMachineCard();
            card.setCardNo(codeEngine.next("上机制造卡", schedule.getScheduleNo()));
            card.setScheduleId(schedule.getId());
            card.setTaskId(schedule.getTaskId());
            card.setBatchNo(schedule.getBatchNo());
            card.setBatchFlagId(schedule.getBatchFlagId());
            card.setWorkshopId(schedule.getWorkshopId());
            card.setProcessId(schedule.getProcessId());
            card.setMachineModel(schedule.getMachineModel());
            card.setEquipmentId(schedule.getEquipmentId());
            card.setStatus(ProdMachineCard.STATUS_DOING);
            if (task != null) {
                card.setCountSpec(task.getActualCount());
                card.setProductSpec(task.getProductSpec());
                card.setRawMaterial(task.getRawMaterial());
            }
            card.setSpeed(paramValue(crafts, "车速"));
            card.setFixedLength(paramValue(crafts, "定长"));
            card.setQuota(paramValue(crafts, "定量"));
            card.setTension(paramValue(crafts, "张力"));
            card.setMergeCount(paramValue(crafts, "并合根数"));
            card.setCraftDesc(crafts.stream()
                    .map(c -> c.getParamName() + "=" + c.getParamValue())
                    .collect(Collectors.joining("; ")));
            card.setQrCode(card.getCardNo());
            card.setPrintTime(LocalDateTime.now());
            cardMapper.insert(card);
            printLog(card, 1);
            cardIds.add(card.getId());
        }
        return cardIds;
    }

    /** 补打：更新打印时间并留痕，返回累计打印次数 */
    @Transactional
    public int print(Long cardId) {
        ProdMachineCard card = requireCard(cardId);
        if (card.getWithdrawTime() != null) {
            throw new BizException("制造卡[" + card.getCardNo() + "]已撤回，不可打印");
        }
        int count = printCount(cardId) + 1;
        card.setPrintTime(LocalDateTime.now());
        cardMapper.updateById(card);
        printLog(card, count);
        return count;
    }

    /** 撤回：制造卡记录撤回时间 + 对应排程逻辑删 + 任务明细删除（释放占用，可重排） */
    @Transactional
    public void withdraw(Long cardId) {
        ProdMachineCard card = requireCard(cardId);
        card.ensureWithdrawable();
        card.setWithdrawTime(LocalDateTime.now());
        cardMapper.updateById(card);
        Long scheduleId = card.getScheduleId();
        if (scheduleId != null && scheduleMapper.selectById(scheduleId) != null) {
            scheduleMapper.deleteById(scheduleId);
            scheduleCraftMapper.delete(Wrappers.<ProdScheduleCraft>lambdaQuery()
                    .eq(ProdScheduleCraft::getScheduleId, scheduleId));
            scheduleMaterialMapper.delete(Wrappers.<ProdScheduleMaterial>lambdaQuery()
                    .eq(ProdScheduleMaterial::getScheduleId, scheduleId));
            scheduleTaskMapper.delete(Wrappers.<ProdScheduleTask>lambdaQuery()
                    .eq(ProdScheduleTask::getScheduleId, scheduleId));
        }
        publisher.publish(new MachineCardWithdrawnEvent(cardId, card.getCardNo(), scheduleId));
    }

    // ---------- 内部 ----------

    private String paramValue(List<ProdScheduleCraft> crafts, String paramName) {
        return crafts.stream().filter(c -> paramName.equals(c.getParamName())).findFirst()
                .map(ProdScheduleCraft::getParamValue).orElse(null);
    }

    private void printLog(ProdMachineCard card, int printCount) {
        FlowCardPrintLog log = new FlowCardPrintLog();
        log.setSourceId(card.getId());
        log.setSourceTable("prod_machine_card");
        log.setPrintTime(LocalDateTime.now());
        var user = AuthContext.get();
        log.setPrinter(user == null ? "system" : user.getUserName());
        log.setPrintCount(printCount);
        printLogMapper.insert(log);
    }

    private int printCount(Long cardId) {
        Long cnt = printLogMapper.selectCount(Wrappers.<FlowCardPrintLog>lambdaQuery()
                .eq(FlowCardPrintLog::getSourceTable, "prod_machine_card")
                .eq(FlowCardPrintLog::getSourceId, cardId));
        return cnt == null ? 0 : cnt.intValue();
    }

    private CardListVO toListVO(ProdMachineCard card) {
        CardListVO vo = new CardListVO();
        vo.setCard(card);
        vo.setWorkshopName(nameOfWorkshop(card.getWorkshopId()));
        vo.setProcessName(nameOfProcess(card.getProcessId()));
        BaseEquipment eq = card.getEquipmentId() == null ? null : equipmentMapper.selectById(card.getEquipmentId());
        vo.setMachineNo(eq == null ? null : eq.getMachineNo());
        ProdSchedule schedule = card.getScheduleId() == null ? null : scheduleMapper.selectById(card.getScheduleId());
        vo.setScheduleNo(schedule == null ? null : schedule.getScheduleNo());
        return vo;
    }

    private CardDetailVO toDetailVO(ProdMachineCard card) {
        CardDetailVO vo = new CardDetailVO();
        vo.setCard(card);
        vo.setWorkshopName(nameOfWorkshop(card.getWorkshopId()));
        vo.setProcessName(nameOfProcess(card.getProcessId()));
        BaseEquipment eq = card.getEquipmentId() == null ? null : equipmentMapper.selectById(card.getEquipmentId());
        vo.setMachineNo(eq == null ? null : eq.getMachineNo());
        return vo;
    }

    private String nameOfWorkshop(Long workshopId) {
        if (workshopId == null) {
            return null;
        }
        BaseWorkshop w = workshopMapper.selectById(workshopId);
        return w == null ? null : w.getWorkshopName();
    }

    private String nameOfProcess(Long processId) {
        if (processId == null) {
            return null;
        }
        BaseProcess p = processMapper.selectById(processId);
        return p == null ? null : p.getProcessName();
    }

    private ProdMachineCard requireCard(Long id) {
        ProdMachineCard card = cardMapper.selectById(id);
        if (card == null) {
            throw new BizException("制造卡不存在: " + id);
        }
        return card;
    }
}
