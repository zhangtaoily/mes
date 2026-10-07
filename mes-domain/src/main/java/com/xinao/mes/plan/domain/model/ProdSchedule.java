package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xinao.mes.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 生产排程（聚合根：上机工艺/用料汇总/任务明细） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("prod_schedule")
public class ProdSchedule extends BaseEntity {

    /** 排程编号（如 PSC260927000030） */
    private String scheduleNo;
    /** 工单单号（如 TPC2026091600005，自动生成） */
    private String workOrderNo;
    /** 车间ID */
    private Long workshopId;
    /** 任务ID */
    private Long taskId;
    /** 批次信息 */
    private String batchNo;
    /** 追溯批次 */
    private String traceBatchNo;
    /** 批号标识ID */
    private Long batchFlagId;
    /** 工序ID */
    private Long processId;
    /** 机型 */
    private String machineModel;
    /** 换机型 */
    private String changeModel;
    /** 机台ID */
    private Long equipmentId;
    /** 排程日期 */
    private LocalDate scheduleDate;
    /** 计划开始时间 */
    private LocalDateTime planStart;
    /** 计划结束时间 */
    private LocalDateTime planEnd;
    /** 排程数量 */
    private BigDecimal scheduleQty;
    /** 序号 */
    private Integer seq;
    /** 备注 */
    private String remark;
}
