package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 地理搜索请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "地理搜索请求")
public record GeoSearchRequest(
    
    @Schema(description = "经度", example = "116.457")
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
    Double longitude,
    
    @Schema(description = "纬度", example = "39.918")
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
    Double latitude,
    
    @Schema(description = "搜索半径(公里)", example = "5.0")
    @NotNull(message = "搜索半径不能为空")
    @Min(value = 1, message = "搜索半径至少1公里")
    @Max(value = 50, message = "搜索半径最多50公里")
    Double radiusKm,
    
    @Schema(description = "最大结果数", example = "20")
    @Min(value = 1, message = "最大结果数至少为1")
    @Max(value = 100, message = "最大结果数最多为100")
    Integer maxResults,
    
    @Schema(description = "充电桩类型过滤", example = "GB_T_DC")
    String connectorType,
    
    @Schema(description = "最小功率(kW)", example = "50.0")
    Double minPower,
    
    @Schema(description = "最大功率(kW)", example = "150.0")
    Double maxPower,
    
    @Schema(description = "运营商过滤", example = "国家电网")
    String operator,
    
    @Schema(description = "只返回可用充电站", example = "true")
    Boolean availableOnly
) {
}
