package com.xinao.mes.plan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/** 排程-任务明细新增入参（剩余可排实时校验） */
@Data
public class ScheduleTaskAddDTO {

    @NotNull(message = "排程不能为空")
    private Long scheduleId;
    @NotNull(message = "生产任务不能为空")
    private Long taskId;
    /** 排程数量（不可超 任务计划产量-已排产量） */
    @NotNull(message = "排程数量不能为空")
    @Positive(message = "排程数量必须大于0")
    private BigDecimal scheduleQty;
}
