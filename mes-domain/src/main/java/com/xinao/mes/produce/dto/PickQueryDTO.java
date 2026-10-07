package com.xinao.mes.produce.dto;

import lombok.Data;

/** 领退料确认分页查询入参 */
@Data
public class PickQueryDTO {

    private String orderNo;
    private String batchNo;
    private Long workshopId;
    private Integer pickType;
    private String docStatus;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
