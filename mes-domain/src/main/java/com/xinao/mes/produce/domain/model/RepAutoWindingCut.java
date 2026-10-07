package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 自络筒切疵记录（自动络筒清纱器切除疵点；切疵类型：电容/光电） */
@Data
@TableName("rep_auto_winding_cut")
public class RepAutoWindingCut {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批号 */
    private String batchNo;
    /** 原料 */
    private String rawMaterial;
    /** 色号 */
    private String colorNo;
    /** 支数 */
    private String countSpec;
    /** 车间 */
    private Long workshopId;
    /** 机台 */
    private Long equipmentId;
    /** 切疵类型：电容/光电 */
    private String cutType;

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
