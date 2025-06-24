package com.ys.charging.station.interfaces.rest;

import com.ys.charging.station.application.service.ConnectorService;
import com.ys.charging.station.interfaces.rest.dto.*;
import com.ys.charging.station.interfaces.rest.exception.*;
import com.ys.charging.station.domain.model.Connector;
import com.ys.charging.station.domain.model.ConnectorStatus;
import com.ys.charging.station.domain.repository.ConnectorRepository;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 充电桩 REST API 控制器
 * 
 * 提供充电桩管理的 RESTful 接口，包括：
 * - 充电桩状态管理
 * - 预约和充电会话控制
 * - 故障管理和心跳监控
 * - 查询和统计功能
 * 
 * @author yang
 * @since 2025-06-23
 */
@RestController
@RequestMapping("/api/v1/connectors")
@Tag(name = "充电桩管理", description = "充电桩相关的 API 接口")
public class ConnectorController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectorController.class);
    
    private final ConnectorService connectorService;
    
    public ConnectorController(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }
    
    /**
     * 根据ID获取充电桩
     * 
     * @param connectorId 充电桩ID
     * @return 充电桩信息
     */
    @GetMapping("/{connectorId}")
    @Operation(summary = "获取充电桩详情", description = "根据ID获取充电桩详细信息")
    public ResponseEntity<ConnectorResponse> getConnector(
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId) {
        
        logger.debug("获取充电桩: connectorId={}", connectorId);
        
        try {
            Connector connector = connectorService.getConnectorById(connectorId);
            ConnectorResponse response = ConnectorResponse.fromDomain(connector);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("充电桩不存在: connectorId={}", connectorId);
            throw new NotFoundException("充电桩不存在");
        }
    }
    
    /**
     * 获取充电站的所有充电桩
     * 
     * @param stationId 充电站ID
     * @return 充电桩列表
     */
    @GetMapping("/station/{stationId}")
    @Operation(summary = "获取充电站充电桩", description = "获取指定充电站的所有充电桩")
    public ResponseEntity<List<ConnectorResponse>> getConnectorsByStation(
            @Parameter(description = "充电站ID") @PathVariable Long stationId) {
        
        logger.debug("获取充电站充电桩: stationId={}", stationId);
        
        List<Connector> connectors = connectorService.getConnectorsByStationId(stationId);
        List<ConnectorResponse> response = connectors.stream()
            .map(ConnectorResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 预约充电桩
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @param request 预约请求
     * @return 预约后的充电桩信息
     */
    @PostMapping("/{connectorId}/reserve")
    @Operation(summary = "预约充电桩", description = "预约指定的充电桩")
    public ResponseEntity<ConnectorResponse> reserveConnector(
            @Parameter(description = "充电站ID") @RequestParam Long stationId,
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId,
            @Valid @RequestBody ReserveConnectorRequest request) {
        
        logger.info("预约充电桩: stationId={}, connectorId={}, userId={}", 
            stationId, connectorId, request.userId());
        
        try {
            Connector connector = connectorService.reserveConnector(
                stationId, connectorId, request.userId(), request.reservationMinutes());
            
            ConnectorResponse response = ConnectorResponse.fromDomain(connector);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("预约充电桩失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 取消预约
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @param request 取消预约请求
     * @return 取消预约后的充电桩信息
     */
    @PostMapping("/{connectorId}/cancel-reservation")
    @Operation(summary = "取消预约", description = "取消充电桩预约")
    public ResponseEntity<ConnectorResponse> cancelReservation(
            @Parameter(description = "充电站ID") @RequestParam Long stationId,
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId,
            @Valid @RequestBody CancelReservationRequest request) {
        
        logger.info("取消预约: stationId={}, connectorId={}, userId={}", 
            stationId, connectorId, request.userId());
        
        try {
            Connector connector = connectorService.cancelReservation(
                stationId, connectorId, request.userId(), request.reason());
            
            ConnectorResponse response = ConnectorResponse.fromDomain(connector);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("取消预约失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 占用充电桩
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @param request 占用请求
     * @return 占用后的充电桩信息
     */
    @PostMapping("/{connectorId}/occupy")
    @Operation(summary = "占用充电桩", description = "用户到达并占用充电桩")
    public ResponseEntity<ConnectorResponse> occupyConnector(
            @Parameter(description = "充电站ID") @RequestParam Long stationId,
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId,
            @Valid @RequestBody OccupyConnectorRequest request) {
        
        logger.info("占用充电桩: stationId={}, connectorId={}, userId={}", 
            stationId, connectorId, request.userId());
        
        try {
            Connector connector = connectorService.occupyConnector(stationId, connectorId, request.userId());
            
            ConnectorResponse response = ConnectorResponse.fromDomain(connector);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("占用充电桩失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 开始充电
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @return 开始充电后的充电桩信息
     */
    @PostMapping("/{connectorId}/start-charging")
    @Operation(summary = "开始充电", description = "开始充电会话")
    public ResponseEntity<ConnectorResponse> startCharging(
            @Parameter(description = "充电站ID") @RequestParam Long stationId,
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId) {
        
        logger.info("开始充电: stationId={}, connectorId={}", stationId, connectorId);
        
        try {
            Connector connector = connectorService.startCharging(stationId, connectorId);
            
            ConnectorResponse response = ConnectorResponse.fromDomain(connector);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("开始充电失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 停止充电
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @return 停止充电后的充电桩信息
     */
    @PostMapping("/{connectorId}/stop-charging")
    @Operation(summary = "停止充电", description = "停止充电会话")
    public ResponseEntity<ConnectorResponse> stopCharging(
            @Parameter(description = "充电站ID") @RequestParam Long stationId,
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId) {
        
        logger.info("停止充电: stationId={}, connectorId={}", stationId, connectorId);
        
        try {
            Connector connector = connectorService.stopCharging(stationId, connectorId);
            
            ConnectorResponse response = ConnectorResponse.fromDomain(connector);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("停止充电失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 释放充电桩
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @return 释放后的充电桩信息
     */
    @PostMapping("/{connectorId}/release")
    @Operation(summary = "释放充电桩", description = "用户拔枪并释放充电桩")
    public ResponseEntity<ConnectorResponse> releaseConnector(
            @Parameter(description = "充电站ID") @RequestParam Long stationId,
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId) {
        
        logger.info("释放充电桩: stationId={}, connectorId={}", stationId, connectorId);
        
        try {
            Connector connector = connectorService.releaseConnector(stationId, connectorId);
            
            ConnectorResponse response = ConnectorResponse.fromDomain(connector);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("释放充电桩失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 设置充电桩故障
     * 
     * @param connectorId 充电桩ID
     * @param request 故障设置请求
     * @return 设置故障后的充电桩信息
     */
    @PostMapping("/{connectorId}/fault")
    @Operation(summary = "设置充电桩故障", description = "设置充电桩为故障状态")
    public ResponseEntity<ConnectorResponse> setConnectorFault(
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId,
            @Valid @RequestBody SetFaultRequest request) {
        
        logger.info("设置充电桩故障: connectorId={}, reason={}", connectorId, request.faultReason());
        
        try {
            Connector connector = connectorService.setConnectorFault(connectorId, request.faultReason());
            
            ConnectorResponse response = ConnectorResponse.fromDomain(connector);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("设置充电桩故障失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 修复充电桩
     * 
     * @param connectorId 充电桩ID
     * @return 修复后的充电桩信息
     */
    @PostMapping("/{connectorId}/repair")
    @Operation(summary = "修复充电桩", description = "修复充电桩故障")
    public ResponseEntity<ConnectorResponse> repairConnector(
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId) {
        
        logger.info("修复充电桩: connectorId={}", connectorId);
        
        try {
            Connector connector = connectorService.repairConnector(connectorId);
            
            ConnectorResponse response = ConnectorResponse.fromDomain(connector);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("修复充电桩失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 更新充电桩心跳
     * 
     * @param connectorId 充电桩ID
     * @return 成功响应
     */
    @PostMapping("/{connectorId}/heartbeat")
    @Operation(summary = "更新心跳", description = "更新充电桩心跳时间")
    public ResponseEntity<Void> updateHeartbeat(
            @Parameter(description = "充电桩ID") @PathVariable Long connectorId) {
        
        logger.debug("更新充电桩心跳: connectorId={}", connectorId);
        
        try {
            connectorService.updateConnectorHeartbeat(connectorId);
            return ResponseEntity.ok().build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("更新心跳失败: {}", e.getMessage());
            throw new BadRequestException(e.getMessage());
        }
    }
    
    /**
     * 根据状态查询充电桩
     * 
     * @param status 充电桩状态
     * @param pageable 分页参数
     * @return 分页的充电桩列表
     */
    @GetMapping("/by-status/{status}")
    @Operation(summary = "按状态查询充电桩", description = "根据状态分页查询充电桩")
    public ResponseEntity<Page<ConnectorResponse>> getConnectorsByStatus(
            @Parameter(description = "充电桩状态") @PathVariable String status,
            @PageableDefault(size = 20) Pageable pageable) {
        
        logger.debug("按状态查询充电桩: status={}", status);
        
        try {
            ConnectorStatus connectorStatus = ConnectorStatus.valueOf(status.toUpperCase());
            Page<Connector> connectors = connectorService.getConnectorsByStatus(connectorStatus, pageable);
            Page<ConnectorResponse> response = connectors.map(ConnectorResponse::fromDomain);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("无效的充电桩状态: {}", status);
            throw new BadRequestException("无效的充电桩状态");
        }
    }
    
    /**
     * 获取可用的充电桩
     * 
     * @param stationId 充电站ID（可选）
     * @return 可用的充电桩列表
     */
    @GetMapping("/available")
    @Operation(summary = "获取可用充电桩", description = "获取可用的充电桩列表")
    public ResponseEntity<List<ConnectorResponse>> getAvailableConnectors(
            @Parameter(description = "充电站ID（可选）") @RequestParam(required = false) Long stationId) {
        
        logger.debug("获取可用充电桩: stationId={}", stationId);
        
        List<Connector> connectors = connectorService.getAvailableConnectors(stationId);
        List<ConnectorResponse> response = connectors.stream()
            .map(ConnectorResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取用户预约的充电桩
     * 
     * @param userId 用户ID
     * @return 用户预约的充电桩列表
     */
    @GetMapping("/user/{userId}/reservations")
    @Operation(summary = "获取用户预约", description = "获取用户预约的充电桩列表")
    public ResponseEntity<List<ConnectorResponse>> getUserReservations(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        
        logger.debug("获取用户预约: userId={}", userId);
        
        List<Connector> connectors = connectorService.getUserReservedConnectors(userId);
        List<ConnectorResponse> response = connectors.stream()
            .map(ConnectorResponse::fromDomain)
            .toList();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取充电桩统计信息
     * 
     * @param stationId 充电站ID（可选）
     * @return 统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取充电桩统计", description = "获取充电桩统计信息")
    public ResponseEntity<ConnectorStatisticsResponse> getConnectorStatistics(
            @Parameter(description = "充电站ID（可选）") @RequestParam(required = false) Long stationId) {
        
        logger.debug("获取充电桩统计: stationId={}", stationId);
        
        ConnectorRepository.ConnectorStatistics statistics = 
            connectorService.getConnectorStatistics(stationId);
        ConnectorStatisticsResponse response = ConnectorStatisticsResponse.fromDomain(statistics);
        
        return ResponseEntity.ok(response);
    }
}
