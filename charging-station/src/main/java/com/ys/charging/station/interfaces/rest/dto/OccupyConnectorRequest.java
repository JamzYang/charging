package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 占用充电桩请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "占用充电桩请求")
public record OccupyConnectorRequest(
    
    @Schema(description = "用户ID", example = "1001")
    @NotNull(message = "用户ID不能为空")
    Long userId
) {
}
