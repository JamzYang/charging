package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 取消预约请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "取消预约请求")
public record CancelReservationRequest(
    
    @Schema(description = "用户ID", example = "1001")
    @NotNull(message = "用户ID不能为空")
    Long userId,
    
    @Schema(description = "取消原因", example = "用户主动取消")
    @NotBlank(message = "取消原因不能为空")
    String reason
) {
}
