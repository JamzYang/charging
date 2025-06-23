package com.ys.charging.station.infrastructure.cache;

import com.ys.charging.station.domain.model.Location;
import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.service.GeoLocationService;
import com.ys.charging.station.domain.service.GeoSearchCriteria;
import com.ys.charging.station.domain.service.GeoSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis GEO 地理位置服务实现
 * 
 * 使用 Redis GEO 命令提供高性能的地理位置查询服务。
 * 支持充电站位置存储、附近搜索、距离计算等功能。
 * 
 * Redis GEO 数据结构：
 * - Key: "stations:geo" - 存储所有充电站的地理位置
 * - Member: stationId - 充电站ID作为成员名
 * - Score: geohash - 地理位置的哈希值
 * 
 * @author yang
 * @since 2025-06-23
 */
@Service
public class RedisGeoLocationService implements GeoLocationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisGeoLocationService.class);
    
    private static final String STATIONS_GEO_KEY = "stations:geo";
    private static final String STATION_INFO_KEY_PREFIX = "station:info:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RedisGeoLocationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public void addStationLocation(Station station) {
        if (station == null || station.getId() == null || station.getLocation() == null) {
            logger.warn("无效的充电站信息，跳过地理位置添加");
            return;
        }
        
        try {
            Location location = station.getLocation();
            Point point = new Point(location.getLongitudeForGeo(), location.getLatitudeForGeo());
            
            // 添加到 Redis GEO
            redisTemplate.opsForGeo().add(STATIONS_GEO_KEY, point, station.getId().toString());
            
            // 缓存充电站基本信息
            cacheStationInfo(station);
            
            logger.debug("成功添加充电站地理位置: stationId={}, location=({}, {})", 
                station.getId(), location.getLongitudeForGeo(), location.getLatitudeForGeo());
                
        } catch (Exception e) {
            logger.error("添加充电站地理位置失败: stationId=" + station.getId(), e);
            throw new RuntimeException("添加充电站地理位置失败", e);
        }
    }
    
    @Override
    public void updateStationLocation(Long stationId, Location newLocation) {
        if (stationId == null || newLocation == null) {
            logger.warn("无效的参数，跳过地理位置更新");
            return;
        }
        
        try {
            // 先移除旧位置
            removeStationLocation(stationId);
            
            // 添加新位置
            Point point = new Point(newLocation.getLongitudeForGeo(), newLocation.getLatitudeForGeo());
            redisTemplate.opsForGeo().add(STATIONS_GEO_KEY, point, stationId.toString());
            
            logger.debug("成功更新充电站地理位置: stationId={}, newLocation=({}, {})", 
                stationId, newLocation.getLongitudeForGeo(), newLocation.getLatitudeForGeo());
                
        } catch (Exception e) {
            logger.error("更新充电站地理位置失败: stationId=" + stationId, e);
            throw new RuntimeException("更新充电站地理位置失败", e);
        }
    }
    
    @Override
    public void removeStationLocation(Long stationId) {
        if (stationId == null) {
            return;
        }
        
        try {
            // 从 Redis GEO 中移除
            redisTemplate.opsForGeo().remove(STATIONS_GEO_KEY, stationId.toString());
            
            // 移除缓存的充电站信息
            redisTemplate.delete(STATION_INFO_KEY_PREFIX + stationId);
            
            logger.debug("成功移除充电站地理位置: stationId={}", stationId);
            
        } catch (Exception e) {
            logger.error("移除充电站地理位置失败: stationId=" + stationId, e);
            throw new RuntimeException("移除充电站地理位置失败", e);
        }
    }
    
    @Override
    public Optional<Location> getStationLocation(Long stationId) {
        if (stationId == null) {
            return Optional.empty();
        }
        
        try {
            List<Point> positions = redisTemplate.opsForGeo().position(STATIONS_GEO_KEY, stationId.toString());
            
            if (positions == null || positions.isEmpty() || positions.get(0) == null) {
                return Optional.empty();
            }
            
            Point point = positions.get(0);
            // 注意：这里只能获取经纬度，无法获取地址信息
            // 实际应用中需要从其他缓存或数据库获取完整的 Location 信息
            Location location = Location.of(point.getX(), point.getY(), "", "", "");
            
            return Optional.of(location);
            
        } catch (Exception e) {
            logger.error("获取充电站地理位置失败: stationId=" + stationId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<GeoSearchResult> searchNearbyStations(GeoSearchCriteria criteria) {
        try {
            // 构建 Redis GEO 搜索条件
            Circle circle = new Circle(
                new Point(criteria.longitude(), criteria.latitude()),
                new Distance(criteria.radiusKm(), Metrics.KILOMETERS)
            );
            
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .includeCoordinates()
                .sortAscending()
                .limit(criteria.maxResults());
            
            // 执行 Redis GEO 搜索
            GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = 
                redisTemplate.opsForGeo().radius(STATIONS_GEO_KEY, circle, args);
            
            if (geoResults == null) {
                return Collections.emptyList();
            }
            
            // 转换为搜索结果
            List<GeoSearchResult> results = new ArrayList<>();
            for (GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult : geoResults) {
                try {
                    Long stationId = Long.parseLong(geoResult.getContent().getName());
                    Double distance = geoResult.getDistance().getValue();
                    Point point = geoResult.getContent().getPoint();
                    
                    // 获取缓存的充电站信息
                    GeoSearchResult result = buildSearchResult(stationId, distance, point, criteria);
                    if (result != null) {
                        results.add(result);
                    }
                    
                } catch (NumberFormatException e) {
                    logger.warn("无效的充电站ID: {}", geoResult.getContent().getName());
                }
            }
            
            // 应用额外的过滤条件
            return applyAdditionalFilters(results, criteria);
            
        } catch (Exception e) {
            logger.error("搜索附近充电站失败: " + criteria.getDescription(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public Optional<Double> calculateDistance(Long stationId1, Long stationId2) {
        if (stationId1 == null || stationId2 == null) {
            return Optional.empty();
        }
        
        try {
            Distance distance = redisTemplate.opsForGeo().distance(
                STATIONS_GEO_KEY, 
                stationId1.toString(), 
                stationId2.toString(), 
                Metrics.KILOMETERS
            );
            
            return distance != null ? Optional.of(distance.getValue()) : Optional.empty();
            
        } catch (Exception e) {
            logger.error("计算充电站距离失败: stationId1={}, stationId2={}", stationId1, stationId2, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<Double> calculateDistanceToLocation(Long stationId, Location targetLocation) {
        if (stationId == null || targetLocation == null) {
            return Optional.empty();
        }
        
        Optional<Location> stationLocation = getStationLocation(stationId);
        if (stationLocation.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            double distance = stationLocation.get().distanceTo(targetLocation);
            return Optional.of(distance);
            
        } catch (Exception e) {
            logger.error("计算到指定位置的距离失败: stationId={}", stationId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<Long> getStationIdsWithinRadius(Location centerLocation, double radiusKm) {
        try {
            Circle circle = new Circle(
                new Point(centerLocation.getLongitudeForGeo(), centerLocation.getLatitudeForGeo()),
                new Distance(radiusKm, Metrics.KILOMETERS)
            );
            
            GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = 
                redisTemplate.opsForGeo().radius(STATIONS_GEO_KEY, circle);
            
            if (geoResults == null) {
                return Collections.emptyList();
            }
            
            return geoResults.getContent().stream()
                .map(result -> {
                    try {
                        return Long.parseLong(result.getContent().getName());
                    } catch (NumberFormatException e) {
                        logger.warn("无效的充电站ID: {}", result.getContent().getName());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("获取半径内充电站ID失败: center=({}, {}), radius={}", 
                centerLocation.getLongitudeForGeo(), centerLocation.getLatitudeForGeo(), radiusKm, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public long countStationsWithinRadius(Location centerLocation, double radiusKm) {
        return getStationIdsWithinRadius(centerLocation, radiusKm).size();
    }
    
    @Override
    public void batchAddStationLocations(List<Station> stations) {
        if (stations == null || stations.isEmpty()) {
            return;
        }
        
        try {
            Map<String, Point> locationMap = new HashMap<>();
            
            for (Station station : stations) {
                if (station.getId() != null && station.getLocation() != null) {
                    Location location = station.getLocation();
                    Point point = new Point(location.getLongitudeForGeo(), location.getLatitudeForGeo());
                    locationMap.put(station.getId().toString(), point);
                    
                    // 缓存充电站信息
                    cacheStationInfo(station);
                }
            }
            
            if (!locationMap.isEmpty()) {
                redisTemplate.opsForGeo().add(STATIONS_GEO_KEY, locationMap);
                logger.info("批量添加充电站地理位置成功: {} 个充电站", locationMap.size());
            }
            
        } catch (Exception e) {
            logger.error("批量添加充电站地理位置失败", e);
            throw new RuntimeException("批量添加充电站地理位置失败", e);
        }
    }
    
    @Override
    public void batchRemoveStationLocations(List<Long> stationIds) {
        if (stationIds == null || stationIds.isEmpty()) {
            return;
        }
        
        try {
            String[] members = stationIds.stream()
                .map(Object::toString)
                .toArray(String[]::new);
            
            redisTemplate.opsForGeo().remove(STATIONS_GEO_KEY, members);
            
            // 移除缓存的充电站信息
            List<String> cacheKeys = stationIds.stream()
                .map(id -> STATION_INFO_KEY_PREFIX + id)
                .collect(Collectors.toList());
            redisTemplate.delete(cacheKeys);
            
            logger.info("批量移除充电站地理位置成功: {} 个充电站", stationIds.size());
            
        } catch (Exception e) {
            logger.error("批量移除充电站地理位置失败", e);
            throw new RuntimeException("批量移除充电站地理位置失败", e);
        }
    }
    
    @Override
    public void clearAllStationLocations() {
        try {
            redisTemplate.delete(STATIONS_GEO_KEY);
            
            // 清除所有充电站信息缓存
            Set<String> cacheKeys = redisTemplate.keys(STATION_INFO_KEY_PREFIX + "*");
            if (cacheKeys != null && !cacheKeys.isEmpty()) {
                redisTemplate.delete(cacheKeys);
            }
            
            logger.warn("已清空所有充电站地理位置数据");
            
        } catch (Exception e) {
            logger.error("清空充电站地理位置数据失败", e);
            throw new RuntimeException("清空充电站地理位置数据失败", e);
        }
    }
    
    @Override
    public GeoDataConsistencyReport checkDataConsistency() {
        // 这里需要与数据库进行比较，暂时返回基础信息
        try {
            // 获取 Redis GEO 中的充电站数量
            // 注意：Redis 没有直接获取 GEO 集合大小的命令，需要通过其他方式
            long totalInGeo = 0; // 实际实现时需要查询
            
            return new GeoDataConsistencyReport(
                totalInGeo, 0, 
                Collections.emptyList(), 
                Collections.emptyList(), 
                Collections.emptyList()
            );
            
        } catch (Exception e) {
            logger.error("检查数据一致性失败", e);
            return new GeoDataConsistencyReport(
                0, 0, 
                Collections.emptyList(), 
                Collections.emptyList(), 
                Collections.emptyList()
            );
        }
    }
    
    // 私有辅助方法
    private void cacheStationInfo(Station station) {
        // 缓存充电站基本信息，用于搜索结果构建
        Map<String, Object> stationInfo = new HashMap<>();
        stationInfo.put("id", station.getId());
        stationInfo.put("name", station.getStationInfo().name());
        stationInfo.put("operator", station.getStationInfo().operator());
        stationInfo.put("status", station.getStatus().name());
        stationInfo.put("totalConnectors", station.getTotalConnectors());
        stationInfo.put("availableConnectors", station.getAvailableConnectors());
        
        redisTemplate.opsForHash().putAll(STATION_INFO_KEY_PREFIX + station.getId(), stationInfo);
    }
    
    private GeoSearchResult buildSearchResult(Long stationId, Double distance, Point point, GeoSearchCriteria criteria) {
        // 从缓存获取充电站信息
        Map<Object, Object> stationInfo = redisTemplate.opsForHash().entries(STATION_INFO_KEY_PREFIX + stationId);
        
        if (stationInfo.isEmpty()) {
            logger.warn("未找到充电站缓存信息: stationId={}", stationId);
            return null;
        }
        
        try {
            String name = (String) stationInfo.get("name");
            String operator = (String) stationInfo.get("operator");
            String statusStr = (String) stationInfo.get("status");
            Integer totalConnectors = (Integer) stationInfo.get("totalConnectors");
            Integer availableConnectors = (Integer) stationInfo.get("availableConnectors");
            
            // 构建 Location 对象（简化版，只包含经纬度）
            Location location = Location.of(point.getX(), point.getY(), "", "", "");
            
            return GeoSearchResult.of(
                stationId, name, operator, location,
                com.ys.charging.station.domain.model.StationStatus.valueOf(statusStr),
                totalConnectors, availableConnectors, distance
            );
            
        } catch (Exception e) {
            logger.error("构建搜索结果失败: stationId={}", stationId, e);
            return null;
        }
    }
    
    private List<GeoSearchResult> applyAdditionalFilters(List<GeoSearchResult> results, GeoSearchCriteria criteria) {
        return results.stream()
            .filter(result -> {
                // 应用可用性过滤
                if (!criteria.includeUnavailable() && !result.isStationAvailable()) {
                    return false;
                }
                
                // 应用状态过滤
                if (criteria.hasStatusFilter() && !criteria.requiredStatuses().contains(result.status())) {
                    return false;
                }
                
                // 其他过滤条件（充电桩类型、功率等）需要查询详细信息
                // 这里暂时跳过，实际实现时需要额外的数据查询
                
                return true;
            })
            .collect(Collectors.toList());
    }
}
