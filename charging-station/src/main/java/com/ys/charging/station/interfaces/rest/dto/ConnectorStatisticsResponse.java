package com.ys.charging.station.interfaces.rest.dto;

import com.ys.charging.station.domain.repository.ConnectorRepository;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 充电桩统计响应 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "充电桩统计信息响应")
public record ConnectorStatisticsResponse(
    
    @Schema(description = "充电站ID", example = "1")
    Long stationId,
    
    @Schema(description = "总充电桩数", example = "10")
    Long totalConnectors,
    
    @Schema(description = "空闲充电桩数", example = "5")
    Long idleConnectors,
    
    @Schema(description = "预约充电桩数", example = "1")
    Long reservedConnectors,
    
    @Schema(description = "占用充电桩数", example = "2")
    Long occupiedConnectors,
    
    @Schema(description = "充电中充电桩数", example = "1")
    Long chargingConnectors,
    
    @Schema(description = "故障充电桩数", example = "1")
    Long faultConnectors,
    
    @Schema(description = "离线充电桩数", example = "0")
    Long offlineConnectors,
    
    @Schema(description = "维护中充电桩数", example = "0")
    Long maintenanceConnectors,
    
    @Schema(description = "快充桩数(≥50kW)", example = "3")
    Long fastChargers,
    
    @Schema(description = "超充桩数(≥150kW)", example = "1")
    Long superChargers,
    
    @Schema(description = "充电桩可用率(%)", example = "90.0")
    Double availabilityRate,
    
    @Schema(description = "充电桩使用率(%)", example = "40.0")
    Double utilizationRate,
    
    @Schema(description = "故障率(%)", example = "10.0")
    Double faultRate
) {
    
    public static ConnectorStatisticsResponse fromDomain(ConnectorRepository.ConnectorStatistics statistics) {
        return new ConnectorStatisticsResponse(
            statistics.stationId(),
            statistics.totalConnectors(),
            statistics.idleConnectors(),
            statistics.reservedConnectors(),
            statistics.occupiedConnectors(),
            statistics.chargingConnectors(),
            statistics.faultConnectors(),
            statistics.offlineConnectors(),
            statistics.maintenanceConnectors(),
            statistics.fastChargers(),
            statistics.superChargers(),
            statistics.availabilityRate(),
            statistics.utilizationRate(),
            statistics.faultRate()
        );
    }
}
