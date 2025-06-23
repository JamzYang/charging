package com.ys.charging.station.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * 车位实体
 * 
 * 管理充电站的车位信息，包括车位编号、地锁状态等。
 * 与充电桩形成一对一关系。
 * 
 * @author yang
 * @since 2025-06-23
 */
@Entity
@Table(name = "parking_spots")
@EntityListeners(AuditingEntityListener.class)
public class ParkingSpot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "station_id", nullable = false)
    private Long stationId;
    
    @NotBlank
    @Column(name = "spot_number", nullable = false, length = 20)
    private String spotNumber;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false)
    private SpotType spotType;
    
    @Column(name = "has_lock", nullable = false)
    private boolean hasLock;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "lock_status")
    private LockStatus lockStatus;
    
    @Column(name = "lock_timeout_at")
    private Instant lockTimeoutAt;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;
    
    /**
     * 车位类型枚举
     */
    public enum SpotType {
        /**
         * 标准车位
         */
        STANDARD("标准车位"),
        
        /**
         * 大型车位（适合大型车辆）
         */
        LARGE("大型车位"),
        
        /**
         * 无障碍车位
         */
        ACCESSIBLE("无障碍车位"),
        
        /**
         * VIP车位
         */
        VIP("VIP车位");
        
        private final String displayName;
        
        SpotType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 地锁状态枚举
     */
    public enum LockStatus {
        /**
         * 升起状态 - 车位被锁定，车辆无法进入
         */
        UP("升起"),
        
        /**
         * 降下状态 - 车位开放，车辆可以进入
         */
        DOWN("降下"),
        
        /**
         * 故障状态 - 地锁出现故障
         */
        FAULT("故障");
        
        private final String displayName;
        
        LockStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * 判断车位是否可用
         */
        public boolean isAvailable() {
            return this == DOWN;
        }
        
        /**
         * 判断车位是否被锁定
         */
        public boolean isLocked() {
            return this == UP;
        }
        
        /**
         * 判断地锁是否故障
         */
        public boolean isFaulty() {
            return this == FAULT;
        }
    }
    
    // 构造函数
    protected ParkingSpot() {
        // JPA required
    }
    
    public ParkingSpot(Long stationId, String spotNumber, SpotType spotType, boolean hasLock) {
        this.stationId = stationId;
        this.spotNumber = spotNumber;
        this.spotType = spotType;
        this.hasLock = hasLock;
        this.lockStatus = hasLock ? LockStatus.UP : null;
    }
    
    /**
     * 创建标准车位
     */
    public static ParkingSpot createStandard(Long stationId, String spotNumber, boolean hasLock) {
        return new ParkingSpot(stationId, spotNumber, SpotType.STANDARD, hasLock);
    }
    
    /**
     * 创建无障碍车位
     */
    public static ParkingSpot createAccessible(Long stationId, String spotNumber, boolean hasLock) {
        return new ParkingSpot(stationId, spotNumber, SpotType.ACCESSIBLE, hasLock);
    }
    
    /**
     * 降下地锁
     */
    public void lockDown() {
        if (!hasLock) {
            throw new IllegalStateException("该车位没有地锁");
        }
        
        if (lockStatus == LockStatus.FAULT) {
            throw new IllegalStateException("地锁故障，无法操作");
        }
        
        this.lockStatus = LockStatus.DOWN;
        // 设置5分钟超时
        this.lockTimeoutAt = Instant.now().plusSeconds(5 * 60);
    }
    
    /**
     * 升起地锁
     */
    public void lockUp() {
        if (!hasLock) {
            throw new IllegalStateException("该车位没有地锁");
        }
        
        if (lockStatus == LockStatus.FAULT) {
            throw new IllegalStateException("地锁故障，无法操作");
        }
        
        this.lockStatus = LockStatus.UP;
        this.lockTimeoutAt = null;
    }
    
    /**
     * 设置地锁故障
     */
    public void setLockFault() {
        if (!hasLock) {
            return;
        }
        
        this.lockStatus = LockStatus.FAULT;
        this.lockTimeoutAt = null;
    }
    
    /**
     * 修复地锁故障
     */
    public void repairLock() {
        if (!hasLock) {
            return;
        }
        
        this.lockStatus = LockStatus.UP;
        this.lockTimeoutAt = null;
    }
    
    /**
     * 检查地锁是否超时
     */
    public boolean isLockTimeout() {
        if (!hasLock || lockStatus != LockStatus.DOWN || lockTimeoutAt == null) {
            return false;
        }
        
        return Instant.now().isAfter(lockTimeoutAt);
    }
    
    /**
     * 处理地锁超时
     */
    public void handleLockTimeout() {
        if (isLockTimeout()) {
            lockUp();
        }
    }
    
    /**
     * 判断车位是否可用
     */
    public boolean isAvailable() {
        if (!hasLock) {
            return true;
        }
        
        return lockStatus != null && lockStatus.isAvailable();
    }
    
    /**
     * 获取车位描述
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(spotType.getDisplayName());
        desc.append(" - ").append(spotNumber);
        
        if (hasLock) {
            desc.append(" (").append(lockStatus.getDisplayName()).append(")");
        }
        
        return desc.toString();
    }
    
    // Getters
    public Long getId() { return id; }
    public Long getStationId() { return stationId; }
    public String getSpotNumber() { return spotNumber; }
    public SpotType getSpotType() { return spotType; }
    public boolean isHasLock() { return hasLock; }
    public LockStatus getLockStatus() { return lockStatus; }
    public Instant getLockTimeoutAt() { return lockTimeoutAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingSpot that = (ParkingSpot) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("ParkingSpot{id=%d, stationId=%d, spotNumber='%s', spotType=%s, hasLock=%s, lockStatus=%s}", 
            id, stationId, spotNumber, spotType, hasLock, lockStatus);
    }
}
