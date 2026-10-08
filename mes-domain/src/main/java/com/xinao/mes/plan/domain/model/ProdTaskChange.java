package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 任务变更（无审批流：编制中→已提交；提交时新值回写原任务） */
@Data
@TableName("prod_task_change")
public class ProdTaskChange {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务变更单号 */
    private String changeNo;
    /** 原任务ID */
    private Long taskId;
    /** 编制中/已提交 */
    private String docStatus;
    /** 变更内容说明 */
    private String changeContent;
    /** 新计划投料量 */
    private BigDecimal newPlanFeedQty;
    /** 新计划产量 */
    private BigDecimal newPlanQty;
    /** 新交期 */
    private LocalDate newDeliveryDate;

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
