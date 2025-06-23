package com.ys.charging.station.domain.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * 充电桩实体
 * 
 * 管理充电桩的状态、预约信息和技术规格。
 * 与车位形成一对一关系，支持复杂的状态流转。
 * 
 * @author yang
 * @since 2025-06-23
 */
@Entity
@Table(name = "connectors", indexes = {
    @Index(name = "idx_connector_station_id", columnList = "station_id"),
    @Index(name = "idx_connector_status", columnList = "status"),
    @Index(name = "idx_connector_parking_spot", columnList = "parking_spot_id")
})
@EntityListeners(AuditingEntityListener.class)
public class Connector {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(name = "station_id", nullable = false)
    private Long stationId;
    
    @Valid
    @Embedded
    private ConnectorInfo connectorInfo;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConnectorStatus status;
    
    @Column(name = "parking_spot_id")
    private Long parkingSpotId;
    
    @Column(name = "reserved_by_user_id")
    private Long reservedByUserId;
    
    @Column(name = "reserved_at")
    private Instant reservedAt;
    
    @Column(name = "reservation_expires_at")
    private Instant reservationExpiresAt;
    
    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;
    
    @Column(name = "fault_reason", length = 500)
    private String faultReason;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;
    
    // 构造函数
    protected Connector() {
        // JPA required
    }
    
    public Connector(Long stationId, ConnectorInfo connectorInfo, Long parkingSpotId) {
        this.stationId = stationId;
        this.connectorInfo = connectorInfo;
        this.parkingSpotId = parkingSpotId;
        this.status = ConnectorStatus.IDLE;
        this.lastHeartbeatAt = Instant.now();
    }
    
    /**
     * 创建充电桩
     */
    public static Connector create(Long stationId, ConnectorInfo connectorInfo, Long parkingSpotId) {
        return new Connector(stationId, connectorInfo, parkingSpotId);
    }
    
    /**
     * 预约充电桩
     */
    public void reserve(Long userId, int reservationMinutes) {
        if (!status.isReservable()) {
            throw new IllegalStateException(
                String.format("充电桩状态为 %s，无法预约", status.getDescription()));
        }
        
        this.status = ConnectorStatus.RESERVED;
        this.reservedByUserId = userId;
        this.reservedAt = Instant.now();
        this.reservationExpiresAt = Instant.now().plusSeconds(reservationMinutes * 60L);
    }
    
    /**
     * 取消预约
     */
    public void cancelReservation() {
        if (status != ConnectorStatus.RESERVED) {
            throw new IllegalStateException("充电桩未被预约，无法取消");
        }
        
        this.status = ConnectorStatus.IDLE;
        this.reservedByUserId = null;
        this.reservedAt = null;
        this.reservationExpiresAt = null;
    }
    
    /**
     * 占用充电桩（用户到达并插枪）
     */
    public void occupy(Long userId) {
        // 检查是否为预约用户
        if (status == ConnectorStatus.RESERVED) {
            if (!Objects.equals(reservedByUserId, userId)) {
                throw new IllegalStateException("该充电桩已被其他用户预约");
            }
        } else if (status != ConnectorStatus.IDLE) {
            throw new IllegalStateException(
                String.format("充电桩状态为 %s，无法占用", status.getDescription()));
        }
        
        this.status = ConnectorStatus.OCCUPIED;
        this.reservedByUserId = userId;
        this.reservedAt = null;
        this.reservationExpiresAt = null;
    }
    
    /**
     * 开始充电
     */
    public void startCharging() {
        if (!status.isChargeable()) {
            throw new IllegalStateException(
                String.format("充电桩状态为 %s，无法开始充电", status.getDescription()));
        }
        
        this.status = ConnectorStatus.CHARGING;
    }
    
    /**
     * 停止充电
     */
    public void stopCharging() {
        if (status != ConnectorStatus.CHARGING) {
            throw new IllegalStateException("充电桩未在充电状态，无法停止");
        }
        
        this.status = ConnectorStatus.OCCUPIED;
    }
    
