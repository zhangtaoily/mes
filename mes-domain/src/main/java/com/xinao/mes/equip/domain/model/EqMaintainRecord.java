package com.xinao.mes.equip.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 保养记录（无逻辑删除列，物理删除） */
@Data
@TableName("eq_maintain_record")
public class EqMaintainRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 记录编码 */
    private String recordCode;
    /** 保养计划ID */
    private Long maintainPlanId;
    /** 车间ID */
    private Long workshopId;
    /** 机台ID */
    private Long equipmentId;
    /** 保养类型 */
    private String maintainType;
    /** 保养项目 */
    private String maintainItem;
    /** 预计保养日期 */
    private LocalDate expectDate;
    /** 保养人 */
    private String maintainUser;
    /** 保养完成日期 */
    private LocalDate finishDate;
    /** 是否完成 */
    private Integer isFinish;
    /** 保养详情 */
    private String detail;
    /** 备注 */
    private String remark;

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
