package com.ys.charging.station.application.service;

import com.ys.charging.station.domain.model.Connector;
import com.ys.charging.station.domain.model.ConnectorStatus;
import com.ys.charging.station.domain.model.Station;
import com.ys.charging.station.domain.repository.ConnectorRepository;
import com.ys.charging.station.domain.repository.StationRepository;
import com.ys.charging.station.domain.service.ConnectorStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 充电桩应用服务
 * 
 * 协调充电桩相关的业务逻辑，包括：
 * - 充电桩状态管理和流转
 * - 预约管理和超时处理
 * - 充电会话控制
 * - 心跳监控和故障处理
 * 
 * @author yang
 * @since 2025-06-23
 */
@Service
@Transactional
public class ConnectorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectorService.class);
    
    private final ConnectorRepository connectorRepository;
    private final StationRepository stationRepository;
    private final ConnectorStateService stateService;
    
    public ConnectorService(ConnectorRepository connectorRepository,
                           StationRepository stationRepository,
                           ConnectorStateService stateService) {
        this.connectorRepository = connectorRepository;
        this.stationRepository = stationRepository;
        this.stateService = stateService;
    }
    
    /**
     * 预约充电桩
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @param userId 用户ID
     * @param reservationMinutes 预约时长（分钟）
     * @return 更新后的充电桩
     */
    @CacheEvict(value = {"connectors", "connector-status"}, key = "#connectorId")
    public Connector reserveConnector(Long stationId, Long connectorId, Long userId, int reservationMinutes) {
        logger.info("预约充电桩: stationId={}, connectorId={}, userId={}, minutes={}", 
            stationId, connectorId, userId, reservationMinutes);
        
        // 获取充电站并验证
        Station station = getStationById(stationId);
        
        // 通过聚合根进行预约（会验证业务规则和发布事件）
        station.reserveConnector(connectorId, userId, reservationMinutes);
        
        // 保存充电站（级联保存充电桩）
        stationRepository.save(station);
        
        // 获取更新后的充电桩
        Connector connector = getConnectorById(connectorId);
        
        // 更新状态缓存
        stateService.setCachedConnectorStatus(connectorId, connector.getStatus(), 3600);
        
        // 记录状态变更
        stateService.recordStatusChange(connectorId, ConnectorStatus.IDLE, ConnectorStatus.RESERVED, 
            "用户预约", userId);
        
        logger.info("充电桩预约成功: connectorId={}, userId={}", connectorId, userId);
        return connector;
    }
    
    /**
     * 取消预约
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @param userId 用户ID
     * @param reason 取消原因
     * @return 更新后的充电桩
     */
    @CacheEvict(value = {"connectors", "connector-status"}, key = "#connectorId")
    public Connector cancelReservation(Long stationId, Long connectorId, Long userId, String reason) {
        logger.info("取消预约: stationId={}, connectorId={}, userId={}, reason={}", 
            stationId, connectorId, userId, reason);
        
        Station station = getStationById(stationId);
        
        // 通过聚合根取消预约
        station.cancelReservation(connectorId, userId, reason);
        
        stationRepository.save(station);
        
        Connector connector = getConnectorById(connectorId);
        
        // 更新状态缓存
        stateService.setCachedConnectorStatus(connectorId, connector.getStatus(), 3600);
        
        // 记录状态变更
        stateService.recordStatusChange(connectorId, ConnectorStatus.RESERVED, ConnectorStatus.IDLE, 
            reason, userId);
        
        logger.info("预约取消成功: connectorId={}, userId={}", connectorId, userId);
        return connector;
    }
    
    /**
     * 占用充电桩（用户到达并插枪）
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @param userId 用户ID
     * @return 更新后的充电桩
     */
    @CacheEvict(value = {"connectors", "connector-status"}, key = "#connectorId")
    public Connector occupyConnector(Long stationId, Long connectorId, Long userId) {
        logger.info("占用充电桩: stationId={}, connectorId={}, userId={}", stationId, connectorId, userId);
        
        Station station = getStationById(stationId);
        
        // 通过聚合根占用充电桩
        station.occupyConnector(connectorId, userId);
        
        stationRepository.save(station);
        
        Connector connector = getConnectorById(connectorId);
        
        // 更新状态缓存
        stateService.setCachedConnectorStatus(connectorId, connector.getStatus(), 3600);
        
        logger.info("充电桩占用成功: connectorId={}, userId={}", connectorId, userId);
        return connector;
    }
    
    /**
     * 开始充电
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @return 更新后的充电桩
     */
    @CacheEvict(value = {"connectors", "connector-status"}, key = "#connectorId")
    public Connector startCharging(Long stationId, Long connectorId) {
        logger.info("开始充电: stationId={}, connectorId={}", stationId, connectorId);
        
        Station station = getStationById(stationId);
        
        // 通过聚合根开始充电
        station.startCharging(connectorId);
        
        stationRepository.save(station);
        
        Connector connector = getConnectorById(connectorId);
        
        // 更新状态缓存
        stateService.setCachedConnectorStatus(connectorId, connector.getStatus(), 3600);
        
        logger.info("充电开始成功: connectorId={}", connectorId);
        return connector;
    }
    
    /**
     * 停止充电
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @return 更新后的充电桩
     */
    @CacheEvict(value = {"connectors", "connector-status"}, key = "#connectorId")
    public Connector stopCharging(Long stationId, Long connectorId) {
        logger.info("停止充电: stationId={}, connectorId={}", stationId, connectorId);
        
        Station station = getStationById(stationId);
        
        // 通过聚合根停止充电
        station.stopCharging(connectorId);
        
        stationRepository.save(station);
        
        Connector connector = getConnectorById(connectorId);
        
        // 更新状态缓存
        stateService.setCachedConnectorStatus(connectorId, connector.getStatus(), 3600);
        
        logger.info("充电停止成功: connectorId={}", connectorId);
        return connector;
    }
    
    /**
     * 释放充电桩（用户拔枪离开）
     * 
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @return 更新后的充电桩
     */
    @CacheEvict(value = {"connectors", "connector-status"}, key = "#connectorId")
    public Connector releaseConnector(Long stationId, Long connectorId) {
        logger.info("释放充电桩: stationId={}, connectorId={}", stationId, connectorId);
        
        Station station = getStationById(stationId);
        
        // 通过聚合根释放充电桩
        station.releaseConnector(connectorId);
        
        stationRepository.save(station);
        
        Connector connector = getConnectorById(connectorId);
        
        // 更新状态缓存
        stateService.setCachedConnectorStatus(connectorId, connector.getStatus(), 3600);
        
        logger.info("充电桩释放成功: connectorId={}", connectorId);
        return connector;
    }
    
    /**
     * 设置充电桩故障
     * 
     * @param connectorId 充电桩ID
     * @param faultReason 故障原因
     * @return 更新后的充电桩
     */
    @CacheEvict(value = {"connectors", "connector-status"}, key = "#connectorId")
    public Connector setConnectorFault(Long connectorId, String faultReason) {
        logger.info("设置充电桩故障: connectorId={}, reason={}", connectorId, faultReason);
        
        Connector connector = getConnectorById(connectorId);
        ConnectorStatus oldStatus = connector.getStatus();
        
        // 设置故障状态
        connector.setFault(faultReason);
        
        connectorRepository.save(connector);
        
        // 更新状态缓存
        stateService.setCachedConnectorStatus(connectorId, connector.getStatus(), 3600);
        
        // 记录状态变更
        stateService.recordStatusChange(connectorId, oldStatus, ConnectorStatus.FAULT, 
            faultReason, null);
        
        logger.info("充电桩故障设置成功: connectorId={}", connectorId);
        return connector;
    }
    
    /**
     * 修复充电桩故障
     * 
     * @param connectorId 充电桩ID
     * @return 更新后的充电桩
     */
    @CacheEvict(value = {"connectors", "connector-status"}, key = "#connectorId")
    public Connector repairConnector(Long connectorId) {
        logger.info("修复充电桩: connectorId={}", connectorId);
        
        Connector connector = getConnectorById(connectorId);
        ConnectorStatus oldStatus = connector.getStatus();
        
        // 修复故障
        connector.repair();
        
        connectorRepository.save(connector);
        
        // 更新状态缓存
        stateService.setCachedConnectorStatus(connectorId, connector.getStatus(), 3600);
        
        // 记录状态变更
        stateService.recordStatusChange(connectorId, oldStatus, ConnectorStatus.IDLE, 
            "故障修复", null);
        
        logger.info("充电桩修复成功: connectorId={}", connectorId);
        return connector;
    }
    
    /**
     * 更新充电桩心跳
     * 
     * @param connectorId 充电桩ID
     */
    public void updateConnectorHeartbeat(Long connectorId) {
        Connector connector = getConnectorById(connectorId);
        
        // 更新心跳时间
        connector.updateHeartbeat();
        
        connectorRepository.save(connector);
        
        // 更新状态服务中的心跳时间
        stateService.updateConnectorHeartbeat(connectorId, Instant.now());
        
        logger.debug("充电桩心跳更新: connectorId={}", connectorId);
    }
    
    /**
     * 根据ID获取充电桩
     * 
     * @param connectorId 充电桩ID
     * @return 充电桩
     */
    @Cacheable(value = "connectors", key = "#connectorId")
    @Transactional(readOnly = true)
    public Connector getConnectorById(Long connectorId) {
        return connectorRepository.findById(connectorId)
            .orElseThrow(() -> new IllegalArgumentException("充电桩不存在: " + connectorId));
    }
    
    /**
     * 获取充电站的所有充电桩
     * 
     * @param stationId 充电站ID
     * @return 充电桩列表
     */
    @Transactional(readOnly = true)
    public List<Connector> getConnectorsByStationId(Long stationId) {
        return connectorRepository.findByStationId(stationId);
    }
    
    /**
     * 获取可用的充电桩
     * 
     * @param stationId 充电站ID，如果为null则查找所有充电站
     * @return 可用的充电桩列表
     */
    @Transactional(readOnly = true)
    public List<Connector> getAvailableConnectors(Long stationId) {
        return connectorRepository.findAvailableConnectors(stationId);
    }
    
    /**
     * 根据状态获取充电桩
     * 
     * @param status 充电桩状态
     * @param pageable 分页参数
     * @return 分页的充电桩列表
     */
    @Transactional(readOnly = true)
    public Page<Connector> getConnectorsByStatus(ConnectorStatus status, Pageable pageable) {
        return connectorRepository.findByStatus(status, pageable);
    }
    
    /**
     * 获取用户预约的充电桩
     * 
     * @param userId 用户ID
     * @return 用户预约的充电桩列表
     */
    @Transactional(readOnly = true)
    public List<Connector> getUserReservedConnectors(Long userId) {
        return connectorRepository.findByReservedByUserId(userId);
    }
    
    /**
     * 处理预约超时
     * 
     * @param stationId 充电站ID，如果为null则处理所有充电站
     * @return 处理的超时数量
     */
    public int handleReservationTimeouts(Long stationId) {
        logger.info("处理预约超时: stationId={}", stationId);
        
        int handledCount = stateService.batchHandleReservationTimeouts(stationId);
        
        if (handledCount > 0) {
            logger.info("预约超时处理完成: 处理了 {} 个超时预约", handledCount);
        }
        
        return handledCount;
    }
    
    /**
     * 检查并处理离线充电桩
     * 
     * @param timeoutMinutes 离线超时时间（分钟）
     * @return 处理的离线充电桩数量
     */
    public int handleOfflineConnectors(int timeoutMinutes) {
        logger.info("检查离线充电桩: timeoutMinutes={}", timeoutMinutes);
        
        int offlineCount = stateService.checkAndHandleOfflineConnectors(timeoutMinutes);
        
        if (offlineCount > 0) {
            logger.warn("检测到 {} 个充电桩离线", offlineCount);
        }
        
        return offlineCount;
    }
    
    /**
     * 获取充电桩统计信息
     * 
     * @param stationId 充电站ID，如果为null则统计全局
     * @return 统计信息
     */
    @Cacheable(value = "statistics", key = "'connector:' + (#stationId ?: 'global')")
    @Transactional(readOnly = true)
    public ConnectorRepository.ConnectorStatistics getConnectorStatistics(Long stationId) {
        return connectorRepository.getStatistics(stationId);
    }
    
    // 私有辅助方法
    private Station getStationById(Long stationId) {
        return stationRepository.findById(stationId)
            .orElseThrow(() -> new IllegalArgumentException("充电站不存在: " + stationId));
    }
}
