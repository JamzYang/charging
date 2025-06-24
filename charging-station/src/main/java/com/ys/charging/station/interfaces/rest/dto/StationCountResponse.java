package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 充电站数量响应 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "充电站数量响应")
public record StationCountResponse(
    
    @Schema(description = "充电站数量", example = "15")
    Long count
) {
    
    public static StationCountResponse of(Long count) {
        return new StationCountResponse(count);
    }
}
