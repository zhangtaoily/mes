package com.xinao.mes.produce.dto;

import lombok.Data;

import java.time.LocalDate;

/** 产量汇报分页查询入参 */
@Data
public class OutputQueryDTO {

    private String batchNo;
    private String outputNo;
    private Long teamId;
    private Long scheduleId;
    private LocalDate reportDateBegin;
    private LocalDate reportDateEnd;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
