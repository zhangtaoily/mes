package com.xinao.mes.plan.application;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinao.mes.common.auth.AuthContext;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.result.PageResult;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.event.MesEventPublisher;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.engine.flow.FlowEngine;
import com.xinao.mes.plan.domain.event.PlanCraftSubmittedEvent;
import com.xinao.mes.plan.domain.event.PlanCraftWithdrawnEvent;
import com.xinao.mes.plan.domain.model.BaseCraftParam;
import com.xinao.mes.plan.domain.model.BaseProcess;
import com.xinao.mes.plan.domain.model.BaseWorkshop;
import com.xinao.mes.plan.domain.model.ErpK3Order;
import com.xinao.mes.plan.domain.model.PlanCraft;
import com.xinao.mes.plan.domain.model.PlanCraftCraft;
import com.xinao.mes.plan.domain.model.PlanCraftMaterial;
import com.xinao.mes.plan.domain.model.PlanCraftProd;
import com.xinao.mes.plan.domain.model.PlanCraftRawQc;
import com.xinao.mes.plan.domain.model.PlanCraftRoute;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.dto.CraftQueryDTO;
import com.xinao.mes.plan.dto.CraftSaveDTO;
import com.xinao.mes.plan.dto.ImportK3DTO;
import com.xinao.mes.plan.dto.PlanCraftApplyDTO;
import com.xinao.mes.plan.dto.RouteSaveDTO;
import com.xinao.mes.plan.infrastructure.mapper.BaseProcessMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import com.xinao.mes.plan.infrastructure.mapper.ErpK3OrderMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftProdMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftRawQcMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftRouteMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.plan.vo.CraftDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 计划工艺单应用服务（聚合根 PlanCraft）
 *
 * <p>引入K3订单（引入量校验+回写+重复拦截）→ 编制（7Tab）→ 提交（发布事件自动生成生产任务）
 * → 撤回（排程占用校验+级联作废自动任务）。
 */
@Service
@RequiredArgsConstructor
public class PlanCraftAppService {

    private final PlanCraftMapper craftMapper;
    private final PlanCraftProdMapper prodMapper;
    private final PlanCraftMaterialMapper materialMapper;
    private final PlanCraftRawQcMapper rawQcMapper;
    private final PlanCraftRouteMapper routeMapper;
    private final PlanCraftCraftMapper craftItemMapper;
    private final ErpK3OrderMapper k3Mapper;
    private final BaseWorkshopMapper workshopMapper;
    private final BaseProcessMapper processMapper;
    private final ProdTaskMapper taskMapper;
    private final ProdScheduleMapper scheduleMapper;
    private final CodeEngine codeEngine;
    private final FlowEngine flowEngine;
    private final MesEventPublisher publisher;
    private final CraftLibraryService craftLibrary;

