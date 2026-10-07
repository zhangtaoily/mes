package com.xinao.mes.plan.dto;

import lombok.Data;

/** K3 订单分页查询入参 */
@Data
public class ErpOrderQueryDTO {

    private String orderNo;
    private String batchNo;
    private String productName;
    /** 是否关闭：null全部 0未关闭 1已关闭 */
    private Integer isClosed;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
