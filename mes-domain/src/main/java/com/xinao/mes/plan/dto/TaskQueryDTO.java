package com.xinao.mes.plan.dto;

import lombok.Data;

/** 生产任务分页查询入参 */
@Data
public class TaskQueryDTO {

    private Long workshopId;
    /** 车间类型过滤：1纺纱 2复精梳 3倒筒 4包装（复精梳/倒筒任务页） */
    private Integer workshopType;
    private String batchNo;
    private String actualCount;
    private String rawMaterial;
    private String docStatus;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
