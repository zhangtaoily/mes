package com.xinao.mes.engine.event;

import java.time.LocalDateTime;

/**
 * 领域事件契约（Spring Event 起步，峰值后可迁 MQ）
 *
 * <p>跨上下文只走事件（如 了批事件 → 触发包装任务生成）；
 * 聚合内强一致走本地事务，不发事件。
 */
public interface DomainEvent {

    /** 事件发生时间 */
    LocalDateTime occurredOn();
}
