package com.xinao.mes.equip.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 维保记录（无逻辑删除列，物理删除） */
@Data
@TableName("eq_repair_record")
public class EqRepairRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 维修编码 */
    private String repairCode;
    /** 车间ID */
    private Long workshopId;
    /** 机台ID */
    private Long equipmentId;
    /** 故障发生时间 */
    private LocalDateTime faultTime;
    /** 故障分类ID */
    private Long faultTypeId;
    /** 维修人 */
    private String repairUser;
    /** 故障解决时间 */
    private LocalDateTime solveTime;
    /** 维修方式ID */
    private Long repairMethodId;
    /** 维修详情 */
    private String detail;
    /** 备注 */
    private String remark;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
