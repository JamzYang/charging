package com.ys.charging.station.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 充电桩预约事件
 * 
 * @param eventId 事件ID
 * @param occurredAt 发生时间
 * @param stationId 充电站ID
 * @param connectorId 充电桩ID
 * @param userId 用户ID
 * @param reservationExpiresAt 预约过期时间
 * 
 * @author yang
 * @since 2025-06-23
 */
public record ConnectorReservedEvent(
    String eventId,
    Instant occurredAt,
    Long stationId,
    Long connectorId,
    Long userId,
    Instant reservationExpiresAt
) implements StationDomainEvent {
    
    public ConnectorReservedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
    
    public static ConnectorReservedEvent of(Long stationId, Long connectorId, Long userId, Instant expiresAt) {
        return new ConnectorReservedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            stationId,
            connectorId,
            userId,
            expiresAt
        );
    }
    
    @Override
    public Long aggregateId() {
        return stationId;
    }
}
