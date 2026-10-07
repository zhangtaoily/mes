package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.mybatis.BaseEntity;
import com.xinao.mes.engine.flow.BillStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 计划工艺单（聚合根：工艺路线/工艺项/用料/生产信息/原料检验快照） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("plan_craft")
public class PlanCraft extends BaseEntity {

    /** 计划工艺单号 */
    private String craftNo;
    /** 编制中/已提交/生效/已关闭/已作废（flow 引擎驱动） */
    private String docStatus;
    /** 车间ID */
    private Long workshopId;
    /** 生产线ID */
    private Long prodLineId;
    /** 生产批次 */
    private String batchNo;
    /** 追溯批次 */
    private String traceBatchNo;
    /** 批号标识ID */
    private Long batchFlagId;
    /** 原料明细 */
    private String rawMaterial;
    /** 原料规格 */
    private String rawSpec;
    /** 色号 */
    private String colorNo;
    /** 实纺支数 */
    private String actualCount;
    /** 投料量 */
    private BigDecimal feedQty;
    /** 合并投料量 */
    private BigDecimal mergeFeedQty;
    /** 生产订单(K3) */
    private String k3OrderNo;
    /** 下单日期 */
    private LocalDate orderDate;
    /** 预计交期 */
    private LocalDate deliveryDate;
    /** 生产标识 */
    private String prodFlag;
    /** 计划产量 */
    private BigDecimal planQty;
    /** 特殊工艺要求 */
    private String specialReq;
    /** 计划工艺纺纱要求 */
    private String planSpinReq;
    /** 技术工艺纺纱要求 */
    private String techSpinReq;
    /** 工艺及倍捻要求 */
    private String craftTwistReq;
    /** 成品要求 */
    private String productReq;
    /** 标签要求 */
    private String labelReq;
    /** 包装要求 */
    private String packReq;
    /** 前纺备注 */
    private String foreRemark;
    /** 翻槽纺纱备注 */
    private String grooveRemark;
    /** 后纺备注 */
    private String afterRemark;
    /** 备注 */
    private String remark;

    /** 仅编制中可编辑/删除 */
    public void ensureEditable() {
        if (!BillStatus.DRAFT.equals(docStatus)) {
            throw new BizException("计划工艺单[" + craftNo + "]仅[编制中]可操作，当前: " + docStatus);
        }
    }

    /** 生成排程前置校验：状态可用 + 工序路线完整 */
    public void ensureCanSchedule(List<PlanCraftRoute> routes) {
        if (BillStatus.DRAFT.equals(docStatus)) {
            throw new BizException("计划工艺单[" + craftNo + "]为[编制中]，请先提交后再生成排程");
        }
        if (BillStatus.VOIDED.equals(docStatus) || BillStatus.CLOSED.equals(docStatus)) {
            throw new BizException("计划工艺单[" + craftNo + "]状态为[" + docStatus + "]，不可生成排程");
        }
        ensureRouteComplete(routes);
    }

    /** 不变量：工序路线完整（至少一道工序且都已选工序）才能生成排程 */
    public void ensureRouteComplete(List<PlanCraftRoute> routes) {
        if (routes == null || routes.isEmpty()) {
            throw new BizException("计划工艺单[" + craftNo + "]工序路线为空，请先编制工序路线");
        }
        long noProcess = routes.stream().filter(r -> r.getProcessId() == null).count();
        if (noProcess > 0) {
            throw new BizException("计划工艺单[" + craftNo + "]有 " + noProcess + " 行工序未选择工序，请补全后再生成排程");
        }
    }
}
