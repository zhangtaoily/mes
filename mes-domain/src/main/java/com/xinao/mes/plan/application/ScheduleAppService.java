package com.xinao.mes.plan.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.event.MesEventPublisher;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.plan.domain.event.ScheduleGeneratedEvent;
import com.xinao.mes.plan.domain.model.BaseEquipment;
import com.xinao.mes.plan.domain.model.BaseProcess;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.domain.model.PlanCraft;
import com.xinao.mes.plan.domain.model.PlanCraftCraft;
import com.xinao.mes.plan.domain.model.PlanCraftMaterial;
import com.xinao.mes.plan.domain.model.PlanCraftRoute;
import com.xinao.mes.plan.domain.model.ProdMachineCard;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdScheduleCraft;
import com.xinao.mes.plan.domain.model.ProdScheduleMaterial;
import com.xinao.mes.plan.domain.model.ProdScheduleTask;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.dto.ScheduleQueryDTO;
import com.xinao.mes.plan.dto.ScheduleSaveDTO;
import com.xinao.mes.plan.dto.ScheduleTaskAddDTO;
import com.xinao.mes.plan.infrastructure.mapper.BaseEquipmentMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseProcessMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import com.xinao.mes.plan.infrastructure.mapper.FlowCardPrintLogMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftRouteMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdMachineCardMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleTaskMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskOrderMapper;
import com.xinao.mes.plan.vo.ScheduleDetailVO;
import com.xinao.mes.plan.vo.ScheduleListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生产排程应用服务（聚合根 ProdSchedule）
 *
 * <p>来源三条：工艺单「生成排程」（按工序路线逐工序自动生成，自动分配机台，末道工序占用任务产量）、
 * 「合并排程」（按批号+车间分组合并）、排程编辑页手工新增。
 * 任务明细排程量不变量：≤ 任务计划产量 - 未删排程已占用量。
 */
@Service
@RequiredArgsConstructor
public class ScheduleAppService {

    private final ProdScheduleMapper scheduleMapper;
    private final ProdScheduleCraftMapper scheduleCraftMapper;
    private final ProdScheduleMaterialMapper scheduleMaterialMapper;
    private final ProdScheduleTaskMapper scheduleTaskMapper;
    private final ProdMachineCardMapper cardMapper;
    private final FlowCardPrintLogMapper printLogMapper;
    private final PlanCraftMapper craftMapper;
    private final PlanCraftRouteMapper craftRouteMapper;
    private final PlanCraftCraftMapper craftItemMapper;
    private final PlanCraftMaterialMapper craftMaterialMapper;
    private final ProdTaskMapper taskMapper;
    private final ProdTaskOrderMapper taskOrderMapper;
    private final BaseEquipmentMapper equipmentMapper;
    private final BaseProcessMapper processMapper;
    private final BaseWorkshopMapper workshopMapper;
    private final CodeEngine codeEngine;
    private final MesEventPublisher publisher;
    private final ProdTaskAppService prodTaskAppService;

    public PageResult<ScheduleListVO> page(ScheduleQueryDTO query) {
        Page<ProdSchedule> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<ProdSchedule> result = scheduleMapper.selectPage(p, Wrappers.<ProdSchedule>lambdaQuery()
                .eq(query.getScheduleDate() != null, ProdSchedule::getScheduleDate, query.getScheduleDate())
                .eq(query.getWorkshopId() != null, ProdSchedule::getWorkshopId, query.getWorkshopId())
                .like(StringUtils.isNotBlank(query.getBatchNo()), ProdSchedule::getBatchNo, query.getBatchNo())
                .eq(query.getProcessId() != null, ProdSchedule::getProcessId, query.getProcessId())
                .orderByDesc(ProdSchedule::getId));
        List<ScheduleListVO> rows = result.getRecords().stream().map(this::toListVO).toList();
        return PageResult.of(result.getTotal(), rows);
    }

