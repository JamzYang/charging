package com.ys.charging.station.interfaces.rest.dto;

import com.ys.charging.station.domain.model.Connector;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 充电桩响应 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "充电桩信息响应")
public record ConnectorResponse(
    
    @Schema(description = "充电桩ID", example = "1")
    Long id,
    
    @Schema(description = "充电站ID", example = "1")
    Long stationId,
    
    @Schema(description = "充电桩编号", example = "A01")
    String connectorNumber,
    
    @Schema(description = "充电桩类型", example = "GB_T_DC")
    String connectorType,
    
    @Schema(description = "最大功率(kW)", example = "60.0")
    BigDecimal maxPower,
    
    @Schema(description = "充电桩状态", example = "IDLE")
    String status,
    
    @Schema(description = "预约用户ID", example = "1001")
    Long reservedByUserId,
    
    @Schema(description = "预约过期时间")
    Instant reservationExpiresAt,
    
    @Schema(description = "故障原因", example = "电源模块故障")
    String faultReason,
    
    @Schema(description = "最后心跳时间")
    Instant lastHeartbeatAt,
    
    @Schema(description = "创建时间")
    Instant createdAt,
    
    @Schema(description = "更新时间")
    Instant updatedAt
) {
    
    public static ConnectorResponse fromDomain(Connector connector) {
        return new ConnectorResponse(
            connector.getId(),
            connector.getStationId(),
            connector.getConnectorNumber(),
            connector.getConnectorInfo().connectorType().name(),
            connector.getConnectorInfo().maxPower(),
            connector.getStatus().name(),
            connector.getReservedByUserId(),
            connector.getReservationExpiresAt(),
            connector.getFaultReason(),
            connector.getLastHeartbeatAt(),
            connector.getCreatedAt(),
            connector.getUpdatedAt()
        );
    }
}
