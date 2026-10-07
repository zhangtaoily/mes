package com.xinao.mes.plan.domain.event;

import com.xinao.mes.engine.event.DomainEvent;

import java.time.LocalDateTime;

/** 计划工艺单已提交（监听方：自动生成生产任务） */
public class PlanCraftSubmittedEvent implements DomainEvent {

    public final Long craftId;
    public final String craftNo;
    public final String batchNo;
    private final LocalDateTime occurredOn = LocalDateTime.now();

    public PlanCraftSubmittedEvent(Long craftId, String craftNo, String batchNo) {
        this.craftId = craftId;
        this.craftNo = craftNo;
        this.batchNo = batchNo;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }
}
