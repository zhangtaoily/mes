package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 蒸纱信息（车号管理；打印/接收闭环） */
@Data
@TableName("rep_yarn_steam")
public class RepYarnSteam {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 打印编码 */
    private String printCode;
    /** 状态 */
    private String status;
    /** 工序 */
    private Long processId;
    /** 车号 */
    private String truckNo;
    /** 颗数 */
    private Integer grainCount;
    /** 重量 */
    private BigDecimal weight;
    /** 车数 */
    private Integer truckCount;
    /** 批号 */
    private String batchNo;
    /** 打印人 */
    private String printUser;
    /** 打印时间 */
    private LocalDateTime printTime;
    /** 接收人 */
    private String receiveUser;
    /** 接收时间 */
    private LocalDateTime receiveTime;

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
