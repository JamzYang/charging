package com.ys.charging.process.domain.repository;

import com.ys.charging.process.domain.model.ChargeSession;
import com.ys.charging.process.domain.model.ChargeSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 充电会话仓储接口
 * 
 * 提供充电会话聚合根的持久化访问能力。
 * 继承 JpaRepository 获得基础的 CRUD 操作，
 * 同时定义业务相关的查询方法。
 * 
 * 查询方法设计原则：
 * - 方法名遵循 Spring Data JPA 命名约定
 * - 复杂查询使用 @Query 注解
 * - 返回类型使用 Optional 处理可能为空的情况
 * - 支持分页和排序
 * 
 * @author yang
 * @since 2025-06-23
 */
@Repository
public interface ChargeSessionRepository extends JpaRepository<ChargeSession, Long> {
    
    /**
     * 根据用户ID查找充电会话
     * 
     * @param userId 用户ID
     * @return 该用户的所有充电会话
     */
    List<ChargeSession> findByUserId(Long userId);
    
    /**
     * 根据用户ID和状态查找充电会话
     * 
     * @param userId 用户ID
     * @param status 会话状态
     * @return 符合条件的充电会话列表
     */
    List<ChargeSession> findByUserIdAndStatus(Long userId, ChargeSessionStatus status);
    
    /**
     * 查找用户当前活跃的充电会话
     * 活跃会话定义为非终态的会话
     * 
     * @param userId 用户ID
     * @return 用户当前活跃的充电会话
     */
    @Query("SELECT cs FROM ChargeSession cs WHERE cs.userId = :userId AND cs.status != 'END'")
    List<ChargeSession> findActiveSessionsByUserId(@Param("userId") Long userId);
    
    /**
     * 根据充电桩ID查找当前会话
     * 
     * @param connectorId 充电桩ID
     * @return 该充电桩当前的会话（如果存在）
     */
    @Query("SELECT cs FROM ChargeSession cs WHERE cs.connectorId = :connectorId AND cs.status != 'END'")
    Optional<ChargeSession> findActiveSessionByConnectorId(@Param("connectorId") Long connectorId);
    
    /**
     * 根据预约ID查找充电会话
     * 
     * @param reservationId 预约ID
     * @return 对应的充电会话
     */
    Optional<ChargeSession> findByReservationId(Long reservationId);
    
    /**
     * 查找指定时间范围内的充电会话
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时间范围内的充电会话列表
     */
    @Query("SELECT cs FROM ChargeSession cs WHERE cs.createdAt BETWEEN :startTime AND :endTime")
    List<ChargeSession> findByCreatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找指定状态的会话数量
     * 
     * @param status 会话状态
     * @return 该状态的会话数量
     */
    long countByStatus(ChargeSessionStatus status);
    
    /**
     * 查找超时的预约会话
     * 用于定时任务清理超时预约
     * 
     * @param timeoutBefore 超时时间点
     * @return 超时的预约会话列表
     */
    @Query("SELECT cs FROM ChargeSession cs WHERE cs.status = 'RESERVED' AND cs.createdAt < :timeoutBefore")
    List<ChargeSession> findTimeoutReservations(@Param("timeoutBefore") LocalDateTime timeoutBefore);
    
    /**
     * 查找未支付的会话
     * 用于定时任务处理未支付订单
     * 
     * @return 未支付的会话列表
     */
    List<ChargeSession> findByStatus(ChargeSessionStatus status);
    
    /**
     * 根据充电站ID统计各状态会话数量
     * 
     * @param stationId 充电站ID
     * @return 状态统计结果
     */
    @Query("SELECT cs.status, COUNT(cs) FROM ChargeSession cs WHERE cs.stationId = :stationId GROUP BY cs.status")
    List<Object[]> countByStationIdGroupByStatus(@Param("stationId") Long stationId);
}
