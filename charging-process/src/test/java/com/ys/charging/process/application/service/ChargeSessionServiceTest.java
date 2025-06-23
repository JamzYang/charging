package com.ys.charging.process.application.service;

import com.ys.charging.process.domain.model.ChargeSession;
import com.ys.charging.process.domain.model.ChargeSessionEvent;
import com.ys.charging.process.domain.model.ChargeSessionStatus;
import com.ys.charging.process.domain.repository.ChargeSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 充电会话应用服务测试
 * 
 * 验证应用服务的业务逻辑，包括：
 * - 事务管理
 * - 业务流程协调
 * - 异常处理
 * - 与仓储的交互
 * 
 * @author yang
 * @since 2025-06-23
 */
@ExtendWith(MockitoExtension.class)
class ChargeSessionServiceTest {
    
    @Mock
    private ChargeSessionRepository chargeSessionRepository;
    
    @InjectMocks
    private ChargeSessionService chargeSessionService;
    
    private ChargeSession chargeSession;
    private static final Long SESSION_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long STATION_ID = 200L;
    private static final Long CONNECTOR_ID = 300L;
    
    @BeforeEach
    void setUp() {
        chargeSession = new ChargeSession(USER_ID, STATION_ID, CONNECTOR_ID);
        // 使用反射设置ID，模拟持久化后的状态
        try {
            var idField = ChargeSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(chargeSession, SESSION_ID);
        } catch (Exception e) {
            // 忽略反射异常
        }
    }
    
    /**
     * 测试创建充电会话
     */
    @Test
    void testCreateSession() {
        // 模拟没有活跃会话
        when(chargeSessionRepository.findActiveSessionsByUserId(USER_ID)).thenReturn(List.of());
        when(chargeSessionRepository.findActiveSessionByConnectorId(CONNECTOR_ID)).thenReturn(Optional.empty());
        when(chargeSessionRepository.save(any(ChargeSession.class))).thenReturn(chargeSession);
        
        ChargeSession result = chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        
        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(STATION_ID, result.getStationId());
        assertEquals(CONNECTOR_ID, result.getConnectorId());
        assertEquals(ChargeSessionStatus.INIT, result.getStatus());
        
        verify(chargeSessionRepository).findActiveSessionsByUserId(USER_ID);
        verify(chargeSessionRepository).findActiveSessionByConnectorId(CONNECTOR_ID);
        verify(chargeSessionRepository).save(any(ChargeSession.class));
    }
    
    /**
     * 测试创建会话时用户已有活跃会话
     */
    @Test
    void testCreateSessionWithActiveSession() {
        // 模拟用户已有活跃会话
        ChargeSession activeSession = new ChargeSession(USER_ID, STATION_ID, CONNECTOR_ID + 1);
        when(chargeSessionRepository.findActiveSessionsByUserId(USER_ID)).thenReturn(List.of(activeSession));
        
        assertThrows(IllegalStateException.class, () -> {
            chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        });
        
        verify(chargeSessionRepository).findActiveSessionsByUserId(USER_ID);
        verify(chargeSessionRepository, never()).save(any());
    }
    
    /**
     * 测试创建会话时充电桩被占用
     */
    @Test
    void testCreateSessionWithOccupiedConnector() {
        // 模拟充电桩被占用
        when(chargeSessionRepository.findActiveSessionsByUserId(USER_ID)).thenReturn(List.of());
        when(chargeSessionRepository.findActiveSessionByConnectorId(CONNECTOR_ID)).thenReturn(Optional.of(chargeSession));
        
        assertThrows(IllegalStateException.class, () -> {
            chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        });
        
        verify(chargeSessionRepository).findActiveSessionsByUserId(USER_ID);
        verify(chargeSessionRepository).findActiveSessionByConnectorId(CONNECTOR_ID);
        verify(chargeSessionRepository, never()).save(any());
    }
    
