package com.xinao.mes.produce.dto;

import com.xinao.mes.produce.domain.model.RepTruck;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 周转车保存入参 */
@Data
public class TruckSaveDTO {

    @NotNull(message = "周转车信息不能为空")
    private RepTruck truck;
}