    /**
     * 释放充电桩（用户拔枪离开）
     */
    public void release() {
        if (status == ConnectorStatus.CHARGING) {
            throw new IllegalStateException("充电桩正在充电，请先停止充电");
        }
        
        this.status = ConnectorStatus.IDLE;
        this.reservedByUserId = null;
        this.reservedAt = null;
        this.reservationExpiresAt = null;
    }
    
    /**
     * 设置故障状态
     */
    public void setFault(String reason) {
        this.status = ConnectorStatus.FAULT;
        this.faultReason = reason;
        // 清除预约信息
        this.reservedByUserId = null;
        this.reservedAt = null;
        this.reservationExpiresAt = null;
    }
    
    /**
     * 设置离线状态
     */
    public void setOffline() {
        this.status = ConnectorStatus.OFFLINE;
        // 清除预约信息
        this.reservedByUserId = null;
        this.reservedAt = null;
        this.reservationExpiresAt = null;
    }
    
    /**
     * 设置维护状态
     */
    public void setMaintenance() {
        this.status = ConnectorStatus.MAINTENANCE;
        // 清除预约信息
        this.reservedByUserId = null;
        this.reservedAt = null;
        this.reservationExpiresAt = null;
    }
    
    /**
     * 修复故障，恢复到空闲状态
     */
    public void repair() {
        if (status == ConnectorStatus.FAULT || status == ConnectorStatus.MAINTENANCE) {
            this.status = ConnectorStatus.IDLE;
            this.faultReason = null;
        }
    }
    
    /**
     * 上线（从离线状态恢复）
     */
    public void goOnline() {
        if (status == ConnectorStatus.OFFLINE) {
            this.status = ConnectorStatus.IDLE;
        }
    }
    
    /**
     * 更新心跳时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeatAt = Instant.now();
        
        // 如果之前是离线状态，自动上线
        if (status == ConnectorStatus.OFFLINE) {
            goOnline();
        }
    }
    
    /**
     * 检查预约是否超时
     */
    public boolean isReservationExpired() {
        return status == ConnectorStatus.RESERVED && 
               reservationExpiresAt != null && 
               Instant.now().isAfter(reservationExpiresAt);
    }
    
    /**
     * 处理预约超时
     */
    public void handleReservationTimeout() {
        if (isReservationExpired()) {
            cancelReservation();
        }
    }
    
    /**
     * 检查是否离线（基于心跳时间）
     */
    public boolean isOfflineByHeartbeat(int timeoutMinutes) {
        if (lastHeartbeatAt == null) {
            return true;
        }
        
        return Instant.now().isAfter(lastHeartbeatAt.plusSeconds(timeoutMinutes * 60L));
    }
    
    /**
     * 检查并处理离线状态
     */
    public void checkAndHandleOffline(int timeoutMinutes) {
        if (status != ConnectorStatus.OFFLINE && isOfflineByHeartbeat(timeoutMinutes)) {
            setOffline();
        }
    }
    
    /**
     * 判断是否可用
     */
    public boolean isAvailable() {
        return status.isAvailable();
    }
    
    /**
     * 判断是否正在使用中
     */
    public boolean isInUse() {
        return status.isInUse();
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        StringBuilder desc = new StringBuilder(status.getDescription());
        
        if (status == ConnectorStatus.RESERVED && reservedByUserId != null) {
            desc.append(" (用户: ").append(reservedByUserId).append(")");
        }
        
        if (status == ConnectorStatus.FAULT && faultReason != null) {
            desc.append(" (").append(faultReason).append(")");
        }
        
        return desc.toString();
    }
    
    // Getters
    public Long getId() { return id; }
    public Long getStationId() { return stationId; }
    public ConnectorInfo getConnectorInfo() { return connectorInfo; }
    public ConnectorStatus getStatus() { return status; }
    public Long getParkingSpotId() { return parkingSpotId; }
    public Long getReservedByUserId() { return reservedByUserId; }
    public Instant getReservedAt() { return reservedAt; }
    public Instant getReservationExpiresAt() { return reservationExpiresAt; }
    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public String getFaultReason() { return faultReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connector connector = (Connector) o;
        return Objects.equals(id, connector.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Connector{id=%d, stationId=%d, connectorNumber='%s', status=%s}", 
            id, stationId, connectorInfo != null ? connectorInfo.connectorNumber() : "null", status);
    }
}