    public ScheduleDetailVO detail(Long id) {
        ProdSchedule schedule = requireSchedule(id);
        ScheduleDetailVO vo = new ScheduleDetailVO();
        vo.setSchedule(schedule);
        vo.setWorkshopName(nameOfWorkshop(schedule.getWorkshopId()));
        vo.setProcessName(nameOfProcess(schedule.getProcessId()));
        BaseEquipment eq = schedule.getEquipmentId() == null ? null : equipmentMapper.selectById(schedule.getEquipmentId());
        vo.setMachineNo(eq == null ? null : eq.getMachineNo());
        vo.setCrafts(scheduleCraftMapper.selectList(Wrappers.<ProdScheduleCraft>lambdaQuery()
                .eq(ProdScheduleCraft::getScheduleId, id).orderByAsc(ProdScheduleCraft::getSortNo)));
        vo.setMaterials(scheduleMaterialMapper.selectList(Wrappers.<ProdScheduleMaterial>lambdaQuery()
                .eq(ProdScheduleMaterial::getScheduleId, id)));
        List<ProdScheduleTask> tasks = scheduleTaskMapper.selectList(Wrappers.<ProdScheduleTask>lambdaQuery()
                .eq(ProdScheduleTask::getScheduleId, id));
        vo.setTasks(tasks.stream().map(t -> {
            ScheduleDetailVO.TaskItemVO item = new ScheduleDetailVO.TaskItemVO();
            item.setId(t.getId());
            item.setTaskId(t.getTaskId());
            ProdTask task = taskMapper.selectById(t.getTaskId());
            item.setTaskNo(task == null ? null : task.getTaskNo());
            item.setProductNo(t.getProductNo());
            item.setProductName(t.getProductName());
            item.setSpec(t.getSpec());
            item.setAuxAttr(t.getAuxAttr());
            item.setRawDetail(t.getRawDetail());
            item.setProcessName(t.getProcessName());
            item.setPlanQty(t.getPlanQty());
            item.setScheduledQty(t.getScheduledQty());
            item.setScheduleQty(t.getScheduleQty());
            item.setRemaining(task == null ? null
                    : (task.getPlanQty() == null ? BigDecimal.ZERO : task.getPlanQty())
                            .subtract(prodTaskAppService.occupiedQty(t.getTaskId())));
            item.setUnit(t.getUnit());
            return item;
        }).toList());
        return vo;
    }

    /** 手工新增/编辑排程 */
    @Transactional
    public Long save(ScheduleSaveDTO dto) {
        ProdSchedule schedule = dto.getSchedule();
        if (schedule == null) {
            throw new BizException("排程主信息不能为空");
        }
        if (schedule.getId() == null) {
            if (StringUtils.isBlank(schedule.getScheduleNo())) {
                schedule.setScheduleNo(codeEngine.next("生产排程", schedule.getBatchNo()));
            }
            if (StringUtils.isBlank(schedule.getWorkOrderNo())) {
                schedule.setWorkOrderNo(codeEngine.next("工单单号", schedule.getBatchNo()));
            }
            if (schedule.getScheduleDate() == null) {
                schedule.setScheduleDate(LocalDate.now());
            }
            scheduleMapper.insert(schedule);
        } else {
            requireSchedule(schedule.getId());
            scheduleMapper.updateById(schedule);
        }
        Long id = schedule.getId();
        scheduleCraftMapper.delete(Wrappers.<ProdScheduleCraft>lambdaQuery().eq(ProdScheduleCraft::getScheduleId, id));
        scheduleMaterialMapper.delete(Wrappers.<ProdScheduleMaterial>lambdaQuery().eq(ProdScheduleMaterial::getScheduleId, id));
        if (dto.getCrafts() != null) {
            int sort = 0;
            for (ProdScheduleCraft c : dto.getCrafts()) {
                sort++;
                c.setId(null);
                c.setScheduleId(id);
                c.setSortNo(sort);
                scheduleCraftMapper.insert(c);
            }
        }
        if (dto.getMaterials() != null) {
            for (ProdScheduleMaterial m : dto.getMaterials()) {
                m.setId(null);
                m.setScheduleId(id);
                scheduleMaterialMapper.insert(m);
            }
        }
        return id;
    }

