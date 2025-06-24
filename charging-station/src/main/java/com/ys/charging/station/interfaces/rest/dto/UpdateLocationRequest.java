package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * 更新充电站位置请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "更新充电站位置请求")
public record UpdateLocationRequest(
    
    @Schema(description = "经度", example = "121.505")
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
    BigDecimal longitude,
    
    @Schema(description = "纬度", example = "31.245")
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
    BigDecimal latitude,
    
    @Schema(description = "详细地址", example = "上海市浦东新区陆家嘴金融中心")
    @NotBlank(message = "详细地址不能为空")
    String address,
    
    @Schema(description = "城市", example = "上海市")
    @NotBlank(message = "城市不能为空")
    String city,
    
    @Schema(description = "省份", example = "上海市")
    @NotBlank(message = "省份不能为空")
    String province
) {
}
