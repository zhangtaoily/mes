package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.exception.BizException;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 上批汇报（聚合根：工序标识/工艺项子表；PDA 与 PC 同源） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rep_batch_on")
public class RepBatchOn extends BaseEntity {

    public static final int SOURCE_PC = 1;
    public static final int SOURCE_PDA = 2;

    /** 上批编号:SR26092900442 */
    private String batchOnNo;
    /** 排程号 */
    private Long scheduleId;
    /** 车间 */
    private Long workshopId;
    /** 班组 */
    private Long teamId;
    /** 班次 */
    private String shift;
    /** 批号 */
    private String batchNo;
    /** 追溯批号 */
    private String traceBatchNo;
    /** 工序 */
    private Long processId;
    /** 工序标识:浅三粗纱管 */
    private String processFlag;
    /** 产品编码 */
    private String productCode;
    /** 产品名称 */
    private String productName;
    /** 批号标识ID（工序标识牌） */
    private Long batchFlagId;
    /** 机台 */
    private Long equipmentId;
    /** 是否下批（0否/1是） */
    private Integer isBatchOff;
    /** 是否了批（1=是，了批后封账不可再上批/报产） */
    private Integer isFinished;
    /** 上批时间 */
    private LocalDateTime batchOnTime;
    /** 下批时间 */
    private LocalDateTime batchOffTime;
    /** 下批人 */
    private String batchOffUser;
    /** 计划产量 */
    private BigDecimal planQty;
    /** 备注 */
    private String remark;
    /** 来源：1PC 2PDA */
    private Integer source;

    /** 下批前置校验：仅活动上批可下批 */
    public void ensureOffable() {
        if (isBatchOff != null && isBatchOff == 1) {
            throw new BizException("上批记录[" + batchOnNo + "]已下批，不可重复操作");
        }
    }
}
