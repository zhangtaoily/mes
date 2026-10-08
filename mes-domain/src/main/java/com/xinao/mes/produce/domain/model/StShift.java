package com.xinao.mes.produce.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 车间排班（st_shift，无逻辑删除列，物理删除） */
@Data
@TableName("st_shift")
public class StShift implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 车间 */
    private Long workshopId;
    /** 班组 */
    private Long teamId;
    /** 班次:常日班/甲乙丙班 */
    private String shiftName;
    /** 上班时间 */
    private String onDutyTime;
    /** 下班时间 */
    private String offDutyTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
