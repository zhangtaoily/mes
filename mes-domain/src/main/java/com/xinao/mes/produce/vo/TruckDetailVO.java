package com.xinao.mes.produce.vo;

import com.xinao.mes.produce.domain.model.RepTruck;
import com.xinao.mes.produce.domain.model.RepTruckMove;
import lombok.Data;

import java.util.List;

/** 周转车详情（移动轨迹） */
@Data
public class TruckDetailVO {

    private RepTruck truck;
    private List<RepTruckMove> moves;
}
