package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 车间出勤（st_attendance，无逻辑删除列，物理删除；唯一键 period_month+workshop_id） */
@Data
@TableName("st_attendance")
public class StAttendance implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 期间(月)，如 2026-10 */
    private String periodMonth;
    /** 车间 */
    private Long workshopId;
    /** 开工天数 */
    private BigDecimal workDays;
    /** 车间管理人员 */
    private Integer managerCount;
    /** 常日班辅助 */
    private Integer dayAuxCount;
    /** 一线员工 */
    private Integer frontlineCount;
    /** 质检组出勤 */
    private Integer qcCount;
    /** 总出勤 */
    private Integer totalCount;
    /** 折算出勤 */
    private BigDecimal convertCount;
    /** 编制中/已提交/审核通过/已撤回 */
    private String docStatus;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
