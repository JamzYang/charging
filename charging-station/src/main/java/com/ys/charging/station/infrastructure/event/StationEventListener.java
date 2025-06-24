package com.ys.charging.station.infrastructure.event;

import com.ys.charging.station.domain.event.*;
import com.ys.charging.station.domain.service.ConnectorStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 充电站领域事件监听器
 * 
 * 处理充电站相关的领域事件，包括：
 * - 状态变更通知
 * - 缓存更新
 * - 外部系统集成
 * - 统计数据更新
 * 
 * @author yang
 * @since 2025-06-23
 */
@Component
public class StationEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(StationEventListener.class);
    
    private final ConnectorStateService stateService;
    
    public StationEventListener(ConnectorStateService stateService) {
        this.stateService = stateService;
    }
    
    /**
     * 处理充电站状态变更事件
     * 
     * @param event 状态变更事件
     */
    @Async
    @EventListener
    public void handleStationStatusChanged(StationStatusChangedEvent event) {
        logger.info("处理充电站状态变更事件: stationId={}, {} -> {}, reason={}", 
            event.stationId(), event.oldStatus(), event.newStatus(), event.reason());
        
        try {
            // 更新统计信息
            updateStationStatistics(event.stationId());
            
            // 如果是服务状态变更，发送通知
            if (event.isServiceStatusChange()) {
                sendServiceStatusNotification(event);
            }
            
            // 记录状态变更历史
            recordStationStatusHistory(event);
            
        } catch (Exception e) {
            logger.error("处理充电站状态变更事件失败: " + event.eventId(), e);
        }
    }
    
    /**
     * 处理充电桩状态变更事件
     * 
     * @param event 状态变更事件
     */
    @Async
    @EventListener
    public void handleConnectorStatusChanged(ConnectorStatusChangedEvent event) {
        logger.info("处理充电桩状态变更事件: connectorId={}, {} -> {}, reason={}", 
            event.connectorId(), event.oldStatus(), event.newStatus(), event.reason());
        
        try {
            // 更新状态缓存
            stateService.setCachedConnectorStatus(event.connectorId(), event.newStatus(), 3600);
            
            // 更新统计信息
            updateConnectorStatistics(event.stationId());
            
            // 如果是可用性变更，更新充电站统计
            if (event.isAvailabilityChange()) {
                updateStationAvailability(event.stationId());
            }
            
            // 如果是充电开始/结束，发送通知
            if (event.isChargingStarted() || event.isChargingEnded()) {
                sendChargingStatusNotification(event);
            }
            
        } catch (Exception e) {
            logger.error("处理充电桩状态变更事件失败: " + event.eventId(), e);
        }
    }
    
    /**
     * 处理充电桩预约事件
     * 
     * @param event 预约事件
     */
    @Async
    @EventListener
    public void handleConnectorReserved(ConnectorReservedEvent event) {
        logger.info("处理充电桩预约事件: connectorId={}, userId={}, expiresAt={}", 
            event.connectorId(), event.userId(), event.reservationExpiresAt());
        
        try {
            // 发送预约确认通知
            sendReservationConfirmation(event);
            
            // 更新统计信息
            updateConnectorStatistics(event.stationId());
            
            // 设置预约超时提醒
            scheduleReservationTimeoutReminder(event);
            
        } catch (Exception e) {
            logger.error("处理充电桩预约事件失败: " + event.eventId(), e);
        }
    }
    
    /**
     * 处理预约取消事件
     * 
     * @param event 预约取消事件
     */
    @Async
    @EventListener
    public void handleReservationCancelled(ConnectorReservationCancelledEvent event) {
        logger.info("处理预约取消事件: connectorId={}, userId={}, reason={}", 
            event.connectorId(), event.userId(), event.reason());
        
        try {
            // 发送取消通知
            sendReservationCancellationNotification(event);
            
            // 更新统计信息
            updateConnectorStatistics(event.stationId());
            
            // 取消超时提醒
            cancelReservationTimeoutReminder(event.connectorId());
            
        } catch (Exception e) {
            logger.error("处理预约取消事件失败: " + event.eventId(), e);
        }
    }
    
    /**
     * 处理充电桩故障事件
     * 
     * @param event 故障事件
     */
    @Async
    @EventListener
    public void handleConnectorFault(ConnectorFaultEvent event) {
        logger.warn("处理充电桩故障事件: connectorId={}, reason={}, severity={}", 
            event.connectorId(), event.faultReason(), event.severity());
        
        try {
            // 发送故障告警
            sendFaultAlert(event);
            
            // 更新统计信息
            updateConnectorStatistics(event.stationId());
            
            // 如果是严重故障，立即通知运维
            if (event.severity() == ConnectorFaultEvent.FaultSeverity.CRITICAL) {
                sendCriticalFaultAlert(event);
            }
            
        } catch (Exception e) {
            logger.error("处理充电桩故障事件失败: " + event.eventId(), e);
        }
    }
    
    /**
     * 处理充电桩修复事件
     * 
     * @param event 修复事件
     */
    @Async
    @EventListener
    public void handleConnectorRepaired(ConnectorRepairedEvent event) {
        logger.info("处理充电桩修复事件: connectorId={}, description={}, repairedBy={}", 
            event.connectorId(), event.repairDescription(), event.repairedBy());
        
        try {
            // 发送修复通知
            sendRepairNotification(event);
            
            // 更新统计信息
            updateConnectorStatistics(event.stationId());
            
            // 清除故障记录
            clearFaultRecord(event.connectorId());
            
        } catch (Exception e) {
            logger.error("处理充电桩修复事件失败: " + event.eventId(), e);
        }
    }
    
    // 私有辅助方法
    private void updateStationStatistics(Long stationId) {
        // 更新充电站统计信息
        logger.debug("更新充电站统计信息: stationId={}", stationId);
    }
    
    private void updateConnectorStatistics(Long stationId) {
        // 更新充电桩统计信息
        logger.debug("更新充电桩统计信息: stationId={}", stationId);
    }
    
    private void updateStationAvailability(Long stationId) {
        // 更新充电站可用性
        logger.debug("更新充电站可用性: stationId={}", stationId);
    }
    
    private void sendServiceStatusNotification(StationStatusChangedEvent event) {
        // 发送服务状态通知
        logger.debug("发送服务状态通知: {}", event.eventId());
    }
    
    private void sendChargingStatusNotification(ConnectorStatusChangedEvent event) {
        // 发送充电状态通知
        logger.debug("发送充电状态通知: {}", event.eventId());
    }
    
    private void sendReservationConfirmation(ConnectorReservedEvent event) {
        // 发送预约确认通知
        logger.debug("发送预约确认通知: {}", event.eventId());
    }
    
    private void sendReservationCancellationNotification(ConnectorReservationCancelledEvent event) {
        // 发送预约取消通知
        logger.debug("发送预约取消通知: {}", event.eventId());
    }
    
    private void sendFaultAlert(ConnectorFaultEvent event) {
        // 发送故障告警
        logger.debug("发送故障告警: {}", event.eventId());
    }
    
    private void sendCriticalFaultAlert(ConnectorFaultEvent event) {
        // 发送严重故障告警
        logger.warn("发送严重故障告警: {}", event.eventId());
    }
    
    private void sendRepairNotification(ConnectorRepairedEvent event) {
        // 发送修复通知
        logger.debug("发送修复通知: {}", event.eventId());
    }
    
    private void recordStationStatusHistory(StationStatusChangedEvent event) {
        // 记录充电站状态变更历史
        logger.debug("记录充电站状态历史: {}", event.eventId());
    }
    
    private void scheduleReservationTimeoutReminder(ConnectorReservedEvent event) {
        // 设置预约超时提醒
        logger.debug("设置预约超时提醒: {}", event.eventId());
    }
    
    private void cancelReservationTimeoutReminder(Long connectorId) {
        // 取消预约超时提醒
        logger.debug("取消预约超时提醒: connectorId={}", connectorId);
    }
    
    private void clearFaultRecord(Long connectorId) {
        // 清除故障记录
        logger.debug("清除故障记录: connectorId={}", connectorId);
    }
}
