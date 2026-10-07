package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.mybatis.BaseEntity;
import com.xinao.mes.engine.flow.BillStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 生产任务（聚合根：订单明细/用料/工序路线/工艺配置/其它信息） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prod_task")
public class ProdTask extends BaseEntity {

    /** 任务编号（如 PI...） */
    private String taskNo;
    /** 来源计划工艺单（submit 自动生成任务时回链） */
    private Long planCraftId;
    /** 主任务/子任务 */
    private String taskType;
    /** 普通/复精梳/倒筒 */
    private String lineVariant;
    /** 编制中/生效/了批/已关闭/已作废（flow 引擎驱动） */
    private String docStatus;
    /** 计划执行/工艺控制 */
    private String execStatus;
    /** 生产批号（如 J2609988-10） */
    private String batchNo;
    /** 车间ID */
    private Long workshopId;
    /** 产线ID */
    private Long prodLineId;
    /** 批号标识ID */
    private Long batchFlagId;
    /** 原料 */
    private String rawMaterial;
    /** 色号 */
    private String colorNo;
    /** 产品编号 */
    private String productNo;
    /** 品名规格 */
    private String productSpec;
    /** 实纺支数 */
    private String actualCount;
    /** 合并投料量 */
    private BigDecimal mergeFeedQty;
    /** 计划投料量 */
    private BigDecimal planFeedQty;
    /** 计划产量 */
    private BigDecimal planQty;
    /** 单位 */
    private String unit;
    /** 生产订单号（K3） */
    private String k3OrderNo;
    /** 下单日期 */
    private LocalDate orderDate;
    /** 预计交期 */
    private LocalDate deliveryDate;
    /** 纺纱方法（如 进口紧密纺） */
    private String spinMethod;
    /** 原料品种（如 长毛） */
    private String rawVariety;
    /** 原料类型（如 全毛） */
    private String rawType;
    /** 股数/支数 */
    private String plyCount;
    /** 了批（1是 0否） */
    private Integer isFinished;
    /** 关闭（1是 0否） */
    private Integer isClosed;
    /** 备注 */
    private String remark;

    public void ensureEditable() {
        if (!BillStatus.DRAFT.equals(docStatus)) {
            throw new BizException("生产任务[" + taskNo + "]仅[编制中]可操作，当前: " + docStatus);
        }
    }

    /** 撤回校验：生效且无排程占用才可撤回 */
    public void ensureWithdrawable(long scheduleCount) {
        if (!BillStatus.EFFECTIVE.equals(docStatus)) {
            throw new BizException("生产任务[" + taskNo + "]仅[生效]可撤回，当前: " + docStatus);
        }
        if (scheduleCount > 0) {
            throw new BizException("生产任务[" + taskNo + "]已存在 " + scheduleCount + " 条排程占用，请先撤回排程");
        }
    }
}
