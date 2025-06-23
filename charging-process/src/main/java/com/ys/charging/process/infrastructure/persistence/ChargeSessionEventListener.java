package com.ys.charging.process.infrastructure.persistence;

import com.ys.charging.process.domain.event.ChargeSessionStatusChangedEvent;
import com.ys.charging.process.domain.event.PaymentConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * 充电会话事件监听器
 * 
 * 负责处理充电会话相关的领域事件，执行副作用操作。
 * 使用 @TransactionalEventListener 确保事件处理与主事务的一致性。
 * 
 * 副作用操作包括：
 * - 发送通知给用户
 * - 调用外部服务
 * - 更新统计数据
 * - 记录审计日志
 * - 触发其他业务流程
 * 
 * 设计原则：
 * - 事件处理应该是幂等的
 * - 避免在事件处理中抛出异常
 * - 复杂的副作用可以异步处理
 * - 保持事件处理的简单性
 * 
 * @author yang
 * @since 2025-06-23
 */
@Component
public class ChargeSessionEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(ChargeSessionEventListener.class);
    
    /**
     * 处理充电会话状态变更事件
     * 
     * 在事务提交后执行，确保主业务已经成功持久化。
     * 根据不同的状态变更执行相应的副作用操作。
     * 
     * @param event 状态变更事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(ChargeSessionStatusChangedEvent event) {
        logger.info("处理充电会话状态变更事件: {}", event.getDescription());
        
        try {
            switch (event.newStatus()) {
                case RESERVED -> handleReservationConfirmed(event);
                case READY_TO_CHARGE -> handleReadyToCharge(event);
                case CHARGING -> handleChargingStarted(event);
                case CHARGE_STOPPED -> handleChargingStopped(event);
                case PAID -> handlePaymentCompleted(event);
                case END -> handleSessionCompleted(event);
                default -> logger.debug("状态 {} 无需特殊处理", event.newStatus());
            }
        } catch (Exception e) {
            logger.error("处理状态变更事件失败: {}", event, e);
            // 注意：这里不重新抛出异常，避免影响主事务
            // 可以考虑将失败的事件放入死信队列或重试机制
        }
    }
    
    /**
     * 处理支付确认事件
     * 
     * @param event 支付确认事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        logger.info("处理支付确认事件: {}", event.getDescription());
        
        try {
            // 发送支付成功通知
            sendPaymentSuccessNotification(event);
            
            // 更新用户账户信息
            updateUserAccountInfo(event);
            
            // 生成发票（如果需要）
            generateInvoiceIfNeeded(event);
            
            // 更新统计数据
            updatePaymentStatistics(event);
            
        } catch (Exception e) {
            logger.error("处理支付确认事件失败: {}", event, e);
        }
    }
    
    /**
     * 处理预约确认
     */
    private void handleReservationConfirmed(ChargeSessionStatusChangedEvent event) {
        logger.info("处理预约确认: 会话ID={}", event.sessionId());
        
        // 发送预约成功通知
        sendReservationNotification(event.sessionId(), "预约成功");
        
        // 启动预约超时监控
        scheduleReservationTimeout(event.sessionId());
    }
    
    /**
     * 处理准备充电状态
     */
    private void handleReadyToCharge(ChargeSessionStatusChangedEvent event) {
        logger.info("处理准备充电状态: 会话ID={}", event.sessionId());
        
        // 通知用户可以开始充电
        sendChargingReadyNotification(event.sessionId());
        
        // 更新充电桩状态
        updateConnectorStatus(event.sessionId(), "OCCUPIED");
    }
    
    /**
     * 处理充电开始
     */
    private void handleChargingStarted(ChargeSessionStatusChangedEvent event) {
        logger.info("处理充电开始: 会话ID={}", event.sessionId());
        
        // 发送充电开始通知
        sendChargingStartNotification(event.sessionId());
        
        // 开始实时数据监控
        startRealTimeMonitoring(event.sessionId());
        
        // 更新统计数据
        updateChargingStatistics(event.sessionId(), "START");
    }
    
    /**
     * 处理充电停止
     */
    private void handleChargingStopped(ChargeSessionStatusChangedEvent event) {
        logger.info("处理充电停止: 会话ID={}", event.sessionId());
        
        // 发送充电完成通知
        sendChargingCompletedNotification(event.sessionId());
        
        // 停止实时数据监控
        stopRealTimeMonitoring(event.sessionId());
        
        // 计算充电费用
        calculateChargingFee(event.sessionId());
        
        // 更新统计数据
        updateChargingStatistics(event.sessionId(), "STOP");
    }
    
    /**
     * 处理支付完成
     */
    private void handlePaymentCompleted(ChargeSessionStatusChangedEvent event) {
        logger.info("处理支付完成: 会话ID={}", event.sessionId());
        
        // 发送支付成功通知
        sendPaymentSuccessNotification(event.sessionId());
        
        // 释放充电桩
        releaseConnector(event.sessionId());
    }
    
    /**
     * 处理会话完成
     */
    private void handleSessionCompleted(ChargeSessionStatusChangedEvent event) {
        logger.info("处理会话完成: 会话ID={}", event.sessionId());
        
        // 发送会话完成通知
        sendSessionCompletedNotification(event.sessionId());
        
        // 清理相关资源
        cleanupSessionResources(event.sessionId());
        
        // 更新用户充电历史
        updateUserChargingHistory(event.sessionId());
    }
    
    // 以下是具体的副作用操作方法，实际实现中需要调用相应的外部服务
    
    private void sendReservationNotification(Long sessionId, String message) {
        logger.debug("发送预约通知: 会话ID={}, 消息={}", sessionId, message);
        // TODO: 实现通知服务调用
    }
    
    private void scheduleReservationTimeout(Long sessionId) {
        logger.debug("启动预约超时监控: 会话ID={}", sessionId);
        // TODO: 实现定时任务调度
    }
    
    private void sendChargingReadyNotification(Long sessionId) {
        logger.debug("发送充电准备通知: 会话ID={}", sessionId);
        // TODO: 实现通知服务调用
    }
    
    private void updateConnectorStatus(Long sessionId, String status) {
        logger.debug("更新充电桩状态: 会话ID={}, 状态={}", sessionId, status);
        // TODO: 调用充电桩管理服务
    }
    
    private void sendChargingStartNotification(Long sessionId) {
        logger.debug("发送充电开始通知: 会话ID={}", sessionId);
        // TODO: 实现通知服务调用
    }
    
    private void startRealTimeMonitoring(Long sessionId) {
        logger.debug("开始实时监控: 会话ID={}", sessionId);
        // TODO: 启动实时数据采集
    }
    
    private void updateChargingStatistics(Long sessionId, String action) {
        logger.debug("更新充电统计: 会话ID={}, 动作={}", sessionId, action);
        // TODO: 更新统计数据
    }
    
    private void sendChargingCompletedNotification(Long sessionId) {
        logger.debug("发送充电完成通知: 会话ID={}", sessionId);
        // TODO: 实现通知服务调用
    }
    
    private void stopRealTimeMonitoring(Long sessionId) {
        logger.debug("停止实时监控: 会话ID={}", sessionId);
        // TODO: 停止实时数据采集
    }
    
    private void calculateChargingFee(Long sessionId) {
        logger.debug("计算充电费用: 会话ID={}", sessionId);
        // TODO: 调用计费服务
    }
    
    private void sendPaymentSuccessNotification(ChargeSessionStatusChangedEvent event) {
        sendPaymentSuccessNotification(event.sessionId());
    }
    
    private void sendPaymentSuccessNotification(Long sessionId) {
        logger.debug("发送支付成功通知: 会话ID={}", sessionId);
        // TODO: 实现通知服务调用
    }
    
    private void sendPaymentSuccessNotification(PaymentConfirmedEvent event) {
        logger.debug("发送支付成功通知: {}", event.getDescription());
        // TODO: 实现通知服务调用
    }
    
    private void releaseConnector(Long sessionId) {
        logger.debug("释放充电桩: 会话ID={}", sessionId);
        // TODO: 调用充电桩管理服务
    }
    
    private void sendSessionCompletedNotification(Long sessionId) {
        logger.debug("发送会话完成通知: 会话ID={}", sessionId);
        // TODO: 实现通知服务调用
    }
    
    private void cleanupSessionResources(Long sessionId) {
        logger.debug("清理会话资源: 会话ID={}", sessionId);
        // TODO: 清理相关资源
    }
    
    private void updateUserChargingHistory(Long sessionId) {
        logger.debug("更新用户充电历史: 会话ID={}", sessionId);
        // TODO: 更新用户历史记录
    }
    
    private void updateUserAccountInfo(PaymentConfirmedEvent event) {
        logger.debug("更新用户账户信息: {}", event.getDescription());
        // TODO: 调用用户服务
    }
    
    private void generateInvoiceIfNeeded(PaymentConfirmedEvent event) {
        if (event.isLargePayment()) {
            logger.debug("生成发票: {}", event.getDescription());
            // TODO: 调用发票服务
        }
    }
    
    private void updatePaymentStatistics(PaymentConfirmedEvent event) {
        logger.debug("更新支付统计: {}", event.getDescription());
        // TODO: 更新支付统计数据
    }
}
