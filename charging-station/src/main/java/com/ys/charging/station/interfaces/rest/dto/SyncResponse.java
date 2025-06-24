package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 同步响应 DTO
 * 
 * @author yang
 * @since 2025-06-24
 */
@Schema(description = "数据同步响应")
public record SyncResponse(
    
    @Schema(description = "同步的记录数", example = "150")
    Long syncedCount,
    
    @Schema(description = "同步状态消息", example = "同步完成")
    String message
) {
    
    public static SyncResponse success(Long count) {
        return new SyncResponse(count, "同步完成");
    }
    
    public static SyncResponse failure(String errorMessage) {
        return new SyncResponse(0L, "同步失败: " + errorMessage);
    }
}
