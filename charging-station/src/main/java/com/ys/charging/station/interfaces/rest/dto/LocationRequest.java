package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * 位置请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "位置信息请求")
public record LocationRequest(
    
    @Schema(description = "经度", example = "116.457")
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
    Double longitude,
    
    @Schema(description = "纬度", example = "39.918")
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
    Double latitude
) {
}
