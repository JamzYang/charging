package com.ys.charging.station.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 地理位置值对象
 * 
 * 封装地理位置相关信息，包括经纬度坐标和地址信息。
 * 提供地理位置计算和验证功能。
 * 
 * 使用 Java 21 record 特性，确保不可变性和类型安全。
 * 
 * @param longitude 经度，范围 -180 到 180
 * @param latitude 纬度，范围 -90 到 90  
 * @param address 详细地址
 * @param city 城市
 * @param province 省份
 * 
 * @author yang
 * @since 2025-06-23
 */
@Embeddable
public record Location(
    @NotNull
    @DecimalMin(value = "-180.0", message = "经度必须在 -180 到 180 之间")
    @DecimalMax(value = "180.0", message = "经度必须在 -180 到 180 之间")
    BigDecimal longitude,
    
    @NotNull
    @DecimalMin(value = "-90.0", message = "纬度必须在 -90 到 90 之间")
    @DecimalMax(value = "90.0", message = "纬度必须在 -90 到 90 之间")
    BigDecimal latitude,
    
    @NotBlank(message = "地址不能为空")
    String address,
    
    @NotBlank(message = "城市不能为空")
    String city,
    
    @NotBlank(message = "省份不能为空")
    String province
) {
    
    /**
     * 创建地理位置对象
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param address 地址
     * @param city 城市
     * @param province 省份
     */
    public Location {
        // 确保精度统一（保留6位小数）
        if (longitude != null) {
            longitude = longitude.setScale(6, RoundingMode.HALF_UP);
        }
        if (latitude != null) {
            latitude = latitude.setScale(6, RoundingMode.HALF_UP);
        }
    }
    
    /**
     * 便捷构造方法 - 使用 double 类型
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param address 地址
     * @param city 城市
     * @param province 省份
     * @return Location 对象
     */
    public static Location of(double longitude, double latitude, String address, String city, String province) {
        return new Location(
            BigDecimal.valueOf(longitude),
            BigDecimal.valueOf(latitude),
            address,
            city,
            province
        );
    }
    
    /**
     * 计算与另一个位置的距离（单位：公里）
     * 使用 Haversine 公式计算球面距离
     * 
     * @param other 另一个位置
     * @return 距离（公里）
     */
    public double distanceTo(Location other) {
        if (other == null) {
            throw new IllegalArgumentException("目标位置不能为空");
        }
        
        double lat1Rad = Math.toRadians(this.latitude.doubleValue());
        double lat2Rad = Math.toRadians(other.latitude.doubleValue());
        double deltaLatRad = Math.toRadians(other.latitude.subtract(this.latitude).doubleValue());
        double deltaLngRad = Math.toRadians(other.longitude.subtract(this.longitude).doubleValue());
        
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // 地球半径（公里）
        double earthRadius = 6371.0;
        return earthRadius * c;
    }
    
    /**
     * 判断是否在指定半径范围内
     * 
     * @param other 目标位置
     * @param radiusKm 半径（公里）
     * @return true 如果在范围内
     */
    public boolean isWithinRadius(Location other, double radiusKm) {
        return distanceTo(other) <= radiusKm;
    }
    
    /**
     * 获取用于 Redis GEO 的经度值
     * 
     * @return 经度的 double 值
     */
    public double getLongitudeForGeo() {
        return longitude.doubleValue();
    }
    
    /**
     * 获取用于 Redis GEO 的纬度值
     * 
     * @return 纬度的 double 值
     */
    public double getLatitudeForGeo() {
        return latitude.doubleValue();
    }
    
    /**
     * 获取完整地址字符串
     * 
     * @return 格式化的地址字符串
     */
    public String getFullAddress() {
        return String.format("%s %s %s", province, city, address);
    }
}
