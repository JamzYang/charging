package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 预约充电桩请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "预约充电桩请求")
public record ReserveConnectorRequest(
    
    @Schema(description = "用户ID", example = "1001")
    @NotNull(message = "用户ID不能为空")
    Long userId,
    
    @Schema(description = "预约时长(分钟)", example = "30")
    @NotNull(message = "预约时长不能为空")
    @Min(value = 5, message = "预约时长至少5分钟")
    @Max(value = 120, message = "预约时长最多120分钟")
    Integer reservationMinutes
) {
}
