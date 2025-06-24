package com.ys.charging.station.domain.service;

import com.ys.charging.station.domain.model.Location;
import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.model.StationStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 地理位置搜索结果
 * 
 * 封装地理位置搜索返回的充电站信息和距离数据。
 * 使用 Java 21 record 特性，确保不可变性。
 * 
 * @param stationId 充电站ID
 * @param stationName 充电站名称
 * @param operator 运营商
 * @param location 地理位置
 * @param status 充电站状态
 * @param totalConnectors 总充电桩数
 * @param availableConnectors 可用充电桩数
 * @param distanceKm 距离（公里）
 * @param hasAvailableConnectors 是否有可用充电桩
 * @param isInBusinessHours 是否在营业时间内
 * 
 * @author yang
 * @since 2025-06-23
 */
public record GeoSearchResult(
    Long stationId,
    String stationName,
    String operator,
    Location location,
    StationStatus status,
    Integer totalConnectors,
    Integer availableConnectors,
    Double distanceKm,
    Boolean hasAvailableConnectors,
    Boolean isInBusinessHours
) {
    
    /**
     * 创建搜索结果
     */
    public GeoSearchResult {
        // 距离精度处理（保留2位小数）
        if (distanceKm != null) {
            distanceKm = BigDecimal.valueOf(distanceKm)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        }
        
        // 设置默认值
        if (hasAvailableConnectors == null) {
            hasAvailableConnectors = availableConnectors != null && availableConnectors > 0;
        }
        
        if (isInBusinessHours == null) {
            isInBusinessHours = true; // 默认认为在营业时间内
        }
    }
    
    /**
     * 创建基础搜索结果
     * 
     * @param stationId 充电站ID
     * @param stationName 充电站名称
     * @param operator 运营商
     * @param location 地理位置
     * @param status 状态
     * @param totalConnectors 总充电桩数
     * @param availableConnectors 可用充电桩数
     * @param distanceKm 距离
     * @return 搜索结果
     */
    public static GeoSearchResult of(Long stationId, String stationName, String operator,
                                    Location location, StationStatus status,
                                    Integer totalConnectors, Integer availableConnectors,
                                    Double distanceKm) {
        return new GeoSearchResult(
            stationId, stationName, operator, location, status,
            totalConnectors, availableConnectors, distanceKm,
            null, null
        );
    }
    
    /**
     * 创建完整的搜索结果
     * 
     * @param stationId 充电站ID
     * @param stationName 充电站名称
     * @param operator 运营商
     * @param location 地理位置
     * @param status 状态
     * @param totalConnectors 总充电桩数
     * @param availableConnectors 可用充电桩数
     * @param distanceKm 距离
     * @param isInBusinessHours 是否在营业时间内
     * @return 搜索结果
     */
    public static GeoSearchResult of(Long stationId, String stationName, String operator,
                                    Location location, StationStatus status,
                                    Integer totalConnectors, Integer availableConnectors,
                                    Double distanceKm, Boolean isInBusinessHours) {
        return new GeoSearchResult(
            stationId, stationName, operator, location, status,
            totalConnectors, availableConnectors, distanceKm,
            null, isInBusinessHours
        );
    }
    
    /**
     * 获取充电站对象（用于 DTO 转换）
     * 注意：这是一个简化的实现，实际应该从完整的 Station 对象转换
     *
     * @return 简化的 Station 对象
     */
    public Station getStation() {
        // 这里返回一个简化的 Station 对象
        // 实际实现中应该从完整的 Station 对象转换而来
        return null; // 暂时返回 null，避免循环依赖
    }

    /**
     * 获取距离（用于 DTO 转换）
     *
     * @return 距离值
     */
    public Double getDistance() {
        return distanceKm;
    }

    /**
     * 获取距离单位（用于 DTO 转换）
     *
     * @return 距离单位
     */
    public String getUnit() {
        return "km";
    }

    /**
     * 判断充电站是否可用
     *
     * @return true 如果充电站可用
     */
    public boolean isStationAvailable() {
        return status.isServiceAvailable() && hasAvailableConnectors && isInBusinessHours;
    }
    
    /**
     * 判断是否为快充站
     * 
     * @return true 如果有快充桩（需要额外查询充电桩详情）
     */
    public boolean isFastChargingStation() {
        // 这里需要根据实际的充电桩信息判断
        // 暂时返回 false，实际实现时需要查询充电桩详情
        return false;
    }
    
    /**
     * 获取距离描述
     * 
     * @return 距离的文字描述
     */
    public String getDistanceDescription() {
        if (distanceKm == null) {
            return "距离未知";
        }
        
        if (distanceKm < 1.0) {
            return String.format("%.0f 米", distanceKm * 1000);
        } else {
            return String.format("%.1f 公里", distanceKm);
        }
    }
    
    /**
     * 获取可用性描述
     * 
     * @return 可用性的文字描述
     */
    public String getAvailabilityDescription() {
        if (!status.isServiceAvailable()) {
            return status.getDescription();
        }
        
        if (!isInBusinessHours) {
            return "非营业时间";
        }
        
        if (!hasAvailableConnectors) {
            return "暂无可用充电桩";
        }
        
        return String.format("%d/%d 可用", availableConnectors, totalConnectors);
    }
    
    /**
     * 获取充电站简要信息
     * 
     * @return 简要信息字符串
     */
    public String getSummary() {
        return String.format("%s (%s) - %s - %s", 
            stationName, operator, getDistanceDescription(), getAvailabilityDescription());
    }
    
    /**
     * 获取充电站详细地址
     * 
     * @return 详细地址
     */
    public String getFullAddress() {
        return location != null ? location.getFullAddress() : "地址未知";
    }
    
    /**
     * 获取经纬度坐标
     * 
     * @return 坐标字符串
     */
    public String getCoordinates() {
        if (location == null) {
            return "坐标未知";
        }
        return String.format("%.6f, %.6f", 
            location.getLongitudeForGeo(), location.getLatitudeForGeo());
    }
    
    /**
     * 计算与指定位置的距离
     * 
     * @param targetLocation 目标位置
     * @return 距离（公里）
     */
    public double calculateDistanceTo(Location targetLocation) {
        if (location == null || targetLocation == null) {
            return Double.MAX_VALUE;
        }
        return location.distanceTo(targetLocation);
    }
    
    /**
     * 判断是否在指定半径内
     * 
     * @param centerLocation 中心位置
     * @param radiusKm 半径（公里）
     * @return true 如果在半径内
     */
    public boolean isWithinRadius(Location centerLocation, double radiusKm) {
        if (location == null || centerLocation == null) {
            return false;
        }
        return location.isWithinRadius(centerLocation, radiusKm);
    }
    
    /**
     * 比较距离（用于排序）
     * 
     * @param other 另一个搜索结果
     * @return 距离比较结果
     */
    public int compareDistanceTo(GeoSearchResult other) {
        if (this.distanceKm == null && other.distanceKm == null) {
            return 0;
        }
        if (this.distanceKm == null) {
            return 1;
        }
        if (other.distanceKm == null) {
            return -1;
        }
        return Double.compare(this.distanceKm, other.distanceKm);
    }
    
    /**
     * 比较可用性（用于排序）
     * 可用的充电站排在前面
     * 
     * @param other 另一个搜索结果
     * @return 可用性比较结果
     */
    public int compareAvailabilityTo(GeoSearchResult other) {
        boolean thisAvailable = this.isStationAvailable();
        boolean otherAvailable = other.isStationAvailable();
        
        if (thisAvailable && !otherAvailable) {
            return -1;
        }
        if (!thisAvailable && otherAvailable) {
            return 1;
        }
        
        // 如果可用性相同，比较可用充电桩数量
        return Integer.compare(
            other.availableConnectors != null ? other.availableConnectors : 0,
            this.availableConnectors != null ? this.availableConnectors : 0
        );
    }
}
