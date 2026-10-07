package com.xinao.mes.plan;

import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.engine.flow.BillStatus;
import com.xinao.mes.plan.domain.model.ErpK3Order;
import com.xinao.mes.plan.domain.model.PlanCraft;
import com.xinao.mes.plan.domain.model.PlanCraftRoute;
import com.xinao.mes.plan.domain.model.ProdMachineCard;
import com.xinao.mes.plan.domain.model.ProdTask;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2 不变量单测（纯领域对象，无 Spring 上下文）：
 * 引入量≤订单数量 / 工序路线完整才能生成排程 / 撤回前置校验
 */
class PlanInvariantTest {

    // ---------- 不变量1：引入量 ≤ 订单数量 ----------

    @Test
    void k3ImportQtyMustNotExceedRemaining() {
        ErpK3Order order = order(new BigDecimal("3000"), new BigDecimal("1000"));
        assertEquals(0, order.remainingQty().compareTo(new BigDecimal("2000")));
        assertDoesNotThrow(() -> order.checkImportable(new BigDecimal("2000")));
        BizException ex = assertThrows(BizException.class, () -> order.checkImportable(new BigDecimal("2001")));
        assertTrue(ex.getMessage().contains("超订单剩余可引入"));
    }

    @Test
    void k3ImportQtyMustBePositive() {
        ErpK3Order order = order(new BigDecimal("3000"), BigDecimal.ZERO);
        assertThrows(BizException.class, () -> order.checkImportable(BigDecimal.ZERO));
        assertThrows(BizException.class, () -> order.checkImportable(new BigDecimal("-1")));
        assertThrows(BizException.class, () -> order.checkImportable(null));
    }

    @Test
    void k3NullImportedMeansFullRemaining() {
        ErpK3Order order = order(new BigDecimal("1500"), null);
        assertEquals(0, order.remainingQty().compareTo(new BigDecimal("1500")));
    }

    // ---------- 不变量2：工序路线完整才能生成排程 ----------

    @Test
    void scheduleRequiresCompleteRoutes() {
        PlanCraft craft = craft(BillStatus.SUBMITTED);
        // 空路线
        BizException empty = assertThrows(BizException.class, () -> craft.ensureCanSchedule(List.of()));
        assertTrue(empty.getMessage().contains("工序路线为空"));
        // 有未选工序的行
        PlanCraftRoute hole = route(1L, null);
        BizException incomplete = assertThrows(BizException.class, () -> craft.ensureCanSchedule(List.of(hole)));
        assertTrue(incomplete.getMessage().contains("未选择工序"));
        // 完整路线通过
        assertDoesNotThrow(() -> craft.ensureCanSchedule(List.of(route(1L, 1L), route(2L, 9L))));
    }

    @Test
    void scheduleRequiresSubmittedCraft() {
        PlanCraft draft = craft(BillStatus.DRAFT);
        BizException ex = assertThrows(BizException.class,
                () -> draft.ensureCanSchedule(List.of(route(1L, 1L))));
        assertTrue(ex.getMessage().contains("请先提交"));
        assertThrows(BizException.class, () -> craft(BillStatus.VOIDED).ensureCanSchedule(List.of(route(1L, 1L))));
    }

    @Test
    void craftOnlyEditableInDraft() {
        assertDoesNotThrow(() -> craft(BillStatus.DRAFT).ensureEditable());
        assertThrows(BizException.class, () -> craft(BillStatus.SUBMITTED).ensureEditable());
        assertThrows(BizException.class, () -> craft(BillStatus.EFFECTIVE).ensureEditable());
    }

    // ---------- 撤回前置校验 ----------

    @Test
    void taskWithdrawableOnlyWhenEffectiveAndNotOccupied() {
        ProdTask task = task(BillStatus.EFFECTIVE);
        assertDoesNotThrow(() -> task.ensureWithdrawable(0));
        BizException occupied = assertThrows(BizException.class, () -> task.ensureWithdrawable(2));
        assertTrue(occupied.getMessage().contains("排程占用"));
        assertThrows(BizException.class, () -> task(BillStatus.DRAFT).ensureWithdrawable(0));
    }

    @Test
    void cardWithdrawableOnlyWhenDoingAndNotWithdrawn() {
        ProdMachineCard card = card(ProdMachineCard.STATUS_DOING, null);
        assertDoesNotThrow(card::ensureWithdrawable);
        assertThrows(BizException.class, () -> card(ProdMachineCard.STATUS_OFF, null).ensureWithdrawable());
        assertThrows(BizException.class,
                () -> card(ProdMachineCard.STATUS_DOING, java.time.LocalDateTime.now()).ensureWithdrawable());
    }

    // ---------- 构造 ----------

    private ErpK3Order order(BigDecimal k3Qty, BigDecimal importedQty) {
        ErpK3Order o = new ErpK3Order();
        o.setOrderNo("FBDC772348");
        o.setK3Qty(k3Qty);
        o.setImportedQty(importedQty);
        o.setUnit("KG");
        return o;
    }

    private PlanCraft craft(String status) {
        PlanCraft c = new PlanCraft();
        c.setCraftNo("PI20260930000001");
        c.setDocStatus(status);
        c.setBatchNo("26091340");
        return c;
    }

    private PlanCraftRoute route(Long id, Long processId) {
        PlanCraftRoute r = new PlanCraftRoute();
        r.setId(id);
        r.setSeq(id == null ? 1 : id.intValue());
        r.setProcessId(processId);
        return r;
    }

    private ProdTask task(String status) {
        ProdTask t = new ProdTask();
        t.setTaskNo("PT20260930000001");
        t.setDocStatus(status);
        return t;
    }

    private ProdMachineCard card(String status, java.time.LocalDateTime withdrawTime) {
        ProdMachineCard c = new ProdMachineCard();
        c.setCardNo("MC20260930000001");
        c.setStatus(status);
        c.setWithdrawTime(withdrawTime);
        return c;
    }
}
