package com.ys.charging.process.domain.model;

/**
 * 充电会话状态枚举
 * 
 * 定义充电会话在整个生命周期中的所有可能状态，
 * 涵盖预约、进场、充电、支付、完成等业务环节。
 * 
 * 状态流转路径：
 * INIT -> RESERVED -> LOCK_READY -> READY_TO_CHARGE -> CHARGE_STARTING 
 * -> CHARGING -> CHARGE_STOPPING -> CHARGE_STOPPED -> PAID -> END
 * 
 * 异常路径：
 * - 预约超时：RESERVED -> END
 * - 取消预约：RESERVED -> END  
 * - 插枪检测：INIT/RESERVED/LOCK_READY -> READY_TO_CHARGE
 * - 拔枪检测：READY_TO_CHARGE -> INIT
 * - 启动失败：CHARGE_STARTING -> READY_TO_CHARGE
 * - 未支付结束：CHARGE_STOPPED -> UNPAID -> END
 * 
 * @author yang
 * @since 2025-06-23
 */
public enum ChargeSessionStatus {
    
    /**
     * 初始状态 - 会话创建但未预约
     */
    INIT("初始状态"),
    
    /**
     * 已预约 - 用户成功预约充电桩
     */
    RESERVED("已预约"),
    
    /**
     * 地锁已降 - 地锁降下，车位可用
     */
    LOCK_READY("地锁已降"),
    
    /**
     * 准备充电 - 充电枪已插入，等待启动充电
     */
    READY_TO_CHARGE("准备充电"),
    
    /**
     * 充电启动中 - 正在启动充电过程
     */
    CHARGE_STARTING("充电启动中"),
    
    /**
     * 充电中 - 正在进行充电
     */
    CHARGING("充电中"),
    
    /**
     * 充电停止中 - 正在停止充电过程
     */
    CHARGE_STOPPING("充电停止中"),
    
    /**
     * 充电已停止 - 充电过程已结束，等待支付
     */
    CHARGE_STOPPED("充电已停止"),
    
    /**
     * 已支付 - 用户已完成支付
     */
    PAID("已支付"),
    
    /**
     * 未支付 - 充电结束但用户未支付
     */
    UNPAID("未支付"),
    
    /**
     * 会话结束 - 整个充电会话完成
     */
    END("会话结束");
    
    private final String description;
    
    ChargeSessionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为终态
     */
    public boolean isTerminal() {
        return this == END;
    }
    
    /**
     * 判断是否为充电相关状态
     */
    public boolean isChargingRelated() {
        return this == CHARGE_STARTING || this == CHARGING || 
               this == CHARGE_STOPPING || this == CHARGE_STOPPED;
    }
    
    /**
     * 判断是否为支付相关状态
     */
    public boolean isPaymentRelated() {
        return this == PAID || this == UNPAID;
    }
}
