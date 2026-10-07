package com.xinao.mes.qc.dto;

import lombok.Data;

/** 检验报告分页查询入参 */
@Data
public class QcReportQueryDTO {

    private String qcNo;
    private String batchNo;
    private Long workshopId;
    /** 原料检/工序检/成品检 */
    private String qcType;
    private String docStatus;
    /** 仅公示中的报告 */
    private Boolean publicOnly;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
