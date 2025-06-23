package com.ys.charging.process.domain.event;

import com.ys.charging.process.domain.model.ChargeSessionEvent;
import com.ys.charging.process.domain.model.ChargeSessionStatus;

import java.time.Instant;

/**
 * 充电会话状态变更事件
 *
 * 当充电会话的状态发生变更时发布此事件。
 * 事件处理器可以基于此事件执行副作用操作，如：
 * - 发送通知给用户
 * - 调用外部服务
 * - 更新统计数据
 * - 记录审计日志
 *
 * 使用 Java 21 的 record 特性，确保事件的不可变性。
 *
 * @param sessionId 充电会话ID
 * @param newStatus 新的状态
 * @param triggerEvent 触发状态变更的事件
 * @param occurredAt 事件发生时间
 *
 * @author yang
 * @since 2025-06-23
 */
public record ChargeSessionStatusChangedEvent(
    Long sessionId,
    ChargeSessionStatus newStatus,
    ChargeSessionEvent triggerEvent,
    Instant occurredAt
) implements DomainEvent {
    
    /**
     * 创建状态变更事件的便捷方法
     * 
     * @param sessionId 充电会话ID
     * @param newStatus 新的状态
     * @param triggerEvent 触发事件
     * @return 状态变更事件实例
     */
    public static ChargeSessionStatusChangedEvent of(Long sessionId, 
                                                   ChargeSessionStatus newStatus, 
                                                   ChargeSessionEvent triggerEvent) {
        return new ChargeSessionStatusChangedEvent(sessionId, newStatus, triggerEvent, Instant.now());
    }
    
    /**
     * 获取事件描述
     * 
     * @return 事件的可读描述
     */
    public String getDescription() {
        return String.format("充电会话 %d 状态变更为 %s，触发事件：%s", 
                           sessionId, newStatus.getDescription(), triggerEvent.getDescription());
    }
    
    /**
     * 判断是否为终态事件
     * 
     * @return true 如果新状态为终态
     */
    public boolean isTerminalStateEvent() {
        return newStatus.isTerminal();
    }
    
    /**
     * 判断是否为充电相关状态事件
     * 
     * @return true 如果新状态为充电相关状态
     */
    public boolean isChargingRelatedEvent() {
        return newStatus.isChargingRelated();
    }
    
    /**
     * 判断是否为支付相关状态事件
     *
     * @return true 如果新状态为支付相关状态
     */
    public boolean isPaymentRelatedEvent() {
        return newStatus.isPaymentRelated();
    }
}
