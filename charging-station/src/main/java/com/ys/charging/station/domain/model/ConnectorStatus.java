package com.ys.charging.station.domain.model;

/**
 * 充电桩状态枚举
 * 
 * 定义充电桩在运营过程中的所有可能状态。
 * 状态变更会影响充电桩的可用性和预约能力。
 * 
 * 状态流转：
 * - 正常流程：IDLE -> RESERVED -> OCCUPIED -> CHARGING -> IDLE
 * - 异常流程：任何状态 -> FAULT/OFFLINE
 * - 维护流程：任何状态 -> MAINTENANCE -> IDLE
 * 
 * @author yang
 * @since 2025-06-23
 */
public enum ConnectorStatus {
    
    /**
     * 空闲 - 充电桩可用，可以被预约或直接使用
     */
    IDLE("空闲"),
    
    /**
     * 已预约 - 充电桩已被用户预约，等待用户到达
     */
    RESERVED("已预约"),
    
    /**
     * 占用 - 用户已到达并插枪，但尚未开始充电
     */
    OCCUPIED("占用"),
    
    /**
     * 充电中 - 正在进行充电
     */
    CHARGING("充电中"),
    
    /**
     * 故障 - 充电桩出现故障，无法提供服务
     */
    FAULT("故障"),
    
    /**
     * 离线 - 充电桩与系统失去连接
     */
    OFFLINE("离线"),
    
    /**
     * 维护中 - 充电桩正在维护
     */
    MAINTENANCE("维护中");
    
    private final String description;
    
    ConnectorStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否可以被预约
     * 
     * @return true 如果可以被预约
     */
    public boolean isReservable() {
        return this == IDLE;
    }
    
    /**
     * 判断是否可以开始充电
     * 
     * @return true 如果可以开始充电
     */
    public boolean isChargeable() {
        return this == OCCUPIED;
    }
    
    /**
     * 判断是否正在使用中
     * 
     * @return true 如果正在使用中
     */
    public boolean isInUse() {
        return this == RESERVED || this == OCCUPIED || this == CHARGING;
    }
    
    /**
     * 判断是否可用（可以提供服务）
     * 
     * @return true 如果可用
     */
    public boolean isAvailable() {
        return this == IDLE || this == RESERVED || this == OCCUPIED || this == CHARGING;
    }
    
    /**
     * 判断是否为故障状态
     * 
     * @return true 如果为故障状态
     */
    public boolean isFaulty() {
        return this == FAULT || this == OFFLINE || this == MAINTENANCE;
    }
}
