package com.ys.charging.station.domain.service;

import com.ys.charging.station.domain.model.ConnectorInfo;
import com.ys.charging.station.domain.model.StationStatus;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 地理位置搜索条件
 * 
 * 封装地理位置搜索的各种条件参数。
 * 使用 Java 21 record 特性，确保不可变性。
 * 
 * @param longitude 中心点经度
 * @param latitude 中心点纬度
 * @param radiusKm 搜索半径（公里）
 * @param maxResults 最大结果数量
 * @param includeUnavailable 是否包含不可用的充电站
 * @param requiredConnectorTypes 需要的充电桩类型
 * @param minPower 最小功率要求（kW）
 * @param maxPower 最大功率限制（kW）
 * @param requiredStatuses 需要的充电站状态
 * 
 * @author yang
 * @since 2025-06-23
 */
public record GeoSearchCriteria(
    double longitude,
    double latitude,
    double radiusKm,
    int maxResults,
    boolean includeUnavailable,
    Set<ConnectorInfo.ConnectorType> requiredConnectorTypes,
    BigDecimal minPower,
    BigDecimal maxPower,
    Set<StationStatus> requiredStatuses
) {
    
    /**
     * 创建搜索条件
     */
    public GeoSearchCriteria {
        // 验证经纬度范围
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("经度必须在 -180 到 180 之间");
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("纬度必须在 -90 到 90 之间");
        }
        
        // 验证搜索半径
        if (radiusKm <= 0) {
            throw new IllegalArgumentException("搜索半径必须大于 0");
        }
        if (radiusKm > 100) {
            throw new IllegalArgumentException("搜索半径不能超过 100 公里");
        }
        
        // 验证结果数量
        if (maxResults <= 0) {
            throw new IllegalArgumentException("最大结果数量必须大于 0");
        }
        if (maxResults > 100) {
            throw new IllegalArgumentException("最大结果数量不能超过 100");
        }
        
        // 验证功率范围
        if (minPower != null && minPower.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("最小功率不能小于 0");
        }
        if (maxPower != null && maxPower.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("最大功率必须大于 0");
        }
        if (minPower != null && maxPower != null && minPower.compareTo(maxPower) > 0) {
            throw new IllegalArgumentException("最小功率不能大于最大功率");
        }
    }
    
    /**
     * 创建基础搜索条件
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径
     * @return 搜索条件
     */
    public static GeoSearchCriteria basic(double longitude, double latitude, double radiusKm) {
        return new GeoSearchCriteria(
            longitude, latitude, radiusKm, 20, false, 
            null, null, null, null
        );
    }
    
    /**
     * 创建包含最大结果数的搜索条件
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径
     * @param maxResults 最大结果数
     * @return 搜索条件
     */
    public static GeoSearchCriteria withLimit(double longitude, double latitude, 
                                             double radiusKm, int maxResults) {
        return new GeoSearchCriteria(
            longitude, latitude, radiusKm, maxResults, false, 
            null, null, null, null
        );
    }
    
    /**
     * 创建快充搜索条件
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径
     * @return 搜索条件
     */
    public static GeoSearchCriteria fastCharging(double longitude, double latitude, double radiusKm) {
        return new GeoSearchCriteria(
            longitude, latitude, radiusKm, 20, false, 
            null, BigDecimal.valueOf(50), null, null
        );
    }
    
    /**
     * 创建超充搜索条件
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径
     * @return 搜索条件
     */
    public static GeoSearchCriteria superCharging(double longitude, double latitude, double radiusKm) {
        return new GeoSearchCriteria(
            longitude, latitude, radiusKm, 20, false, 
            null, BigDecimal.valueOf(150), null, null
        );
    }
    
    /**
     * 添加充电桩类型过滤
     * 
     * @param connectorTypes 充电桩类型
     * @return 新的搜索条件
     */
    public GeoSearchCriteria withConnectorTypes(Set<ConnectorInfo.ConnectorType> connectorTypes) {
        return new GeoSearchCriteria(
            longitude, latitude, radiusKm, maxResults, includeUnavailable,
            connectorTypes, minPower, maxPower, requiredStatuses
        );
    }
    
    /**
     * 添加功率范围过滤
     * 
     * @param minPower 最小功率
     * @param maxPower 最大功率
     * @return 新的搜索条件
     */
    public GeoSearchCriteria withPowerRange(BigDecimal minPower, BigDecimal maxPower) {
        return new GeoSearchCriteria(
            longitude, latitude, radiusKm, maxResults, includeUnavailable,
            requiredConnectorTypes, minPower, maxPower, requiredStatuses
        );
    }
    
    /**
     * 添加状态过滤
     * 
     * @param statuses 充电站状态
     * @return 新的搜索条件
     */
    public GeoSearchCriteria withStatuses(Set<StationStatus> statuses) {
        return new GeoSearchCriteria(
            longitude, latitude, radiusKm, maxResults, includeUnavailable,
            requiredConnectorTypes, minPower, maxPower, statuses
        );
    }
    
    /**
     * 包含不可用的充电站
     * 
     * @return 新的搜索条件
     */
    public GeoSearchCriteria includeUnavailable() {
        return new GeoSearchCriteria(
            longitude, latitude, radiusKm, maxResults, true,
            requiredConnectorTypes, minPower, maxPower, requiredStatuses
        );
    }
    
    /**
     * 判断是否有充电桩类型过滤
     * 
     * @return true 如果有类型过滤
     */
    public boolean hasConnectorTypeFilter() {
        return requiredConnectorTypes != null && !requiredConnectorTypes.isEmpty();
    }
    
    /**
     * 判断是否有功率过滤
     * 
     * @return true 如果有功率过滤
     */
    public boolean hasPowerFilter() {
        return minPower != null || maxPower != null;
    }
    
    /**
     * 判断是否有状态过滤
     * 
     * @return true 如果有状态过滤
     */
    public boolean hasStatusFilter() {
        return requiredStatuses != null && !requiredStatuses.isEmpty();
    }
    
    /**
     * 获取搜索描述
     * 
     * @return 搜索条件的文字描述
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(String.format("在 (%.6f, %.6f) 半径 %.1f 公里内", longitude, latitude, radiusKm));
        
        if (hasConnectorTypeFilter()) {
            desc.append("，充电桩类型: ").append(requiredConnectorTypes);
        }
        
        if (hasPowerFilter()) {
            if (minPower != null && maxPower != null) {
                desc.append(String.format("，功率范围: %.1f-%.1f kW", 
                    minPower.doubleValue(), maxPower.doubleValue()));
            } else if (minPower != null) {
                desc.append(String.format("，最小功率: %.1f kW", minPower.doubleValue()));
            } else if (maxPower != null) {
                desc.append(String.format("，最大功率: %.1f kW", maxPower.doubleValue()));
            }
        }
        
        if (hasStatusFilter()) {
            desc.append("，状态: ").append(requiredStatuses);
        }
        
        desc.append(String.format("，最多 %d 个结果", maxResults));
        
        if (includeUnavailable) {
            desc.append("，包含不可用充电站");
        }
        
        return desc.toString();
    }
}
