package com.xinao.mes.produce.dto;

import lombok.Data;

/** 投料记录分页查询入参 */
@Data
public class FeedQueryDTO {

    private String feedNo;
    private String batchNo;
    private String taskNo;
    private Long workshopId;
    private Integer isFinish;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
