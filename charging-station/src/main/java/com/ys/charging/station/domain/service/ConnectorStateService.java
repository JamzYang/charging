package com.ys.charging.station.domain.service;

import com.ys.charging.station.domain.model.Connector;
import com.ys.charging.station.domain.model.ConnectorStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 充电桩状态管理服务接口
 * 
 * 负责充电桩状态的复杂业务逻辑，包括：
 * - 状态流转验证和管理
 * - 预约超时处理
 * - 心跳监控和离线检测
 * - 状态缓存和同步
 * 
 * @author yang
 * @since 2025-06-23
 */
public interface ConnectorStateService {
    
    /**
     * 验证状态转换是否合法
     * 
     * @param currentStatus 当前状态
     * @param targetStatus 目标状态
     * @return true 如果转换合法
     */
    boolean isValidStateTransition(ConnectorStatus currentStatus, ConnectorStatus targetStatus);
    
    /**
     * 获取状态转换的可能路径
     * 
     * @param currentStatus 当前状态
     * @return 可以转换到的状态列表
     */
    List<ConnectorStatus> getValidNextStates(ConnectorStatus currentStatus);
    
    /**
     * 处理充电桩预约超时
     * 
     * @param connectorId 充电桩ID
     * @return true 如果处理了超时
     */
    boolean handleReservationTimeout(Long connectorId);
    
    /**
     * 批量处理预约超时
     * 
     * @param stationId 充电站ID，如果为null则处理所有充电站
     * @return 处理的超时数量
     */
    int batchHandleReservationTimeouts(Long stationId);
    
    /**
     * 更新充电桩心跳
     * 
     * @param connectorId 充电桩ID
     * @param heartbeatTime 心跳时间
     */
    void updateConnectorHeartbeat(Long connectorId, Instant heartbeatTime);
    
    /**
     * 检查并处理离线充电桩
     * 
     * @param timeoutMinutes 离线超时时间（分钟）
     * @return 处理的离线充电桩数量
     */
    int checkAndHandleOfflineConnectors(int timeoutMinutes);
    
    /**
     * 获取充电桩状态统计
     * 
     * @param stationId 充电站ID
     * @return 状态统计信息
     */
    ConnectorStatusStatistics getConnectorStatusStatistics(Long stationId);
    
    /**
     * 获取全局充电桩状态统计
     * 
     * @return 全局状态统计信息
     */
    ConnectorStatusStatistics getGlobalConnectorStatusStatistics();
    
    /**
     * 刷新充电桩状态缓存
     * 
     * @param connectorId 充电桩ID
     */
    void refreshConnectorStatusCache(Long connectorId);
    
    /**
     * 批量刷新充电桩状态缓存
     * 
     * @param connectorIds 充电桩ID列表
     */
    void batchRefreshConnectorStatusCache(List<Long> connectorIds);
    
    /**
     * 获取缓存的充电桩状态
     * 
     * @param connectorId 充电桩ID
     * @return 缓存的状态，如果不存在则返回 empty
     */
    Optional<ConnectorStatus> getCachedConnectorStatus(Long connectorId);
    
    /**
     * 设置充电桩状态缓存
     * 
     * @param connectorId 充电桩ID
     * @param status 状态
     * @param ttlSeconds 缓存过期时间（秒）
     */
    void setCachedConnectorStatus(Long connectorId, ConnectorStatus status, long ttlSeconds);
    
    /**
     * 清除充电桩状态缓存
     * 
     * @param connectorId 充电桩ID
     */
    void clearConnectorStatusCache(Long connectorId);
    
    /**
     * 获取状态变更历史
     * 
     * @param connectorId 充电桩ID
     * @param limit 限制数量
     * @return 状态变更历史列表
     */
    List<ConnectorStatusHistory> getConnectorStatusHistory(Long connectorId, int limit);
    
    /**
     * 记录状态变更历史
     * 
     * @param connectorId 充电桩ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @param reason 变更原因
     * @param userId 相关用户ID（可选）
     */
    void recordStatusChange(Long connectorId, ConnectorStatus oldStatus, ConnectorStatus newStatus, 
                           String reason, Long userId);
    
    /**
     * 充电桩状态统计信息
     */
    record ConnectorStatusStatistics(
        Long stationId,
        int totalConnectors,
        int idleConnectors,
        int reservedConnectors,
        int occupiedConnectors,
        int chargingConnectors,
        int faultConnectors,
        int offlineConnectors,
        int maintenanceConnectors,
        double availabilityRate,
        double utilizationRate,
        Instant lastUpdated
    ) {
        
        /**
         * 获取可用充电桩数量
         */
        public int getAvailableConnectors() {
            return idleConnectors;
        }
        
        /**
         * 获取使用中充电桩数量
         */
        public int getInUseConnectors() {
            return reservedConnectors + occupiedConnectors + chargingConnectors;
        }
        
        /**
         * 获取不可用充电桩数量
         */
        public int getUnavailableConnectors() {
            return faultConnectors + offlineConnectors + maintenanceConnectors;
        }
        
        /**
         * 计算可用率
         */
        public double calculateAvailabilityRate() {
            if (totalConnectors == 0) {
                return 0.0;
            }
            return (double) (totalConnectors - getUnavailableConnectors()) / totalConnectors * 100;
        }
        
        /**
         * 计算使用率
         */
        public double calculateUtilizationRate() {
            if (totalConnectors == 0) {
                return 0.0;
            }
            return (double) getInUseConnectors() / totalConnectors * 100;
        }
        
        /**
         * 获取统计摘要
         */
        public String getSummary() {
            return String.format(
                "总计: %d, 可用: %d, 使用中: %d, 不可用: %d, 可用率: %.1f%%, 使用率: %.1f%%",
                totalConnectors, getAvailableConnectors(), getInUseConnectors(), 
                getUnavailableConnectors(), availabilityRate, utilizationRate
            );
        }
    }
    
    /**
     * 充电桩状态变更历史
     */
    record ConnectorStatusHistory(
        Long connectorId,
        ConnectorStatus oldStatus,
        ConnectorStatus newStatus,
        String reason,
        Long userId,
        Instant changedAt
    ) {
        
        /**
         * 获取状态变更描述
         */
        public String getDescription() {
            StringBuilder desc = new StringBuilder();
            desc.append(String.format("%s → %s", 
                oldStatus.getDescription(), newStatus.getDescription()));
            
            if (reason != null && !reason.trim().isEmpty()) {
                desc.append(" (").append(reason).append(")");
            }
            
            if (userId != null) {
                desc.append(" [用户: ").append(userId).append("]");
            }
            
            return desc.toString();
        }
        
        /**
         * 判断是否为可用性变更
         */
        public boolean isAvailabilityChange() {
            return oldStatus.isAvailable() != newStatus.isAvailable();
        }
        
        /**
         * 判断是否为故障相关变更
         */
        public boolean isFaultRelated() {
            return oldStatus.isFaulty() || newStatus.isFaulty();
        }
    }
}
