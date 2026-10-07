package com.xinao.mes.qc.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.mybatis.BaseEntity;
import com.xinao.mes.engine.flow.BillStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 检验报告（聚合根：qc_report_item 指标矩阵纵表）
 *
 * <p>M4 不变量：作废报告不计入统计且可恢复（doc_status=已作废，统计口径排除）；
 * 衍生指标由 script 引擎按 qc_formula 计算（$BQ 取试样值集合），并按方案上下限判定越限。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qc_report")
public class QcReport extends BaseEntity {

    /** 质检单号 QC2026092901254 */
    private String qcNo;
    /** 车间ID */
    private Long workshopId;
    /** 产线ID */
    private Long prodLineId;
    /** 原料检/工序检/成品检 */
    private String qcType;
    /** 0车间 1中心实验室 */
    private Integer labType;
    /** 设备ID */
    private Long equipmentId;
    /** 班组ID */
    private Long teamId;
    /** 生产批次/取样批次 */
    private String batchNo;
    /** 缸号（中心实验室） */
    private String dyeVatNo;
    /** 检验员 */
    private String inspector;
    /** 检验日期 */
    private LocalDate inspectDate;
    /** 取样数量 */
    private BigDecimal sampleQty;
    /** 留样数量 */
    private BigDecimal retainQty;
    /** 质检方案（选定后生成指标矩阵） */
    private Long schemeId;
    /** 原料信息（中心实验室） */
    private String rawMaterial;
    /** 落纺次数 */
    private String spinCountDrop;
    /** 纺纱支数 */
    private String spinCount;
    /** 原料批次号 */
    private String rawBatchNo;
    /** 混合投料量 */
    private BigDecimal mergeFeedQty;
    /** 编制中/已提交/已作废（flow 引擎驱动，作废可取消恢复） */
    private String docStatus;
    /** 是否公示（公示后对外公开检验数据） */
    private Integer isPublic;
    /** 备注 */
    private String remark;

    /** 编制中才可编辑/提交 */
    public void ensureEditable() {
        if (!BillStatus.DRAFT.equals(docStatus)) {
            throw new BizException("检验报告[" + qcNo + "]仅[编制中]可操作，当前: " + docStatus);
        }
    }

    /** 作废/取消作废校验 */
    public void ensureVoidable(boolean voiding) {
        if (voiding && BillStatus.VOIDED.equals(docStatus)) {
            throw new BizException("检验报告[" + qcNo + "]已作废，勿重复作废");
        }
        if (!voiding && !BillStatus.VOIDED.equals(docStatus)) {
            throw new BizException("检验报告[" + qcNo + "]未作废，无需取消作废");
        }
    }
}
