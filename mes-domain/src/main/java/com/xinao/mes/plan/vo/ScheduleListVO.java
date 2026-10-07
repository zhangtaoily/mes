package com.xinao.mes.plan.vo;

import com.xinao.mes.plan.domain.model.ProdSchedule;
import lombok.Data;

/** 排程列表行（带车间/工序/机台名称） */
@Data
public class ScheduleListVO {

    private ProdSchedule schedule;
    private String workshopName;
    private String processName;
    private String machineNo;
    private String taskNo;
}
