package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 投料记录（聚合根：明细来自生产任务用料清单；投料完毕后子批投料封账） */
@Data
@TableName("mt_feed")
public class MtFeed {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 投料单号:MF... */
    private String feedNo;
    /** 生产任务号:PI... */
    private String taskNo;
    /** 生产车间 */
    private Long workshopId;
    /** 工艺单车间 */
    private Long craftWorkshopId;
    /** 班组 */
    private Long teamId;
    /** 班次 */
    private Long shiftId;
    /** 批号 */
    private String batchNo;
    /** 原料明细 */
    private String rawDetail;
    /** 投料量 */
    private BigDecimal feedQty;
    /** 投料完毕（1=是，完毕后投料封账方可了批） */
    private Integer isFinish;
    /** 投料人 */
    private String feedBy;
    /** 投料时间 */
    private LocalDateTime feedTime;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
