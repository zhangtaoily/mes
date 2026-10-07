package com.xinao.mes.plan.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 生产任务其它信息（环境与工艺开关） */
@Data
@TableName("prod_task_extra")
public class ProdTaskExtra {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务ID */
    private Long taskId;
    /** 生产批号 */
    private String batchNo;
    /** 车间名称 */
    private String workshopName;
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
    /** 绕针板（1是 0否） */
    private Integer wrapNeedle;
    /** 绕圆梳（1是 0否） */
    private Integer wrapRing;
    /** 绕气泡（1是 0否） */
    private Integer wrapBalloon;
    /** 绕皮辊（1是 0否） */
    private Integer wrapRoller;
    /** 绕罗拉（1是 0否） */
    private Integer wrapRoller2;
    /** 撒滑石粉（1是 0否） */
    private Integer talcPowder;
    /** 类型 */
    private String extraType;
    /** 备注 */
    private String remark;
}
