package com.ys.charging.station.interfaces.rest;

import com.ys.charging.station.application.service.GeoSearchService;
import com.ys.charging.station.interfaces.rest.dto.*;
import com.ys.charging.station.interfaces.rest.exception.*;
import java.math.BigDecimal;
import com.ys.charging.station.domain.model.Location;
import com.ys.charging.station.domain.service.GeoLocationService;
import com.ys.charging.station.domain.service.GeoSearchCriteria;
import com.ys.charging.station.domain.service.GeoSearchResult;
import com.ys.charging.station.interfaces.rest.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 地理搜索 REST API 控制器
 * 
 * 提供地理位置搜索的 RESTful 接口，包括：
 * - 附近充电站搜索
 * - 距离计算
 * - 地理位置数据管理
 * - 搜索结果优化
 * 
 * @author yang
 * @since 2025-06-23
 */
@RestController
@RequestMapping("/api/v1/geo")
@Tag(name = "地理搜索", description = "地理位置搜索相关的 API 接口")
public class GeoSearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(GeoSearchController.class);
    
    private final GeoSearchService geoSearchService;
    
    public GeoSearchController(GeoSearchService geoSearchService) {
        this.geoSearchService = geoSearchService;
    }
    
    /**
     * 搜索附近的充电站
     * 
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    @PostMapping("/search/nearby")
    @Operation(summary = "搜索附近充电站", description = "根据位置和条件搜索附近的充电站")
    public ResponseEntity<List<GeoSearchResultResponse>> searchNearbyStations(
            @Valid @RequestBody GeoSearchRequest request) {
        
        logger.info("搜索附近充电站: lng={}, lat={}, radius={}km", 
            request.longitude(), request.latitude(), request.radiusKm());
        
        try {
            // 构建搜索条件
            GeoSearchCriteria criteria = GeoSearchCriteria.builder(
                request.longitude(), request.latitude(), request.radiusKm())
                .connectorType(request.connectorType())
                .powerRange(
                    request.minPower() != null ? BigDecimal.valueOf(request.minPower()) : null,
                    request.maxPower() != null ? BigDecimal.valueOf(request.maxPower()) : null
                )
                .operator(request.operator())
                .availableOnly(request.availableOnly() != null ? request.availableOnly() : false)
                .build();
            
            // 执行搜索
            List<GeoSearchResult> results = geoSearchService.searchNearbyStations(criteria);
            
            // 转换为响应对象
            List<GeoSearchResultResponse> response = results.stream()
                .map(GeoSearchResultResponse::fromDomain)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("搜索参数无效: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            logger.error("搜索附近充电站失败", e);
            throw new InternalServerErrorException("搜索失败");
        }
    }
    
    /**
     * 简单的附近搜索
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径（公里）
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    @GetMapping("/search/nearby")
    @Operation(summary = "简单附近搜索", description = "简单的附近充电站搜索")
    public ResponseEntity<List<GeoSearchResultResponse>> searchNearby(
            @Parameter(description = "经度") @RequestParam double longitude,
            @Parameter(description = "纬度") @RequestParam double latitude,
            @Parameter(description = "搜索半径（公里）") @RequestParam(defaultValue = "5.0") double radiusKm,
            @Parameter(description = "最大结果数") @RequestParam(defaultValue = "20") int maxResults) {
        
        logger.debug("简单附近搜索: lng={}, lat={}, radius={}km, maxResults={}", 
            longitude, latitude, radiusKm, maxResults);
        
        try {
            List<GeoSearchResult> results = geoSearchService.searchNearby(longitude, latitude, radiusKm, maxResults);
            
            List<GeoSearchResultResponse> response = results.stream()
                .map(GeoSearchResultResponse::fromDomain)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("搜索参数无效: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 搜索快充站
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径（公里）
     * @return 快充站搜索结果
     */
    @GetMapping("/search/fast-charging")
    @Operation(summary = "搜索快充站", description = "搜索附近的快充站（功率≥50kW）")
    public ResponseEntity<List<GeoSearchResultResponse>> searchFastChargingStations(
            @Parameter(description = "经度") @RequestParam double longitude,
            @Parameter(description = "纬度") @RequestParam double latitude,
            @Parameter(description = "搜索半径（公里）") @RequestParam(defaultValue = "10.0") double radiusKm) {
        
        logger.debug("搜索快充站: lng={}, lat={}, radius={}km", longitude, latitude, radiusKm);
        
        try {
            List<GeoSearchResult> results = geoSearchService.searchFastChargingStations(longitude, latitude, radiusKm);
            
            List<GeoSearchResultResponse> response = results.stream()
                .map(GeoSearchResultResponse::fromDomain)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("搜索参数无效: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 搜索超充站
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 搜索半径（公里）
     * @return 超充站搜索结果
     */
    @GetMapping("/search/super-charging")
    @Operation(summary = "搜索超充站", description = "搜索附近的超充站（功率≥150kW）")
    public ResponseEntity<List<GeoSearchResultResponse>> searchSuperChargingStations(
            @Parameter(description = "经度") @RequestParam double longitude,
            @Parameter(description = "纬度") @RequestParam double latitude,
            @Parameter(description = "搜索半径（公里）") @RequestParam(defaultValue = "20.0") double radiusKm) {
        
        logger.debug("搜索超充站: lng={}, lat={}, radius={}km", longitude, latitude, radiusKm);
        
        try {
            List<GeoSearchResult> results = geoSearchService.searchSuperChargingStations(longitude, latitude, radiusKm);
            
            List<GeoSearchResultResponse> response = results.stream()
                .map(GeoSearchResultResponse::fromDomain)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("搜索参数无效: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 获取最近的充电站
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param maxResults 最大结果数
     * @return 最近的充电站列表
     */
    @GetMapping("/nearest")
    @Operation(summary = "获取最近充电站", description = "获取最近的充电站列表")
    public ResponseEntity<List<GeoSearchResultResponse>> getNearestStations(
            @Parameter(description = "经度") @RequestParam double longitude,
            @Parameter(description = "纬度") @RequestParam double latitude,
            @Parameter(description = "最大结果数") @RequestParam(defaultValue = "5") int maxResults) {
        
        logger.debug("获取最近充电站: lng={}, lat={}, maxResults={}", longitude, latitude, maxResults);
        
        try {
            List<GeoSearchResult> results = geoSearchService.getNearestStations(longitude, latitude, maxResults);
            
            List<GeoSearchResultResponse> response = results.stream()
                .map(GeoSearchResultResponse::fromDomain)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("搜索参数无效: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 获取最近的可用充电站
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param maxResults 最大结果数
     * @return 最近的可用充电站列表
     */
    @GetMapping("/nearest/available")
    @Operation(summary = "获取最近可用充电站", description = "获取最近的可用充电站列表")
    public ResponseEntity<List<GeoSearchResultResponse>> getNearestAvailableStations(
            @Parameter(description = "经度") @RequestParam double longitude,
            @Parameter(description = "纬度") @RequestParam double latitude,
            @Parameter(description = "最大结果数") @RequestParam(defaultValue = "5") int maxResults) {
        
        logger.debug("获取最近可用充电站: lng={}, lat={}, maxResults={}", longitude, latitude, maxResults);
        
        try {
            List<GeoSearchResult> results = geoSearchService.getNearestAvailableStations(longitude, latitude, maxResults);
            
            List<GeoSearchResultResponse> response = results.stream()
                .map(GeoSearchResultResponse::fromDomain)
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("搜索参数无效: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 计算两个充电站之间的距离
     * 
     * @param stationId1 充电站1的ID
     * @param stationId2 充电站2的ID
     * @return 距离信息
     */
    @GetMapping("/distance/stations")
    @Operation(summary = "计算充电站距离", description = "计算两个充电站之间的距离")
    public ResponseEntity<DistanceResponse> calculateDistanceBetweenStations(
            @Parameter(description = "充电站1的ID") @RequestParam Long stationId1,
            @Parameter(description = "充电站2的ID") @RequestParam Long stationId2) {
        
        logger.debug("计算充电站距离: stationId1={}, stationId2={}", stationId1, stationId2);
        
        try {
            Optional<Double> distance = geoSearchService.calculateDistanceBetweenStations(stationId1, stationId2);
            
            if (distance.isPresent()) {
                DistanceResponse response = new DistanceResponse(distance.get(), "km");
                return ResponseEntity.ok(response);
            } else {
                throw new NotFoundException("无法计算距离，充电站不存在或位置信息缺失");
            }
            
        } catch (IllegalArgumentException e) {
            logger.warn("计算距离参数无效: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 计算充电站到指定位置的距离
     * 
     * @param stationId 充电站ID
     * @param request 目标位置请求
     * @return 距离信息
     */
    @PostMapping("/distance/to-location")
    @Operation(summary = "计算到位置距离", description = "计算充电站到指定位置的距离")
    public ResponseEntity<DistanceResponse> calculateDistanceToLocation(
            @Parameter(description = "充电站ID") @RequestParam Long stationId,
            @Valid @RequestBody LocationRequest request) {
        
        logger.debug("计算到位置距离: stationId={}, lng={}, lat={}", 
            stationId, request.longitude(), request.latitude());
        
        try {
            Location targetLocation = Location.of(
                BigDecimal.valueOf(request.longitude()),
                BigDecimal.valueOf(request.latitude()),
                "", "", "");
            Optional<Double> distance = geoSearchService.calculateDistanceToLocation(stationId, targetLocation);
            
            if (distance.isPresent()) {
                DistanceResponse response = new DistanceResponse(distance.get(), "km");
                return ResponseEntity.ok(response);
            } else {
                throw new NotFoundException("无法计算距离，充电站不存在或位置信息缺失");
            }
            
        } catch (IllegalArgumentException e) {
            logger.warn("计算距离参数无效: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 统计指定半径内的充电站数量
     * 
     * @param longitude 经度
     * @param latitude 纬度
     * @param radiusKm 半径（公里）
     * @return 充电站数量
     */
    @GetMapping("/count")
    @Operation(summary = "统计充电站数量", description = "统计指定半径内的充电站数量")
    public ResponseEntity<CountResponse> countStationsWithinRadius(
            @Parameter(description = "经度") @RequestParam double longitude,
            @Parameter(description = "纬度") @RequestParam double latitude,
            @Parameter(description = "半径（公里）") @RequestParam double radiusKm) {
        
        logger.debug("统计充电站数量: lng={}, lat={}, radius={}km", longitude, latitude, radiusKm);
        
        try {
            Location centerLocation = Location.of(
                BigDecimal.valueOf(longitude),
                BigDecimal.valueOf(latitude),
                "", "", "");
            long count = geoSearchService.countStationsWithinRadius(centerLocation, radiusKm);
            
            CountResponse response = new CountResponse(count);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("统计参数无效: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 同步地理位置数据
     * 
     * @return 同步结果
     */
    @PostMapping("/sync")
    @Operation(summary = "同步地理数据", description = "同步充电站地理位置数据")
    public ResponseEntity<SyncResponse> syncGeoData() {
        logger.info("开始同步地理位置数据");
        
        try {
            int syncedCount = geoSearchService.syncStationGeoData();
            SyncResponse response = new SyncResponse(syncedCount, "同步完成");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("同步地理位置数据失败", e);
            throw new InternalServerErrorException("同步失败");
        }
    }
    
    /**
     * 检查地理位置数据一致性
     * 
     * @return 一致性检查报告
     */
    @GetMapping("/consistency-check")
    @Operation(summary = "检查数据一致性", description = "检查地理位置数据一致性")
    public ResponseEntity<ConsistencyReportResponse> checkDataConsistency() {
        logger.info("检查地理位置数据一致性");
        
        try {
            GeoLocationService.GeoDataConsistencyReport report = geoSearchService.checkGeoDataConsistency();
            ConsistencyReportResponse response = ConsistencyReportResponse.fromDomain(report);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查数据一致性失败", e);
            throw new InternalServerErrorException("检查失败");
        }
    }
    
    /**
     * 修复地理位置数据不一致问题
     * 
     * @return 修复结果
     */
    @PostMapping("/repair")
    @Operation(summary = "修复数据不一致", description = "修复地理位置数据不一致问题")
    public ResponseEntity<RepairResponse> repairDataInconsistency() {
        logger.info("修复地理位置数据不一致问题");
        
        try {
            int repairedCount = geoSearchService.repairGeoDataInconsistency();
            RepairResponse response = new RepairResponse(repairedCount, "修复完成");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("修复数据不一致失败", e);
            throw new InternalServerErrorException("修复失败");
        }
    }
}