    /**
     * 测试处理命令
     */
    @Test
    void testProcessCommand() {
        when(chargeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(chargeSession));
        when(chargeSessionRepository.save(chargeSession)).thenReturn(chargeSession);
        
        chargeSessionService.processCommand(SESSION_ID, ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        
        assertEquals(ChargeSessionStatus.RESERVED, chargeSession.getStatus());
        verify(chargeSessionRepository).findById(SESSION_ID);
        verify(chargeSessionRepository).save(chargeSession);
    }
    
    /**
     * 测试处理不存在的会话命令
     */
    @Test
    void testProcessCommandWithNonExistentSession() {
        when(chargeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            chargeSessionService.processCommand(SESSION_ID, ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        });
        
        verify(chargeSessionRepository).findById(SESSION_ID);
        verify(chargeSessionRepository, never()).save(any());
    }
    
    /**
     * 测试预约充电桩
     */
    @Test
    void testReserveConnector() {
        Long reservationId = 12345L;
        when(chargeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(chargeSession));
        when(chargeSessionRepository.save(chargeSession)).thenReturn(chargeSession);
        
        chargeSessionService.reserveConnector(SESSION_ID, reservationId);
        
        assertEquals(reservationId, chargeSession.getReservationId());
        assertEquals(ChargeSessionStatus.RESERVED, chargeSession.getStatus());
        verify(chargeSessionRepository, times(2)).findById(SESSION_ID); // getSessionById + processCommand
        verify(chargeSessionRepository).save(chargeSession);
    }
    
    /**
     * 测试启动充电
     */
    @Test
    void testStartCharging() {
        chargeSession.handleEvent(ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        chargeSession.handleEvent(ChargeSessionEvent.PLUG_DETECTED);
        // 准备测试状态
        
        when(chargeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(chargeSession));
        when(chargeSessionRepository.save(chargeSession)).thenReturn(chargeSession);
        
        chargeSessionService.startCharging(SESSION_ID);
        
        assertEquals(ChargeSessionStatus.CHARGE_STARTING, chargeSession.getStatus());
        verify(chargeSessionRepository).findById(SESSION_ID);
        verify(chargeSessionRepository).save(chargeSession);
    }
    
    /**
     * 测试确认充电停止
     */
    @Test
    void testConfirmChargingStop() {
        // 设置到充电状态
        chargeSession.handleEvent(ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        chargeSession.handleEvent(ChargeSessionEvent.PLUG_DETECTED);
        chargeSession.handleEvent(ChargeSessionEvent.START_REQUESTED);
        chargeSession.handleEvent(ChargeSessionEvent.START_CONFIRMED);
        // 准备测试状态
        
        BigDecimal energyConsumed = new BigDecimal("25.5");
        when(chargeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(chargeSession));
        when(chargeSessionRepository.save(chargeSession)).thenReturn(chargeSession);
        
        chargeSessionService.confirmChargingStop(SESSION_ID, energyConsumed);
        
        assertEquals(ChargeSessionStatus.CHARGE_STOPPED, chargeSession.getStatus());
        assertEquals(energyConsumed, chargeSession.getEnergyConsumed());
        verify(chargeSessionRepository, times(2)).findById(SESSION_ID);
        verify(chargeSessionRepository).save(chargeSession);
    }
    
    /**
     * 测试确认支付
     */
    @Test
    void testConfirmPayment() {
        // 设置到充电停止状态
        chargeSession.handleEvent(ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        chargeSession.handleEvent(ChargeSessionEvent.PLUG_DETECTED);
        chargeSession.handleEvent(ChargeSessionEvent.START_REQUESTED);
        chargeSession.handleEvent(ChargeSessionEvent.START_CONFIRMED);
        chargeSession.handleEvent(ChargeSessionEvent.STOP_CONFIRMED);
        // 准备测试状态
        
        BigDecimal amount = new BigDecimal("50.00");
        when(chargeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(chargeSession));
        when(chargeSessionRepository.save(chargeSession)).thenReturn(chargeSession);
        
        chargeSessionService.confirmPayment(SESSION_ID, amount);
        
        assertEquals(ChargeSessionStatus.PAID, chargeSession.getStatus());
        assertEquals(amount, chargeSession.getTotalAmount());
        verify(chargeSessionRepository).findById(SESSION_ID);
        verify(chargeSessionRepository).save(chargeSession);
    }
    
    /**
     * 测试插枪检测
     */
    @Test
    void testHandlePlugDetected() {
        when(chargeSessionRepository.findActiveSessionByConnectorId(CONNECTOR_ID)).thenReturn(Optional.of(chargeSession));
        when(chargeSessionRepository.save(chargeSession)).thenReturn(chargeSession);
        
        chargeSessionService.handlePlugDetected(CONNECTOR_ID);
        
        verify(chargeSessionRepository).findActiveSessionByConnectorId(CONNECTOR_ID);
        verify(chargeSessionRepository).save(chargeSession);
    }
    
    /**
     * 测试插枪检测但无活跃会话
     */
    @Test
    void testHandlePlugDetectedWithNoActiveSession() {
        when(chargeSessionRepository.findActiveSessionByConnectorId(CONNECTOR_ID)).thenReturn(Optional.empty());
        
        chargeSessionService.handlePlugDetected(CONNECTOR_ID);
        
        verify(chargeSessionRepository).findActiveSessionByConnectorId(CONNECTOR_ID);
        verify(chargeSessionRepository, never()).save(any());
    }
    
    /**
     * 测试获取用户会话
     */
    @Test
    void testGetUserSessions() {
        List<ChargeSession> sessions = Arrays.asList(chargeSession);
        when(chargeSessionRepository.findByUserId(USER_ID)).thenReturn(sessions);
        
        List<ChargeSession> result = chargeSessionService.getUserSessions(USER_ID);
        
        assertEquals(sessions, result);
        verify(chargeSessionRepository).findByUserId(USER_ID);
    }
    
    /**
     * 测试获取用户活跃会话
     */
    @Test
    void testGetUserActiveSessions() {
        List<ChargeSession> activeSessions = Arrays.asList(chargeSession);
        when(chargeSessionRepository.findActiveSessionsByUserId(USER_ID)).thenReturn(activeSessions);
        
        List<ChargeSession> result = chargeSessionService.getUserActiveSessions(USER_ID);
        
        assertEquals(activeSessions, result);
        verify(chargeSessionRepository).findActiveSessionsByUserId(USER_ID);
    }
    
    /**
     * 测试处理预约超时
     */
    @Test
    void testHandleReservationTimeouts() {
        int timeoutMinutes = 30;
        LocalDateTime timeoutBefore = LocalDateTime.now().minusMinutes(timeoutMinutes);
        
        // 创建超时的预约会话
        ChargeSession timeoutSession1 = new ChargeSession(USER_ID, STATION_ID, CONNECTOR_ID);
        ChargeSession timeoutSession2 = new ChargeSession(USER_ID + 1, STATION_ID, CONNECTOR_ID + 1);
        timeoutSession1.handleEvent(ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        timeoutSession2.handleEvent(ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        
        List<ChargeSession> timeoutSessions = Arrays.asList(timeoutSession1, timeoutSession2);
        when(chargeSessionRepository.findTimeoutReservations(any(LocalDateTime.class))).thenReturn(timeoutSessions);
        when(chargeSessionRepository.save(any(ChargeSession.class))).thenReturn(timeoutSession1, timeoutSession2);
        
        int result = chargeSessionService.handleReservationTimeouts(timeoutMinutes);
        
        assertEquals(2, result);
        assertEquals(ChargeSessionStatus.END, timeoutSession1.getStatus());
        assertEquals(ChargeSessionStatus.END, timeoutSession2.getStatus());
        verify(chargeSessionRepository).findTimeoutReservations(any(LocalDateTime.class));
        verify(chargeSessionRepository, times(2)).save(any(ChargeSession.class));
    }
    
    /**
     * 测试根据ID获取会话
     */
    @Test
    void testGetSessionById() {
        when(chargeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(chargeSession));
        
        ChargeSession result = chargeSessionService.getSessionById(SESSION_ID);
        
        assertEquals(chargeSession, result);
        verify(chargeSessionRepository).findById(SESSION_ID);
    }
    
    /**
     * 测试获取不存在的会话
     */
    @Test
    void testGetSessionByIdNotFound() {
        when(chargeSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            chargeSessionService.getSessionById(SESSION_ID);
        });
        
        verify(chargeSessionRepository).findById(SESSION_ID);
    }
}
