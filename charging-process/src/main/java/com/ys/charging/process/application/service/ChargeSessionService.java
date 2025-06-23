package com.ys.charging.process.application.service;

import com.ys.charging.process.domain.model.ChargeSession;
import com.ys.charging.process.domain.model.ChargeSessionEvent;
import com.ys.charging.process.domain.model.ChargeSessionStatus;
import com.ys.charging.process.domain.repository.ChargeSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 充电会话应用服务
 * 
 * 作为应用层的核心服务，负责：
 * - 协调聚合根的业务操作
 * - 管理事务边界
 * - 处理跨聚合的业务逻辑
 * - 提供应用层的业务接口
 * 
 * 设计原则：
 * - 每个公共方法都是一个事务边界
 * - 不包含业务规则，业务规则在聚合根中
 * - 负责加载和保存聚合根
 * - 处理应用层的异常和验证
 * 
 * @author yang
 * @since 2025-06-23
 */
@Service
@Transactional
public class ChargeSessionService {
    
    private final ChargeSessionRepository chargeSessionRepository;
    
    @Autowired
    public ChargeSessionService(ChargeSessionRepository chargeSessionRepository) {
        this.chargeSessionRepository = chargeSessionRepository;
    }
    
    /**
     * 处理充电会话命令
     * 
     * 这是状态机的主要入口点，负责：
     * 1. 加载聚合根
     * 2. 调用聚合根的业务方法
     * 3. 保存聚合根（触发事件发布）
     * 
     * @param sessionId 会话ID
     * @param event 触发事件
     * @param context 上下文参数
     * @throws IllegalArgumentException 如果会话不存在
     */
    public void processCommand(Long sessionId, ChargeSessionEvent event, Object... context) {
        ChargeSession session = chargeSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("充电会话不存在: " + sessionId));
        
        session.handleEvent(event, context);
        chargeSessionRepository.save(session);
    }
    
    /**
     * 创建新的充电会话
     * 
     * @param userId 用户ID
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     * @return 创建的充电会话
     */
    public ChargeSession createSession(Long userId, Long stationId, Long connectorId) {
        // 检查用户是否有活跃会话
        List<ChargeSession> activeSessions = chargeSessionRepository.findActiveSessionsByUserId(userId);
        if (!activeSessions.isEmpty()) {
            throw new IllegalStateException("用户已有活跃的充电会话");
        }
        
        // 检查充电桩是否被占用
        Optional<ChargeSession> existingSession = chargeSessionRepository.findActiveSessionByConnectorId(connectorId);
        if (existingSession.isPresent()) {
            throw new IllegalStateException("充电桩已被占用");
        }
        
        ChargeSession session = new ChargeSession(userId, stationId, connectorId);
        return chargeSessionRepository.save(session);
    }
    
    /**
     * 预约充电桩
     * 
     * @param sessionId 会话ID
     * @param reservationId 预约ID
     */
    public void reserveConnector(Long sessionId, Long reservationId) {
        ChargeSession session = getSessionById(sessionId);
        session.setReservationId(reservationId);
        session.handleEvent(ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        chargeSessionRepository.save(session);
    }
    
    /**
     * 启动充电
     * 
     * @param sessionId 会话ID
     */
    public void startCharging(Long sessionId) {
        processCommand(sessionId, ChargeSessionEvent.START_REQUESTED);
    }
    
    /**
     * 确认充电启动
     * 
     * @param sessionId 会话ID
     */
    public void confirmChargingStart(Long sessionId) {
        processCommand(sessionId, ChargeSessionEvent.START_CONFIRMED);
    }
    
    /**
     * 停止充电
     * 
     * @param sessionId 会话ID
     */
    public void stopCharging(Long sessionId) {
        processCommand(sessionId, ChargeSessionEvent.STOP_REQUESTED);
    }
    
    /**
     * 确认充电停止
     * 
     * @param sessionId 会话ID
     * @param energyConsumed 消耗电量
     */
    public void confirmChargingStop(Long sessionId, BigDecimal energyConsumed) {
        ChargeSession session = getSessionById(sessionId);
        session.setEnergyConsumed(energyConsumed);
        session.handleEvent(ChargeSessionEvent.STOP_CONFIRMED);
        chargeSessionRepository.save(session);
    }
    
    /**
     * 确认支付
     * 
     * @param sessionId 会话ID
     * @param amount 支付金额
     */
    public void confirmPayment(Long sessionId, BigDecimal amount) {
        processCommand(sessionId, ChargeSessionEvent.PAYMENT_CONFIRMED, amount);
    }
    
    /**
     * 插枪检测
     * 
     * @param connectorId 充电桩ID
     */
    public void handlePlugDetected(Long connectorId) {
        Optional<ChargeSession> sessionOpt = chargeSessionRepository.findActiveSessionByConnectorId(connectorId);
        if (sessionOpt.isPresent()) {
            ChargeSession session = sessionOpt.get();
            session.handleEvent(ChargeSessionEvent.PLUG_DETECTED);
            chargeSessionRepository.save(session);
        }
    }
    
    /**
     * 拔枪检测
     * 
     * @param connectorId 充电桩ID
     */
    public void handleUnplugDetected(Long connectorId) {
        Optional<ChargeSession> sessionOpt = chargeSessionRepository.findActiveSessionByConnectorId(connectorId);
        if (sessionOpt.isPresent()) {
            ChargeSession session = sessionOpt.get();
            session.handleEvent(ChargeSessionEvent.UNPLUG_DETECTED);
            chargeSessionRepository.save(session);
        }
    }
    
    /**
     * 根据ID获取充电会话
     * 
     * @param sessionId 会话ID
     * @return 充电会话
     */
    @Transactional(readOnly = true)
    public ChargeSession getSessionById(Long sessionId) {
        return chargeSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("充电会话不存在: " + sessionId));
    }
    
    /**
     * 获取用户的充电会话列表
     * 
     * @param userId 用户ID
     * @return 充电会话列表
     */
    @Transactional(readOnly = true)
    public List<ChargeSession> getUserSessions(Long userId) {
        return chargeSessionRepository.findByUserId(userId);
    }
    
    /**
     * 获取用户活跃的充电会话
     * 
     * @param userId 用户ID
     * @return 活跃的充电会话列表
     */
    @Transactional(readOnly = true)
    public List<ChargeSession> getUserActiveSessions(Long userId) {
        return chargeSessionRepository.findActiveSessionsByUserId(userId);
    }
    
    /**
     * 处理预约超时
     * 
     * @param timeoutMinutes 超时分钟数
     * @return 处理的超时会话数量
     */
    public int handleReservationTimeouts(int timeoutMinutes) {
        LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<ChargeSession> timeoutSessions = chargeSessionRepository.findTimeoutReservations(timeoutBefore);
        
        for (ChargeSession session : timeoutSessions) {
            session.handleEvent(ChargeSessionEvent.RESERVATION_TIMEOUT);
            chargeSessionRepository.save(session);
        }
        
        return timeoutSessions.size();
    }
}
