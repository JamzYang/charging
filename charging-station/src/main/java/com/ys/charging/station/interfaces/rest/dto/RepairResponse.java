package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 修复响应 DTO
 * 
 * @author yang
 * @since 2025-06-24
 */
@Schema(description = "数据修复响应")
public record RepairResponse(
    
    @Schema(description = "修复的记录数", example = "5")
    Long repairedCount,
    
    @Schema(description = "修复状态消息", example = "修复完成")
    String message
) {
    
    public static RepairResponse success(Long count) {
        return new RepairResponse(count, "修复完成");
    }
    
    public static RepairResponse failure(String errorMessage) {
        return new RepairResponse(0L, "修复失败: " + errorMessage);
    }
}
