package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 计数响应 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "计数响应")
public record CountResponse(
    
    @Schema(description = "数量", example = "15")
    Long count
) {
    
    public static CountResponse of(Long count) {
        return new CountResponse(count);
    }
}
