package com.xinao.mes.plan.dto;

import lombok.Data;

/** 计划工艺单分页查询入参 */
@Data
public class CraftQueryDTO {

    private Long workshopId;
    private String batchNo;
    private String actualCount;
    private String rawMaterial;
    private String docStatus;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
