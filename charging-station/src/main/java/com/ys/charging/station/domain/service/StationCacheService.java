package com.ys.charging.station.domain.service;

import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.model.Connector;
import com.ys.charging.station.domain.model.StationStatus;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 充电站缓存服务接口
 * 
 * 定义充电站相关数据的缓存操作，包括：
 * - 充电站基本信息缓存
 * - 充电桩信息缓存
 * - 统计数据缓存
 * - 缓存失效和刷新策略
 * 
 * 实现多级缓存策略，优化查询性能。
 * 
 * @author yang
 * @since 2025-06-23
 */
public interface StationCacheService {
    
    /**
     * 缓存充电站信息
     * 
     * @param station 充电站对象
     * @param ttl 缓存过期时间
     */
    void cacheStation(Station station, Duration ttl);
    
    /**
     * 获取缓存的充电站信息
     * 
     * @param stationId 充电站ID
     * @return 缓存的充电站信息，如果不存在则返回 empty
     */
    Optional<Station> getCachedStation(Long stationId);
    
    /**
     * 批量缓存充电站信息
     * 
     * @param stations 充电站列表
     * @param ttl 缓存过期时间
     */
    void batchCacheStations(List<Station> stations, Duration ttl);
    
    /**
     * 批量获取缓存的充电站信息
     * 
     * @param stationIds 充电站ID列表
     * @return 缓存的充电站信息列表
     */
    List<Station> getBatchCachedStations(List<Long> stationIds);
    
    /**
     * 缓存充电桩信息
     * 
     * @param connector 充电桩对象
     * @param ttl 缓存过期时间
     */
    void cacheConnector(Connector connector, Duration ttl);
    
    /**
     * 获取缓存的充电桩信息
     * 
     * @param connectorId 充电桩ID
     * @return 缓存的充电桩信息，如果不存在则返回 empty
     */
    Optional<Connector> getCachedConnector(Long connectorId);
    
    /**
     * 缓存充电站的充电桩列表
     * 
     * @param stationId 充电站ID
     * @param connectors 充电桩列表
     * @param ttl 缓存过期时间
     */
    void cacheStationConnectors(Long stationId, List<Connector> connectors, Duration ttl);
    
    /**
     * 获取缓存的充电站充电桩列表
     * 
     * @param stationId 充电站ID
     * @return 缓存的充电桩列表
     */
    List<Connector> getCachedStationConnectors(Long stationId);
    
    /**
     * 缓存充电站统计信息
     * 
     * @param stationId 充电站ID
     * @param statistics 统计信息
     * @param ttl 缓存过期时间
     */
    void cacheStationStatistics(Long stationId, ConnectorStateService.ConnectorStatusStatistics statistics, Duration ttl);
    
    /**
     * 获取缓存的充电站统计信息
     * 
     * @param stationId 充电站ID
     * @return 缓存的统计信息，如果不存在则返回 empty
     */
    Optional<ConnectorStateService.ConnectorStatusStatistics> getCachedStationStatistics(Long stationId);
    
    /**
     * 缓存按状态分组的充电站列表
     * 
     * @param status 充电站状态
     * @param stationIds 充电站ID列表
     * @param ttl 缓存过期时间
     */
    void cacheStationsByStatus(StationStatus status, List<Long> stationIds, Duration ttl);
    
    /**
     * 获取缓存的按状态分组的充电站列表
     * 
     * @param status 充电站状态
     * @return 缓存的充电站ID列表
     */
    List<Long> getCachedStationsByStatus(StationStatus status);
    
    /**
     * 缓存可用充电站列表
     * 
     * @param stationIds 可用充电站ID列表
     * @param ttl 缓存过期时间
     */
    void cacheAvailableStations(List<Long> stationIds, Duration ttl);
    
    /**
     * 获取缓存的可用充电站列表
     * 
     * @return 可用充电站ID列表
     */
    List<Long> getCachedAvailableStations();
    
    /**
     * 使充电站缓存失效
     * 
     * @param stationId 充电站ID
     */
    void evictStation(Long stationId);
    
    /**
     * 使充电桩缓存失效
     * 
     * @param connectorId 充电桩ID
     */
    void evictConnector(Long connectorId);
    
    /**
     * 使充电站的所有相关缓存失效
     * 
     * @param stationId 充电站ID
     */
    void evictStationRelatedCaches(Long stationId);
    
    /**
     * 刷新充电站缓存
     * 
     * @param stationId 充电站ID
     * @return true 如果刷新成功
     */
    boolean refreshStationCache(Long stationId);
    
    /**
     * 批量刷新充电站缓存
     * 
     * @param stationIds 充电站ID列表
     * @return 成功刷新的数量
     */
    int batchRefreshStationCache(List<Long> stationIds);
    
    /**
     * 预热缓存
     * 
     * @param stationIds 需要预热的充电站ID列表，如果为空则预热所有
     */
    void warmupCache(List<Long> stationIds);
    
    /**
     * 清空所有缓存
     * 
     * 注意：这是一个危险操作，通常只在测试或维护时使用
     */
    void clearAllCaches();
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    CacheStatistics getCacheStatistics();
    
    /**
     * 缓存统计信息
     */
    record CacheStatistics(
        long totalCacheSize,
        long stationCacheSize,
        long connectorCacheSize,
        double hitRate,
        double missRate,
        long evictionCount,
        String summary
    ) {
        
        /**
         * 获取缓存效率描述
         */
        public String getEfficiencyDescription() {
            if (hitRate >= 0.9) {
                return "优秀";
            } else if (hitRate >= 0.8) {
                return "良好";
            } else if (hitRate >= 0.7) {
                return "一般";
            } else {
                return "需要优化";
            }
        }
        
        /**
         * 判断是否需要优化
         */
        public boolean needsOptimization() {
            return hitRate < 0.7 || evictionCount > totalCacheSize * 0.1;
        }
        
        /**
         * 获取优化建议
         */
        public List<String> getOptimizationSuggestions() {
            List<String> suggestions = new java.util.ArrayList<>();
            
            if (hitRate < 0.7) {
                suggestions.add("缓存命中率较低，建议增加缓存时间或预热更多数据");
            }
            
            if (evictionCount > totalCacheSize * 0.1) {
                suggestions.add("缓存驱逐频繁，建议增加缓存容量或调整过期策略");
            }
            
            if (stationCacheSize == 0) {
                suggestions.add("充电站缓存为空，建议启用缓存预热");
            }
            
            if (suggestions.isEmpty()) {
                suggestions.add("缓存运行良好，无需特别优化");
            }
            
            return suggestions;
        }
    }
}
