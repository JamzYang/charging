package com.ys.charging.station.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 充电桩预约取消事件
 * 
 * @param eventId 事件ID
 * @param occurredAt 发生时间
 * @param stationId 充电站ID
 * @param connectorId 充电桩ID
 * @param userId 用户ID
 * @param reason 取消原因
 * 
 * @author yang
 * @since 2025-06-23
 */
public record ConnectorReservationCancelledEvent(
    String eventId,
    Instant occurredAt,
    Long stationId,
    Long connectorId,
    Long userId,
    String reason
) implements StationDomainEvent {
    
    public ConnectorReservationCancelledEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
    
    public static ConnectorReservationCancelledEvent of(Long stationId, Long connectorId, Long userId, String reason) {
        return new ConnectorReservationCancelledEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            stationId,
            connectorId,
            userId,
            reason
        );
    }
    
    @Override
    public Long aggregateId() {
        return stationId;
    }
}
