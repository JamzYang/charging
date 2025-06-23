package com.ys.charging.station.domain.repository;

import com.ys.charging.station.domain.model.Connector;
import com.ys.charging.station.domain.model.ConnectorStatus;
import com.ys.charging.station.domain.model.ConnectorInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 充电桩仓储接口
 * 
 * 定义充电桩实体的持久化操作契约。
 * 支持复杂的状态查询和统计功能。
 * 
 * @author yang
 * @since 2025-06-23
 */
public interface ConnectorRepository {
    
    /**
     * 保存充电桩
     * 
     * @param connector 充电桩实体
     * @return 保存后的充电桩
     */
    Connector save(Connector connector);
    
    /**
     * 根据ID查找充电桩
     * 
     * @param id 充电桩ID
     * @return 充电桩，如果不存在则返回 empty
     */
    Optional<Connector> findById(Long id);
    
    /**
     * 根据充电站ID查找所有充电桩
     * 
     * @param stationId 充电站ID
     * @return 充电桩列表
     */
    List<Connector> findByStationId(Long stationId);
    
    /**
     * 根据充电站ID和状态查找充电桩
     * 
     * @param stationId 充电站ID
     * @param status 充电桩状态
     * @return 充电桩列表
     */
    List<Connector> findByStationIdAndStatus(Long stationId, ConnectorStatus status);
    
    /**
     * 根据状态查找充电桩
     * 
     * @param status 充电桩状态
     * @return 充电桩列表
     */
    List<Connector> findByStatus(ConnectorStatus status);
    
    /**
     * 根据状态分页查找充电桩
     * 
     * @param status 充电桩状态
     * @param pageable 分页参数
     * @return 分页的充电桩列表
     */
    Page<Connector> findByStatus(ConnectorStatus status, Pageable pageable);
    
    /**
     * 查找可预约的充电桩
     * 
     * @param stationId 充电站ID，如果为null则查找所有充电站
     * @return 可预约的充电桩列表
     */
    List<Connector> findReservableConnectors(Long stationId);
    
    /**
     * 查找可用的充电桩（空闲状态）
     * 
     * @param stationId 充电站ID，如果为null则查找所有充电站
     * @return 可用的充电桩列表
     */
    List<Connector> findAvailableConnectors(Long stationId);
    
    /**
     * 查找正在使用中的充电桩
     * 
     * @param stationId 充电站ID，如果为null则查找所有充电站
     * @return 使用中的充电桩列表
     */
    List<Connector> findInUseConnectors(Long stationId);
    
    /**
     * 根据用户ID查找预约的充电桩
     * 
     * @param userId 用户ID
     * @return 用户预约的充电桩列表
     */
    List<Connector> findByReservedByUserId(Long userId);
    
    /**
     * 查找预约超时的充电桩
     * 
     * @param currentTime 当前时间
     * @return 预约超时的充电桩列表
     */
    List<Connector> findExpiredReservations(Instant currentTime);
    
    /**
     * 查找心跳超时的充电桩
     * 
     * @param timeoutThreshold 超时阈值时间
     * @return 心跳超时的充电桩列表
     */
    List<Connector> findHeartbeatTimeoutConnectors(Instant timeoutThreshold);
    
    /**
     * 根据充电桩类型查找
     * 
     * @param connectorType 充电桩类型
     * @return 指定类型的充电桩列表
     */
    List<Connector> findByConnectorType(ConnectorInfo.ConnectorType connectorType);
    
    /**
     * 根据功率范围查找充电桩
     * 
     * @param minPower 最小功率
     * @param maxPower 最大功率
     * @return 指定功率范围的充电桩列表
     */
    List<Connector> findByPowerRange(java.math.BigDecimal minPower, java.math.BigDecimal maxPower);
    
    /**
     * 查找快充桩（功率 >= 50kW）
     * 
     * @param stationId 充电站ID，如果为null则查找所有充电站
     * @return 快充桩列表
     */
    List<Connector> findFastChargers(Long stationId);
    
    /**
     * 查找超充桩（功率 >= 150kW）
     * 
     * @param stationId 充电站ID，如果为null则查找所有充电站
     * @return 超充桩列表
     */
    List<Connector> findSuperChargers(Long stationId);
    
    /**
     * 统计充电桩总数
     * 
     * @return 充电桩总数
     */
    long count();
    
