package com.ys.charging.station.interfaces.rest.dto;

import com.ys.charging.station.domain.repository.StationRepository;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 充电站统计响应 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "充电站统计信息响应")
public record StationStatisticsResponse(
    
    @Schema(description = "总充电站数", example = "100")
    Long totalStations,
    
    @Schema(description = "运营中充电站数", example = "80")
    Long operatingStations,
    
    @Schema(description = "维护中充电站数", example = "10")
    Long maintenanceStations,
    
    @Schema(description = "故障充电站数", example = "5")
    Long faultStations,
    
    @Schema(description = "关闭充电站数", example = "5")
    Long closedStations,
    
    @Schema(description = "总充电桩数", example = "500")
    Long totalConnectors,
    
    @Schema(description = "可用充电桩数", example = "400")
    Long availableConnectors,
    
    @Schema(description = "平均每站充电桩数", example = "5.0")
    Double averageConnectorsPerStation,
    
    @Schema(description = "充电桩可用率(%)", example = "80.0")
    Double connectorAvailabilityRate
) {
    
    public static StationStatisticsResponse fromDomain(StationRepository.StationStatistics statistics) {
        return new StationStatisticsResponse(
            statistics.totalStations(),
            statistics.operatingStations(),
            statistics.maintenanceStations(),
            statistics.faultStations(),
            statistics.closedStations(),
            statistics.totalConnectors(),
            statistics.availableConnectors(),
            statistics.averageConnectorsPerStation(),
            statistics.connectorAvailabilityRate()
        );
    }
}
