package com.ys.charging.process.domain.service;

import com.ys.charging.process.domain.model.ChargeSessionEvent;
import com.ys.charging.process.domain.model.ChargeSessionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态转换表测试
 * 
 * 验证状态转换表的正确性，包括：
 * - 所有有效的状态转换
 * - 无效的状态转换
 * - 边界条件
 * - 业务规则一致性
 * 
 * @author yang
 * @since 2025-06-23
 */
class TransitionTableTest {
    
    /**
     * 测试有效的状态转换
     */
    @ParameterizedTest
    @MethodSource("validTransitions")
    void testValidTransitions(ChargeSessionStatus currentStatus, ChargeSessionEvent event, ChargeSessionStatus expectedStatus) {
        Optional<ChargeSessionStatus> result = TransitionTable.getNext(currentStatus, event);
        
        assertTrue(result.isPresent(), 
            String.format("转换应该有效: %s + %s", currentStatus, event));
        assertEquals(expectedStatus, result.get(),
            String.format("转换结果不正确: %s + %s 应该得到 %s", currentStatus, event, expectedStatus));
    }
    
    /**
     * 测试无效的状态转换
     */
    @ParameterizedTest
    @MethodSource("invalidTransitions")
    void testInvalidTransitions(ChargeSessionStatus currentStatus, ChargeSessionEvent event) {
        Optional<ChargeSessionStatus> result = TransitionTable.getNext(currentStatus, event);
        
        assertFalse(result.isPresent(),
            String.format("转换应该无效: %s + %s", currentStatus, event));
    }
    
    /**
     * 测试状态转换验证方法
     */
    @Test
    void testIsValidTransition() {
        // 有效转换
        assertTrue(TransitionTable.isValidTransition(ChargeSessionStatus.INIT, ChargeSessionEvent.RESERVE_CMD_SUCCESS));
        assertTrue(TransitionTable.isValidTransition(ChargeSessionStatus.READY_TO_CHARGE, ChargeSessionEvent.START_REQUESTED));
        
        // 无效转换
        assertFalse(TransitionTable.isValidTransition(ChargeSessionStatus.INIT, ChargeSessionEvent.STOP_CONFIRMED));
        assertFalse(TransitionTable.isValidTransition(ChargeSessionStatus.END, ChargeSessionEvent.START_REQUESTED));
    }
    
    /**
     * 测试获取可能的转换
     */
    @Test
    void testGetPossibleTransitions() {
        var transitions = TransitionTable.getPossibleTransitions(ChargeSessionStatus.INIT);
        assertEquals(2, transitions.size());
        assertTrue(transitions.containsKey(ChargeSessionEvent.RESERVE_CMD_SUCCESS));
        assertTrue(transitions.containsKey(ChargeSessionEvent.PLUG_DETECTED));
        
        // 终态应该没有转换
        var endTransitions = TransitionTable.getPossibleTransitions(ChargeSessionStatus.END);
        assertTrue(endTransitions.isEmpty());
    }
    
    /**
     * 测试典型的业务流程
     */
    @Test
    void testTypicalBusinessFlow() {
        // 正常预约流程
        ChargeSessionStatus status = ChargeSessionStatus.INIT;
        
        // 预约成功
        status = TransitionTable.getNext(status, ChargeSessionEvent.RESERVE_CMD_SUCCESS).orElseThrow();
        assertEquals(ChargeSessionStatus.RESERVED, status);
        
        // 插枪
        status = TransitionTable.getNext(status, ChargeSessionEvent.PLUG_DETECTED).orElseThrow();
        assertEquals(ChargeSessionStatus.READY_TO_CHARGE, status);
        
        // 启动充电
        status = TransitionTable.getNext(status, ChargeSessionEvent.START_REQUESTED).orElseThrow();
        assertEquals(ChargeSessionStatus.CHARGE_STARTING, status);
        
        // 确认启动
        status = TransitionTable.getNext(status, ChargeSessionEvent.START_CONFIRMED).orElseThrow();
        assertEquals(ChargeSessionStatus.CHARGING, status);
        
        // 停止充电
        status = TransitionTable.getNext(status, ChargeSessionEvent.STOP_CONFIRMED).orElseThrow();
        assertEquals(ChargeSessionStatus.CHARGE_STOPPED, status);
        
        // 支付
        status = TransitionTable.getNext(status, ChargeSessionEvent.PAYMENT_CONFIRMED).orElseThrow();
        assertEquals(ChargeSessionStatus.PAID, status);
        
        // 拔枪结束
        status = TransitionTable.getNext(status, ChargeSessionEvent.UNPLUG_DETECTED).orElseThrow();
        assertEquals(ChargeSessionStatus.END, status);
    }
    
