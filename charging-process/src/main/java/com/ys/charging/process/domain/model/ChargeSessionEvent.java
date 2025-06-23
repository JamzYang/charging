package com.ys.charging.process.domain.model;

/**
 * 充电会话事件枚举
 * 
 * 定义触发充电会话状态转换的所有事件类型。
 * 这些事件可能来自用户操作、系统内部逻辑或外部设备。
 * 
 * 事件分类：
 * - 预约相关：RESERVE_CMD_SUCCESS, RESERVATION_TIMEOUT, CANCEL_RESERVATION
 * - 设备相关：PLUG_DETECTED, UNPLUG_DETECTED, LOCK_READY
 * - 充电相关：START_REQUESTED, START_CONFIRMED, START_FAILED, STOP_REQUESTED, STOP_CONFIRMED
 * - 支付相关：PAYMENT_CONFIRMED, PAYMENT_TIMEOUT
 * 
 * @author yang
 * @since 2025-06-23
 */
public enum ChargeSessionEvent {
    
    /**
     * 预约命令成功 - 用户成功预约充电桩
     */
    RESERVE_CMD_SUCCESS("预约成功"),
    
    /**
     * 插枪检测 - 检测到充电枪插入
     */
    PLUG_DETECTED("插枪检测"),
    
    /**
     * 地锁准备就绪 - 地锁已降下，车位可用
     */
    LOCK_READY("地锁就绪"),
    
    /**
     * 启动充电请求 - 用户请求启动充电
     */
    START_REQUESTED("启动充电请求"),
    
    /**
     * 启动充电确认 - 充电桩确认启动充电
     */
    START_CONFIRMED("启动充电确认"),
    
    /**
     * 启动充电失败 - 充电桩启动充电失败
     */
    START_FAILED("启动充电失败"),
    
    /**
     * 停止充电请求 - 用户或系统请求停止充电
     */
    STOP_REQUESTED("停止充电请求"),
    
    /**
     * 停止充电确认 - 充电桩确认停止充电
     */
    STOP_CONFIRMED("停止充电确认"),
    
    /**
     * 支付确认 - 用户完成支付
     */
    PAYMENT_CONFIRMED("支付确认"),
    
    /**
     * 拔枪检测 - 检测到充电枪拔出
     */
    UNPLUG_DETECTED("拔枪检测"),
    
    /**
     * 预约超时 - 预约时间到期
     */
    RESERVATION_TIMEOUT("预约超时"),
    
    /**
     * 取消预约 - 用户主动取消预约
     */
    CANCEL_RESERVATION("取消预约"),
    
    /**
     * 支付超时 - 支付时间到期
     */
    PAYMENT_TIMEOUT("支付超时");
    
    private final String description;
    
    ChargeSessionEvent(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为用户主动触发的事件
     */
    public boolean isUserTriggered() {
        return this == START_REQUESTED || this == STOP_REQUESTED || 
               this == PAYMENT_CONFIRMED || this == CANCEL_RESERVATION;
    }
    
    /**
     * 判断是否为设备触发的事件
     */
    public boolean isDeviceTriggered() {
        return this == PLUG_DETECTED || this == UNPLUG_DETECTED || 
               this == START_CONFIRMED || this == STOP_CONFIRMED || this == LOCK_READY;
    }
    
    /**
     * 判断是否为系统超时事件
     */
    public boolean isTimeoutEvent() {
        return this == RESERVATION_TIMEOUT || this == PAYMENT_TIMEOUT;
    }
}
