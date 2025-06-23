package com.ys.charging.station.domain.event;

import com.ys.charging.station.domain.model.StationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 充电站状态变更事件
 * 
 * 当充电站状态发生变更时发布此事件。
 * 使用 Java 21 record 特性，确保不可变性。
 * 
 * @param eventId 事件ID
 * @param occurredAt 发生时间
 * @param stationId 充电站ID
 * @param oldStatus 原状态
 * @param newStatus 新状态
 * @param reason 变更原因
 * 
 * @author yang
 * @since 2025-06-23
 */
public record StationStatusChangedEvent(
    String eventId,
    Instant occurredAt,
    Long stationId,
    StationStatus oldStatus,
    StationStatus newStatus,
    String reason
) implements StationDomainEvent {
    
    /**
     * 创建充电站状态变更事件
     */
    public StationStatusChangedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
    
    /**
     * 创建状态变更事件
     * 
     * @param stationId 充电站ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @param reason 变更原因
     * @return 事件对象
     */
    public static StationStatusChangedEvent of(Long stationId, StationStatus oldStatus, 
                                              StationStatus newStatus, String reason) {
        return new StationStatusChangedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            stationId,
            oldStatus,
            newStatus,
            reason
        );
    }
    
    @Override
    public Long aggregateId() {
        return stationId;
    }
    
    /**
     * 判断是否为服务状态变更
     * 
     * @return true 如果影响服务可用性
     */
    public boolean isServiceStatusChange() {
        return oldStatus.isServiceAvailable() != newStatus.isServiceAvailable();
    }
    
    /**
     * 判断是否为服务恢复
     * 
     * @return true 如果从不可用变为可用
     */
    public boolean isServiceRecovered() {
        return !oldStatus.isServiceAvailable() && newStatus.isServiceAvailable();
    }
    
    /**
     * 判断是否为服务中断
     * 
     * @return true 如果从可用变为不可用
     */
    public boolean isServiceInterrupted() {
        return oldStatus.isServiceAvailable() && !newStatus.isServiceAvailable();
    }
}
