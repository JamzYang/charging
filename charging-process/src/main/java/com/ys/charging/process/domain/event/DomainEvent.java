package com.ys.charging.process.domain.event;

import java.time.Instant;

/**
 * 领域事件统一接口
 *
 * 使用 Java 21 的 sealed interface 特性，限制实现类只能是指定的事件类型。
 * 所有领域事件都必须包含发生时间，用于事件溯源和审计。
 *
 * 设计原则：
 * - 事件是不可变的，使用 record 实现
 * - 事件名称应该使用过去时，表示已经发生的事实
 * - 事件应该包含足够的信息供事件处理器使用
 *
 * @author yang
 * @since 2025-06-23
 */
public sealed interface DomainEvent
    permits ChargeSessionStatusChangedEvent, PaymentConfirmedEvent {
    
    /**
     * 事件发生时间
     * 
     * @return 事件发生的时间戳
     */
    Instant occurredAt();
    
    /**
     * 获取事件类型名称
     * 
     * @return 事件类型的简单类名
     */
    default String getEventType() {
        return this.getClass().getSimpleName();
    }
}
