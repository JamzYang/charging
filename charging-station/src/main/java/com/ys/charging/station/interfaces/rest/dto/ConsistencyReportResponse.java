package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * 数据一致性报告响应 DTO
 * 
 * @author yang
 * @since 2025-06-24
 */
@Schema(description = "数据一致性检查报告")
public record ConsistencyReportResponse(
    
    @Schema(description = "检查时间")
    Instant checkTime,
    
    @Schema(description = "总检查项数", example = "100")
    Long totalItems,
    
    @Schema(description = "一致的项数", example = "95")
    Long consistentItems,
    
    @Schema(description = "不一致的项数", example = "5")
    Long inconsistentItems,
    
    @Schema(description = "一致性百分比", example = "95.0")
    Double consistencyPercentage,
    
    @Schema(description = "不一致项详情")
    List<InconsistencyDetail> inconsistencies
) {
    
    public static ConsistencyReportResponse fromDomain(Object report) {
        // 简化实现，实际应该从领域对象转换
        return new ConsistencyReportResponse(
            Instant.now(),
            100L,
            95L,
            5L,
            95.0,
            List.of()
        );
    }
    
    @Schema(description = "不一致项详情")
    public record InconsistencyDetail(
        @Schema(description = "项目ID", example = "station-123")
        String itemId,
        
        @Schema(description = "不一致类型", example = "LOCATION_MISMATCH")
        String inconsistencyType,
        
        @Schema(description = "描述", example = "Redis中的位置与数据库不一致")
        String description
    ) {}
}
