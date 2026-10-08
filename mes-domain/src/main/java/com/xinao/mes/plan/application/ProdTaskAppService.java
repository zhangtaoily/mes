package com.xinao.mes.plan.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.engine.flow.FlowEngine;
import com.xinao.mes.plan.domain.event.PlanCraftSubmittedEvent;
import com.xinao.mes.plan.domain.model.BaseCraftParam;
import com.xinao.mes.plan.domain.model.BaseProcess;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.domain.model.PlanCraft;
import com.xinao.mes.plan.domain.model.PlanCraftCraft;
import com.xinao.mes.plan.domain.model.PlanCraftMaterial;
import com.xinao.mes.plan.domain.model.PlanCraftProd;
import com.xinao.mes.plan.domain.model.PlanCraftRoute;
import com.xinao.mes.plan.domain.model.ProdScheduleTask;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.domain.model.ProdTaskCraft;
import com.xinao.mes.plan.domain.model.ProdTaskExtra;
import com.xinao.mes.plan.domain.model.ProdTaskMaterial;
import com.xinao.mes.plan.domain.model.ProdTaskOrder;
import com.xinao.mes.plan.domain.model.ProdTaskRoute;
import com.xinao.mes.plan.dto.TaskCraftApplyDTO;
import com.xinao.mes.plan.dto.TaskQueryDTO;
import com.xinao.mes.plan.dto.TaskSaveDTO;
import com.xinao.mes.plan.infrastructure.mapper.BaseProcessMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftProdMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftRouteMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleTaskMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskExtraMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskOrderMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskRouteMapper;
import com.xinao.mes.plan.vo.TaskDetailVO;
import com.xinao.mes.plan.vo.TaskRemainingVO;
import com.xinao.mes.produce.application.TaskFinishRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产任务应用服务（聚合根 ProdTask）
 *
 * <p>任务来源两条：手工新增（编制中→提交生效）；工艺单 submit 事件自动生成（直接生效快照）。
 * 工艺配置引用技术/车间工艺库带出快照。
 */
@Service
@RequiredArgsConstructor
public class ProdTaskAppService {

    private final ProdTaskMapper taskMapper;
    private final BaseWorkshopMapper workshopMapper;
    private final ProdTaskOrderMapper orderMapper;
    private final ProdTaskMaterialMapper materialMapper;
    private final ProdTaskRouteMapper routeMapper;
    private final ProdTaskCraftMapper craftMapper;
    private final ProdTaskExtraMapper extraMapper;
    private final ProdScheduleTaskMapper scheduleTaskMapper;
    private final PlanCraftMapper planCraftMapper;
    private final PlanCraftProdMapper planProdMapper;
    private final PlanCraftMaterialMapper planMaterialMapper;
    private final PlanCraftRouteMapper planRouteMapper;
    private final PlanCraftCraftMapper planCraftItemMapper;
    private final BaseProcessMapper processMapper;
    private final CodeEngine codeEngine;
    private final FlowEngine flowEngine;
    private final CraftLibraryService craftLibrary;
    private final TaskFinishRule taskFinishRule;

