package com.xinao.mes.equip.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 责任机台（人员-机台点检责任，无逻辑删除列） */
@Data
@TableName("eq_duty_machine")
public class EqDutyMachine {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 车间ID */
    private Long workshopId;
    /** 点检人员 */
    private String checkUser;
    /** 人员工号 */
    private String empNo;
    /** 机台ID */
    private Long equipmentId;
    /** 备注 */
    private String remark;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
