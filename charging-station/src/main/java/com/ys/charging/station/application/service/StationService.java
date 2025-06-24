package com.ys.charging.station.application.service;

import com.ys.charging.station.domain.model.*;
import com.ys.charging.station.domain.repository.StationRepository;
import com.ys.charging.station.domain.service.GeoLocationService;
import com.ys.charging.station.domain.service.StationCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 充电站应用服务
 * 
 * 协调充电站相关的业务逻辑，包括：
 * - 充电站的创建、更新、删除
 * - 状态管理和业务规则验证
 * - 缓存管理和地理位置同步
 * - 事务管理和领域事件发布
 * 
 * @author yang
 * @since 2025-06-23
 */
@Service
@Transactional
public class StationService {
    
    private static final Logger logger = LoggerFactory.getLogger(StationService.class);
    
    private final StationRepository stationRepository;
    private final GeoLocationService geoLocationService;
    private final StationCacheService cacheService;
    
    public StationService(StationRepository stationRepository,
                         GeoLocationService geoLocationService,
                         StationCacheService cacheService) {
        this.stationRepository = stationRepository;
        this.geoLocationService = geoLocationService;
        this.cacheService = cacheService;
    }
    
    /**
     * 创建充电站
     * 
     * @param stationInfo 充电站信息
     * @param location 地理位置
     * @param businessHours 营业时间
     * @return 创建的充电站
     */
    public Station createStation(StationInfo stationInfo, Location location, BusinessHours businessHours) {
        logger.info("创建充电站: name={}, operator={}", stationInfo.name(), stationInfo.operator());
        
        // 验证业务规则
        validateStationCreation(stationInfo, location);
        
        // 创建充电站聚合根
        Station station = Station.create(stationInfo, location, businessHours);
        
        // 保存到数据库
        Station savedStation = stationRepository.save(station);
        
        // 同步到地理位置服务
        geoLocationService.addStationLocation(savedStation);
        
        // 缓存充电站信息
        cacheService.cacheStation(savedStation, Duration.ofHours(2));
        
        logger.info("充电站创建成功: id={}, name={}", savedStation.getId(), stationInfo.name());
        return savedStation;
    }
    
    /**
     * 更新充电站信息
     * 
     * @param stationId 充电站ID
     * @param stationInfo 新的充电站信息
     * @return 更新后的充电站
     */
    @CacheEvict(value = "stations", key = "#stationId")
    public Station updateStationInfo(Long stationId, StationInfo stationInfo) {
        logger.info("更新充电站信息: stationId={}", stationId);
        
        Station station = getStationById(stationId);
        
        // 创建新的充电站对象（值对象不可变）
        Station updatedStation = new Station(stationInfo, station.getLocation(), station.getBusinessHours());
        // 设置ID和其他属性
        // 注意：这里需要通过反射或其他方式设置ID，实际实现中可能需要调整
        
        Station savedStation = stationRepository.save(updatedStation);
        
        // 更新缓存
        cacheService.cacheStation(savedStation, Duration.ofHours(2));
        
        logger.info("充电站信息更新成功: stationId={}", stationId);
        return savedStation;
    }
    
    /**
     * 更新充电站地理位置
     * 
     * @param stationId 充电站ID
     * @param newLocation 新的地理位置
     * @return 更新后的充电站
     */
    @CacheEvict(value = "stations", key = "#stationId")
    public Station updateStationLocation(Long stationId, Location newLocation) {
        logger.info("更新充电站地理位置: stationId={}", stationId);
        
        Station station = getStationById(stationId);
        
        // 创建新的充电站对象
        Station updatedStation = new Station(station.getStationInfo(), newLocation, station.getBusinessHours());
        
        Station savedStation = stationRepository.save(updatedStation);
        
        // 同步更新地理位置服务
        geoLocationService.updateStationLocation(stationId, newLocation);
        
        // 更新缓存
        cacheService.cacheStation(savedStation, Duration.ofHours(2));
        
        logger.info("充电站地理位置更新成功: stationId={}", stationId);
        return savedStation;
    }
    
    /**
     * 更改充电站状态
     * 
     * @param stationId 充电站ID
     * @param newStatus 新状态
     * @param reason 变更原因
     * @return 更新后的充电站
     */
    @CacheEvict(value = "stations", key = "#stationId")
    public Station changeStationStatus(Long stationId, StationStatus newStatus, String reason) {
        logger.info("更改充电站状态: stationId={}, newStatus={}, reason={}", stationId, newStatus, reason);
        
        Station station = getStationById(stationId);
        
        // 通过聚合根方法更改状态（会发布领域事件）
        station.changeStatus(newStatus, reason);
        
        Station savedStation = stationRepository.save(station);
        
        // 更新缓存
        cacheService.cacheStation(savedStation, Duration.ofHours(2));
        
        // 如果充电站关闭，从地理位置服务中移除
        if (newStatus == StationStatus.CLOSED) {
            geoLocationService.removeStationLocation(stationId);
        }
        
        logger.info("充电站状态更改成功: stationId={}, newStatus={}", stationId, newStatus);
        return savedStation;
    }
    
    /**
     * 添加充电桩到充电站
     * 
     * @param stationId 充电站ID
     * @param connectorInfo 充电桩信息
     * @param parkingSpot 车位信息
     * @return 更新后的充电站
     */
    @CacheEvict(value = "stations", key = "#stationId")
    public Station addConnectorToStation(Long stationId, ConnectorInfo connectorInfo, ParkingSpot parkingSpot) {
        logger.info("添加充电桩到充电站: stationId={}, connectorNumber={}", 
            stationId, connectorInfo.connectorNumber());
        
        Station station = getStationById(stationId);
        
        // 通过聚合根方法添加充电桩
        station.addConnector(connectorInfo, parkingSpot);
        
        Station savedStation = stationRepository.save(station);
        
        // 更新缓存
        cacheService.cacheStation(savedStation, Duration.ofHours(2));
        
        logger.info("充电桩添加成功: stationId={}, connectorNumber={}", 
            stationId, connectorInfo.connectorNumber());
        return savedStation;
    }
    
