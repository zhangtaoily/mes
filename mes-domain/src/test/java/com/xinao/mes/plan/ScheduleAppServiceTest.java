package com.xinao.mes.plan;

import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.engine.code.CodeEngine;
import com.xinao.mes.engine.event.MesEventPublisher;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.plan.application.ProdTaskAppService;
import com.xinao.mes.plan.application.ScheduleAppService;
import com.xinao.mes.plan.domain.model.BaseEquipment;
import com.xinao.mes.plan.domain.model.PlanCraft;
import com.xinao.mes.plan.domain.model.PlanCraftCraft;
import com.xinao.mes.plan.domain.model.PlanCraftRoute;
import com.xinao.mes.plan.domain.model.ProdSchedule;
import com.xinao.mes.plan.domain.model.ProdScheduleTask;
import com.xinao.mes.plan.domain.model.ProdTask;
import com.xinao.mes.plan.dto.ScheduleTaskAddDTO;
import com.xinao.mes.plan.infrastructure.mapper.BaseEquipmentMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseProcessMapper;
import com.xinao.mes.plan.infrastructure.mapper.BaseWorkshopMapper;
import com.xinao.mes.plan.infrastructure.mapper.FlowCardPrintLogMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.PlanCraftRouteMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdMachineCardMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleCraftMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleMaterialMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdScheduleTaskMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskMapper;
import com.xinao.mes.plan.infrastructure.mapper.ProdTaskOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 排程应用服务单测：生成排程（自动分配/末道占用）+ 排程量≤剩余可排 不变量 + 重复生成拦截
 */
class ScheduleAppServiceTest {

    private ProdScheduleMapper scheduleMapper;
    private ProdScheduleTaskMapper scheduleTaskMapper;
    private PlanCraftMapper craftMapper;
    private PlanCraftRouteMapper craftRouteMapper;
    private PlanCraftCraftMapper craftItemMapper;
    private PlanCraftMaterialMapper craftMaterialMapper;
    private ProdTaskMapper taskMapper;
    private ProdTaskOrderMapper taskOrderMapper;
    private BaseEquipmentMapper equipmentMapper;
    private CodeEngine codeEngine;
    private ProdTaskAppService prodTaskAppService;
    private ScheduleAppService service;

