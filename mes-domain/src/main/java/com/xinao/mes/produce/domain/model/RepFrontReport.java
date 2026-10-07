package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 前纺生产情况汇报（挂工艺管理菜单，前纺过程工艺异常记录；绕毛类字段为三级枚举：无/轻微/严重） */
@Data
@TableName("rep_front_report")
public class RepFrontReport {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 车间 */
    private Long workshopId;
    /** 生产批号 */
    private String batchNo;
    /** 汇报日期 */
    private LocalDate reportDate;
    /** 温度 */
    private BigDecimal temperature;
    /** 湿度 */
    private BigDecimal humidity;
    /** 原料含油 */
    private BigDecimal rawOil;
    /** 成品含油 */
    private BigDecimal productOil;
    /** 机型 */
    private String machineModel;
    /** 绕针板(无/轻微/严重) */
    private String wrapComber;
    /** 绕圆梳 */
    private String wrapCircleComber;
    /** 绕气泡 */
    private String wrapBubble;
    /** 绕皮辊 */
    private String wrapApron;
    /** 绕罗拉 */
    private String wrapRoller;
    /** 撒滑石粉 */
    private String useTalcum;
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
