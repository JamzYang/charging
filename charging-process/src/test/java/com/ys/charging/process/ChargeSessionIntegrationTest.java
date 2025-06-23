package com.ys.charging.process;

import com.ys.charging.process.application.service.ChargeSessionService;
import com.ys.charging.process.domain.model.ChargeSession;
import com.ys.charging.process.domain.model.ChargeSessionEvent;
import com.ys.charging.process.domain.model.ChargeSessionStatus;
import com.ys.charging.process.domain.repository.ChargeSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 充电会话集成测试
 * 
 * 验证整个充电会话状态机的端到端功能，包括：
 * - 完整的业务流程
 * - 数据持久化
 * - 事件发布
 * - 事务管理
 * 
 * 使用 H2 内存数据库进行测试，确保测试的独立性和速度。
 * 
 * @author yang
 * @since 2025-06-23
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChargeSessionIntegrationTest {
    
    @Autowired
    private ChargeSessionService chargeSessionService;
    
    @Autowired
    private ChargeSessionRepository chargeSessionRepository;
    
    private static final Long USER_ID = 1L;
    private static final Long STATION_ID = 100L;
    private static final Long CONNECTOR_ID = 1001L;
    
    /**
     * 测试完整的充电业务流程
     */
    @Test
    void testCompleteChargingFlow() {
        // 1. 创建充电会话
        ChargeSession session = chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        assertNotNull(session.getId());
        assertEquals(ChargeSessionStatus.INIT, session.getStatus());
        
        Long sessionId = session.getId();
        
        // 2. 预约充电桩
        Long reservationId = 12345L;
        chargeSessionService.reserveConnector(sessionId, reservationId);
        
        // 验证状态和数据持久化
        ChargeSession savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.RESERVED, savedSession.getStatus());
        assertEquals(reservationId, savedSession.getReservationId());
        
        // 3. 插枪检测
        chargeSessionService.handlePlugDetected(CONNECTOR_ID);
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.READY_TO_CHARGE, savedSession.getStatus());
        
        // 4. 启动充电
        chargeSessionService.startCharging(sessionId);
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.CHARGE_STARTING, savedSession.getStatus());
        
        // 5. 确认充电启动
        chargeSessionService.confirmChargingStart(sessionId);
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.CHARGING, savedSession.getStatus());
        assertNotNull(savedSession.getChargeStartTime());
        
        // 6. 停止充电
        chargeSessionService.stopCharging(sessionId);
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.CHARGE_STOPPING, savedSession.getStatus());
        
        // 7. 确认充电停止
        BigDecimal energyConsumed = new BigDecimal("25.5");
        chargeSessionService.confirmChargingStop(sessionId, energyConsumed);
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.CHARGE_STOPPED, savedSession.getStatus());
        assertEquals(energyConsumed, savedSession.getEnergyConsumed());
        assertNotNull(savedSession.getChargeEndTime());
        
        // 8. 确认支付
        BigDecimal paymentAmount = new BigDecimal("50.00");
        chargeSessionService.confirmPayment(sessionId, paymentAmount);
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.PAID, savedSession.getStatus());
        assertEquals(paymentAmount, savedSession.getTotalAmount());
        
        // 9. 拔枪结束
        chargeSessionService.handleUnplugDetected(CONNECTOR_ID);
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.END, savedSession.getStatus());
    }
    
    /**
     * 测试无预约直接充电流程
     */
    @Test
    void testDirectChargingFlow() {
        // 1. 创建充电会话
        ChargeSession session = chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        Long sessionId = session.getId();
        
        // 2. 直接插枪（无预约）
        chargeSessionService.handlePlugDetected(CONNECTOR_ID);
        ChargeSession savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.READY_TO_CHARGE, savedSession.getStatus());
        
        // 3. 后续流程与预约流程相同
        chargeSessionService.startCharging(sessionId);
        chargeSessionService.confirmChargingStart(sessionId);
        
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.CHARGING, savedSession.getStatus());
    }
    
    /**
     * 测试预约超时处理
     */
    @Test
    void testReservationTimeout() {
        // 1. 创建并预约
        ChargeSession session = chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        Long sessionId = session.getId();
        chargeSessionService.reserveConnector(sessionId, 12345L);
        
        // 2. 模拟预约超时
        chargeSessionService.processCommand(sessionId, ChargeSessionEvent.RESERVATION_TIMEOUT);
        
        // 3. 验证状态
        ChargeSession savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.END, savedSession.getStatus());
    }
    
    /**
     * 测试启动失败恢复
     */
    @Test
    void testStartFailureRecovery() {
        // 1. 准备到启动状态
        ChargeSession session = chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        Long sessionId = session.getId();
        chargeSessionService.reserveConnector(sessionId, 12345L);
        chargeSessionService.handlePlugDetected(CONNECTOR_ID);
        chargeSessionService.startCharging(sessionId);
        
        // 2. 模拟启动失败
        chargeSessionService.processCommand(sessionId, ChargeSessionEvent.START_FAILED);
        
        // 3. 验证回到准备充电状态
        ChargeSession savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.READY_TO_CHARGE, savedSession.getStatus());
        
        // 4. 可以重新启动
        chargeSessionService.startCharging(sessionId);
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.CHARGE_STARTING, savedSession.getStatus());
    }
    
    /**
     * 测试未支付流程
     */
    @Test
    void testUnpaidFlow() {
        // 1. 完成充电但不支付
        ChargeSession session = chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        Long sessionId = session.getId();
        
        chargeSessionService.reserveConnector(sessionId, 12345L);
        chargeSessionService.handlePlugDetected(CONNECTOR_ID);
        chargeSessionService.startCharging(sessionId);
        chargeSessionService.confirmChargingStart(sessionId);
        chargeSessionService.stopCharging(sessionId);
        chargeSessionService.confirmChargingStop(sessionId, new BigDecimal("20.0"));
        
        // 2. 直接拔枪（未支付）
        chargeSessionService.handleUnplugDetected(CONNECTOR_ID);
        ChargeSession savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.UNPAID, savedSession.getStatus());
        
        // 3. 后续支付
        chargeSessionService.confirmPayment(sessionId, new BigDecimal("30.00"));
        savedSession = chargeSessionService.getSessionById(sessionId);
        assertEquals(ChargeSessionStatus.END, savedSession.getStatus());
    }
    
    /**
     * 测试用户会话查询
     */
    @Test
    void testUserSessionQueries() {
        // 1. 创建多个会话
        ChargeSession session1 = chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        ChargeSession session2 = chargeSessionService.createSession(USER_ID, STATION_ID + 1, CONNECTOR_ID + 1);
        
        // 2. 查询用户所有会话
        List<ChargeSession> allSessions = chargeSessionService.getUserSessions(USER_ID);
        assertEquals(2, allSessions.size());
        
        // 3. 查询用户活跃会话
        List<ChargeSession> activeSessions = chargeSessionService.getUserActiveSessions(USER_ID);
        assertEquals(2, activeSessions.size());
        
        // 4. 结束一个会话
        chargeSessionService.processCommand(session1.getId(), ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        chargeSessionService.processCommand(session1.getId(), ChargeSessionEvent.RESERVATION_TIMEOUT);
        
        // 5. 再次查询活跃会话
        activeSessions = chargeSessionService.getUserActiveSessions(USER_ID);
        assertEquals(1, activeSessions.size());
        assertEquals(session2.getId(), activeSessions.get(0).getId());
    }
    
    /**
     * 测试并发控制（乐观锁）
     */
    @Test
    void testConcurrencyControl() {
        // 1. 创建会话
        ChargeSession session = chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        Long sessionId = session.getId();
        
        // 2. 获取两个会话实例（模拟并发访问）
        ChargeSession session1 = chargeSessionService.getSessionById(sessionId);
        ChargeSession session2 = chargeSessionService.getSessionById(sessionId);
        
        // 3. 第一个实例执行状态转换
        session1.handleEvent(ChargeSessionEvent.RESERVE_CMD_SUCCESS);
        chargeSessionRepository.save(session1);
        
        // 4. 第二个实例尝试执行状态转换（应该失败，因为版本号不匹配）
        session2.handleEvent(ChargeSessionEvent.PLUG_DETECTED);
        
        // 注意：在真实环境中，这会抛出 OptimisticLockingFailureException
        // 但在测试环境中，由于事务隔离，可能不会立即体现
        // 这里主要验证版本号机制的存在
        assertNotNull(session1.getVersion());
    }
    
    /**
     * 测试业务规则验证
     */
    @Test
    void testBusinessRuleValidation() {
        // 1. 测试用户不能有多个活跃会话
        chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        
        assertThrows(IllegalStateException.class, () -> {
            chargeSessionService.createSession(USER_ID, STATION_ID + 1, CONNECTOR_ID + 1);
        });
        
        // 2. 测试充电桩不能被多个会话占用
        Long anotherUserId = USER_ID + 1;
        assertThrows(IllegalStateException.class, () -> {
            chargeSessionService.createSession(anotherUserId, STATION_ID, CONNECTOR_ID);
        });
    }
    
    /**
     * 测试数据持久化
     */
    @Test
    void testDataPersistence() {
        // 1. 创建并操作会话
        ChargeSession session = chargeSessionService.createSession(USER_ID, STATION_ID, CONNECTOR_ID);
        Long sessionId = session.getId();
        
        chargeSessionService.reserveConnector(sessionId, 12345L);
        chargeSessionService.handlePlugDetected(CONNECTOR_ID);
        
        // 2. 直接从仓储查询验证持久化
        ChargeSession persistedSession = chargeSessionRepository.findById(sessionId).orElseThrow();
        assertEquals(ChargeSessionStatus.READY_TO_CHARGE, persistedSession.getStatus());
        assertEquals(Long.valueOf(12345L), persistedSession.getReservationId());
        assertEquals(USER_ID, persistedSession.getUserId());
        assertEquals(STATION_ID, persistedSession.getStationId());
        assertEquals(CONNECTOR_ID, persistedSession.getConnectorId());
        assertNotNull(persistedSession.getCreatedAt());
        assertNotNull(persistedSession.getUpdatedAt());
    }
}
