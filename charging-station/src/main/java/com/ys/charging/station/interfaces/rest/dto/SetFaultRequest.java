package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 设置故障请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "设置充电桩故障请求")
public record SetFaultRequest(
    
    @Schema(description = "故障原因", example = "电源模块故障")
    @NotBlank(message = "故障原因不能为空")
    String faultReason
) {
}
