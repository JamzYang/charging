package com.ys.charging.station.interfaces.rest.dto;

import com.ys.charging.station.domain.model.Station;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/**
 * 充电站详情响应 DTO
 * 
 * @author yang
 * @since 2025-06-23
 */
@Schema(description = "充电站详情响应")
public record StationDetailResponse(
    
    @Schema(description = "充电站ID", example = "1")
    Long id,
    
    @Schema(description = "充电站名称", example = "国贸充电站")
    String name,
    
    @Schema(description = "运营商", example = "国家电网")
    String operator,
    
    @Schema(description = "联系电话", example = "010-12345678")
    String phone,
    
    @Schema(description = "描述信息", example = "位于CBD核心区域的快充站")
    String description,
    
    @Schema(description = "设施信息", example = "停车场,便利店,洗手间")
    String facilities,
    
    @Schema(description = "经度", example = "116.457")
    BigDecimal longitude,
    
    @Schema(description = "纬度", example = "39.918")
    BigDecimal latitude,
    
    @Schema(description = "详细地址", example = "北京市朝阳区建国门外大街1号")
    String address,
    
    @Schema(description = "城市", example = "北京市")
    String city,
    
    @Schema(description = "省份", example = "北京市")
    String province,
    
    @Schema(description = "营业开始时间", example = "06:00")
    LocalTime openTime,
    
    @Schema(description = "营业结束时间", example = "22:00")
    LocalTime closeTime,
    
    @Schema(description = "是否24小时营业", example = "false")
    Boolean is24Hours,
    
    @Schema(description = "充电站状态", example = "OPERATING")
    String status,
    
    @Schema(description = "总充电桩数", example = "10")
    Integer totalConnectors,
    
    @Schema(description = "可用充电桩数", example = "7")
    Integer availableConnectors,
    
    @Schema(description = "充电桩列表")
    List<ConnectorResponse> connectors,
    
    @Schema(description = "创建时间")
    Instant createdAt,
    
    @Schema(description = "更新时间")
    Instant updatedAt
) {
    
    public static StationDetailResponse fromDomain(Station station) {
        List<ConnectorResponse> connectorResponses = station.getConnectors().stream()
            .map(ConnectorResponse::fromDomain)
            .toList();
            
        return new StationDetailResponse(
            station.getId(),
            station.getStationInfo().name(),
            station.getStationInfo().operator(),
            station.getStationInfo().phone(),
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
            connectorResponses,
            station.getCreatedAt(),
            station.getUpdatedAt()
        );
    }
}
