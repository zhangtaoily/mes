package com.xinao.mes.equip.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 点检记录（无逻辑删除列，物理删除） */
@Data
@TableName("eq_check_record")
public class EqCheckRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 记录编码 */
    private String recordCode;
    /** 点检计划ID */
    private Long checkPlanId;
    /** 车间ID */
    private Long workshopId;
    /** 点检人 */
    private String checkUser;
    /** 点检日期 */
    private LocalDate checkDate;
    /** 描述（列名 describe 为保留字） */
    @TableField("`describe`")
    private String describe;
    /** 备注 */
    private String remark;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
