package com.ys.charging.process.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 支付确认事件
 *
 * 当用户完成充电费用支付时发布此事件。
 * 事件处理器可以基于此事件执行相关操作，如：
 * - 更新订单状态
 * - 发送支付成功通知
 * - 生成发票
 * - 更新用户账户余额
 * - 触发会话结束流程
 *
 * 使用 Java 21 的 record 特性，确保事件的不可变性。
 *
 * @param sessionId 充电会话ID
 * @param amount 支付金额
 * @param occurredAt 事件发生时间
 *
 * @author yang
 * @since 2025-06-23
 */
public record PaymentConfirmedEvent(
    Long sessionId,
    BigDecimal amount,
    Instant occurredAt
) implements DomainEvent {
    
    /**
     * 创建支付确认事件的便捷方法
     * 
     * @param sessionId 充电会话ID
     * @param amount 支付金额
     * @return 支付确认事件实例
     */
    public static PaymentConfirmedEvent of(Long sessionId, BigDecimal amount) {
        return new PaymentConfirmedEvent(sessionId, amount, Instant.now());
    }
    
    /**
     * 获取事件描述
     * 
     * @return 事件的可读描述
     */
    public String getDescription() {
        return String.format("充电会话 %d 支付确认，金额：%.2f 元", sessionId, amount);
    }
    
    /**
     * 判断是否为大额支付
     * 
     * @return true 如果支付金额超过 100 元
     */
    public boolean isLargePayment() {
        return amount.compareTo(BigDecimal.valueOf(100)) > 0;
    }
    
    /**
     * 获取支付金额（元）
     *
     * @return 支付金额，保留两位小数
     */
    public BigDecimal getAmountInYuan() {
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
