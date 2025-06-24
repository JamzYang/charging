package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 距离响应 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "距离计算响应")
public record DistanceResponse(
    
    @Schema(description = "距离", example = "15.8")
    Double distance,
    
    @Schema(description = "距离单位", example = "km")
    String unit
) {
    
    public static DistanceResponse of(Double distance, String unit) {
        return new DistanceResponse(distance, unit);
    }
    
    public static DistanceResponse ofKilometers(Double distanceKm) {
        return new DistanceResponse(distanceKm, "km");
    }
}