    /**
     * 测试无预约直接充电流程
     */
    @Test
    void testDirectChargingFlow() {
        ChargeSessionStatus status = ChargeSessionStatus.INIT;
        
        // 直接插枪
        status = TransitionTable.getNext(status, ChargeSessionEvent.PLUG_DETECTED).orElseThrow();
        assertEquals(ChargeSessionStatus.READY_TO_CHARGE, status);
        
        // 后续流程与预约流程相同
        status = TransitionTable.getNext(status, ChargeSessionEvent.START_REQUESTED).orElseThrow();
        assertEquals(ChargeSessionStatus.CHARGE_STARTING, status);
    }
    
    /**
     * 测试异常流程
     */
    @Test
    void testExceptionFlows() {
        // 预约超时
        ChargeSessionStatus status = ChargeSessionStatus.RESERVED;
        status = TransitionTable.getNext(status, ChargeSessionEvent.RESERVATION_TIMEOUT).orElseThrow();
        assertEquals(ChargeSessionStatus.END, status);
        
        // 取消预约
        status = ChargeSessionStatus.RESERVED;
        status = TransitionTable.getNext(status, ChargeSessionEvent.CANCEL_RESERVATION).orElseThrow();
        assertEquals(ChargeSessionStatus.END, status);
        
        // 启动失败
        status = ChargeSessionStatus.CHARGE_STARTING;
        status = TransitionTable.getNext(status, ChargeSessionEvent.START_FAILED).orElseThrow();
        assertEquals(ChargeSessionStatus.READY_TO_CHARGE, status);
        
        // 未支付结束
        status = ChargeSessionStatus.CHARGE_STOPPED;
        status = TransitionTable.getNext(status, ChargeSessionEvent.UNPLUG_DETECTED).orElseThrow();
        assertEquals(ChargeSessionStatus.UNPAID, status);
    }
    
