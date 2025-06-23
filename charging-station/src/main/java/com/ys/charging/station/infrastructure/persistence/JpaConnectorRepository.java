package com.ys.charging.station.infrastructure.persistence;

import com.ys.charging.station.domain.model.Connector;
import com.ys.charging.station.domain.model.ConnectorStatus;
import com.ys.charging.station.domain.model.ConnectorInfo;
import com.ys.charging.station.domain.repository.ConnectorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA 充电桩仓储实现
 * 
 * 基于 Spring Data JPA 实现充电桩的数据访问操作。
 * 提供复杂的状态查询和统计功能。
 * 
 * @author yang
 * @since 2025-06-23
 */
@Repository
public class JpaConnectorRepository implements ConnectorRepository {
    
    private final ConnectorJpaRepository jpaRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public JpaConnectorRepository(ConnectorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Connector save(Connector connector) {
        return jpaRepository.save(connector);
    }
    
    @Override
    public Optional<Connector> findById(Long id) {
        return jpaRepository.findById(id);
    }
    
    @Override
    public List<Connector> findByStationId(Long stationId) {
        return jpaRepository.findByStationId(stationId);
    }
    
    @Override
    public List<Connector> findByStationIdAndStatus(Long stationId, ConnectorStatus status) {
        return jpaRepository.findByStationIdAndStatus(stationId, status);
    }
    
    @Override
    public List<Connector> findByStatus(ConnectorStatus status) {
        return jpaRepository.findByStatus(status);
    }
    
    @Override
    public Page<Connector> findByStatus(ConnectorStatus status, Pageable pageable) {
        return jpaRepository.findByStatus(status, pageable);
    }
    
    @Override
    public List<Connector> findReservableConnectors(Long stationId) {
        if (stationId != null) {
            return jpaRepository.findByStationIdAndStatus(stationId, ConnectorStatus.IDLE);
        } else {
            return jpaRepository.findByStatus(ConnectorStatus.IDLE);
        }
    }
    
    @Override
    public List<Connector> findAvailableConnectors(Long stationId) {
        return findReservableConnectors(stationId);
    }
    
    @Override
    public List<Connector> findInUseConnectors(Long stationId) {
        return jpaRepository.findInUseConnectors(stationId);
    }
    
    @Override
    public List<Connector> findByReservedByUserId(Long userId) {
        return jpaRepository.findByReservedByUserId(userId);
    }
    
    @Override
    public List<Connector> findExpiredReservations(Instant currentTime) {
        return jpaRepository.findExpiredReservations(currentTime);
    }
    
    @Override
    public List<Connector> findHeartbeatTimeoutConnectors(Instant timeoutThreshold) {
        return jpaRepository.findHeartbeatTimeoutConnectors(timeoutThreshold);
    }
    
    @Override
    public List<Connector> findByConnectorType(ConnectorInfo.ConnectorType connectorType) {
        return jpaRepository.findByConnectorInfoConnectorType(connectorType);
    }
    
    @Override
    public List<Connector> findByPowerRange(BigDecimal minPower, BigDecimal maxPower) {
        return jpaRepository.findByPowerRange(minPower, maxPower);
    }
    
    @Override
    public List<Connector> findFastChargers(Long stationId) {
        return jpaRepository.findFastChargers(stationId);
    }
    
    @Override
    public List<Connector> findSuperChargers(Long stationId) {
        return jpaRepository.findSuperChargers(stationId);
    }
    
    @Override
    public long count() {
        return jpaRepository.count();
    }
    
    @Override
    public long countByStationId(Long stationId) {
        return jpaRepository.countByStationId(stationId);
    }
    
    @Override
    public long countByStatus(ConnectorStatus status) {
        return jpaRepository.countByStatus(status);
    }
    
    @Override
    public long countByStationIdAndStatus(Long stationId, ConnectorStatus status) {
        return jpaRepository.countByStationIdAndStatus(stationId, status);
    }
    
    @Override
    public long countAvailableConnectors(Long stationId) {
        if (stationId != null) {
            return jpaRepository.countByStationIdAndStatus(stationId, ConnectorStatus.IDLE);
        } else {
            return jpaRepository.countByStatus(ConnectorStatus.IDLE);
        }
    }
    
    @Override
    public long countInUseConnectors(Long stationId) {
        return jpaRepository.countInUseConnectors(stationId);
    }
    
    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
    
    @Override
    public boolean existsByStationIdAndConnectorNumber(Long stationId, String connectorNumber) {
        return jpaRepository.existsByStationIdAndConnectorInfoConnectorNumber(stationId, connectorNumber);
    }
    
    @Override
    public void delete(Connector connector) {
        jpaRepository.delete(connector);
    }
    
    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
    
    @Override
    public void deleteByStationId(Long stationId) {
        jpaRepository.deleteByStationId(stationId);
    }
    
    @Override
    public List<Connector> saveAll(List<Connector> connectors) {
        return jpaRepository.saveAll(connectors);
    }
    
    @Override
    public List<Connector> findAllById(List<Long> ids) {
        return jpaRepository.findAllById(ids);
    }
    
    @Override
    @Transactional
    public int batchUpdateStatus(List<Long> connectorIds, ConnectorStatus newStatus) {
        return jpaRepository.batchUpdateStatus(connectorIds, newStatus);
    }
    
    @Override
    public void refresh(Connector connector) {
        entityManager.refresh(connector);
    }
    
    @Override
    public ConnectorStatistics getStatistics(Long stationId) {
        return jpaRepository.getConnectorStatistics(stationId);
    }
    
    /**
     * Spring Data JPA 仓储接口
     */
    interface ConnectorJpaRepository extends JpaRepository<Connector, Long> {
        
        /**
         * 根据充电站ID查找充电桩
         */
        List<Connector> findByStationId(Long stationId);
        
        /**
         * 根据充电站ID和状态查找充电桩
         */
        List<Connector> findByStationIdAndStatus(Long stationId, ConnectorStatus status);
        
        /**
         * 根据状态查找充电桩
         */
        List<Connector> findByStatus(ConnectorStatus status);
        
        /**
         * 根据状态分页查找充电桩
         */
        Page<Connector> findByStatus(ConnectorStatus status, Pageable pageable);
        
        /**
         * 根据预约用户ID查找充电桩
         */
        List<Connector> findByReservedByUserId(Long userId);
        
        /**
         * 根据充电桩类型查找
         */
        List<Connector> findByConnectorInfoConnectorType(ConnectorInfo.ConnectorType connectorType);
        
        /**
         * 根据充电站ID统计数量
         */
        long countByStationId(Long stationId);
        
        /**
         * 根据状态统计数量
         */
        long countByStatus(ConnectorStatus status);
        
        /**
         * 根据充电站ID和状态统计数量
         */
        long countByStationIdAndStatus(Long stationId, ConnectorStatus status);
        
        /**
         * 检查充电站和编号是否存在
         */
        boolean existsByStationIdAndConnectorInfoConnectorNumber(Long stationId, String connectorNumber);
        
        /**
         * 根据充电站ID删除充电桩
         */
        void deleteByStationId(Long stationId);
        
        /**
         * 查找使用中的充电桩
         */
        @Query("SELECT c FROM Connector c WHERE " +
               "(:stationId IS NULL OR c.stationId = :stationId) AND " +
               "c.status IN ('RESERVED', 'OCCUPIED', 'CHARGING')")
        List<Connector> findInUseConnectors(@Param("stationId") Long stationId);
        
        /**
         * 查找预约超时的充电桩
         */
        @Query("SELECT c FROM Connector c WHERE " +
               "c.status = 'RESERVED' AND c.reservationExpiresAt < :currentTime")
        List<Connector> findExpiredReservations(@Param("currentTime") Instant currentTime);
        
        /**
         * 查找心跳超时的充电桩
         */
        @Query("SELECT c FROM Connector c WHERE " +
               "c.status != 'OFFLINE' AND " +
               "(c.lastHeartbeatAt IS NULL OR c.lastHeartbeatAt < :timeoutThreshold)")
        List<Connector> findHeartbeatTimeoutConnectors(@Param("timeoutThreshold") Instant timeoutThreshold);
        
        /**
         * 根据功率范围查找充电桩
         */
        @Query("SELECT c FROM Connector c WHERE " +
               "c.connectorInfo.maxPower BETWEEN :minPower AND :maxPower")
        List<Connector> findByPowerRange(@Param("minPower") BigDecimal minPower, 
                                        @Param("maxPower") BigDecimal maxPower);
        
        /**
         * 查找快充桩
         */
        @Query("SELECT c FROM Connector c WHERE " +
               "(:stationId IS NULL OR c.stationId = :stationId) AND " +
               "c.connectorInfo.maxPower >= 50")
        List<Connector> findFastChargers(@Param("stationId") Long stationId);
        
        /**
         * 查找超充桩
         */
        @Query("SELECT c FROM Connector c WHERE " +
               "(:stationId IS NULL OR c.stationId = :stationId) AND " +
               "c.connectorInfo.maxPower >= 150")
        List<Connector> findSuperChargers(@Param("stationId") Long stationId);
        
        /**
         * 统计使用中充电桩数量
         */
        @Query("SELECT COUNT(c) FROM Connector c WHERE " +
               "(:stationId IS NULL OR c.stationId = :stationId) AND " +
               "c.status IN ('RESERVED', 'OCCUPIED', 'CHARGING')")
        long countInUseConnectors(@Param("stationId") Long stationId);
        
        /**
         * 批量更新充电桩状态
         */
        @Modifying
        @Query("UPDATE Connector c SET c.status = :newStatus WHERE c.id IN :connectorIds")
        int batchUpdateStatus(@Param("connectorIds") List<Long> connectorIds, 
                             @Param("newStatus") ConnectorStatus newStatus);
        
        /**
         * 获取充电桩统计信息
         */
        @Query("SELECT new com.ys.charging.station.domain.repository.ConnectorRepository$ConnectorStatistics(" +
               ":stationId, " +
               "COUNT(c), " +
               "SUM(CASE WHEN c.status = 'IDLE' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN c.status = 'RESERVED' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN c.status = 'OCCUPIED' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN c.status = 'CHARGING' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN c.status = 'FAULT' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN c.status = 'OFFLINE' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN c.status = 'MAINTENANCE' THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN c.connectorInfo.maxPower >= 50 THEN 1 ELSE 0 END), " +
               "SUM(CASE WHEN c.connectorInfo.maxPower >= 150 THEN 1 ELSE 0 END), " +
               "CASE WHEN COUNT(c) > 0 THEN " +
               "CAST(SUM(CASE WHEN c.status NOT IN ('FAULT', 'OFFLINE', 'MAINTENANCE') THEN 1 ELSE 0 END) AS double) / COUNT(c) * 100 " +
               "ELSE 0.0 END, " +
               "CASE WHEN COUNT(c) > 0 THEN " +
               "CAST(SUM(CASE WHEN c.status IN ('RESERVED', 'OCCUPIED', 'CHARGING') THEN 1 ELSE 0 END) AS double) / COUNT(c) * 100 " +
               "ELSE 0.0 END, " +
               "CASE WHEN COUNT(c) > 0 THEN " +
               "CAST(SUM(CASE WHEN c.status IN ('FAULT', 'OFFLINE', 'MAINTENANCE') THEN 1 ELSE 0 END) AS double) / COUNT(c) * 100 " +
               "ELSE 0.0 END" +
               ") FROM Connector c WHERE (:stationId IS NULL OR c.stationId = :stationId)")
        ConnectorStatistics getConnectorStatistics(@Param("stationId") Long stationId);
    }
}
