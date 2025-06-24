package com.ys.charging.station.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 创建充电站请求 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "创建充电站请求")
public record CreateStationRequest(
    
    @Schema(description = "充电站名称", example = "北京国贸充电站")
    @NotBlank(message = "充电站名称不能为空")
    @Size(max = 100, message = "充电站名称长度不能超过100个字符")
    String name,
    
    @Schema(description = "运营商", example = "国家电网")
    @NotBlank(message = "运营商不能为空")
    @Size(max = 50, message = "运营商名称长度不能超过50个字符")
    String operator,
    
    @Schema(description = "联系电话", example = "010-12345678")
    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    String contactPhone,
    
    @Schema(description = "描述信息", example = "位于国贸CBD核心区域，交通便利")
    @Size(max = 500, message = "描述信息长度不能超过500个字符")
    String description,
    
    @Schema(description = "配套设施", example = "停车场,便利店,洗手间")
    @Size(max = 200, message = "配套设施信息长度不能超过200个字符")
    String facilities,
    
    @Schema(description = "经度", example = "116.457")
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
    @Digits(integer = 3, fraction = 6, message = "经度格式不正确")
    BigDecimal longitude,
    
    @Schema(description = "纬度", example = "39.918")
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
    @Digits(integer = 2, fraction = 6, message = "纬度格式不正确")
    BigDecimal latitude,
    
    @Schema(description = "详细地址", example = "朝阳区建国门外大街1号")
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址长度不能超过200个字符")
    String address,
    
    @Schema(description = "城市", example = "北京市")
    @NotBlank(message = "城市不能为空")
    @Size(max = 50, message = "城市名称长度不能超过50个字符")
    String city,
    
    @Schema(description = "省份", example = "北京市")
    @NotBlank(message = "省份不能为空")
    @Size(max = 50, message = "省份名称长度不能超过50个字符")
    String province,
    
    @Schema(description = "开始营业时间", example = "06:00:00")
    LocalTime openTime,
    
    @Schema(description = "结束营业时间", example = "22:00:00")
    LocalTime closeTime,
    
    @Schema(description = "是否24小时营业", example = "false")
    Boolean is24Hours
) {
    
    /**
     * 验证营业时间的一致性
     */
    public boolean isValidBusinessHours() {
        if (is24Hours != null && is24Hours) {
            return openTime == null && closeTime == null;
        }
        
        if (openTime != null && closeTime != null) {
            return !openTime.equals(closeTime);
        }
        
        return openTime == null && closeTime == null;
    }
    
    /**
     * 获取请求摘要
     */
    public String getSummary() {
        return String.format("创建充电站: %s (%s) - %s, %s", name, operator, city, province);
    }
}
