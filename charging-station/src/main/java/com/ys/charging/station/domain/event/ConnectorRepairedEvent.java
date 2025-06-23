package com.ys.charging.station.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 充电桩修复事件
 * 
 * @param eventId 事件ID
 * @param occurredAt 发生时间
 * @param stationId 充电站ID
 * @param connectorId 充电桩ID
 * @param repairDescription 修复描述
 * @param repairedBy 修复人员
 * 
 * @author yang
 * @since 2025-06-23
 */
public record ConnectorRepairedEvent(
    String eventId,
    Instant occurredAt,
    Long stationId,
    Long connectorId,
    String repairDescription,
    String repairedBy
) implements StationDomainEvent {
    
    public ConnectorRepairedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
    
    public static ConnectorRepairedEvent of(Long stationId, Long connectorId, String repairDescription, String repairedBy) {
        return new ConnectorRepairedEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            stationId,
            connectorId,
            repairDescription,
            repairedBy
        );
    }
    
    @Override
    public Long aggregateId() {
        return stationId;
    }
}