    public PageResult<ProdTask> page(TaskQueryDTO query) {
        Page<ProdTask> p = Page.of(query.getPageNum(), query.getPageSize());
        // 车间类型过滤（复精梳/倒筒任务页）：先取该类型车间 id 集合再 in 查询
        List<Long> workshopIds = null;
        if (query.getWorkshopType() != null) {
            workshopIds = workshopMapper.selectList(Wrappers.<BaseWorkshop>lambdaQuery()
                            .eq(BaseWorkshop::getWorkshopType, query.getWorkshopType()))
                    .stream().map(BaseWorkshop::getId).toList();
            if (workshopIds.isEmpty()) {
                return PageResult.of(0L, List.of());
            }
        }
        Page<ProdTask> result = taskMapper.selectPage(p, Wrappers.<ProdTask>lambdaQuery()
                .in(workshopIds != null, ProdTask::getWorkshopId, workshopIds)
                .eq(query.getWorkshopId() != null, ProdTask::getWorkshopId, query.getWorkshopId())
                .like(StringUtils.isNotBlank(query.getBatchNo()), ProdTask::getBatchNo, query.getBatchNo())
                .like(StringUtils.isNotBlank(query.getActualCount()), ProdTask::getActualCount, query.getActualCount())
                .like(StringUtils.isNotBlank(query.getRawMaterial()), ProdTask::getRawMaterial, query.getRawMaterial())
                .eq(StringUtils.isNotBlank(query.getDocStatus()), ProdTask::getDocStatus, query.getDocStatus())
                .orderByDesc(ProdTask::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public TaskDetailVO detail(Long id) {
        ProdTask task = requireTask(id);
        TaskDetailVO vo = new TaskDetailVO();
        vo.setTask(task);
        vo.setOrders(orderMapper.selectList(Wrappers.<ProdTaskOrder>lambdaQuery().eq(ProdTaskOrder::getTaskId, id)));
        vo.setMaterials(materialMapper.selectList(Wrappers.<ProdTaskMaterial>lambdaQuery().eq(ProdTaskMaterial::getTaskId, id)));
        vo.setRoutes(routeMapper.selectList(Wrappers.<ProdTaskRoute>lambdaQuery()
                .eq(ProdTaskRoute::getTaskId, id).orderByAsc(ProdTaskRoute::getSeq)));
        vo.setCrafts(craftMapper.selectList(Wrappers.<ProdTaskCraft>lambdaQuery().eq(ProdTaskCraft::getTaskId, id)));
        vo.setExtra(extraMapper.selectOne(Wrappers.<ProdTaskExtra>lambdaQuery()
                .eq(ProdTaskExtra::getTaskId, id).last("LIMIT 1")));
        vo.setButtons(flowEngine.buttons("prod_task", task.getDocStatus(), roles()));
        return vo;
    }

    @Transactional
    public Long save(TaskSaveDTO dto) {
        ProdTask task = dto.getTask();
        if (task == null) {
            throw new BizException("任务主信息不能为空");
        }
        if (task.getId() == null) {
            if (StringUtils.isBlank(task.getTaskNo())) {
                task.setTaskNo(codeEngine.next("生产任务", task.getK3OrderNo()));
            }
            task.setDocStatus(BillStatus.DRAFT);
            taskMapper.insert(task);
        } else {
            ProdTask db = requireTask(task.getId());
            db.ensureEditable();
            taskMapper.updateById(task);
        }
        Long id = task.getId();
        orderMapper.delete(Wrappers.<ProdTaskOrder>lambdaQuery().eq(ProdTaskOrder::getTaskId, id));
        materialMapper.delete(Wrappers.<ProdTaskMaterial>lambdaQuery().eq(ProdTaskMaterial::getTaskId, id));
        routeMapper.delete(Wrappers.<ProdTaskRoute>lambdaQuery().eq(ProdTaskRoute::getTaskId, id));
        extraMapper.delete(Wrappers.<ProdTaskExtra>lambdaQuery().eq(ProdTaskExtra::getTaskId, id));
        if (dto.getOrders() != null) {
            for (ProdTaskOrder o : dto.getOrders()) {
                o.setId(null);
                o.setTaskId(id);
                orderMapper.insert(o);
            }
        }
        if (dto.getMaterials() != null) {
            for (ProdTaskMaterial m : dto.getMaterials()) {
                m.setId(null);
                m.setTaskId(id);
                materialMapper.insert(m);
            }
        }
        if (dto.getRoutes() != null) {
            int seq = 0;
            for (ProdTaskRoute r : dto.getRoutes()) {
                seq++;
                r.setId(null);
                r.setTaskId(id);
                r.setSeq(seq);
                if (r.getProcessName() == null && r.getProcessId() != null) {
                    r.setProcessName(processName(r.getProcessId()));
                }
                routeMapper.insert(r);
            }
        }
        if (dto.getExtra() != null) {
            dto.getExtra().setId(null);
            dto.getExtra().setTaskId(id);
            extraMapper.insert(dto.getExtra());
        }
        return id;
    }

    /** 提交：编制中→生效 */
    @Transactional
    public void submit(List<Long> ids) {
        fireBatch(ids, "submit");
    }

    /** 撤回：生效→编制中，有排程占用拦截 */
    @Transactional
    public void withdraw(List<Long> ids) {
        for (Long id : ids) {
            ProdTask task = requireTask(id);
            // 占用 = 未删排程上的任务明细数
            long occupied = occupiedRows(task.getId());
            task.ensureWithdrawable(occupied);
            fireUpdate(task, "withdraw");
        }
    }

    /** 了批：生效→了批（produce 域校验：投料完毕 + 追溯批次链闭环；通过后联动上批闭环/批号标识释放） */
    @Transactional
    public void finish(List<Long> ids) {
        for (Long id : ids) {
            ProdTask task = requireTask(id);
            taskFinishRule.checkCanFinish(task);
            fireUpdate(task, "finish");
            taskFinishRule.onTaskFinished(task);
        }
    }

    /** 关闭：生效/了批→已关闭（终态） */
    @Transactional
    public void close(List<Long> ids) {
        fireBatch(ids, "close");
    }

    /** 作废：编制中→已作废 */
    @Transactional
    public void voidTask(List<Long> ids) {
        fireBatch(ids, "void");
    }

    @Transactional
    public void delete(Long id) {
        ProdTask task = requireTask(id);
        task.ensureEditable();
        taskMapper.deleteById(id);
        orderMapper.delete(Wrappers.<ProdTaskOrder>lambdaQuery().eq(ProdTaskOrder::getTaskId, id));
        materialMapper.delete(Wrappers.<ProdTaskMaterial>lambdaQuery().eq(ProdTaskMaterial::getTaskId, id));
        routeMapper.delete(Wrappers.<ProdTaskRoute>lambdaQuery().eq(ProdTaskRoute::getTaskId, id));
        craftMapper.delete(Wrappers.<ProdTaskCraft>lambdaQuery().eq(ProdTaskCraft::getTaskId, id));
        extraMapper.delete(Wrappers.<ProdTaskExtra>lambdaQuery().eq(ProdTaskExtra::getTaskId, id));
    }

    /**
     * 工艺单 submit 事件监听：自动生成生产任务（直接生效的快照）。
     * 幂等：该工艺单存在未作废任务时跳过（撤回重提场景旧任务已作废，会重新生成）。
     */
    @Transactional
    public void generateFromCraft(PlanCraftSubmittedEvent event) {
        PlanCraft craft = planCraftMapper.selectById(event.craftId);
        if (craft == null) {
            return;
        }
        List<ProdTask> linked = taskMapper.selectList(Wrappers.<ProdTask>lambdaQuery()
                .eq(ProdTask::getPlanCraftId, craft.getId()));
        boolean hasActive = linked.stream().anyMatch(t -> !BillStatus.VOIDED.equals(t.getDocStatus()));
        if (hasActive) {
            return;
        }
        List<PlanCraftProd> prods = planProdMapper.selectList(Wrappers.<PlanCraftProd>lambdaQuery()
                .eq(PlanCraftProd::getPlanCraftId, craft.getId()));
        PlanCraftProd firstProd = prods.isEmpty() ? null : prods.get(0);

        ProdTask task = new ProdTask();
        task.setTaskNo(codeEngine.next("生产任务", craft.getCraftNo()));
        task.setPlanCraftId(craft.getId());
        task.setTaskType("主任务");
        task.setLineVariant("普通");
        // PRD 3.3：任务提交即生效；自动生成任务直接生效，可立即被排程引用
        task.setDocStatus(BillStatus.EFFECTIVE);
        task.setExecStatus("计划执行");
        task.setBatchNo(craft.getBatchNo());
        task.setWorkshopId(craft.getWorkshopId());
        task.setProdLineId(craft.getProdLineId());
        task.setBatchFlagId(craft.getBatchFlagId());
        task.setRawMaterial(craft.getRawMaterial());
        task.setColorNo(craft.getColorNo());
        task.setActualCount(craft.getActualCount());
        task.setMergeFeedQty(craft.getMergeFeedQty());
        task.setPlanFeedQty(craft.getFeedQty());
        task.setPlanQty(craft.getPlanQty());
        task.setUnit("KG");
        task.setK3OrderNo(craft.getK3OrderNo());
        task.setOrderDate(craft.getOrderDate());
        task.setDeliveryDate(craft.getDeliveryDate());
        if (firstProd != null) {
            task.setProductNo(firstProd.getProductNo());
            task.setProductSpec(firstProd.getProductSpec());
            task.setPlyCount(firstProd.getPlyCount());
        }
        taskMapper.insert(task);

        for (PlanCraftProd p : prods) {
            ProdTaskOrder o = new ProdTaskOrder();
            o.setTaskId(task.getId());
            o.setSpinBatchNo(craft.getBatchNo());
            o.setProductName(p.getProductName());
            o.setRawMaterial(craft.getRawMaterial());
            o.setPlyCount(p.getPlyCount());
            o.setOriginPlanQty(craft.getPlanQty());
            o.setPlanQty(craft.getPlanQty());
            o.setFeedQty(craft.getFeedQty());
            o.setUnit(p.getUnit());
            orderMapper.insert(o);
        }
        for (PlanCraftMaterial m : planMaterialMapper.selectList(Wrappers.<PlanCraftMaterial>lambdaQuery()
                .eq(PlanCraftMaterial::getPlanCraftId, craft.getId()))) {
            ProdTaskMaterial tm = new ProdTaskMaterial();
            tm.setTaskId(task.getId());
            tm.setRawBatchNo(m.getRawBatchNo());
            tm.setRawName(m.getRawName());
            tm.setPlanFeedQty(m.getFeedQty());
            tm.setWorkshopQty(m.getWorkshopQty());
            tm.setUnit(m.getUnit());
            materialMapper.insert(tm);
        }
        List<PlanCraftRoute> craftRoutes = planRouteMapper.selectList(Wrappers.<PlanCraftRoute>lambdaQuery()
                .eq(PlanCraftRoute::getPlanCraftId, craft.getId()).orderByAsc(PlanCraftRoute::getSeq));
        Map<Long, String> processNames = new HashMap<>();
        for (PlanCraftRoute r : craftRoutes) {
            if (r.getProcessId() != null && !processNames.containsKey(r.getProcessId())) {
                processNames.put(r.getProcessId(), processName(r.getProcessId()));
            }
            ProdTaskRoute tr = new ProdTaskRoute();
            tr.setTaskId(task.getId());
            tr.setSeq(r.getSeq());
            tr.setProcessId(r.getProcessId());
            tr.setProcessName(processNames.get(r.getProcessId()));
            tr.setIsFeed(r.getIsFeed());
            tr.setIsReport(r.getIsReport());
            tr.setMaterialUnit(r.getMaterialUnit());
            tr.setProdUnit(r.getProdUnit());
            tr.setPlanQty(r.getPlanQty());
            tr.setWorkshopId(r.getWorkshopId());
            routeMapper.insert(tr);
        }
        for (PlanCraftCraft c : planCraftItemMapper.selectList(Wrappers.<PlanCraftCraft>lambdaQuery()
                .eq(PlanCraftCraft::getPlanCraftId, craft.getId()))) {
            ProdTaskCraft tc = new ProdTaskCraft();
            tc.setTaskId(task.getId());
            tc.setProcessName(c.getProcessName() != null ? c.getProcessName() : processNames.get(c.getProcessId()));
            tc.setParamName(c.getParamName());
            tc.setParamValue(c.getParamValue());
            tc.setLowerLimit(c.getLowerLimit());
            tc.setUpperLimit(c.getUpperLimit());
            tc.setRefSource(c.getRefSource());
            tc.setRefSchemeNo(c.getRefSchemeNo());
            craftMapper.insert(tc);
        }
    }

    /** 工艺配置：引用技术/车间工艺库带出（allProcesses=true 全部工序批量引用） */
    @Transactional
    public int applyCraft(TaskCraftApplyDTO dto) {
        ProdTask task = requireTask(dto.getTaskId());
        List<ProdTaskRoute> routes = routeMapper.selectList(Wrappers.<ProdTaskRoute>lambdaQuery()
                .eq(ProdTaskRoute::getTaskId, task.getId()).orderByAsc(ProdTaskRoute::getSeq));
        Set<Long> processIds;
        if (dto.isAllProcesses()) {
            processIds = routes.stream().map(ProdTaskRoute::getProcessId).filter(r -> r != null).collect(Collectors.toSet());
            if (processIds.isEmpty()) {
                throw new BizException("任务无工序路线，请先维护工序路线");
            }
        } else {
            if (dto.getProcessId() == null) {
                throw new BizException("请选择工序");
            }
            processIds = Set.of(dto.getProcessId());
        }
        List<BaseCraftParam> params = craftLibrary.params(dto.getRefSource(), dto.getSchemeId(), processIds, dto.getMachineModel());
        if (params.isEmpty()) {
            throw new BizException("所选方案在所选工序下无工艺项，请确认来源/方案/机型");
        }
        String schemeNo = craftLibrary.schemeNo(dto.getRefSource(), dto.getSchemeId());
        Map<Long, String> processNames = new HashMap<>();
        for (Long pid : processIds) {
            processNames.put(pid, processName(pid));
        }
        // 覆盖式引用：清掉所选工序的旧引用
        List<String> names = new ArrayList<>(processNames.values());
        for (ProdTaskCraft old : craftMapper.selectList(Wrappers.<ProdTaskCraft>lambdaQuery()
                .eq(ProdTaskCraft::getTaskId, task.getId()))) {
            if (names.contains(old.getProcessName())) {
                craftMapper.deleteById(old.getId());
            }
        }
        for (BaseCraftParam p : params) {
            ProdTaskCraft tc = new ProdTaskCraft();
            tc.setTaskId(task.getId());
            tc.setProcessName(processNames.get(p.getProcessId()));
            tc.setParamName(p.getParamName());
            tc.setParamValue(p.getParamValue());
            tc.setLowerLimit(p.getLowerLimit());
            tc.setUpperLimit(p.getUpperLimit());
            tc.setRefSource(dto.getRefSource());
            tc.setRefSchemeNo(schemeNo);
            craftMapper.insert(tc);
        }
        return params.size();
    }

    /** 排程-任务明细新增面板：生效任务 + 实时剩余可排（排满由前端置灰） */
    public List<TaskRemainingVO> availableTasks() {
        List<ProdTask> tasks = taskMapper.selectList(Wrappers.<ProdTask>lambdaQuery()
                .eq(ProdTask::getDocStatus, BillStatus.EFFECTIVE).orderByDesc(ProdTask::getId));
        List<TaskRemainingVO> result = new ArrayList<>();
        for (ProdTask t : tasks) {
            TaskRemainingVO vo = new TaskRemainingVO();
            vo.setTaskId(t.getId());
            vo.setTaskNo(t.getTaskNo());
            vo.setBatchNo(t.getBatchNo());
            vo.setProductNo(t.getProductNo());
            vo.setProductName(firstName(orderMapper.selectList(Wrappers.<ProdTaskOrder>lambdaQuery()
                    .eq(ProdTaskOrder::getTaskId, t.getId()).last("LIMIT 1"))));
            vo.setSpec(t.getProductSpec());
            vo.setRawDetail(t.getRawMaterial());
            vo.setPlanQty(t.getPlanQty());
            BigDecimal occupied = occupiedQty(t.getId());
            vo.setScheduledQty(occupied);
            vo.setRemaining((t.getPlanQty() == null ? BigDecimal.ZERO : t.getPlanQty()).subtract(occupied));
            vo.setUnit(t.getUnit());
            result.add(vo);
        }
        return result;
    }

    /** 任务已排产量 = 未删排程上的任务明细排程量合计（排程撤回即释放） */
    public BigDecimal occupiedQty(Long taskId) {
        List<ProdScheduleTask> rows = scheduleTaskMapper.selectList(Wrappers.<ProdScheduleTask>lambdaQuery()
                .eq(ProdScheduleTask::getTaskId, taskId)
                .inSql(ProdScheduleTask::getScheduleId, "SELECT id FROM prod_schedule WHERE deleted = 0"));
        return rows.stream().map(r -> r.getScheduleQty() == null ? BigDecimal.ZERO : r.getScheduleQty())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long occupiedRows(Long taskId) {
        return scheduleTaskMapper.selectCount(Wrappers.<ProdScheduleTask>lambdaQuery()
                .eq(ProdScheduleTask::getTaskId, taskId)
                .inSql(ProdScheduleTask::getScheduleId, "SELECT id FROM prod_schedule WHERE deleted = 0"));
    }

    private void fireBatch(List<Long> ids, String action) {
        for (Long id : ids) {
            fireUpdate(requireTask(id), action);
        }
    }

    private void fireUpdate(ProdTask task, String action) {
        String to = flowEngine.fire("prod_task", task.getDocStatus(), action, roles());
        if (taskMapper.update(null, Wrappers.<ProdTask>lambdaUpdate()
                .eq(ProdTask::getId, task.getId()).eq(ProdTask::getDocStatus, task.getDocStatus())
                .set(ProdTask::getDocStatus, to)) == 0) {
            throw new BizException("任务[" + task.getTaskNo() + "]状态已变更，请刷新后重试");
        }
    }

    private String firstName(List<ProdTaskOrder> orders) {
        return orders.isEmpty() ? null : orders.get(0).getProductName();
    }

    private String processName(Long processId) {
        if (processId == null) {
            return null;
        }
        BaseProcess p = processMapper.selectById(processId);
        return p == null ? null : p.getProcessName();
    }

    private ProdTask requireTask(Long id) {
        ProdTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException("生产任务不存在: " + id);
        }
        return task;
    }

    private Set<String> roles() {
        var user = AuthContext.get();
        return user == null || user.getRoles() == null ? Set.of() : user.getRoles();
    }
}
