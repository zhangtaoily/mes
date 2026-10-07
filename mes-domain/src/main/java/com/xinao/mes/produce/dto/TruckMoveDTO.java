package com.xinao.mes.produce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 周转车移动动作入参（opName：绑定/称重/入库/移库/出库） */
@Data
public class TruckMoveDTO {

    @NotNull(message = "请选择周转车")
    private Long truckId;

    @NotBlank(message = "操作名称不能为空")
    private String opName;

    private String toLocation;
    private String scanWorkshop;
    private String recombedBatchNo;
    private String spinBatchNo;
    private String bagColor;
    private Integer ballCount;
    private BigDecimal grossWeight;
    private BigDecimal netWeight;
    private BigDecimal standardWeight;
    private BigDecimal moistureStandard;
    private BigDecimal moistureActual;
    private BigDecimal moveNetWeight;
    private Integer moveBallCount;
    private Integer isFirst;
    private Integer isFinished;
}
