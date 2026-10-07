package com.xinao.mes.produce.dto;

import lombok.Data;

/** 上批汇报分页查询入参 */
@Data
public class BatchOnQueryDTO {

    private Long workshopId;
    private String batchNo;
    private String batchOnNo;
    private Integer isBatchOff;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
