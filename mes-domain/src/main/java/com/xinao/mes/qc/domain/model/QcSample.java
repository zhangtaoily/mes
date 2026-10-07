package com.xinao.mes.qc.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 抽样登记（对下批批次登记抽样；列表含投料量/是否登记/任务了批） */
@Data
@TableName("qc_sample")
public class QcSample {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号 */
    private String batchNo;
    /** 工序ID */
    private Long processId;
    /** 设备ID */
    private Long equipmentId;
    /** 车间ID */
    private Long workshopId;
    /** 投料量 */
    private BigDecimal feedQty;
    /** 上报时间 */
    private LocalDateTime reportTime;
    /** 是否登记 */
    private Integer isRegistered;
    /** 登记时间 */
    private LocalDateTime registerTime;
    /** 是否下批 */
    private Integer isBatchOff;
    /** 下批时间 */
    private LocalDateTime batchOffTime;
    /** 任务了批 */
    private Integer taskFinished;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
