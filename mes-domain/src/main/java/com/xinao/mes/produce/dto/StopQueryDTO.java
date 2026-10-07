package com.xinao.mes.produce.dto;

import lombok.Data;

import java.time.LocalDate;

/** 启停汇报分页查询入参 */
@Data
public class StopQueryDTO {

    private Long workshopId;
    private Long equipmentId;
    private String stopNo;
    private LocalDate reportDateBegin;
    private LocalDate reportDateEnd;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
