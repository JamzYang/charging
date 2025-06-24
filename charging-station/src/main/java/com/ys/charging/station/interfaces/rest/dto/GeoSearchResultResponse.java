package com.ys.charging.station.interfaces.rest.dto;

import com.ys.charging.station.domain.service.GeoSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 地理搜索结果响应 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "地理搜索结果响应")
public record GeoSearchResultResponse(
    
    @Schema(description = "充电站信息")
    StationResponse station,
    
    @Schema(description = "距离", example = "2.5")
    Double distance,
    
    @Schema(description = "距离单位", example = "km")
    String unit
) {
    
    public static GeoSearchResultResponse fromDomain(GeoSearchResult result) {
        return new GeoSearchResultResponse(
            StationResponse.fromDomain(result.getStation()),
            result.getDistance(),
            result.getUnit()
        );
    }
}
