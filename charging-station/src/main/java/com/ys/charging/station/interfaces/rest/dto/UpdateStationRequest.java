package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 更新充电站信息请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "更新充电站信息请求")
public record UpdateStationRequest(
    
    @Schema(description = "充电站名称", example = "国贸充电站")
    @NotBlank(message = "充电站名称不能为空")
    String name,
    
    @Schema(description = "运营商", example = "国家电网")
    @NotBlank(message = "运营商不能为空")
    String operator,
    
    @Schema(description = "联系电话", example = "010-12345678")
    String contactPhone,
    
    @Schema(description = "描述信息", example = "位于CBD核心区域的快充站")
    String description,
    
    @Schema(description = "设施信息", example = "停车场,便利店,洗手间,餐厅")
    String facilities
) {
}
