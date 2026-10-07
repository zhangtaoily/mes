package com.xinao.mes.plan.dto;

import lombok.Data;

/** 生产任务分页查询入参 */
@Data
public class TaskQueryDTO {

    private Long workshopId;
    private String batchNo;
    private String actualCount;
    private String rawMaterial;
    private String docStatus;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
