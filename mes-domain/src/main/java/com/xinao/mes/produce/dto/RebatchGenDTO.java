package com.xinao.mes.produce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** 翻批记录生成入参（按车间+班组+时段汇总上批汇报） */
@Data
public class RebatchGenDTO {

    @NotNull(message = "请选择车间")
    private Long workshopId;

    @NotNull(message = "请选择班组")
    private Long teamId;

    @NotNull(message = "请选择开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "请选择结束时间")
    private LocalDateTime endTime;
}
