package com.ys.charging.process.domain.model;

/**
 * 非法状态转换异常
 * 
 * 当尝试执行不合法的状态转换时抛出此异常。
 * 这是一个领域异常，表示违反了业务规则。
 * 
 * 使用场景：
 * - 状态机中不允许的状态转换
 * - 业务规则验证失败
 * - 聚合根状态不一致
 * 
 * @author yang
 * @since 2025-06-23
 */
public class IllegalStateTransitionException extends RuntimeException {
    
    private final ChargeSessionStatus currentStatus;
    private final ChargeSessionEvent triggerEvent;
    
    /**
     * 构造函数
     * 
     * @param currentStatus 当前状态
     * @param triggerEvent 触发事件
     */
    public IllegalStateTransitionException(ChargeSessionStatus currentStatus, ChargeSessionEvent triggerEvent) {
        super(String.format("非法状态转换: 当前状态=%s, 触发事件=%s", currentStatus, triggerEvent));
        this.currentStatus = currentStatus;
        this.triggerEvent = triggerEvent;
    }
    
    /**
     * 构造函数
     * 
     * @param currentStatus 当前状态
     * @param triggerEvent 触发事件
     * @param message 自定义错误消息
     */
    public IllegalStateTransitionException(ChargeSessionStatus currentStatus, ChargeSessionEvent triggerEvent, String message) {
        super(message);
        this.currentStatus = currentStatus;
        this.triggerEvent = triggerEvent;
    }
    
    /**
     * 构造函数
     * 
     * @param currentStatus 当前状态
     * @param triggerEvent 触发事件
     * @param message 自定义错误消息
     * @param cause 原因异常
     */
    public IllegalStateTransitionException(ChargeSessionStatus currentStatus, ChargeSessionEvent triggerEvent, String message, Throwable cause) {
        super(message, cause);
        this.currentStatus = currentStatus;
        this.triggerEvent = triggerEvent;
    }
    
    /**
     * 获取当前状态
     * 
     * @return 当前状态
     */
    public ChargeSessionStatus getCurrentStatus() {
        return currentStatus;
    }
    
    /**
     * 获取触发事件
     * 
     * @return 触发事件
     */
    public ChargeSessionEvent getTriggerEvent() {
        return triggerEvent;
    }
    
    /**
     * 获取详细的错误信息
     * 
     * @return 详细错误信息
     */
    public String getDetailedMessage() {
        return String.format("状态转换失败 - 当前状态: %s (%s), 触发事件: %s (%s), 错误: %s",
                currentStatus, currentStatus.getDescription(),
                triggerEvent, triggerEvent.getDescription(),
                getMessage());
    }
}
