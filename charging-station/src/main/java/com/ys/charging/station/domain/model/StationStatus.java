package com.ys.charging.station.domain.model;

/**
 * 充电站状态枚举
 * 
 * 定义充电站在运营过程中的所有可能状态。
 * 状态变更会影响充电站的可用性和服务能力。
 * 
 * 状态说明：
 * - OPERATING：正常运营，可提供充电服务
 * - MAINTENANCE：维护中，暂停服务但计划恢复
 * - FAULT：故障状态，需要修复后才能恢复服务
 * - CLOSED：已关闭，长期不提供服务
 * 
 * @author yang
 * @since 2025-06-23
 */
public enum StationStatus {
    
    /**
     * 运营中 - 充电站正常运营，可提供充电服务
     */
    OPERATING("运营中"),
    
    /**
     * 维护中 - 充电站正在维护，暂停服务
     */
    MAINTENANCE("维护中"),
    
    /**
     * 故障 - 充电站出现故障，无法提供服务
     */
    FAULT("故障"),
    
    /**
     * 关闭 - 充电站已关闭，长期不提供服务
     */
    CLOSED("关闭");
    
    private final String description;
    
    StationStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否可以提供充电服务
     * 
     * @return true 如果可以提供服务
     */
    public boolean isServiceAvailable() {
        return this == OPERATING;
    }
    
    /**
     * 判断是否为临时状态（可恢复）
     * 
     * @return true 如果是临时状态
     */
    public boolean isTemporary() {
        return this == MAINTENANCE || this == FAULT;
    }
    
    /**
     * 判断是否为永久关闭状态
     * 
     * @return true 如果是永久关闭
     */
    public boolean isPermanentlyClosed() {
        return this == CLOSED;
    }
}