    /**
     * 从充电站移除充电桩
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @return 更新后的充电站
     */
    @CacheEvict(value = "stations", key = "#stationId")
    public Station removeConnectorFromStation(Long stationId, Long connectorId) {
        logger.info("从充电站移除充电桩: stationId={}, connectorId={}", stationId, connectorId);
        
        Station station = getStationById(stationId);
        
        // 通过聚合根方法移除充电桩
        station.removeConnector(connectorId);
        
        Station savedStation = stationRepository.save(station);
        
        // 更新缓存
        cacheService.cacheStation(savedStation, Duration.ofHours(2));
        
        logger.info("充电桩移除成功: stationId={}, connectorId={}", stationId, connectorId);
        return savedStation;
    }
    
    /**
     * 根据ID获取充电站
     * 
     * @param stationId 充电站ID
     * @return 充电站
     */
    @Cacheable(value = "stations", key = "#stationId")
    @Transactional(readOnly = true)
    public Station getStationById(Long stationId) {
        return stationRepository.findById(stationId)
            .orElseThrow(() -> new IllegalArgumentException("充电站不存在: " + stationId));
    }
    
    /**
     * 根据ID获取充电站（包含充电桩信息）
     * 
     * @param stationId 充电站ID
     * @return 充电站
     */
    @Transactional(readOnly = true)
    public Station getStationWithConnectors(Long stationId) {
        return stationRepository.findByIdWithConnectors(stationId)
            .orElseThrow(() -> new IllegalArgumentException("充电站不存在: " + stationId));
    }
    
    /**
     * 根据状态查找充电站
     * 
     * @param status 充电站状态
     * @param pageable 分页参数
     * @return 分页的充电站列表
     */
    @Transactional(readOnly = true)
    public Page<Station> getStationsByStatus(StationStatus status, Pageable pageable) {
        return stationRepository.findByStatus(status, pageable);
    }
    
    /**
     * 搜索充电站
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 匹配的充电站列表
     */
    @Transactional(readOnly = true)
    public Page<Station> searchStations(String keyword, Pageable pageable) {
        return stationRepository.searchByKeyword(keyword, pageable);
    }
    
    /**
     * 获取有可用充电桩的充电站
     * 
     * @return 有可用充电桩的充电站列表
     */
    @Cacheable(value = "station-lists", key = "'available'")
    @Transactional(readOnly = true)
    public List<Station> getStationsWithAvailableConnectors() {
        return stationRepository.findStationsWithAvailableConnectors();
    }
    
    /**
     * 获取运营中的充电站
     * 
     * @return 运营中的充电站列表
     */
    @Cacheable(value = "station-lists", key = "'operating'")
    @Transactional(readOnly = true)
    public List<Station> getOperatingStations() {
        return stationRepository.findOperatingStations();
    }
    
    /**
     * 删除充电站
     * 
     * @param stationId 充电站ID
     */
    @CacheEvict(value = {"stations", "station-lists"}, allEntries = true)
    public void deleteStation(Long stationId) {
        logger.info("删除充电站: stationId={}", stationId);
        
        Station station = getStationById(stationId);
        
        // 验证是否可以删除
        if (station.hasAvailableConnectors()) {
            throw new IllegalStateException("充电站还有可用充电桩，无法删除");
        }
        
        // 从地理位置服务中移除
        geoLocationService.removeStationLocation(stationId);
        
        // 清除相关缓存
        cacheService.evictStationRelatedCaches(stationId);
        
        // 删除充电站
        stationRepository.delete(station);
        
        logger.info("充电站删除成功: stationId={}", stationId);
    }
    
    /**
     * 获取充电站统计信息
     * 
     * @return 统计信息
     */
    @Cacheable(value = "statistics", key = "'station'")
    @Transactional(readOnly = true)
    public StationRepository.StationStatistics getStationStatistics() {
        return stationRepository.getStatistics();
    }
    
    /**
     * 批量创建充电站
     * 
     * @param stations 充电站列表
     * @return 创建的充电站列表
     */
    public List<Station> batchCreateStations(List<Station> stations) {
        logger.info("批量创建充电站: count={}", stations.size());
        
        // 验证所有充电站
        for (Station station : stations) {
            validateStationCreation(station.getStationInfo(), station.getLocation());
        }
        
        // 批量保存
        List<Station> savedStations = stationRepository.saveAll(stations);
        
        // 批量同步到地理位置服务
        geoLocationService.batchAddStationLocations(savedStations);
        
        // 批量缓存
        cacheService.batchCacheStations(savedStations, Duration.ofHours(2));
        
        logger.info("批量创建充电站成功: count={}", savedStations.size());
        return savedStations;
    }
    
    // 私有辅助方法
    private void validateStationCreation(StationInfo stationInfo, Location location) {
        // 检查名称和运营商的唯一性
        if (stationRepository.existsByNameAndOperator(stationInfo.name(), stationInfo.operator())) {
            throw new IllegalArgumentException(
                String.format("充电站已存在: name=%s, operator=%s", stationInfo.name(), stationInfo.operator()));
        }
        
        // 验证地理位置的合理性
        if (location.longitude().doubleValue() == 0.0 && location.latitude().doubleValue() == 0.0) {
            throw new IllegalArgumentException("地理位置不能为 (0, 0)");
        }
    }
}