    /** 删除排程（有未撤回制造卡不可删） */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            requireSchedule(id);
            Long cards = cardMapper.selectCount(Wrappers.<ProdMachineCard>lambdaQuery()
                    .eq(ProdMachineCard::getScheduleId, id).isNull(ProdMachineCard::getWithdrawTime));
            if (cards != null && cards > 0) {
                throw new BizException("排程已打印制造卡，请先撤回制造卡后再删除");
            }
            scheduleMapper.deleteById(id);
            scheduleCraftMapper.delete(Wrappers.<ProdScheduleCraft>lambdaQuery().eq(ProdScheduleCraft::getScheduleId, id));
            scheduleMaterialMapper.delete(Wrappers.<ProdScheduleMaterial>lambdaQuery().eq(ProdScheduleMaterial::getScheduleId, id));
            scheduleTaskMapper.delete(Wrappers.<ProdScheduleTask>lambdaQuery().eq(ProdScheduleTask::getScheduleId, id));
        }
    }

    /**
     * 工艺单「生成排程」：按工序路线逐工序自动生成（自动分配机台/序号/计划时间），
     * 末道工序排程插入任务明细占用任务产量；重复生成拦截（撤回制造卡后可重排）。
     */
    @Transactional
    public Map<String, Object> generateFromCraft(Long craftId) {
        PlanCraft craft = craftMapper.selectById(craftId);
        if (craft == null) {
            throw new BizException("计划工艺单不存在: " + craftId);
        }
        List<PlanCraftRoute> routes = craftRouteMapper.selectList(Wrappers.<PlanCraftRoute>lambdaQuery()
                .eq(PlanCraftRoute::getPlanCraftId, craftId).orderByAsc(PlanCraftRoute::getSeq));
        craft.ensureCanSchedule(routes);
        ProdTask task = requireLinkedTask(craftId);
        Long exist = scheduleMapper.selectCount(Wrappers.<ProdSchedule>lambdaQuery()
                .eq(ProdSchedule::getTaskId, task.getId()));
        if (exist != null && exist > 0) {
            throw new BizException("工艺单[" + craft.getCraftNo() + "]已生成过排程，请先撤回制造卡/删除排程后再重新生成");
        }
        String workOrderNo = codeEngine.next("工单单号", craft.getBatchNo());
        List<Long> scheduleIds = new ArrayList<>();
        PlanCraftRoute last = routes.get(routes.size() - 1);
        for (PlanCraftRoute route : routes) {
            ProdSchedule s = buildSchedule(craft, route, task, workOrderNo);
            scheduleMapper.insert(s);
            copyCrafts(route, s);
            scheduleIds.add(s.getId());
            if (route.getId().equals(last.getId())) {
                insertTaskOccupation(s, task, s.getScheduleQty(), route.getProcessId());
            }
            publisher.publish(new ScheduleGeneratedEvent(s.getId(), s.getScheduleNo(), task.getId(), s.getBatchNo()));
        }
        copyMaterials(craft, scheduleIds);
        return Map.of("workOrderNo", workOrderNo, "scheduleIds", scheduleIds, "count", scheduleIds.size());
    }

    /**
     * 「合并排程」：勾选≥2张工艺单，按批号+车间分组合并投料自动生成；
     * 编制中告警跳过；工序合并计划量；末道工序按各任务分别占用。
     */
    @Transactional
    public Map<String, Object> mergeFromCrafts(List<Long> craftIds) {
        if (craftIds == null || craftIds.size() < 2) {
            throw new BizException("合并排程需勾选≥2张工艺单");
        }
        List<PlanCraft> crafts = craftMapper.selectBatchIds(craftIds);
        List<String> warnings = new ArrayList<>();
        List<PlanCraft> usable = new ArrayList<>();
        for (PlanCraft c : crafts) {
            if (BillStatus.DRAFT.equals(c.getDocStatus())) {
                warnings.add("工艺单[" + c.getCraftNo() + "]为编制中，已跳过");
            } else if (BillStatus.VOIDED.equals(c.getDocStatus()) || BillStatus.CLOSED.equals(c.getDocStatus())) {
                warnings.add("工艺单[" + c.getCraftNo() + "]状态为" + c.getDocStatus() + "，已跳过");
            } else {
                usable.add(c);
            }
        }
        Map<String, List<PlanCraft>> groups = usable.stream()
                .collect(Collectors.groupingBy(c -> c.getBatchNo() + "|" + c.getWorkshopId(), LinkedHashMap::new, Collectors.toList()));
        List<String> workOrderNos = new ArrayList<>();
        int count = 0;
        for (List<PlanCraft> group : groups.values()) {
            if (group.size() < 2) {
                warnings.add("批号[" + group.get(0).getBatchNo() + "]可用工艺单仅1张，无需合并，已跳过");
                continue;
            }
            // 工序合并：processId → 合并计划量 / 最小seq（保持工序链顺序）
            Map<Long, BigDecimal> qtyByProcess = new LinkedHashMap<>();
            Map<Long, Integer> seqByProcess = new LinkedHashMap<>();
            Map<Long, PlanCraftRoute> routeByProcess = new LinkedHashMap<>();
            Map<Long, ProdTask> taskByCraft = new LinkedHashMap<>();
            Map<Long, BigDecimal> lastQtyByTask = new LinkedHashMap<>();
            for (PlanCraft c : group) {
                List<PlanCraftRoute> routes = craftRouteMapper.selectList(Wrappers.<PlanCraftRoute>lambdaQuery()
                        .eq(PlanCraftRoute::getPlanCraftId, c.getId()).orderByAsc(PlanCraftRoute::getSeq));
                c.ensureRouteComplete(routes);
                ProdTask task = requireLinkedTask(c.getId());
                taskByCraft.put(c.getId(), task);
                Long occupiedSchedules = scheduleMapper.selectCount(Wrappers.<ProdSchedule>lambdaQuery()
                        .eq(ProdSchedule::getTaskId, task.getId()));
                if (occupiedSchedules != null && occupiedSchedules > 0) {
                    throw new BizException("工艺单[" + c.getCraftNo() + "]已生成过排程，请先撤回后再合并");
                }
                PlanCraftRoute last = routes.get(routes.size() - 1);
                lastQtyByTask.put(task.getId(), last.getPlanQty() != null ? last.getPlanQty() : c.getPlanQty());
                for (PlanCraftRoute r : routes) {
                    if (r.getProcessId() == null) {
                        continue;
                    }
                    BigDecimal qty = r.getPlanQty() != null ? r.getPlanQty() : c.getPlanQty();
                    qtyByProcess.merge(r.getProcessId(), qty == null ? BigDecimal.ZERO : qty, BigDecimal::add);
                    seqByProcess.merge(r.getProcessId(), r.getSeq() == null ? 999 : r.getSeq(), Math::min);
                    routeByProcess.putIfAbsent(r.getProcessId(), r);
                }
            }
            List<Long> processIds = qtyByProcess.keySet().stream()
                    .sorted(Comparator.comparingInt(pid -> seqByProcess.getOrDefault(pid, 999))).toList();
            Long lastProcessId = processIds.get(processIds.size() - 1);
            String workOrderNo = codeEngine.next("工单单号", group.get(0).getBatchNo());
            workOrderNos.add(workOrderNo);
            ProdTask firstTask = taskByCraft.get(group.get(0).getId());
            List<Long> scheduleIds = new ArrayList<>();
            for (Long pid : processIds) {
                PlanCraftRoute rep = routeByProcess.get(pid);
                ProdSchedule s = new ProdSchedule();
                s.setScheduleNo(codeEngine.next("生产排程", workOrderNo));
                s.setWorkOrderNo(workOrderNo);
                s.setWorkshopId(rep.getWorkshopId() != null ? rep.getWorkshopId() : group.get(0).getWorkshopId());
                s.setTaskId(firstTask.getId());
                s.setBatchNo(group.get(0).getBatchNo());
                s.setTraceBatchNo(group.get(0).getTraceBatchNo());
                s.setBatchFlagId(group.get(0).getBatchFlagId());
                s.setProcessId(pid);
                s.setScheduleQty(qtyByProcess.get(pid));
                s.setSeq(seqByProcess.get(pid));
                fillEquipmentAndTime(s, s.getWorkshopId(), rep.getEquipmentId());
                scheduleMapper.insert(s);
                copyCrafts(rep, s);
                scheduleIds.add(s.getId());
                count++;
                if (pid.equals(lastProcessId)) {
                    // 末道工序：按各来源任务分别占用
                    for (PlanCraft c : group) {
                        ProdTask t = taskByCraft.get(c.getId());
                        insertTaskOccupation(s, t, lastQtyByTask.get(t.getId()), pid);
                    }
                }
                publisher.publish(new ScheduleGeneratedEvent(s.getId(), s.getScheduleNo(), firstTask.getId(), s.getBatchNo()));
            }
            // 用料合并（按原料批号+名称汇总投料量）
            Map<String, ProdScheduleMaterial> merged = new LinkedHashMap<>();
            for (PlanCraft c : group) {
                for (PlanCraftMaterial m : craftMaterialMapper.selectList(Wrappers.<PlanCraftMaterial>lambdaQuery()
                        .eq(PlanCraftMaterial::getPlanCraftId, c.getId()))) {
                    String key = m.getRawBatchNo() + "|" + m.getRawName();
                    ProdScheduleMaterial sm = merged.computeIfAbsent(key, k -> {
                        ProdScheduleMaterial x = new ProdScheduleMaterial();
                        x.setRawBatchNo(m.getRawBatchNo());
                        x.setRawName(m.getRawName());
                        x.setUnit(m.getUnit());
                        x.setPlanQty(BigDecimal.ZERO);
                        return x;
                    });
                    sm.setPlanQty(sm.getPlanQty().add(m.getFeedQty() == null ? BigDecimal.ZERO : m.getFeedQty()));
                }
            }
            for (Long sid : scheduleIds) {
                for (ProdScheduleMaterial sm : merged.values()) {
                    sm.setId(null);
                    sm.setScheduleId(sid);
                    scheduleMaterialMapper.insert(sm);
                }
            }
        }
        return Map.of("warnings", warnings, "workOrderNos", workOrderNos, "count", count);
    }

    /** 任务明细新增：从生产任务带出 + 排程量≤剩余可排 实时校验 */
    @Transactional
    public void addTaskDetail(ScheduleTaskAddDTO dto) {
        ProdSchedule schedule = requireSchedule(dto.getScheduleId());
        ProdTask task = taskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BizException("生产任务不存在: " + dto.getTaskId());
        }
        if (!BillStatus.EFFECTIVE.equals(task.getDocStatus())) {
            throw new BizException("任务[" + task.getTaskNo() + "]状态为" + task.getDocStatus() + "，仅[生效]可排产");
        }
        BigDecimal occupied = prodTaskAppService.occupiedQty(task.getId());
        BigDecimal planQty = task.getPlanQty() == null ? BigDecimal.ZERO : task.getPlanQty();
        BigDecimal remaining = planQty.subtract(occupied);
        if (remaining.compareTo(dto.getScheduleQty()) < 0) {
            throw new BizException("排程数量超剩余可排: 计划" + planQty + " 已排" + occupied
                    + " 剩余" + remaining + "，本次" + dto.getScheduleQty());
        }
        ProdScheduleTask row = new ProdScheduleTask();
        row.setScheduleId(schedule.getId());
        row.setTaskId(task.getId());
        row.setProductNo(task.getProductNo());
        row.setProductName(productNameOf(task.getId()));
        row.setSpec(task.getProductSpec());
        row.setRawDetail(task.getRawMaterial());
        row.setProcessName(nameOfProcess(schedule.getProcessId()));
        row.setPlanQty(task.getPlanQty());
        row.setScheduledQty(occupied.add(dto.getScheduleQty()));
        row.setScheduleQty(dto.getScheduleQty());
        row.setUnit(task.getUnit());
        scheduleTaskMapper.insert(row);
    }

    /** 任务明细移除（释放占用） */
    @Transactional
    public void removeTaskDetail(Long detailId) {
        scheduleTaskMapper.deleteById(detailId);
    }

    /** 刷新排程：重算全部任务明细的已排产量快照 */
    @Transactional
    public int refresh() {
        List<ProdScheduleTask> rows = scheduleTaskMapper.selectList(Wrappers.<ProdScheduleTask>lambdaQuery()
                .inSql(ProdScheduleTask::getScheduleId, "SELECT id FROM prod_schedule WHERE deleted = 0"));
        Map<Long, BigDecimal> totals = rows.stream().collect(Collectors.groupingBy(ProdScheduleTask::getTaskId,
                Collectors.reducing(BigDecimal.ZERO, r -> r.getScheduleQty() == null ? BigDecimal.ZERO : r.getScheduleQty(), BigDecimal::add)));
        int updated = 0;
        for (ProdScheduleTask row : rows) {
            BigDecimal total = totals.get(row.getTaskId());
            if (!total.equals(row.getScheduledQty())) {
                scheduleTaskMapper.update(null, Wrappers.<ProdScheduleTask>lambdaUpdate()
                        .eq(ProdScheduleTask::getId, row.getId())
                        .set(ProdScheduleTask::getScheduledQty, total));
                updated++;
            }
        }
        return updated;
    }

    // ---------- 内部 ----------

    private ProdSchedule buildSchedule(PlanCraft craft, PlanCraftRoute route, ProdTask task, String workOrderNo) {
        ProdSchedule s = new ProdSchedule();
        s.setScheduleNo(codeEngine.next("生产排程", workOrderNo));
        s.setWorkOrderNo(workOrderNo);
        s.setWorkshopId(route.getWorkshopId() != null ? route.getWorkshopId() : craft.getWorkshopId());
        s.setTaskId(task.getId());
        s.setBatchNo(craft.getBatchNo());
        s.setTraceBatchNo(craft.getTraceBatchNo());
        s.setBatchFlagId(craft.getBatchFlagId());
        s.setProcessId(route.getProcessId());
        s.setScheduleQty(route.getPlanQty() != null ? route.getPlanQty() : craft.getPlanQty());
        s.setSeq(route.getSeq());
        s.setRemark("由工艺单" + craft.getCraftNo() + "生成");
        fillEquipmentAndTime(s, s.getWorkshopId(), route.getEquipmentId());
        return s;
    }

    /** 自动分配机台与计划时间：优先工序行已选机台，否则取车间首台（equipment_id 非空约束） */
    private void fillEquipmentAndTime(ProdSchedule s, Long workshopId, Long routeEquipmentId) {
        BaseEquipment eq = null;
        if (routeEquipmentId != null) {
            eq = equipmentMapper.selectById(routeEquipmentId);
        }
        if (eq == null && workshopId != null) {
            eq = equipmentMapper.selectOne(Wrappers.<BaseEquipment>lambdaQuery()
                    .eq(BaseEquipment::getWorkshopId, workshopId).last("LIMIT 1"));
        }
        if (eq == null) {
            throw new BizException("车间[" + nameOfWorkshop(workshopId) + "]无机台可分配，请先维护设备");
        }
        s.setEquipmentId(eq.getId());
        s.setMachineModel(eq.getMachineModel());
        s.setScheduleDate(LocalDate.now());
        s.setPlanStart(LocalDateTime.now());
        s.setPlanEnd(LocalDateTime.now().plusDays(1));
    }

    private void copyCrafts(PlanCraftRoute route, ProdSchedule s) {
        List<PlanCraftCraft> items = craftItemMapper.selectList(Wrappers.<PlanCraftCraft>lambdaQuery()
                .eq(PlanCraftCraft::getRouteId, route.getId()).orderByAsc(PlanCraftCraft::getSortNo));
        int sort = 0;
        for (PlanCraftCraft c : items) {
            sort++;
            ProdScheduleCraft sc = new ProdScheduleCraft();
            sc.setScheduleId(s.getId());
            sc.setParamName(c.getParamName());
            sc.setParamValue(c.getParamValue());
            sc.setLowerLimit(c.getLowerLimit());
            sc.setUpperLimit(c.getUpperLimit());
            sc.setSortNo(sort);
            scheduleCraftMapper.insert(sc);
        }
    }

    private void copyMaterials(PlanCraft craft, List<Long> scheduleIds) {
        List<PlanCraftMaterial> materials = craftMaterialMapper.selectList(Wrappers.<PlanCraftMaterial>lambdaQuery()
                .eq(PlanCraftMaterial::getPlanCraftId, craft.getId()));
        for (Long sid : scheduleIds) {
            for (PlanCraftMaterial m : materials) {
                ProdScheduleMaterial sm = new ProdScheduleMaterial();
                sm.setScheduleId(sid);
                sm.setRawBatchNo(m.getRawBatchNo());
                sm.setRawName(m.getRawName());
                sm.setPlanQty(m.getFeedQty());
                sm.setUnit(m.getUnit());
                scheduleMaterialMapper.insert(sm);
            }
        }
    }

    /** 末道工序排程插入任务明细（占用任务产量） */
    private void insertTaskOccupation(ProdSchedule s, ProdTask task, BigDecimal qty, Long processId) {
        ProdScheduleTask row = new ProdScheduleTask();
        row.setScheduleId(s.getId());
        row.setTaskId(task.getId());
        row.setProductNo(task.getProductNo());
        row.setProductName(productNameOf(task.getId()));
        row.setSpec(task.getProductSpec());
        row.setRawDetail(task.getRawMaterial());
        row.setProcessName(nameOfProcess(processId));
        row.setPlanQty(task.getPlanQty());
        row.setScheduledQty(prodTaskAppService.occupiedQty(task.getId()).add(qty == null ? BigDecimal.ZERO : qty));
        row.setScheduleQty(qty);
        row.setUnit(task.getUnit());
        scheduleTaskMapper.insert(row);
    }

    private ProdTask requireLinkedTask(Long craftId) {
        List<ProdTask> linked = taskMapper.selectList(Wrappers.<ProdTask>lambdaQuery()
                .eq(ProdTask::getPlanCraftId, craftId).orderByDesc(ProdTask::getId));
        return linked.stream().filter(t -> BillStatus.EFFECTIVE.equals(t.getDocStatus())
                        || BillStatus.BATCH_OFF.equals(t.getDocStatus()))
                .findFirst()
                .orElseThrow(() -> new BizException("工艺单尚未生成生效的生产任务（提交后自动生成），无法排程"));
    }

    private String productNameOf(Long taskId) {
        List<com.xinao.mes.plan.domain.model.ProdTaskOrder> orders = taskOrderMapper.selectList(
                Wrappers.<com.xinao.mes.plan.domain.model.ProdTaskOrder>lambdaQuery()
                        .eq(com.xinao.mes.plan.domain.model.ProdTaskOrder::getTaskId, taskId)
                        .last("LIMIT 1"));
        return orders.isEmpty() ? null : orders.get(0).getProductName();
    }

    private ScheduleListVO toListVO(ProdSchedule s) {
        ScheduleListVO vo = new ScheduleListVO();
        vo.setSchedule(s);
        vo.setWorkshopName(nameOfWorkshop(s.getWorkshopId()));
        vo.setProcessName(nameOfProcess(s.getProcessId()));
        BaseEquipment eq = s.getEquipmentId() == null ? null : equipmentMapper.selectById(s.getEquipmentId());
        vo.setMachineNo(eq == null ? null : eq.getMachineNo());
        ProdTask task = s.getTaskId() == null ? null : taskMapper.selectById(s.getTaskId());
        vo.setTaskNo(task == null ? null : task.getTaskNo());
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

    private ProdSchedule requireSchedule(Long id) {
        ProdSchedule s = scheduleMapper.selectById(id);
        if (s == null) {
            throw new BizException("生产排程不存在: " + id);
        }
        return s;
    }
}
