package com.xinao.mes.plan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/** 引入K3订单入参（编辑页「引入K3订单」/ 生产订单 lookup 确认后调用） */
@Data
public class ImportK3DTO {

    @NotNull(message = "K3订单不能为空")
    private Long orderId;
    /** 引入车间（订单车间名匹配不到基础数据时兜底） */
    private Long workshopId;
    /** 引入量（即投料量/计划产量，回写订单引入量） */
    @NotNull(message = "引入量不能为空")
    @Positive(message = "引入量必须大于0")
    private BigDecimal importQty;
}
