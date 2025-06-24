package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 更改充电站状态请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "更改充电站状态请求")
public record ChangeStatusRequest(
    
    @Schema(description = "新状态", example = "MAINTENANCE", 
            allowableValues = {"OPERATING", "MAINTENANCE", "FAULT", "CLOSED"})
    @NotBlank(message = "状态不能为空")
    String status,
    
    @Schema(description = "状态变更原因", example = "定期维护")
    String reason
) {
}
