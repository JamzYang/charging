package com.ys.charging.station.interfaces.rest;

import com.ys.charging.station.application.service.StationService;
import com.ys.charging.station.interfaces.rest.dto.*;
import com.ys.charging.station.interfaces.rest.exception.*;
import com.ys.charging.station.domain.model.*;
import com.ys.charging.station.domain.repository.StationRepository;
import com.ys.charging.station.interfaces.rest.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 充电站 REST API 控制器
 * 
 * 提供充电站管理的 RESTful 接口，包括：
 * - 充电站的 CRUD 操作
 * - 状态管理和查询
 * - 搜索和分页
 * - 统计信息获取
 * 
 * @author yang
 * @since 2025-06-23
 */
@RestController
@RequestMapping("/api/v1/stations")
@Tag(name = "充电站管理", description = "充电站相关的 API 接口")
public class StationController {
    
    private static final Logger logger = LoggerFactory.getLogger(StationController.class);
    
    private final StationService stationService;
    
    public StationController(StationService stationService) {
        this.stationService = stationService;
    }
    
    /**
     * 创建充电站
     * 
     * @param request 创建请求
     * @return 创建的充电站信息
     */
    @PostMapping
    @Operation(summary = "创建充电站", description = "创建新的充电站")
    public ResponseEntity<StationResponse> createStation(@Valid @RequestBody CreateStationRequest request) {
        logger.info("创建充电站请求: name={}, operator={}", request.name(), request.operator());
        
        try {
            // 转换为领域对象
            StationInfo stationInfo = new StationInfo(
                request.name(), request.operator(), request.contactPhone(), 
                request.description(), request.facilities()
            );
            
            Location location = Location.of(
                request.longitude().doubleValue(), request.latitude().doubleValue(),
                request.address(), request.city(), request.province()
            );
            
            BusinessHours businessHours = new BusinessHours(
                request.openTime(), request.closeTime(), request.is24Hours()
            );
            
            // 创建充电站
            Station station = stationService.createStation(stationInfo, location, businessHours);
            
            // 转换为响应对象
            StationResponse response = StationResponse.fromDomain(station);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("创建充电站失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            logger.error("创建充电站异常", e);
            throw new InternalServerErrorException("创建充电站失败");
        }
    }
    
    /**
     * 根据ID获取充电站
     * 
     * @param stationId 充电站ID
     * @return 充电站信息
     */
    @GetMapping("/{stationId}")
    @Operation(summary = "获取充电站详情", description = "根据ID获取充电站详细信息")
    public ResponseEntity<StationResponse> getStation(
            @Parameter(description = "充电站ID") @PathVariable Long stationId) {
        
        logger.debug("获取充电站: stationId={}", stationId);
        
        try {
            Station station = stationService.getStationById(stationId);
            StationResponse response = StationResponse.fromDomain(station);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("充电站不存在: stationId={}", stationId);
            throw new NotFoundException("充电站不存在");
        }
    }
    
    /**
     * 获取充电站（包含充电桩信息）
     * 
     * @param stationId 充电站ID
     * @return 充电站详细信息
     */
    @GetMapping("/{stationId}/with-connectors")
    @Operation(summary = "获取充电站详情（含充电桩）", description = "获取充电站及其所有充电桩信息")
    public ResponseEntity<StationDetailResponse> getStationWithConnectors(
            @Parameter(description = "充电站ID") @PathVariable Long stationId) {
        
        logger.debug("获取充电站详情: stationId={}", stationId);
        
        try {
            Station station = stationService.getStationWithConnectors(stationId);
            StationDetailResponse response = StationDetailResponse.fromDomain(station);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("充电站不存在: stationId={}", stationId);
            throw new NotFoundException("充电站不存在");
        }
    }
    
    /**
     * 更新充电站信息
     * 
     * @param stationId 充电站ID
     * @param request 更新请求
     * @return 更新后的充电站信息
     */
    @PutMapping("/{stationId}")
    @Operation(summary = "更新充电站信息", description = "更新充电站基本信息")
    public ResponseEntity<StationResponse> updateStation(
            @Parameter(description = "充电站ID") @PathVariable Long stationId,
            @Valid @RequestBody UpdateStationRequest request) {
        
        logger.info("更新充电站: stationId={}", stationId);
        
        try {
            StationInfo stationInfo = new StationInfo(
                request.name(), request.operator(), request.contactPhone(),
                request.description(), request.facilities()
            );
            
            Station station = stationService.updateStationInfo(stationId, stationInfo);
            StationResponse response = StationResponse.fromDomain(station);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("更新充电站失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 更新充电站地理位置
     * 
     * @param stationId 充电站ID
     * @param request 位置更新请求
     * @return 更新后的充电站信息
     */
    @PutMapping("/{stationId}/location")
    @Operation(summary = "更新充电站位置", description = "更新充电站地理位置信息")
    public ResponseEntity<StationResponse> updateStationLocation(
            @Parameter(description = "充电站ID") @PathVariable Long stationId,
            @Valid @RequestBody UpdateLocationRequest request) {
        
        logger.info("更新充电站位置: stationId={}", stationId);
        
        try {
            Location location = Location.of(
                request.longitude().doubleValue(), request.latitude().doubleValue(),
                request.address(), request.city(), request.province()
            );
            
            Station station = stationService.updateStationLocation(stationId, location);
            StationResponse response = StationResponse.fromDomain(station);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("更新充电站位置失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 更改充电站状态
     * 
     * @param stationId 充电站ID
     * @param request 状态更改请求
     * @return 更新后的充电站信息
     */
    @PutMapping("/{stationId}/status")
    @Operation(summary = "更改充电站状态", description = "更改充电站运营状态")
    public ResponseEntity<StationResponse> changeStationStatus(
            @Parameter(description = "充电站ID") @PathVariable Long stationId,
            @Valid @RequestBody ChangeStatusRequest request) {
        
        logger.info("更改充电站状态: stationId={}, status={}", stationId, request.status());
        
        try {
            StationStatus status = StationStatus.valueOf(request.status());
            Station station = stationService.changeStationStatus(stationId, status, request.reason());
            StationResponse response = StationResponse.fromDomain(station);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("更改充电站状态失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 删除充电站
     * 
     * @param stationId 充电站ID
     * @return 删除结果
     */
    @DeleteMapping("/{stationId}")
    @Operation(summary = "删除充电站", description = "删除指定的充电站")
    public ResponseEntity<Void> deleteStation(
            @Parameter(description = "充电站ID") @PathVariable Long stationId) {
        
        logger.info("删除充电站: stationId={}", stationId);
        
        try {
            stationService.deleteStation(stationId);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("删除充电站失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("充电站无法删除: {}", e.getMessage());
            throw new ConflictException(e.getMessage());
        }
    }
    
    /**
     * 分页查询充电站
     * 
     * @param pageable 分页参数
     * @return 分页的充电站列表
     */
    @GetMapping
    @Operation(summary = "分页查询充电站", description = "分页获取充电站列表")
    public ResponseEntity<Page<StationResponse>> getStations(
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("分页查询充电站: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Station> stations = stationService.getStationsByStatus(null, pageable);
        Page<StationResponse> response = stations.map(StationResponse::fromDomain);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据状态查询充电站
     * 
     * @param status 充电站状态
     * @param pageable 分页参数
     * @return 分页的充电站列表
     */
    @GetMapping("/by-status/{status}")
    @Operation(summary = "按状态查询充电站", description = "根据状态分页查询充电站")
    public ResponseEntity<Page<StationResponse>> getStationsByStatus(
            @Parameter(description = "充电站状态") @PathVariable String status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("按状态查询充电站: status={}", status);
        
        try {
            StationStatus stationStatus = StationStatus.valueOf(status.toUpperCase());
            Page<Station> stations = stationService.getStationsByStatus(stationStatus, pageable);
            Page<StationResponse> response = stations.map(StationResponse::fromDomain);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("无效的充电站状态: {}", status);
            throw new BadRequestException("无效的充电站状态");
        }
    }
    
    /**
     * 搜索充电站
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 匹配的充电站列表
     */
    @GetMapping("/search")
    @Operation(summary = "搜索充电站", description = "根据关键词搜索充电站")
    public ResponseEntity<Page<StationResponse>> searchStations(
            @Parameter(description = "搜索关键词") @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("搜索充电站: keyword={}", keyword);
        
        Page<Station> stations = stationService.searchStations(keyword, pageable);
        Page<StationResponse> response = stations.map(StationResponse::fromDomain);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取有可用充电桩的充电站
     * 
     * @return 有可用充电桩的充电站列表
     */
    @GetMapping("/available")
    @Operation(summary = "获取可用充电站", description = "获取有可用充电桩的充电站列表")
    public ResponseEntity<List<StationResponse>> getAvailableStations() {
        logger.debug("获取可用充电站");
        
        List<Station> stations = stationService.getStationsWithAvailableConnectors();
        List<StationResponse> response = stations.stream()
            .map(StationResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取运营中的充电站
     * 
     * @return 运营中的充电站列表
     */
    @GetMapping("/operating")
    @Operation(summary = "获取运营中充电站", description = "获取所有运营中的充电站")
    public ResponseEntity<List<StationResponse>> getOperatingStations() {
        logger.debug("获取运营中充电站");
        
        List<Station> stations = stationService.getOperatingStations();
        List<StationResponse> response = stations.stream()
            .map(StationResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取充电站统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取充电站统计", description = "获取充电站统计信息")
    public ResponseEntity<StationStatisticsResponse> getStationStatistics() {
        logger.debug("获取充电站统计信息");
        
        StationRepository.StationStatistics statistics = stationService.getStationStatistics();
        StationStatisticsResponse response = StationStatisticsResponse.fromDomain(statistics);
        
        return ResponseEntity.ok(response);
    }
}
