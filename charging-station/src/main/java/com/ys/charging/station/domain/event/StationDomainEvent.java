package com.ys.charging.station.domain.event;

import java.time.Instant;

/**
 * 充电站领域事件基础接口
 * 
 * 使用 Java 21 sealed interface 特性，确保类型安全和完整性。
 * 所有充电站相关的领域事件都必须实现此接口。
 * 
 * @author yang
 * @since 2025-06-23
 */
public sealed interface StationDomainEvent
    permits StationStatusChangedEvent, ConnectorStatusChangedEvent, ConnectorReservedEvent,
            ConnectorReservationCancelledEvent, ConnectorFaultEvent, ConnectorRepairedEvent {
    
    /**
     * 获取事件ID
     * 
     * @return 事件唯一标识
     */
    String eventId();
    
    /**
     * 获取事件发生时间
     * 
     * @return 事件时间戳
     */
    Instant occurredAt();
    
    /**
     * 获取聚合根ID
     * 
     * @return 聚合根标识
     */
    Long aggregateId();
    
    /**
     * 获取事件类型
     * 
     * @return 事件类型名称
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }
    
    /**
     * 获取事件版本
     * 
     * @return 事件版本号
     */
    default String version() {
        return "1.0";
    }
}