    /**
     * 提供有效转换的测试数据
     */
    static Stream<Arguments> validTransitions() {
        return Stream.of(
            // INIT 状态的转换
            Arguments.of(ChargeSessionStatus.INIT, ChargeSessionEvent.RESERVE_CMD_SUCCESS, ChargeSessionStatus.RESERVED),
            Arguments.of(ChargeSessionStatus.INIT, ChargeSessionEvent.PLUG_DETECTED, ChargeSessionStatus.READY_TO_CHARGE),
            
            // RESERVED 状态的转换
            Arguments.of(ChargeSessionStatus.RESERVED, ChargeSessionEvent.LOCK_READY, ChargeSessionStatus.LOCK_READY),
            Arguments.of(ChargeSessionStatus.RESERVED, ChargeSessionEvent.PLUG_DETECTED, ChargeSessionStatus.READY_TO_CHARGE),
            Arguments.of(ChargeSessionStatus.RESERVED, ChargeSessionEvent.RESERVATION_TIMEOUT, ChargeSessionStatus.END),
            Arguments.of(ChargeSessionStatus.RESERVED, ChargeSessionEvent.CANCEL_RESERVATION, ChargeSessionStatus.END),
            
            // LOCK_READY 状态的转换
            Arguments.of(ChargeSessionStatus.LOCK_READY, ChargeSessionEvent.PLUG_DETECTED, ChargeSessionStatus.READY_TO_CHARGE),
            Arguments.of(ChargeSessionStatus.LOCK_READY, ChargeSessionEvent.RESERVATION_TIMEOUT, ChargeSessionStatus.END),
            Arguments.of(ChargeSessionStatus.LOCK_READY, ChargeSessionEvent.CANCEL_RESERVATION, ChargeSessionStatus.END),
            
            // READY_TO_CHARGE 状态的转换
            Arguments.of(ChargeSessionStatus.READY_TO_CHARGE, ChargeSessionEvent.START_REQUESTED, ChargeSessionStatus.CHARGE_STARTING),
            Arguments.of(ChargeSessionStatus.READY_TO_CHARGE, ChargeSessionEvent.UNPLUG_DETECTED, ChargeSessionStatus.INIT),
            
            // CHARGE_STARTING 状态的转换
            Arguments.of(ChargeSessionStatus.CHARGE_STARTING, ChargeSessionEvent.START_CONFIRMED, ChargeSessionStatus.CHARGING),
            Arguments.of(ChargeSessionStatus.CHARGE_STARTING, ChargeSessionEvent.START_FAILED, ChargeSessionStatus.READY_TO_CHARGE),
            
            // CHARGING 状态的转换
            Arguments.of(ChargeSessionStatus.CHARGING, ChargeSessionEvent.STOP_REQUESTED, ChargeSessionStatus.CHARGE_STOPPING),
            Arguments.of(ChargeSessionStatus.CHARGING, ChargeSessionEvent.STOP_CONFIRMED, ChargeSessionStatus.CHARGE_STOPPED),
            
            // CHARGE_STOPPING 状态的转换
            Arguments.of(ChargeSessionStatus.CHARGE_STOPPING, ChargeSessionEvent.STOP_CONFIRMED, ChargeSessionStatus.CHARGE_STOPPED),
            
            // CHARGE_STOPPED 状态的转换
            Arguments.of(ChargeSessionStatus.CHARGE_STOPPED, ChargeSessionEvent.PAYMENT_CONFIRMED, ChargeSessionStatus.PAID),
            Arguments.of(ChargeSessionStatus.CHARGE_STOPPED, ChargeSessionEvent.UNPLUG_DETECTED, ChargeSessionStatus.UNPAID),
            Arguments.of(ChargeSessionStatus.CHARGE_STOPPED, ChargeSessionEvent.PAYMENT_TIMEOUT, ChargeSessionStatus.UNPAID),
            
            // PAID 状态的转换
            Arguments.of(ChargeSessionStatus.PAID, ChargeSessionEvent.UNPLUG_DETECTED, ChargeSessionStatus.END),
            
            // UNPAID 状态的转换
            Arguments.of(ChargeSessionStatus.UNPAID, ChargeSessionEvent.PAYMENT_CONFIRMED, ChargeSessionStatus.END)
        );
    }
    
    /**
     * 提供无效转换的测试数据
     */
    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
            // 从终态不能转换
            Arguments.of(ChargeSessionStatus.END, ChargeSessionEvent.START_REQUESTED),
            Arguments.of(ChargeSessionStatus.END, ChargeSessionEvent.PLUG_DETECTED),
            
            // 不合理的转换
            Arguments.of(ChargeSessionStatus.INIT, ChargeSessionEvent.STOP_CONFIRMED),
            Arguments.of(ChargeSessionStatus.INIT, ChargeSessionEvent.PAYMENT_CONFIRMED),
            Arguments.of(ChargeSessionStatus.CHARGING, ChargeSessionEvent.RESERVE_CMD_SUCCESS),
            Arguments.of(ChargeSessionStatus.PAID, ChargeSessionEvent.START_REQUESTED)
        );
    }
}
