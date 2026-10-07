package com.xinao.mes.produce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 周转车车辆拆分入参 */
@Data
public class TruckSplitDTO {

    @NotNull(message = "请选择源车")
    private Long truckId;

    @NotBlank(message = "新车号不能为空")
    private String newTruckNo;

    private BigDecimal moveNetWeight;
    private Integer moveBallCount;
    private String toLocation;
}
