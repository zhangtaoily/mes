package com.xinao.mes.plan.domain.event;

import com.xinao.mes.engine.event.DomainEvent;

import java.time.LocalDateTime;

/** 计划工艺单已撤回（回到编制中，生成的任务可作废重排） */
public class PlanCraftWithdrawnEvent implements DomainEvent {

    public final Long craftId;
    public final String craftNo;
    private final LocalDateTime occurredOn = LocalDateTime.now();

    public PlanCraftWithdrawnEvent(Long craftId, String craftNo) {
        this.craftId = craftId;
        this.craftNo = craftNo;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }
}
