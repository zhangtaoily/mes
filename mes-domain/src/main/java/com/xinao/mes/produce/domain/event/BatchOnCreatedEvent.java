package com.xinao.mes.produce.domain.event;

import com.xinao.mes.engine.event.DomainEvent;

import java.time.LocalDateTime;

/** 批次已上批（PDA/PC 同源；批号标识自此占用，下批/了批释放） */
public class BatchOnCreatedEvent implements DomainEvent {

    public final Long batchOnId;
    public final String batchOnNo;
    public final Long scheduleId;
    public final String batchNo;
    public final Long batchFlagId;
    public final String traceBatchNo;
    private final LocalDateTime occurredOn = LocalDateTime.now();

    public BatchOnCreatedEvent(Long batchOnId, String batchOnNo, Long scheduleId,
                               String batchNo, Long batchFlagId, String traceBatchNo) {
        this.batchOnId = batchOnId;
        this.batchOnNo = batchOnNo;
        this.scheduleId = scheduleId;
        this.batchNo = batchNo;
        this.batchFlagId = batchFlagId;
        this.traceBatchNo = traceBatchNo;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }
}