    private final AtomicLong idGen = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        scheduleMapper = mock(ProdScheduleMapper.class);
        scheduleTaskMapper = mock(ProdScheduleTaskMapper.class);
        craftMapper = mock(PlanCraftMapper.class);
        craftRouteMapper = mock(PlanCraftRouteMapper.class);
        craftItemMapper = mock(PlanCraftCraftMapper.class);
        craftMaterialMapper = mock(PlanCraftMaterialMapper.class);
        taskMapper = mock(ProdTaskMapper.class);
        taskOrderMapper = mock(ProdTaskOrderMapper.class);
        equipmentMapper = mock(BaseEquipmentMapper.class);
        codeEngine = mock(CodeEngine.class);
        prodTaskAppService = mock(ProdTaskAppService.class);
        service = new ScheduleAppService(
                scheduleMapper,
                mock(ProdScheduleCraftMapper.class),
                mock(ProdScheduleMaterialMapper.class),
                scheduleTaskMapper,
                mock(ProdMachineCardMapper.class),
                mock(FlowCardPrintLogMapper.class),
                craftMapper,
                craftRouteMapper,
                craftItemMapper,
                craftMaterialMapper,
                taskMapper,
                taskOrderMapper,
                equipmentMapper,
                mock(BaseProcessMapper.class),
                mock(BaseWorkshopMapper.class),
                codeEngine,
                mock(MesEventPublisher.class),
                prodTaskAppService);
        // insert 时为实体分配自增id，模拟 DB 行为
        doAnswer(inv -> {
            ProdSchedule s = inv.getArgument(0);
            s.setId(idGen.incrementAndGet());
            return 1;
        }).when(scheduleMapper).insert(any(ProdSchedule.class));
    }

    // ---------- 生成排程：按工序路线逐工序 + 末道工序占用 ----------

    @Test
    void generateFromCraftCreatesSchedulePerRouteAndOccupiesLast() {
        PlanCraft craft = craft(BillStatus.SUBMITTED);
        ProdTask task = task(BillStatus.EFFECTIVE, new BigDecimal("3000"));
        PlanCraftRoute r1 = route(11L, 1, 1L);
        PlanCraftRoute r2 = route(12L, 2, 9L);
        when(craftMapper.selectById(1L)).thenReturn(craft);
        when(craftRouteMapper.selectList(any())).thenReturn(List.of(r1, r2));
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(scheduleMapper.selectCount(any())).thenReturn(0L);
        when(codeEngine.next(eq("工单单号"), any())).thenReturn("TPC20260930000001");
        when(codeEngine.next(eq("生产排程"), any())).thenReturn("PSC20260930000001");
        BaseEquipment eq = new BaseEquipment();
        eq.setId(7L);
        eq.setMachineModel("VTS-08");
        when(equipmentMapper.selectOne(any())).thenReturn(eq);
        when(craftItemMapper.selectList(any())).thenReturn(List.of(craftItem(12L, "车速", "9500")));
        when(craftMaterialMapper.selectList(any())).thenReturn(List.of());
        when(prodTaskAppService.occupiedQty(99L)).thenReturn(BigDecimal.ZERO);
        when(taskOrderMapper.selectList(any())).thenReturn(List.of());

        var result = service.generateFromCraft(1L);

        assertEquals(2, ((List<?>) result.get("scheduleIds")).size());
        // 两道工序各一条排程
        verify(scheduleMapper, times(2)).insert(any(ProdSchedule.class));
        // 仅末道工序插入任务明细占用
        ArgumentCaptor<ProdScheduleTask> captor = ArgumentCaptor.forClass(ProdScheduleTask.class);
        verify(scheduleTaskMapper, times(1)).insert(captor.capture());
        ProdScheduleTask occupation = captor.getValue();
        assertEquals(99L, occupation.getTaskId());
        assertEquals(0, occupation.getScheduleQty().compareTo(new BigDecimal("3000")));
    }

    @Test
    void generateFromCraftBlocksWhenAlreadyGenerated() {
        PlanCraft craft = craft(BillStatus.SUBMITTED);
        ProdTask task = task(BillStatus.EFFECTIVE, new BigDecimal("3000"));
        when(craftMapper.selectById(1L)).thenReturn(craft);
        when(craftRouteMapper.selectList(any())).thenReturn(List.of(route(11L, 1, 1L)));
        when(taskMapper.selectList(any())).thenReturn(List.of(task));
        when(scheduleMapper.selectCount(any())).thenReturn(2L);

        BizException ex = assertThrows(BizException.class, () -> service.generateFromCraft(1L));
        assertTrue(ex.getMessage().contains("已生成过排程"));
    }

    @Test
    void generateFromCraftBlocksWhenNoEffectiveTask() {
        PlanCraft craft = craft(BillStatus.SUBMITTED);
        when(craftMapper.selectById(1L)).thenReturn(craft);
        when(craftRouteMapper.selectList(any())).thenReturn(List.of(route(11L, 1, 1L)));
        when(taskMapper.selectList(any())).thenReturn(List.of());

        BizException ex = assertThrows(BizException.class, () -> service.generateFromCraft(1L));
        assertTrue(ex.getMessage().contains("生产任务"));
    }

    // ---------- 排程量 ≤ 剩余可排 ----------

    @Test
    void addTaskDetailRejectsQtyOverRemaining() {
        ProdSchedule schedule = new ProdSchedule();
        schedule.setId(5L);
        schedule.setProcessId(9L);
        ProdTask task = task(BillStatus.EFFECTIVE, new BigDecimal("1000"));
        task.setUnit("KG");
        when(scheduleMapper.selectById(5L)).thenReturn(schedule);
        when(taskMapper.selectById(99L)).thenReturn(task);
        when(prodTaskAppService.occupiedQty(99L)).thenReturn(new BigDecimal("300"));

        ScheduleTaskAddDTO over = addDTO(5L, 99L, "701");
        BizException ex = assertThrows(BizException.class, () -> service.addTaskDetail(over));
        assertTrue(ex.getMessage().contains("超剩余可排"));

        // 恰好等于剩余量可提交
        service.addTaskDetail(addDTO(5L, 99L, "700"));
        ArgumentCaptor<ProdScheduleTask> captor = ArgumentCaptor.forClass(ProdScheduleTask.class);
        verify(scheduleTaskMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getScheduleQty().compareTo(new BigDecimal("700")));
        assertEquals(0, captor.getValue().getScheduledQty().compareTo(new BigDecimal("1000")));
    }

    @Test
    void addTaskDetailRejectsNonEffectiveTask() {
        ProdSchedule schedule = new ProdSchedule();
        schedule.setId(5L);
        schedule.setProcessId(9L);
        when(scheduleMapper.selectById(5L)).thenReturn(schedule);
        when(taskMapper.selectById(99L)).thenReturn(task(BillStatus.DRAFT, new BigDecimal("1000")));

        assertThrows(BizException.class, () -> service.addTaskDetail(addDTO(5L, 99L, "100")));
    }

    // ---------- 构造 ----------

    private PlanCraft craft(String status) {
        PlanCraft c = new PlanCraft();
        c.setId(1L);
        c.setCraftNo("PI20260930000001");
        c.setDocStatus(status);
        c.setWorkshopId(1L);
        c.setBatchNo("26091340");
        c.setPlanQty(new BigDecimal("3000"));
        return c;
    }

    private ProdTask task(String status, BigDecimal planQty) {
        ProdTask t = new ProdTask();
        t.setId(99L);
        t.setTaskNo("PT20260930000001");
        t.setDocStatus(status);
        t.setPlanQty(planQty);
        return t;
    }

    private PlanCraftRoute route(Long id, int seq, Long processId) {
        PlanCraftRoute r = new PlanCraftRoute();
        r.setId(id);
        r.setSeq(seq);
        r.setProcessId(processId);
        r.setPlanCraftId(1L);
        r.setPlanQty(new BigDecimal("3000"));
        return r;
    }

    private PlanCraftCraft craftItem(Long routeId, String name, String value) {
        PlanCraftCraft c = new PlanCraftCraft();
        c.setRouteId(routeId);
        c.setProcessId(9L);
        c.setParamName(name);
        c.setParamValue(value);
        return c;
    }

    private ScheduleTaskAddDTO addDTO(Long scheduleId, Long taskId, String qty) {
        ScheduleTaskAddDTO dto = new ScheduleTaskAddDTO();
        dto.setScheduleId(scheduleId);
        dto.setTaskId(taskId);
        dto.setScheduleQty(new BigDecimal(qty));
        return dto;
    }
}
