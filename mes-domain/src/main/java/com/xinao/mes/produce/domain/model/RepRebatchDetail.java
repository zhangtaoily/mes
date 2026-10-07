package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 翻批记录明细（批次上下批时间与人员，翻批效率分析数据源） */
@Data
@TableName("rep_rebatch_detail")
public class RepRebatchDetail {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 翻批日志ID（关联 rep_rebatch_log.id） */
    private Long rebatchLogId;
    /** 车间 */
    private Long workshopId;
    /** 班组 */
    private Long teamId;
    /** 批号 */
    private String batchNo;
    /** 追溯批号 */
    private String traceBatchNo;
    /** 原料 */
    private String rawMaterial;
    /** 产品编码 */
    private String productNo;
    /** 产品规格 */
    private String productSpec;
    /** 工序 */
    private Long processId;
    /** 机台 */
    private Long equipmentId;
    /** 是否下批（0否/1是） */
    private Integer isBatchOff;
    /** 是否了批（0否/1是） */
    private Integer isFinished;
    /** 上批时间 */
    private LocalDateTime batchOnTime;
    /** 上批人 */
    private String batchOnUser;
    /** 下批时间 */
    private LocalDateTime batchOffTime;
    /** 下批人 */
    private String batchOffUser;
}
