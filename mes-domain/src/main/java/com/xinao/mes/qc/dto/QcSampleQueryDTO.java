package com.xinao.mes.qc.dto;

import lombok.Data;

/** 抽样登记分页查询入参 */
@Data
public class QcSampleQueryDTO {

    private String batchNo;
    private Long workshopId;
    private Integer isRegistered;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