    public PageResult<PlanCraft> page(CraftQueryDTO query) {
        Page<PlanCraft> p = Page.of(query.getPageNum(), query.getPageSize());
        Page<PlanCraft> result = craftMapper.selectPage(p, Wrappers.<PlanCraft>lambdaQuery()
                .eq(query.getWorkshopId() != null, PlanCraft::getWorkshopId, query.getWorkshopId())
                .like(StringUtils.isNotBlank(query.getBatchNo()), PlanCraft::getBatchNo, query.getBatchNo())
                .like(StringUtils.isNotBlank(query.getActualCount()), PlanCraft::getActualCount, query.getActualCount())
                .like(StringUtils.isNotBlank(query.getRawMaterial()), PlanCraft::getRawMaterial, query.getRawMaterial())
                .eq(StringUtils.isNotBlank(query.getDocStatus()), PlanCraft::getDocStatus, query.getDocStatus())
                .orderByDesc(PlanCraft::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 详情：7Tab 聚合 + 工序行工艺项 + 当前状态可用按钮 */
    public CraftDetailVO detail(Long id) {
        PlanCraft craft = requireCraft(id);
        CraftDetailVO vo = new CraftDetailVO();
        vo.setCraft(craft);
        vo.setProds(prodMapper.selectList(Wrappers.<PlanCraftProd>lambdaQuery().eq(PlanCraftProd::getPlanCraftId, id)));
        vo.setMaterials(materialMapper.selectList(Wrappers.<PlanCraftMaterial>lambdaQuery().eq(PlanCraftMaterial::getPlanCraftId, id)));
        vo.setRawQcs(rawQcMapper.selectList(Wrappers.<PlanCraftRawQc>lambdaQuery().eq(PlanCraftRawQc::getPlanCraftId, id)));
        List<PlanCraftRoute> routes = routeMapper.selectList(Wrappers.<PlanCraftRoute>lambdaQuery()
                .eq(PlanCraftRoute::getPlanCraftId, id).orderByAsc(PlanCraftRoute::getSeq));
        List<PlanCraftCraft> items = craftItemMapper.selectList(Wrappers.<PlanCraftCraft>lambdaQuery()
                .eq(PlanCraftCraft::getPlanCraftId, id).orderByAsc(PlanCraftCraft::getSortNo));
        Map<Long, List<PlanCraftCraft>> byRoute = items.stream()
                .filter(i -> i.getRouteId() != null)
                .collect(Collectors.groupingBy(PlanCraftCraft::getRouteId));
        Map<Long, String> processNames = processNames(routes);
        vo.setRoutes(routes.stream().map(r -> {
            CraftDetailVO.RouteVO rv = new CraftDetailVO.RouteVO();
            rv.setRoute(r);
            rv.setProcessName(processNames.get(r.getProcessId()));
            List<PlanCraftCraft> crafts = byRoute.getOrDefault(r.getId(), List.of());
            rv.setCrafts(crafts);
            rv.setCrafted(!crafts.isEmpty());
            return rv;
        }).collect(Collectors.toList()));
        vo.setButtons(flowEngine.buttons("plan_craft", craft.getDocStatus(), roles()));
        return vo;
    }

    /** 保存主信息 + 生产信息/用料汇总/原料检验（id=null 新增取号，否则仅编制中可改） */
    @Transactional
    public Long save(CraftSaveDTO dto) {
        PlanCraft craft = dto.getCraft();
        if (craft == null) {
            throw new BizException("工艺单主信息不能为空");
        }
        if (craft.getId() == null) {
            if (StringUtils.isBlank(craft.getCraftNo())) {
                craft.setCraftNo(codeEngine.next("计划工艺单", craft.getK3OrderNo()));
            }
            craft.setDocStatus(BillStatus.DRAFT);
            craftMapper.insert(craft);
        } else {
            PlanCraft db = requireCraft(craft.getId());
            db.ensureEditable();
            craftMapper.updateById(craft);
        }
        Long id = craft.getId();
        replaceChildren(id, dto);
        return id;
    }

    /** 工序路线整表保存（含逐工序编制的工艺项），seq 按列表顺序重排 */
    @Transactional
    public void saveRoute(RouteSaveDTO dto) {
        PlanCraft craft = requireCraft(dto.getPlanCraftId());
        craft.ensureEditable();
        routeMapper.delete(Wrappers.<PlanCraftRoute>lambdaQuery().eq(PlanCraftRoute::getPlanCraftId, craft.getId()));
        craftItemMapper.delete(Wrappers.<PlanCraftCraft>lambdaQuery().eq(PlanCraftCraft::getPlanCraftId, craft.getId()));
        if (dto.getRoutes() == null) {
            return;
        }
        int seq = 0;
        for (RouteSaveDTO.Item item : dto.getRoutes()) {
            PlanCraftRoute route = item.getRoute();
            if (route == null || route.getProcessId() == null) {
                continue;
            }
            seq++;
            route.setId(null);
            route.setPlanCraftId(craft.getId());
            route.setSeq(seq);
            if (route.getWorkshopId() == null) {
                route.setWorkshopId(craft.getWorkshopId());
            }
            routeMapper.insert(route);
            if (item.getCrafts() != null) {
                int sort = 0;
                for (PlanCraftCraft c : item.getCrafts()) {
                    sort++;
                    c.setId(null);
                    c.setPlanCraftId(craft.getId());
                    c.setRouteId(route.getId());
                    c.setProcessId(route.getProcessId());
                    c.setProcessName(processName(route.getProcessId()));
                    c.setSortNo(sort);
                    craftItemMapper.insert(c);
                }
            }
        }
    }

    /** 编制工艺单：从技术/车间工艺库按 工序(+机型) 带出工艺项（routeId=null 全部工序一键带出） */
    @Transactional
    public int applyCraftItems(PlanCraftApplyDTO dto) {
        PlanCraft craft = requireCraft(dto.getPlanCraftId());
        craft.ensureEditable();
        List<PlanCraftRoute> routes = routeMapper.selectList(Wrappers.<PlanCraftRoute>lambdaQuery()
                .eq(PlanCraftRoute::getPlanCraftId, craft.getId()).orderByAsc(PlanCraftRoute::getSeq));
        List<PlanCraftRoute> targets = dto.getRouteId() == null ? routes
                : routes.stream().filter(r -> dto.getRouteId().equals(r.getId())).toList();
        if (targets.isEmpty()) {
            throw new BizException("工序路线为空或目标工序行不存在，请先编制工序路线");
        }
        String schemeNo = craftLibrary.schemeNo(dto.getRefSource(), dto.getSchemeId());
        int inserted = 0;
        for (PlanCraftRoute route : targets) {
            if (route.getProcessId() == null) {
                continue;
            }
            List<BaseCraftParam> params = craftLibrary.params(dto.getRefSource(), dto.getSchemeId(),
                    List.of(route.getProcessId()), dto.getMachineModel());
            if (params.isEmpty()) {
                continue;
            }
            // 覆盖式引用：清掉该工序行旧工艺项后重带
            craftItemMapper.delete(Wrappers.<PlanCraftCraft>lambdaQuery()
                    .eq(PlanCraftCraft::getPlanCraftId, craft.getId())
                    .eq(PlanCraftCraft::getRouteId, route.getId()));
            int sort = 0;
            for (BaseCraftParam p : params) {
                sort++;
                PlanCraftCraft item = new PlanCraftCraft();
                item.setPlanCraftId(craft.getId());
                item.setRouteId(route.getId());
                item.setProcessId(route.getProcessId());
                item.setProcessName(processName(route.getProcessId()));
                item.setParamName(p.getParamName());
                item.setParamValue(p.getParamValue());
                item.setLowerLimit(p.getLowerLimit());
                item.setUpperLimit(p.getUpperLimit());
                item.setUnit(p.getUnit());
                item.setRefSource(dto.getRefSource());
                item.setRefSchemeNo(schemeNo);
                item.setSortNo(sort);
                craftItemMapper.insert(item);
                inserted++;
            }
        }
        if (inserted == 0) {
            throw new BizException("所选方案在目标工序下无工艺项，请确认来源/方案/机型");
        }
        return inserted;
    }

    /** 引入K3订单：单选带出主信息 + 引入量校验回写 + 重复引入拦截 */
    @Transactional
    public Long importFromK3(ImportK3DTO dto) {
        ErpK3Order order = k3Mapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BizException("K3订单不存在: " + dto.getOrderId());
        }
        order.checkImportable(dto.getImportQty());
        Long dup = craftMapper.selectCount(Wrappers.<PlanCraft>lambdaQuery()
                .eq(PlanCraft::getK3OrderNo, order.getOrderNo()));
        if (dup != null && dup > 0) {
            throw new BizException("K3订单[" + order.getOrderNo() + "]已引入过工艺单，不可重复引入");
        }
        BaseWorkshop ws = null;
        if (StringUtils.isNotBlank(order.getWorkshopName())) {
            ws = workshopMapper.selectOne(Wrappers.<BaseWorkshop>lambdaQuery()
                    .eq(BaseWorkshop::getWorkshopName, order.getWorkshopName()).last("LIMIT 1"));
        }
        if (ws == null && dto.getWorkshopId() == null) {
            throw new BizException("无法确定生产车间：订单车间[" + order.getWorkshopName() + "]未匹配到基础数据，请手工选择车间");
        }
        PlanCraft craft = new PlanCraft();
        craft.setCraftNo(codeEngine.next("计划工艺单", order.getOrderNo()));
        craft.setDocStatus(BillStatus.DRAFT);
        craft.setWorkshopId(ws != null ? ws.getId() : dto.getWorkshopId());
        craft.setBatchNo(order.getBatchNo());
        craft.setRawMaterial(order.getMaterialDetail());
        craft.setRawSpec(order.getMaterialSpec());
        craft.setColorNo(order.getColorNo());
        craft.setActualCount(order.getSpinCount());
        craft.setFeedQty(dto.getImportQty());
        craft.setK3OrderNo(order.getOrderNo());
        craft.setOrderDate(order.getOrderDate());
        craft.setDeliveryDate(order.getPlanEndDate());
        craft.setPlanQty(dto.getImportQty());
        craftMapper.insert(craft);
        // CAS 回写引入量（并发引入同一订单时仅一方成功；importedQty 为 NULL 时按 isNull 匹配，显式 0 按 eq 匹配）
        BigDecimal old = order.getImportedQty() == null ? BigDecimal.ZERO : order.getImportedQty();
        var uw = Wrappers.<ErpK3Order>lambdaUpdate()
                .eq(ErpK3Order::getId, order.getId())
                .set(ErpK3Order::getImportedQty, old.add(dto.getImportQty()));
        if (order.getImportedQty() == null) {
            uw.isNull(ErpK3Order::getImportedQty);
        } else {
            uw.eq(ErpK3Order::getImportedQty, old);
        }
        if (k3Mapper.update(null, uw) == 0) {
            throw new BizException("引入量回写失败：订单被并发引入，请刷新后重试");
        }
        return craft.getId();
    }

    /** 提交：编制中→已提交，发布事件（监听方自动生成生产任务） */
    @Transactional
    public void submit(List<Long> ids) {
        for (Long id : ids) {
            PlanCraft craft = requireCraft(id);
            String to = flowEngine.fire("plan_craft", craft.getDocStatus(), "submit", roles());
            if (craftMapper.update(null, Wrappers.<PlanCraft>lambdaUpdate()
                    .eq(PlanCraft::getId, id).eq(PlanCraft::getDocStatus, craft.getDocStatus())
                    .set(PlanCraft::getDocStatus, to)) == 0) {
                throw new BizException("工艺单[" + craft.getCraftNo() + "]状态已变更，请刷新后重试");
            }
            publisher.publish(new PlanCraftSubmittedEvent(id, craft.getCraftNo(), craft.getBatchNo()));
        }
    }

    /** 撤回：已提交→编制中；有排程占用拦截，级联作废自动生成的生产任务 */
    @Transactional
    public void withdraw(List<Long> ids) {
        for (Long id : ids) {
            PlanCraft craft = requireCraft(id);
            List<ProdTask> linked = taskMapper.selectList(Wrappers.<ProdTask>lambdaQuery()
                    .eq(ProdTask::getPlanCraftId, id));
            for (ProdTask task : linked) {
                Long occupied = scheduleMapper.selectCount(Wrappers.<ProdSchedule>lambdaQuery()
                        .eq(ProdSchedule::getTaskId, task.getId()));
                if (occupied != null && occupied > 0) {
                    throw new BizException("工艺单[" + craft.getCraftNo() + "]的任务[" + task.getTaskNo()
                            + "]已存在排程，请先撤回制造卡后再撤回工艺单");
                }
            }
            String to = flowEngine.fire("plan_craft", craft.getDocStatus(), "withdraw", roles());
            if (craftMapper.update(null, Wrappers.<PlanCraft>lambdaUpdate()
                    .eq(PlanCraft::getId, id).eq(PlanCraft::getDocStatus, craft.getDocStatus())
                    .set(PlanCraft::getDocStatus, to)) == 0) {
                throw new BizException("工艺单[" + craft.getCraftNo() + "]状态已变更，请刷新后重试");
            }
            // 级联：自动生成的任务 生效→编制中→已作废（沿用 flow 配置，保留痕迹）
            for (ProdTask task : linked) {
                if (BillStatus.EFFECTIVE.equals(task.getDocStatus())) {
                    String s1 = flowEngine.fire("prod_task", task.getDocStatus(), "withdraw", roles());
                    String s2 = flowEngine.fire("prod_task", s1, "void", roles());
                    taskMapper.update(null, Wrappers.<ProdTask>lambdaUpdate()
                            .eq(ProdTask::getId, task.getId())
                            .eq(ProdTask::getDocStatus, task.getDocStatus())
                            .set(ProdTask::getDocStatus, s2));
                }
            }
            publisher.publish(new PlanCraftWithdrawnEvent(id, craft.getCraftNo()));
        }
    }

    /** 作废（编制中/已提交→已作废），释放K3引入量 */
    @Transactional
    public void voidCraft(Long id) {
        PlanCraft craft = requireCraft(id);
        String to = flowEngine.fire("plan_craft", craft.getDocStatus(), "void", roles());
        craftMapper.update(null, Wrappers.<PlanCraft>lambdaUpdate()
                .eq(PlanCraft::getId, id).eq(PlanCraft::getDocStatus, craft.getDocStatus())
                .set(PlanCraft::getDocStatus, to));
        releaseImport(craft);
    }

    /** 删除（仅编制中），子表物理删除，释放K3引入量 */
    @Transactional
    public void delete(Long id) {
        PlanCraft craft = requireCraft(id);
        craft.ensureEditable();
        releaseImport(craft);
        craftMapper.deleteById(id);
        prodMapper.delete(Wrappers.<PlanCraftProd>lambdaQuery().eq(PlanCraftProd::getPlanCraftId, id));
        materialMapper.delete(Wrappers.<PlanCraftMaterial>lambdaQuery().eq(PlanCraftMaterial::getPlanCraftId, id));
        rawQcMapper.delete(Wrappers.<PlanCraftRawQc>lambdaQuery().eq(PlanCraftRawQc::getPlanCraftId, id));
        routeMapper.delete(Wrappers.<PlanCraftRoute>lambdaQuery().eq(PlanCraftRoute::getPlanCraftId, id));
        craftItemMapper.delete(Wrappers.<PlanCraftCraft>lambdaQuery().eq(PlanCraftCraft::getPlanCraftId, id));
    }

    private void releaseImport(PlanCraft craft) {
        if (StringUtils.isBlank(craft.getK3OrderNo()) || craft.getPlanQty() == null) {
            return;
        }
        ErpK3Order order = k3Mapper.selectOne(Wrappers.<ErpK3Order>lambdaQuery()
                .eq(ErpK3Order::getOrderNo, craft.getK3OrderNo()).last("LIMIT 1"));
        if (order == null) {
            return;
        }
        BigDecimal old = order.getImportedQty() == null ? BigDecimal.ZERO : order.getImportedQty();
        BigDecimal released = old.subtract(craft.getPlanQty()).max(BigDecimal.ZERO);
        var uw = Wrappers.<ErpK3Order>lambdaUpdate()
                .eq(ErpK3Order::getId, order.getId())
                .set(ErpK3Order::getImportedQty, released);
        if (order.getImportedQty() == null) {
            uw.isNull(ErpK3Order::getImportedQty);
        } else {
            uw.eq(ErpK3Order::getImportedQty, old);
        }
        k3Mapper.update(null, uw);
    }

    private void replaceChildren(Long id, CraftSaveDTO dto) {
        prodMapper.delete(Wrappers.<PlanCraftProd>lambdaQuery().eq(PlanCraftProd::getPlanCraftId, id));
        materialMapper.delete(Wrappers.<PlanCraftMaterial>lambdaQuery().eq(PlanCraftMaterial::getPlanCraftId, id));
        rawQcMapper.delete(Wrappers.<PlanCraftRawQc>lambdaQuery().eq(PlanCraftRawQc::getPlanCraftId, id));
        if (dto.getProds() != null) {
            for (PlanCraftProd p : dto.getProds()) {
                p.setId(null);
                p.setPlanCraftId(id);
                prodMapper.insert(p);
            }
        }
        if (dto.getMaterials() != null) {
            for (PlanCraftMaterial m : dto.getMaterials()) {
                m.setId(null);
                m.setPlanCraftId(id);
                materialMapper.insert(m);
            }
        }
        if (dto.getRawQcs() != null) {
            for (PlanCraftRawQc q : dto.getRawQcs()) {
                q.setId(null);
                q.setPlanCraftId(id);
                rawQcMapper.insert(q);
            }
        }
    }

    private Map<Long, String> processNames(List<PlanCraftRoute> routes) {
        Set<Long> ids = routes.stream().map(PlanCraftRoute::getProcessId).filter(r -> r != null).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return processMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(BaseProcess::getId, BaseProcess::getProcessName));
    }

    private String processName(Long processId) {
        if (processId == null) {
            return null;
        }
        BaseProcess p = processMapper.selectById(processId);
        return p == null ? null : p.getProcessName();
    }

    private PlanCraft requireCraft(Long id) {
        PlanCraft craft = craftMapper.selectById(id);
        if (craft == null) {
            throw new BizException("计划工艺单不存在: " + id);
        }
        return craft;
    }

    private Set<String> roles() {
        var user = AuthContext.get();
        return user == null || user.getRoles() == null ? Set.of() : user.getRoles();
    }
}
