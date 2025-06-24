package com.ys.charging.station.domain.model;

import com.ys.charging.station.domain.event.*;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 充电站聚合根
 * 
 * 管理充电站的完整生命周期，包括基本信息、状态管理、充电桩集合等。
 * 作为聚合根，负责维护业务不变量和发布领域事件。
 * 
 * @author yang
 * @since 2025-06-23
 */
@Entity
@Table(name = "stations", indexes = {
    @Index(name = "idx_station_status", columnList = "status"),
    @Index(name = "idx_station_operator", columnList = "operator"),
    @Index(name = "idx_station_location", columnList = "longitude, latitude")
})
@EntityListeners(AuditingEntityListener.class)
public class Station extends AbstractAggregateRoot<Station> {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Valid
    @Embedded
    private StationInfo stationInfo;
    
    @Valid
    @Embedded
    private Location location;
    
    @Valid
    @Embedded
    private BusinessHours businessHours;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StationStatus status;
    
    @OneToMany(mappedBy = "stationId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Connector> connectors = new HashSet<>();
    
    @OneToMany(mappedBy = "stationId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ParkingSpot> parkingSpots = new HashSet<>();
    
    @Column(name = "total_connectors")
    private Integer totalConnectors;
    
    @Column(name = "available_connectors")
    private Integer availableConnectors;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;
    
    // 构造函数
    protected Station() {
        // JPA required
    }
    
    public Station(StationInfo stationInfo, Location location, BusinessHours businessHours) {
        this.stationInfo = stationInfo;
        this.location = location;
        this.businessHours = businessHours;
        this.status = StationStatus.OPERATING;
        this.totalConnectors = 0;
        this.availableConnectors = 0;
    }
    
    /**
     * 创建充电站
     */
    public static Station create(StationInfo stationInfo, Location location, BusinessHours businessHours) {
        return new Station(stationInfo, location, businessHours);
    }
    
    /**
     * 添加充电桩
     */
    public void addConnector(ConnectorInfo connectorInfo, ParkingSpot parkingSpot) {
        // 验证业务规则
        validateCanAddConnector();
        
        // 创建充电桩
        Connector connector = new Connector(this.id, connectorInfo, parkingSpot.getId());
        connectors.add(connector);
        
        // 添加车位
        parkingSpots.add(parkingSpot);
        
        // 更新统计信息
        updateConnectorCounts();
    }
    
    /**
     * 移除充电桩
     */
    public void removeConnector(Long connectorId) {
        Connector connector = findConnectorById(connectorId);
        if (connector == null) {
            throw new IllegalArgumentException("充电桩不存在: " + connectorId);
        }
        
        // 检查是否可以移除
        if (connector.isInUse()) {
            throw new IllegalStateException("充电桩正在使用中，无法移除");
        }
        
        connectors.remove(connector);
        
        // 移除关联的车位
        parkingSpots.removeIf(spot -> Objects.equals(spot.getId(), connector.getParkingSpotId()));
        
        // 更新统计信息
        updateConnectorCounts();
    }
    
    /**
     * 更改充电站状态
     */
    public void changeStatus(StationStatus newStatus, String reason) {
        if (this.status == newStatus) {
            return;
        }
        
        StationStatus oldStatus = this.status;
        this.status = newStatus;
        
        // 发布状态变更事件
        registerEvent(StationStatusChangedEvent.of(this.id, oldStatus, newStatus, reason));
        
        // 如果充电站关闭，需要处理所有充电桩
        if (newStatus == StationStatus.CLOSED || newStatus == StationStatus.FAULT) {
            handleStationUnavailable(reason);
        }
    }
    
    /**
     * 预约充电桩
     */
    public void reserveConnector(Long connectorId, Long userId, int reservationMinutes) {
        Connector connector = findConnectorById(connectorId);
        if (connector == null) {
            throw new IllegalArgumentException("充电桩不存在: " + connectorId);
        }
        
        // 验证充电站状态
        validateStationAvailable();
        
        // 验证营业时间
        validateBusinessHours();
        
        // 预约充电桩
        connector.reserve(userId, reservationMinutes);
        
        // 控制地锁
        controlParkingSpotLock(connector.getParkingSpotId(), true);
        
        // 发布预约事件
        registerEvent(ConnectorReservedEvent.of(
            this.id, connectorId, userId, connector.getReservationExpiresAt()));
        
        // 更新统计信息
        updateConnectorCounts();
    }
    
    /**
     * 取消预约
     */
    public void cancelReservation(Long connectorId, Long userId, String reason) {
        Connector connector = findConnectorById(connectorId);
        if (connector == null) {
            throw new IllegalArgumentException("充电桩不存在: " + connectorId);
        }
        
        // 验证用户权限
        if (!Objects.equals(connector.getReservedByUserId(), userId)) {
            throw new IllegalStateException("只能取消自己的预约");
        }
        
        connector.cancelReservation();
        
        // 控制地锁
        controlParkingSpotLock(connector.getParkingSpotId(), false);
        
        // 发布取消预约事件
        registerEvent(ConnectorReservationCancelledEvent.of(
            this.id, connectorId, userId, reason));
        
        // 更新统计信息
        updateConnectorCounts();
    }
    
    /**
     * 占用充电桩
     */
    public void occupyConnector(Long connectorId, Long userId) {
        Connector connector = findConnectorById(connectorId);
        if (connector == null) {
            throw new IllegalArgumentException("充电桩不存在: " + connectorId);
        }
        
        validateStationAvailable();
        
        ConnectorStatus oldStatus = connector.getStatus();
        connector.occupy(userId);
        
        // 发布状态变更事件
        registerEvent(ConnectorStatusChangedEvent.of(
            this.id, connectorId, oldStatus, connector.getStatus(), userId, "用户占用"));
        
        updateConnectorCounts();
    }
    
    /**
     * 开始充电
     */
    public void startCharging(Long connectorId) {
        Connector connector = findConnectorById(connectorId);
        if (connector == null) {
            throw new IllegalArgumentException("充电桩不存在: " + connectorId);
        }
        
        ConnectorStatus oldStatus = connector.getStatus();
        connector.startCharging();
        
        // 发布状态变更事件
        registerEvent(ConnectorStatusChangedEvent.of(
            this.id, connectorId, oldStatus, connector.getStatus(), 
            connector.getReservedByUserId(), "开始充电"));
        
        updateConnectorCounts();
    }
    
    /**
     * 停止充电
     */
    public void stopCharging(Long connectorId) {
        Connector connector = findConnectorById(connectorId);
        if (connector == null) {
            throw new IllegalArgumentException("充电桩不存在: " + connectorId);
        }
        
        ConnectorStatus oldStatus = connector.getStatus();
        connector.stopCharging();
        
        // 发布状态变更事件
        registerEvent(ConnectorStatusChangedEvent.of(
            this.id, connectorId, oldStatus, connector.getStatus(), 
            connector.getReservedByUserId(), "停止充电"));
        
        updateConnectorCounts();
    }
    
    /**
     * 释放充电桩
     */
    public void releaseConnector(Long connectorId) {
        Connector connector = findConnectorById(connectorId);
        if (connector == null) {
            throw new IllegalArgumentException("充电桩不存在: " + connectorId);
        }
        
        ConnectorStatus oldStatus = connector.getStatus();
        Long userId = connector.getReservedByUserId();
        connector.release();
        
        // 控制地锁
        controlParkingSpotLock(connector.getParkingSpotId(), false);
        
        // 发布状态变更事件
        registerEvent(ConnectorStatusChangedEvent.of(
            this.id, connectorId, oldStatus, connector.getStatus(), userId, "释放充电桩"));
        
        updateConnectorCounts();
    }
    
    // 私有辅助方法
    private Connector findConnectorById(Long connectorId) {
        return connectors.stream()
            .filter(c -> Objects.equals(c.getId(), connectorId))
            .findFirst()
            .orElse(null);
    }
    
    private void validateCanAddConnector() {
        if (status == StationStatus.CLOSED) {
            throw new IllegalStateException("已关闭的充电站无法添加充电桩");
        }
    }
    
    private void validateStationAvailable() {
        if (!status.isServiceAvailable()) {
            throw new IllegalStateException("充电站当前不可用: " + status.getDescription());
        }
    }
    
    private void validateBusinessHours() {
        if (!businessHours.isOpenNow()) {
            throw new IllegalStateException("充电站当前不在营业时间内");
        }
    }
    
    private void handleStationUnavailable(String reason) {
        // 取消所有预约
        connectors.stream()
            .filter(c -> c.getStatus() == ConnectorStatus.RESERVED)
            .forEach(c -> {
                c.cancelReservation();
                registerEvent(ConnectorReservationCancelledEvent.of(
                    this.id, c.getId(), c.getReservedByUserId(), reason));
            });
        
        updateConnectorCounts();
    }
    
    private void controlParkingSpotLock(Long parkingSpotId, boolean lockDown) {
        if (parkingSpotId == null) {
            return;
        }
        
        parkingSpots.stream()
            .filter(spot -> Objects.equals(spot.getId(), parkingSpotId))
            .findFirst()
            .ifPresent(spot -> {
                if (lockDown) {
                    spot.lockDown();
                } else {
                    spot.lockUp();
                }
            });
    }
    
    private void updateConnectorCounts() {
        this.totalConnectors = connectors.size();
        this.availableConnectors = (int) connectors.stream()
            .filter(c -> c.getStatus().isReservable())
            .count();
    }
    
    // 查询方法
    public List<Connector> getAvailableConnectors() {
        return connectors.stream()
            .filter(c -> c.getStatus().isReservable())
            .collect(Collectors.toList());
    }
    
    public List<Connector> getConnectorsByStatus(ConnectorStatus status) {
        return connectors.stream()
            .filter(c -> c.getStatus() == status)
            .collect(Collectors.toList());
    }
    
    public boolean hasAvailableConnectors() {
        return availableConnectors > 0;
    }
    
    public boolean isOperating() {
        return status == StationStatus.OPERATING;
    }
    
    public boolean isInBusinessHours() {
        return businessHours.isOpenNow();
    }
    
    // Getters
    public Long getId() { return id; }
    public StationInfo getStationInfo() { return stationInfo; }
    public Location getLocation() { return location; }
    public BusinessHours getBusinessHours() { return businessHours; }
    public StationStatus getStatus() { return status; }
    public Set<Connector> getConnectors() { return Collections.unmodifiableSet(connectors); }
    public Set<ParkingSpot> getParkingSpots() { return Collections.unmodifiableSet(parkingSpots); }
    public Integer getTotalConnectors() { return totalConnectors; }
    public Integer getAvailableConnectorCount() { return availableConnectors; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;
        return Objects.equals(id, station.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Station{id=%d, name='%s', status=%s, totalConnectors=%d, availableConnectors=%d}", 
            id, stationInfo != null ? stationInfo.name() : "null", status, totalConnectors, availableConnectors);
    }
}
