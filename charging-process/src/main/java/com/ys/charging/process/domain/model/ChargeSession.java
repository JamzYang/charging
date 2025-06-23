package com.ys.charging.process.domain.model;

import com.ys.charging.process.domain.event.ChargeSessionStatusChangedEvent;
import com.ys.charging.process.domain.event.PaymentConfirmedEvent;
import com.ys.charging.process.domain.service.TransitionTable;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 充电会话聚合根
 * 
 * 作为充电会话的聚合根，封装所有业务规则，保证数据一致性。
 * 负责处理状态转换、发布领域事件、维护业务不变量。
 * 
 * 核心职责：
 * - 维护充电会话的完整生命周期
 * - 验证和执行状态转换
 * - 发布领域事件驱动副作用
 * - 确保业务规则的一致性
 * 
 * 并发控制：
 * - 使用 @Version 实现乐观锁，防止并发冲突
 * - 通过聚合根边界确保事务一致性
 * 
 * @author yang
 * @since 2025-06-23
 */
@Entity
@Table(name = "charge_session")
@EntityListeners(AuditingEntityListener.class)
public class ChargeSession extends AbstractAggregateRoot<ChargeSession> {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 充电站ID
     */
    @Column(name = "station_id", nullable = false)
    private Long stationId;
    
    /**
     * 充电桩ID
     */
    @Column(name = "connector_id", nullable = false)
    private Long connectorId;
    
    /**
     * 当前状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChargeSessionStatus status;
    
    /**
     * 预约ID（可选）
     */
    @Column(name = "reservation_id")
    private Long reservationId;
    
    /**
     * 充电开始时间
     */
    @Column(name = "charge_start_time")
    private LocalDateTime chargeStartTime;
    
    /**
     * 充电结束时间
     */
    @Column(name = "charge_end_time")
    private LocalDateTime chargeEndTime;
    
    /**
     * 充电电量（kWh）
     */
    @Column(name = "energy_consumed", precision = 10, scale = 3)
    private BigDecimal energyConsumed;
    
    /**
     * 充电费用
     */
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    /**
     * 乐观锁版本号
     */
    @Version
    private Long version;
    
    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * JPA 默认构造函数
     */
    protected ChargeSession() {}
    
    /**
     * 创建新的充电会话
     * 
     * @param userId 用户ID
     * @param stationId 充电站ID
     * @param connectorId 充电桩ID
     */
    public ChargeSession(Long userId, Long stationId, Long connectorId) {
        this.userId = userId;
        this.stationId = stationId;
        this.connectorId = connectorId;
        this.status = ChargeSessionStatus.INIT;
        this.energyConsumed = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
    }
    
    /**
     * 处理事件并执行状态转换
     * 
     * 这是状态机的核心方法，负责：
     * 1. 验证状态转换的合法性
     * 2. 更新聚合根状态
     * 3. 发布领域事件
     * 
     * @param event 触发事件
     * @param context 上下文参数（可选）
     * @throws IllegalStateException 如果状态转换不合法
     */
    public void handleEvent(ChargeSessionEvent event, Object... context) {
        ChargeSessionStatus nextStatus = TransitionTable.getNext(this.status, event)
            .orElseThrow(() -> new IllegalStateException(
                String.format("非法状态转换: %s + %s", this.status, event)));
        
        // 执行状态转换前的业务逻辑
        executePreTransitionLogic(event, context);
        
        // 更新状态
        ChargeSessionStatus previousStatus = this.status;
        this.status = nextStatus;
        
        // 执行状态转换后的业务逻辑
        executePostTransitionLogic(event, previousStatus, context);
        
        // 发布状态变更事件
        registerEvent(ChargeSessionStatusChangedEvent.of(this.id, this.status, event));
    }
    
    /**
     * 状态转换前的业务逻辑
     */
    private void executePreTransitionLogic(ChargeSessionEvent event, Object... context) {
        switch (event) {
            case START_CONFIRMED -> this.chargeStartTime = LocalDateTime.now();
            case STOP_CONFIRMED -> this.chargeEndTime = LocalDateTime.now();
            case PAYMENT_CONFIRMED -> {
                if (context.length > 0 && context[0] instanceof BigDecimal amount) {
                    this.totalAmount = amount;
                    // 发布支付确认事件
                    registerEvent(PaymentConfirmedEvent.of(this.id, amount));
                }
            }
        }
    }
    
    /**
     * 状态转换后的业务逻辑
     */
    private void executePostTransitionLogic(ChargeSessionEvent event, ChargeSessionStatus previousStatus, Object... context) {
        // 可以在这里添加状态转换后的特殊逻辑
        // 例如：计算充电时长、更新统计信息等
    }
    
    // Getters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getStationId() { return stationId; }
    public Long getConnectorId() { return connectorId; }
    public ChargeSessionStatus getStatus() { return status; }
    public Long getReservationId() { return reservationId; }
    public LocalDateTime getChargeStartTime() { return chargeStartTime; }
    public LocalDateTime getChargeEndTime() { return chargeEndTime; }
    public BigDecimal getEnergyConsumed() { return energyConsumed; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // Setters for specific business operations
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public void setEnergyConsumed(BigDecimal energyConsumed) { this.energyConsumed = energyConsumed; }
}
