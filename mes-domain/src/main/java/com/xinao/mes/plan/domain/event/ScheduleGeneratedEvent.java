package com.xinao.mes.plan.domain.event;

import com.xinao.mes.engine.event.DomainEvent;

import java.time.LocalDateTime;

/** 排程已生成（按工艺单工序路线自动生成） */
public class ScheduleGeneratedEvent implements DomainEvent {

    public final Long scheduleId;
    public final String scheduleNo;
    public final Long taskId;
    public final String batchNo;
    private final LocalDateTime occurredOn = LocalDateTime.now();

    public ScheduleGeneratedEvent(Long scheduleId, String scheduleNo, Long taskId, String batchNo) {
        this.scheduleId = scheduleId;
        this.scheduleNo = scheduleNo;
        this.taskId = taskId;
        this.batchNo = batchNo;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }
}
