package com.ys.charging.station.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * 充电桩故障事件
 * 
 * @param eventId 事件ID
 * @param occurredAt 发生时间
 * @param stationId 充电站ID
 * @param connectorId 充电桩ID
 * @param faultReason 故障原因
 * @param severity 严重程度
 * 
 * @author yang
 * @since 2025-06-23
 */
public record ConnectorFaultEvent(
    String eventId,
    Instant occurredAt,
    Long stationId,
    Long connectorId,
    String faultReason,
    FaultSeverity severity
) implements StationDomainEvent {
    
    public enum FaultSeverity {
        LOW("轻微"), MEDIUM("中等"), HIGH("严重"), CRITICAL("紧急");
        
        private final String displayName;
        
        FaultSeverity(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public ConnectorFaultEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (severity == null) {
            severity = FaultSeverity.MEDIUM;
        }
    }
    
    public static ConnectorFaultEvent of(Long stationId, Long connectorId, String faultReason, FaultSeverity severity) {
        return new ConnectorFaultEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            stationId,
            connectorId,
            faultReason,
            severity
        );
    }
    
    @Override
    public Long aggregateId() {
        return stationId;
    }
}
