package com.ys.charging.station.interfaces.rest.dto;

import com.ys.charging.station.domain.model.Station;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * 充电站响应 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "充电站信息响应")
public record StationResponse(
    
    @Schema(description = "充电站ID", example = "1")
    Long id,
    
    @Schema(description = "充电站名称", example = "北京国贸充电站")
    String name,
    
    @Schema(description = "运营商", example = "国家电网")
    String operator,
    
    @Schema(description = "联系电话", example = "010-12345678")
    String contactPhone,
    
    @Schema(description = "描述信息", example = "位于国贸CBD核心区域，交通便利")
    String description,
    
    @Schema(description = "配套设施", example = "停车场,便利店,洗手间")
    String facilities,
    
    @Schema(description = "经度", example = "116.457")
    BigDecimal longitude,
    
    @Schema(description = "纬度", example = "39.918")
    BigDecimal latitude,
    
    @Schema(description = "详细地址", example = "朝阳区建国门外大街1号")
    String address,
    
    @Schema(description = "城市", example = "北京市")
    String city,
    
    @Schema(description = "省份", example = "北京市")
    String province,
    
    @Schema(description = "开始营业时间", example = "06:00:00")
    LocalTime openTime,
    
    @Schema(description = "结束营业时间", example = "22:00:00")
    LocalTime closeTime,
    
    @Schema(description = "是否24小时营业", example = "false")
    Boolean is24Hours,
    
    @Schema(description = "充电站状态", example = "OPERATING")
    String status,
    
    @Schema(description = "总充电桩数", example = "8")
    Integer totalConnectors,
    
    @Schema(description = "可用充电桩数", example = "6")
    Integer availableConnectors,
    
    @Schema(description = "是否在营业时间内", example = "true")
    Boolean inBusinessHours,
    
    @Schema(description = "创建时间", example = "2025-06-23T10:00:00Z")
    Instant createdAt,
    
    @Schema(description = "更新时间", example = "2025-06-23T10:00:00Z")
    Instant updatedAt
) {
    
    /**
     * 从领域对象转换为响应 DTO
     * 
     * @param station 充电站领域对象
     * @return 响应 DTO
     */
    public static StationResponse fromDomain(Station station) {
        return new StationResponse(
            station.getId(),
            station.getStationInfo().name(),
            station.getStationInfo().operator(),
            station.getStationInfo().contactPhone(),
            station.getStationInfo().description(),
            station.getStationInfo().facilities(),
            station.getLocation().longitude(),
            station.getLocation().latitude(),
            station.getLocation().address(),
            station.getLocation().city(),
            station.getLocation().province(),
            station.getBusinessHours().openTime(),
            station.getBusinessHours().closeTime(),
            station.getBusinessHours().is24Hours(),
            station.getStatus().name(),
            station.getTotalConnectors(),
            station.getAvailableConnectorCount(),
            station.isInBusinessHours(),
            station.getCreatedAt(),
            station.getUpdatedAt()
        );
    }
    
    /**
     * 获取可用率
     */
    public double getAvailabilityRate() {
        if (totalConnectors == null || totalConnectors == 0) {
            return 0.0;
        }
        return (double) (availableConnectors != null ? availableConnectors : 0) / totalConnectors * 100;
    }
    
    /**
     * 判断是否有可用充电桩
     */
    public boolean hasAvailableConnectors() {
        return availableConnectors != null && availableConnectors > 0;
    }
    
    /**
     * 判断是否为运营状态
     */
    public boolean isOperating() {
        return "OPERATING".equals(status);
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        return switch (status) {
            case "OPERATING" -> "运营中";
            case "MAINTENANCE" -> "维护中";
            case "FAULT" -> "故障";
            case "CLOSED" -> "已关闭";
            default -> "未知状态";
        };
    }
    
    /**
     * 获取营业时间描述
     */
    public String getBusinessHoursDescription() {
        if (is24Hours != null && is24Hours) {
            return "24小时营业";
        }
        
        if (openTime != null && closeTime != null) {
            return String.format("%s - %s", openTime, closeTime);
        }
        
        return "营业时间未设置";
    }
    
    /**
     * 获取充电站摘要信息
     */
    public String getSummary() {
        return String.format("%s (%s) - %s, 总桩数: %d, 可用: %d", 
            name, operator, getStatusDescription(), 
            totalConnectors != null ? totalConnectors : 0,
            availableConnectors != null ? availableConnectors : 0);
    }
}
