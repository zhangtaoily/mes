package com.xinao.mes.pack.dto;

import lombok.Data;

/** 包装执行分页查询入参 */
@Data
public class PkExecuteQueryDTO {

    private String executeNo;
    private String packBatchNo;
    private String warehouseBatchNo;
    private Long pkTaskId;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
