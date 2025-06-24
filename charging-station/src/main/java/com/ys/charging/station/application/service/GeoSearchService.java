package com.ys.charging.station.application.service;

import com.ys.charging.station.domain.model.Location;
import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.repository.StationRepository;
import com.ys.charging.station.domain.service.GeoLocationService;
import com.ys.charging.station.domain.service.GeoSearchCriteria;
import com.ys.charging.station.domain.service.GeoSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 地理搜索应用服务
 * 
 * 协调地理位置相关的业务逻辑，包括：
 * - 附近充电站搜索
 * - 距离计算和路径规划
 * - 地理位置数据同步
 * - 搜索结果优化和排序
 * 
 * @author yang
 * @since 2025-06-23
 */
@Service
@Transactional(readOnly = true)
public class GeoSearchService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeoSearchService.class);
    
    private final GeoLocationService geoLocationService;
    private final StationRepository stationRepository;
    
    public GeoSearchService(GeoLocationService geoLocationService,
                           StationRepository stationRepository) {
        this.geoLocationService = geoLocationService;
        this.stationRepository = stationRepository;
    }
    
    /**
     * 搜索附近的充电站
     * 
     * @param criteria 搜索条件
     * @return 搜索结果列表，按距离排序
     */
    @Cacheable(value = "geo-search-results", key = "#criteria.toString()")
    public List<GeoSearchResult> searchNearbyStations(GeoSearchCriteria criteria) {
        logger.info("搜索附近充电站: {}", criteria.getDescription());
        
        try {
            // 使用 Redis GEO 进行高性能地理搜索
            List<GeoSearchResult> results = geoLocationService.searchNearbyStations(criteria);
            
            // 增强搜索结果（添加详细信息）
            List<GeoSearchResult> enhancedResults = enhanceSearchResults(results);
            
            // 应用业务规则排序
            List<GeoSearchResult> sortedResults = applySortingRules(enhancedResults, criteria);
            
            logger.info("搜索完成: 找到 {} 个充电站", sortedResults.size());
            return sortedResults;
            
        } catch (Exception e) {
            logger.error("搜索附近充电站失败: " + criteria.getDescription(), e);
            
            // 降级到数据库搜索
            return fallbackToDbSearch(criteria);
        }
    }
    
    /**
     * 搜索指定位置附近的充电站（简化版）
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径（公里）
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    public List<GeoSearchResult> searchNearby(double longitude, double latitude, 
                                             double radiusKm, int maxResults) {
        GeoSearchCriteria criteria = GeoSearchCriteria.withLimit(longitude, latitude, radiusKm, maxResults);
        return searchNearbyStations(criteria);
    }
    
    /**
     * 搜索快充站
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径（公里）
     * @return 快充站搜索结果
     */
    public List<GeoSearchResult> searchFastChargingStations(double longitude, double latitude, double radiusKm) {
        GeoSearchCriteria criteria = GeoSearchCriteria.fastCharging(longitude, latitude, radiusKm);
        return searchNearbyStations(criteria);
    }
    
    /**
     * 搜索超充站
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径（公里）
     * @return 超充站搜索结果
     */
    public List<GeoSearchResult> searchSuperChargingStations(double longitude, double latitude, double radiusKm) {
        GeoSearchCriteria criteria = GeoSearchCriteria.superCharging(longitude, latitude, radiusKm);
        return searchNearbyStations(criteria);
    }
    
    /**
     * 计算两个充电站之间的距离
     * 
     * @param stationId1 充电站1的ID
     * @param stationId2 充电站2的ID
     * @return 距离（公里），如果任一充电站不存在则返回 empty
     */
    public Optional<Double> calculateDistanceBetweenStations(Long stationId1, Long stationId2) {
        logger.debug("计算充电站距离: stationId1={}, stationId2={}", stationId1, stationId2);
        
        return geoLocationService.calculateDistance(stationId1, stationId2);
    }
    
    /**
     * 计算充电站到指定位置的距离
     * 
     * @param stationId 充电站ID
     * @param targetLocation 目标位置
     * @return 距离（公里），如果充电站不存在则返回 empty
     */
    public Optional<Double> calculateDistanceToLocation(Long stationId, Location targetLocation) {
        logger.debug("计算到指定位置的距离: stationId={}", stationId);
        
        return geoLocationService.calculateDistanceToLocation(stationId, targetLocation);
    }
    
    /**
     * 获取指定半径内的充电站数量
     * 
     * @param centerLocation 中心位置
     * @param radiusKm 半径（公里）
     * @return 充电站数量
     */
    public long countStationsWithinRadius(Location centerLocation, double radiusKm) {
        return geoLocationService.countStationsWithinRadius(centerLocation, radiusKm);
    }
    
    /**
     * 获取最近的充电站
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param maxResults 最大结果数
     * @return 最近的充电站列表
     */
    public List<GeoSearchResult> getNearestStations(double longitude, double latitude, int maxResults) {
        // 使用较大的搜索半径确保能找到充电站
        GeoSearchCriteria criteria = GeoSearchCriteria.withLimit(longitude, latitude, 50.0, maxResults);
        
        List<GeoSearchResult> results = searchNearbyStations(criteria);
        
        // 按距离排序并限制结果数量
        return results.stream()
            .sorted((r1, r2) -> r1.compareDistanceTo(r2))
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取最近的可用充电站
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param maxResults 最大结果数
     * @return 最近的可用充电站列表
     */
    public List<GeoSearchResult> getNearestAvailableStations(double longitude, double latitude, int maxResults) {
        GeoSearchCriteria criteria = GeoSearchCriteria.withLimit(longitude, latitude, 50.0, maxResults * 2);
        
        List<GeoSearchResult> results = searchNearbyStations(criteria);
        
        // 过滤可用充电站并按距离排序
        return results.stream()
            .filter(GeoSearchResult::isStationAvailable)
            .sorted((r1, r2) -> r1.compareDistanceTo(r2))
            .limit(maxResults)
            .collect(Collectors.toList());
    }
    
    /**
     * 同步充电站地理位置数据
     * 
     * @return 同步的充电站数量
     */
    @Transactional
    public int syncStationGeoData() {
        logger.info("开始同步充电站地理位置数据");
        
        try {
            // 获取所有运营中的充电站
            List<Station> operatingStations = stationRepository.findOperatingStations();
            
            // 批量同步到地理位置服务
            geoLocationService.batchAddStationLocations(operatingStations);
            
            logger.info("充电站地理位置数据同步完成: {} 个充电站", operatingStations.size());
            return operatingStations.size();
            
        } catch (Exception e) {
            logger.error("同步充电站地理位置数据失败", e);
            throw new RuntimeException("同步地理位置数据失败", e);
        }
    }
    
    /**
     * 检查地理位置数据一致性
     * 
     * @return 一致性检查报告
     */
    public GeoLocationService.GeoDataConsistencyReport checkGeoDataConsistency() {
        logger.info("检查地理位置数据一致性");
        
        GeoLocationService.GeoDataConsistencyReport report = geoLocationService.checkDataConsistency();
        
        if (!report.isConsistent()) {
            logger.warn("地理位置数据不一致: {}", report.getSummary());
        } else {
            logger.info("地理位置数据一致性检查通过: {}", report.getSummary());
        }
        
        return report;
    }
    
    /**
     * 修复地理位置数据不一致问题
     * 
     * @return 修复的数据条数
     */
    @Transactional
    public int repairGeoDataInconsistency() {
        logger.info("开始修复地理位置数据不一致问题");
        
        GeoLocationService.GeoDataConsistencyReport report = checkGeoDataConsistency();
        
        if (report.isConsistent()) {
            logger.info("地理位置数据一致，无需修复");
            return 0;
        }
        
        int repairedCount = 0;
        
        // 修复缺失的地理位置数据
        if (!report.missingInGeo().isEmpty()) {
            List<Station> missingStations = stationRepository.findAllById(report.missingInGeo());
            geoLocationService.batchAddStationLocations(missingStations);
            repairedCount += missingStations.size();
            logger.info("修复了 {} 个缺失的地理位置数据", missingStations.size());
        }
        
        // 移除多余的地理位置数据
        if (!report.missingInDatabase().isEmpty()) {
            geoLocationService.batchRemoveStationLocations(report.missingInDatabase());
            repairedCount += report.missingInDatabase().size();
            logger.info("移除了 {} 个多余的地理位置数据", report.missingInDatabase().size());
        }
        
        logger.info("地理位置数据修复完成: 修复了 {} 条数据", repairedCount);
        return repairedCount;
    }
    
    // 私有辅助方法
    private List<GeoSearchResult> enhanceSearchResults(List<GeoSearchResult> results) {
        // 这里可以添加额外的业务信息，如实时充电桩状态、价格信息等
        // 当前简化实现，直接返回原结果
        return results;
    }
    
    private List<GeoSearchResult> applySortingRules(List<GeoSearchResult> results, GeoSearchCriteria criteria) {
        // 应用业务规则排序：
        // 1. 可用的充电站优先
        // 2. 距离近的优先
        // 3. 充电桩数量多的优先
        
        return results.stream()
            .sorted((r1, r2) -> {
                // 首先按可用性排序
                int availabilityComparison = r1.compareAvailabilityTo(r2);
                if (availabilityComparison != 0) {
                    return availabilityComparison;
                }
                
                // 然后按距离排序
                int distanceComparison = r1.compareDistanceTo(r2);
                if (distanceComparison != 0) {
                    return distanceComparison;
                }
                
                // 最后按可用充电桩数量排序
                return Integer.compare(
                    r2.availableConnectors() != null ? r2.availableConnectors() : 0,
                    r1.availableConnectors() != null ? r1.availableConnectors() : 0
                );
            })
            .collect(Collectors.toList());
    }
    
    private List<GeoSearchResult> fallbackToDbSearch(GeoSearchCriteria criteria) {
        logger.warn("降级到数据库搜索: {}", criteria.getDescription());
        
        try {
            // 使用数据库的地理位置范围查询作为降级方案
            double buffer = 0.01; // 大约1公里的缓冲区
            List<Station> stations = stationRepository.findByLocationBounds(
                java.math.BigDecimal.valueOf(criteria.longitude() - buffer),
                java.math.BigDecimal.valueOf(criteria.longitude() + buffer),
                java.math.BigDecimal.valueOf(criteria.latitude() - buffer),
                java.math.BigDecimal.valueOf(criteria.latitude() + buffer)
            );
            
            // 转换为搜索结果并计算距离
            Location centerLocation = Location.of(criteria.longitude(), criteria.latitude(), "", "", "");
            
            return stations.stream()
                .filter(station -> {
                    double distance = station.getLocation().distanceTo(centerLocation);
                    return distance <= criteria.radiusKm();
                })
                .map(station -> {
                    double distance = station.getLocation().distanceTo(centerLocation);
                    return GeoSearchResult.of(
                        station.getId(),
                        station.getStationInfo().name(),
                        station.getStationInfo().operator(),
                        station.getLocation(),
                        station.getStatus(),
                        station.getTotalConnectors(),
                        station.getAvailableConnectors(),
                        distance,
                        station.isInBusinessHours()
                    );
                })
                .sorted((r1, r2) -> r1.compareDistanceTo(r2))
                .limit(criteria.maxResults())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("数据库降级搜索也失败了", e);
            return List.of();
        }
    }
}
