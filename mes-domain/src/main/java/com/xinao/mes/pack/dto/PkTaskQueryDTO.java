package com.xinao.mes.pack.dto;

import lombok.Data;

/** 包装任务分页查询入参 */
@Data
public class PkTaskQueryDTO {

    private String taskNo;
    private String batchNo;
    private String warehouseBatchNo;
    /** 普通/委外/倒毛 */
    private String taskVariant;
    /** 任务了批 0/1 */
    private Integer isFinished;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
