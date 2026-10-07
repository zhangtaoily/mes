package com.xinao.mes.produce.domain.event;

import com.xinao.mes.engine.event.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 产量已汇报（监听方：累计任务工序产出量/订单产出量，刷新排程实绩数据源） */
public class OutputReportedEvent implements DomainEvent {

    public final Long outputId;
    public final String outputNo;
    public final Long scheduleId;
    public final Long taskId;
    public final Long processId;
    public final BigDecimal outputQty;
    private final LocalDateTime occurredOn = LocalDateTime.now();

    public OutputReportedEvent(Long outputId, String outputNo, Long scheduleId,
                               Long taskId, Long processId, BigDecimal outputQty) {
        this.outputId = outputId;
        this.outputNo = outputNo;
        this.scheduleId = scheduleId;
        this.taskId = taskId;
        this.processId = processId;
        this.outputQty = outputQty;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }
}
