package com.xinao.mes.produce.dto;

import lombok.Data;

/** 现场汇报通用分页查询入参（前纺汇报/切疵/蒸纱） */
@Data
public class FrontReportQueryDTO {

    private Long workshopId;
    private String batchNo;
    /** 蒸纱：车号/状态 */
    private String truckNo;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
