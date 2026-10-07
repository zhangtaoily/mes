package com.xinao.mes.engine.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 领域事件发布器（统一出口，后续迁 MQ 时仅改此实现）
 */
@Component
@RequiredArgsConstructor
public class MesEventPublisher {

    private final ApplicationEventPublisher delegate;

    public void publish(DomainEvent event) {
        delegate.publishEvent(event);
    }
}
