package com.ys.charging.station.domain.service;

import com.ys.charging.station.domain.model.Location;
import com.ys.charging.station.domain.model.Station;

import java.util.List;
import java.util.Optional;

/**
 * 地理位置服务接口
 * 
 * 定义地理位置相关的核心业务操作，包括：
 * - 充电站地理位置的存储和更新
 * - 基于地理位置的搜索和查询
 * - 距离计算和范围查询
 * 
 * 实现类将使用 Redis GEO 提供高性能的地理位置查询能力。
 * 
 * @author yang
 * @since 2025-06-23
 */
public interface GeoLocationService {
    
    /**
     * 添加充电站地理位置到 Redis GEO
     * 
     * @param station 充电站对象
     */
    void addStationLocation(Station station);
    
    /**
     * 更新充电站地理位置
     * 
     * @param stationId 充电站ID
     * @param newLocation 新的地理位置
     */
    void updateStationLocation(Long stationId, Location newLocation);
    
    /**
     * 移除充电站地理位置
     * 
     * @param stationId 充电站ID
     */
    void removeStationLocation(Long stationId);
    
    /**
     * 获取充电站的地理位置
     * 
     * @param stationId 充电站ID
     * @return 地理位置，如果不存在则返回 empty
     */
    Optional<Location> getStationLocation(Long stationId);
    
    /**
     * 搜索附近的充电站
     * 
     * @param criteria 搜索条件
     * @return 搜索结果列表，按距离排序
     */
    List<GeoSearchResult> searchNearbyStations(GeoSearchCriteria criteria);
    
    /**
     * 计算两个充电站之间的距离
     * 
     * @param stationId1 充电站1的ID
     * @param stationId2 充电站2的ID
     * @return 距离（公里），如果任一充电站不存在则返回 empty
     */
    Optional<Double> calculateDistance(Long stationId1, Long stationId2);
    
    /**
     * 计算充电站到指定位置的距离
     * 
     * @param stationId 充电站ID
     * @param targetLocation 目标位置
     * @return 距离（公里），如果充电站不存在则返回 empty
     */
    Optional<Double> calculateDistanceToLocation(Long stationId, Location targetLocation);
    
    /**
     * 获取指定半径内的所有充电站ID
     * 
     * @param centerLocation 中心位置
     * @param radiusKm 半径（公里）
     * @return 充电站ID列表
     */
    List<Long> getStationIdsWithinRadius(Location centerLocation, double radiusKm);
    
    /**
     * 获取指定半径内的充电站数量
     * 
     * @param centerLocation 中心位置
     * @param radiusKm 半径（公里）
     * @return 充电站数量
     */
    long countStationsWithinRadius(Location centerLocation, double radiusKm);
    
    /**
     * 批量添加充电站地理位置
     * 
     * @param stations 充电站列表
     */
    void batchAddStationLocations(List<Station> stations);
    
    /**
     * 批量移除充电站地理位置
     * 
     * @param stationIds 充电站ID列表
     */
    void batchRemoveStationLocations(List<Long> stationIds);
    
    /**
     * 清空所有充电站地理位置数据
     * 
     * 注意：这是一个危险操作，通常只在测试或数据迁移时使用
     */
    void clearAllStationLocations();
    
    /**
     * 检查地理位置数据的一致性
     * 
     * @return 一致性检查结果
     */
    GeoDataConsistencyReport checkDataConsistency();
    
    /**
     * 地理位置数据一致性报告
     */
    record GeoDataConsistencyReport(
        long totalStationsInGeo,
        long totalStationsInDatabase,
        List<Long> missingInGeo,
        List<Long> missingInDatabase,
        List<Long> locationMismatch
    ) {
        
        /**
         * 判断数据是否一致
         * 
         * @return true 如果数据一致
         */
        public boolean isConsistent() {
            return missingInGeo.isEmpty() && 
                   missingInDatabase.isEmpty() && 
                   locationMismatch.isEmpty();
        }
        
        /**
         * 获取一致性报告摘要
         * 
         * @return 报告摘要
         */
        public String getSummary() {
            if (isConsistent()) {
                return String.format("数据一致：GEO中有 %d 个充电站，数据库中有 %d 个充电站", 
                    totalStationsInGeo, totalStationsInDatabase);
            }
            
            StringBuilder summary = new StringBuilder();
            summary.append(String.format("数据不一致：GEO中有 %d 个充电站，数据库中有 %d 个充电站", 
                totalStationsInGeo, totalStationsInDatabase));
            
            if (!missingInGeo.isEmpty()) {
                summary.append(String.format("，%d 个充电站在GEO中缺失", missingInGeo.size()));
            }
            
            if (!missingInDatabase.isEmpty()) {
                summary.append(String.format("，%d 个充电站在数据库中缺失", missingInDatabase.size()));
            }
            
            if (!locationMismatch.isEmpty()) {
                summary.append(String.format("，%d 个充电站位置不匹配", locationMismatch.size()));
            }
            
            return summary.toString();
        }
    }
}
