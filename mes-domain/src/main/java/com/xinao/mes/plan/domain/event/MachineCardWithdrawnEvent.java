package com.xinao.mes.plan.domain.event;

import com.xinao.mes.engine.event.DomainEvent;

import java.time.LocalDateTime;

/** 制造卡已撤回（对应排程恢复可重排，占用释放） */
public class MachineCardWithdrawnEvent implements DomainEvent {

    public final Long cardId;
    public final String cardNo;
    public final Long scheduleId;
    private final LocalDateTime occurredOn = LocalDateTime.now();

    public MachineCardWithdrawnEvent(Long cardId, String cardNo, Long scheduleId) {
        this.cardId = cardId;
        this.cardNo = cardNo;
        this.scheduleId = scheduleId;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }
}
