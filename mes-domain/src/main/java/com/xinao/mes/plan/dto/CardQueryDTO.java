package com.xinao.mes.plan.dto;

import lombok.Data;

/** 上机制造卡分页查询入参 */
@Data
public class CardQueryDTO {

    private String status;
    private String batchNo;
    private String cardNo;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
