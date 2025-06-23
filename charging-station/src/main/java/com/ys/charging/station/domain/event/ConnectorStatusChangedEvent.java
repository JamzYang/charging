package com.ys.charging.station.domain.event;

import com.ys.charging.station.domain.model.ConnectorStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * 充电桩状态变更事件
 * 
 * 当充电桩状态发生变更时发布此事件。
 * 使用 Java 21 record 特性，确保不可变性。
 * 
 * @param eventId 事件ID
 * @param occurredAt 发生时间
 * @param stationId 充电站ID
 * @param connectorId 充电桩ID
 * @param oldStatus 原状态
 * @param newStatus 新状态
 * @param userId 相关用户ID（可选）
 * @param reason 变更原因
 * 
 * @author yang
 * @since 2025-06-23
 */
public record ConnectorStatusChangedEvent(
    String eventId,
    Instant occurredAt,
    Long stationId,
    Long connectorId,
    ConnectorStatus oldStatus,
    ConnectorStatus newStatus,
    Long userId,
    String reason
) implements StationDomainEvent {
    
    /**
     * 创建充电桩状态变更事件
     */
    public ConnectorStatusChangedEvent {
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
     * @param connectorId 充电桩ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @param reason 变更原因
     * @return 事件对象
     */
    public static ConnectorStatusChangedEvent of(Long stationId, Long connectorId, 
                                                ConnectorStatus oldStatus, ConnectorStatus newStatus, 
                                                String reason) {
        return new ConnectorStatusChangedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            stationId,
            connectorId,
            oldStatus,
            newStatus,
            null,
            reason
        );
    }
    
    /**
     * 创建包含用户信息的状态变更事件
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @param userId 用户ID
     * @param reason 变更原因
     * @return 事件对象
     */
    public static ConnectorStatusChangedEvent of(Long stationId, Long connectorId, 
                                                ConnectorStatus oldStatus, ConnectorStatus newStatus, 
                                                Long userId, String reason) {
        return new ConnectorStatusChangedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            stationId,
            connectorId,
            oldStatus,
            newStatus,
            userId,
            reason
        );
    }
    
    @Override
    public Long aggregateId() {
        return stationId;
    }
    
    /**
     * 判断是否为可用性变更
     * 
     * @return true 如果影响充电桩可用性
     */
    public boolean isAvailabilityChange() {
        return oldStatus.isAvailable() != newStatus.isAvailable();
    }
    
    /**
     * 判断是否为充电桩恢复可用
     * 
     * @return true 如果从不可用变为可用
     */
    public boolean isConnectorRecovered() {
        return !oldStatus.isAvailable() && newStatus.isAvailable();
    }
    
    /**
     * 判断是否为充电桩不可用
     * 
     * @return true 如果从可用变为不可用
     */
    public boolean isConnectorUnavailable() {
        return oldStatus.isAvailable() && !newStatus.isAvailable();
    }
    
    /**
     * 判断是否为充电开始
     * 
     * @return true 如果开始充电
     */
    public boolean isChargingStarted() {
        return newStatus == ConnectorStatus.CHARGING && oldStatus != ConnectorStatus.CHARGING;
    }
    
    /**
     * 判断是否为充电结束
     * 
     * @return true 如果结束充电
     */
    public boolean isChargingEnded() {
        return oldStatus == ConnectorStatus.CHARGING && newStatus != ConnectorStatus.CHARGING;
    }
    
    /**
     * 判断是否为故障事件
     * 
     * @return true 如果变为故障状态
     */
    public boolean isFaultEvent() {
        return newStatus.isFaulty() && !oldStatus.isFaulty();
    }
    
    /**
     * 判断是否为修复事件
     * 
     * @return true 如果从故障状态恢复
     */
    public boolean isRepairEvent() {
        return oldStatus.isFaulty() && !newStatus.isFaulty();
    }
}