    /**
     * 根据充电站ID统计充电桩数量
     * 
     * @param stationId 充电站ID
     * @return 指定充电站的充电桩数量
     */
    long countByStationId(Long stationId);
    
    /**
     * 根据状态统计充电桩数量
     * 
     * @param status 充电桩状态
     * @return 指定状态的充电桩数量
     */
    long countByStatus(ConnectorStatus status);
    
    /**
     * 根据充电站ID和状态统计充电桩数量
     * 
     * @param stationId 充电站ID
     * @param status 充电桩状态
     * @return 指定充电站和状态的充电桩数量
     */
    long countByStationIdAndStatus(Long stationId, ConnectorStatus status);
    
    /**
     * 统计可用充电桩数量
     * 
     * @param stationId 充电站ID，如果为null则统计所有充电站
     * @return 可用充电桩数量
     */
    long countAvailableConnectors(Long stationId);
    
    /**
     * 统计使用中充电桩数量
     * 
     * @param stationId 充电站ID，如果为null则统计所有充电站
     * @return 使用中充电桩数量
     */
    long countInUseConnectors(Long stationId);
    
    /**
     * 检查充电桩是否存在
     * 
     * @param id 充电桩ID
     * @return true 如果存在
     */
    boolean existsById(Long id);
    
    /**
     * 检查指定充电站和编号的充电桩是否存在
     * 
     * @param stationId 充电站ID
     * @param connectorNumber 充电桩编号
     * @return true 如果存在
     */
    boolean existsByStationIdAndConnectorNumber(Long stationId, String connectorNumber);
    
    /**
     * 删除充电桩
     * 
     * @param connector 充电桩实体
     */
    void delete(Connector connector);
    
    /**
     * 根据ID删除充电桩
     * 
     * @param id 充电桩ID
     */
    void deleteById(Long id);
    
    /**
     * 根据充电站ID删除所有充电桩
     * 
     * @param stationId 充电站ID
     */
    void deleteByStationId(Long stationId);
    
    /**
     * 批量保存充电桩
     * 
     * @param connectors 充电桩列表
     * @return 保存后的充电桩列表
     */
    List<Connector> saveAll(List<Connector> connectors);
    
    /**
     * 批量查找充电桩
     * 
     * @param ids 充电桩ID列表
     * @return 充电桩列表
     */
    List<Connector> findAllById(List<Long> ids);
    
    /**
     * 批量更新充电桩状态
     * 
     * @param connectorIds 充电桩ID列表
     * @param newStatus 新状态
     * @return 更新的数量
     */
    int batchUpdateStatus(List<Long> connectorIds, ConnectorStatus newStatus);
    
    /**
     * 刷新充电桩实体
     * 
     * @param connector 充电桩实体
     */
    void refresh(Connector connector);
    
    /**
     * 获取充电桩统计信息
     * 
     * @param stationId 充电站ID，如果为null则统计全局
     * @return 统计信息
     */
    ConnectorStatistics getStatistics(Long stationId);
    
    /**
     * 充电桩统计信息
     */
    record ConnectorStatistics(
        Long stationId,
        long totalConnectors,
        long idleConnectors,
        long reservedConnectors,
        long occupiedConnectors,
        long chargingConnectors,
        long faultConnectors,
        long offlineConnectors,
        long maintenanceConnectors,
        long fastChargers,
        long superChargers,
        double availabilityRate,
        double utilizationRate,
        double faultRate
    ) {
        
        /**
         * 获取可用充电桩数量
         */
        public long getAvailableConnectors() {
            return idleConnectors;
        }
        
        /**
         * 获取使用中充电桩数量
         */
        public long getInUseConnectors() {
            return reservedConnectors + occupiedConnectors + chargingConnectors;
        }
        
        /**
         * 获取不可用充电桩数量
         */
        public long getUnavailableConnectors() {
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
         * 计算故障率
         */
        public double calculateFaultRate() {
            if (totalConnectors == 0) {
                return 0.0;
            }
            return (double) getUnavailableConnectors() / totalConnectors * 100;
        }
        
        /**
         * 获取统计摘要
         */
        public String getSummary() {
            return String.format(
                "总计: %d, 可用: %d, 使用中: %d, 故障: %d, 快充: %d, 超充: %d, 可用率: %.1f%%, 使用率: %.1f%%",
                totalConnectors, getAvailableConnectors(), getInUseConnectors(), 
                getUnavailableConnectors(), fastChargers, superChargers,
                availabilityRate, utilizationRate
            );
        }
    }
}
