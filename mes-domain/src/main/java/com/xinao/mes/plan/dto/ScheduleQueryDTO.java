package com.xinao.mes.plan.dto;

import lombok.Data;

import java.time.LocalDate;

/** 生产排程分页查询入参 */
@Data
public class ScheduleQueryDTO {

    private LocalDate scheduleDate;
    private Long workshopId;
    private String batchNo;
    private Long processId;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
