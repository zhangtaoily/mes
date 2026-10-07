package com.xinao.mes.produce.dto;

import lombok.Data;

/** 周转车分页查询入参 */
@Data
public class TruckQueryDTO {

    private String truckNo;
    private String batchNo;
    private Long workshopId;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
