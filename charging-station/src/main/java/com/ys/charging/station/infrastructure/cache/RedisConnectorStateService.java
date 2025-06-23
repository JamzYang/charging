package com.ys.charging.station.infrastructure.cache;

import com.ys.charging.station.domain.model.ConnectorStatus;
import com.ys.charging.station.domain.service.ConnectorStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis 充电桩状态管理服务实现
 * 
 * 使用 Redis 提供高性能的充电桩状态缓存和管理功能。
 * 支持状态流转验证、超时处理、心跳监控等核心功能。
 * 
 * Redis 数据结构：
 * - connector:status:{id} - 充电桩状态缓存
 * - connector:heartbeat:{id} - 充电桩心跳时间
 * - connector:reservation:{id} - 预约信息
 * - connector:history:{id} - 状态变更历史
 * - station:statistics:{id} - 充电站统计信息
 * 
 * @author yang
 * @since 2025-06-23
 */
@Service
public class RedisConnectorStateService implements ConnectorStateService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisConnectorStateService.class);
    
    private static final String CONNECTOR_STATUS_KEY_PREFIX = "connector:status:";
    private static final String CONNECTOR_HEARTBEAT_KEY_PREFIX = "connector:heartbeat:";
    private static final String CONNECTOR_RESERVATION_KEY_PREFIX = "connector:reservation:";
    private static final String CONNECTOR_HISTORY_KEY_PREFIX = "connector:history:";
    private static final String STATION_STATISTICS_KEY_PREFIX = "station:statistics:";
    private static final String GLOBAL_STATISTICS_KEY = "global:connector:statistics";
    
    private static final long DEFAULT_STATUS_CACHE_TTL = 3600; // 1小时
    private static final int MAX_HISTORY_SIZE = 100; // 最大历史记录数
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 状态转换矩阵
    private static final Map<ConnectorStatus, Set<ConnectorStatus>> STATE_TRANSITION_MATRIX = Map.of(
        ConnectorStatus.IDLE, Set.of(ConnectorStatus.RESERVED, ConnectorStatus.OCCUPIED, 
                                    ConnectorStatus.FAULT, ConnectorStatus.OFFLINE, ConnectorStatus.MAINTENANCE),
        ConnectorStatus.RESERVED, Set.of(ConnectorStatus.OCCUPIED, ConnectorStatus.IDLE, 
                                        ConnectorStatus.FAULT, ConnectorStatus.OFFLINE),
        ConnectorStatus.OCCUPIED, Set.of(ConnectorStatus.CHARGING, ConnectorStatus.IDLE, 
                                        ConnectorStatus.FAULT, ConnectorStatus.OFFLINE),
        ConnectorStatus.CHARGING, Set.of(ConnectorStatus.OCCUPIED, ConnectorStatus.FAULT, 
                                        ConnectorStatus.OFFLINE),
        ConnectorStatus.FAULT, Set.of(ConnectorStatus.IDLE, ConnectorStatus.MAINTENANCE),
        ConnectorStatus.OFFLINE, Set.of(ConnectorStatus.IDLE, ConnectorStatus.FAULT),
        ConnectorStatus.MAINTENANCE, Set.of(ConnectorStatus.IDLE, ConnectorStatus.FAULT)
    );
    
    public RedisConnectorStateService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public boolean isValidStateTransition(ConnectorStatus currentStatus, ConnectorStatus targetStatus) {
        if (currentStatus == null || targetStatus == null) {
            return false;
        }
        
        if (currentStatus == targetStatus) {
            return true; // 相同状态总是有效的
        }
        
        Set<ConnectorStatus> validNextStates = STATE_TRANSITION_MATRIX.get(currentStatus);
        return validNextStates != null && validNextStates.contains(targetStatus);
    }
    
    @Override
    public List<ConnectorStatus> getValidNextStates(ConnectorStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptyList();
        }
        
        Set<ConnectorStatus> validStates = STATE_TRANSITION_MATRIX.get(currentStatus);
        return validStates != null ? new ArrayList<>(validStates) : Collections.emptyList();
    }
    
    @Override
    public boolean handleReservationTimeout(Long connectorId) {
        if (connectorId == null) {
            return false;
        }
        
        try {
            String reservationKey = CONNECTOR_RESERVATION_KEY_PREFIX + connectorId;
            Map<Object, Object> reservation = redisTemplate.opsForHash().entries(reservationKey);
            
            if (reservation.isEmpty()) {
                return false; // 没有预约信息
            }
            
            String expiresAtStr = (String) reservation.get("expiresAt");
            if (expiresAtStr == null) {
                return false;
            }
            
            Instant expiresAt = Instant.parse(expiresAtStr);
            if (Instant.now().isAfter(expiresAt)) {
                // 预约已超时，清除预约信息并更新状态
                redisTemplate.delete(reservationKey);
                setCachedConnectorStatus(connectorId, ConnectorStatus.IDLE, DEFAULT_STATUS_CACHE_TTL);
                
                // 记录状态变更
                recordStatusChange(connectorId, ConnectorStatus.RESERVED, ConnectorStatus.IDLE, 
                    "预约超时", null);
                
                logger.info("处理充电桩预约超时: connectorId={}", connectorId);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("处理预约超时失败: connectorId=" + connectorId, e);
            return false;
        }
    }
    
    @Override
    public int batchHandleReservationTimeouts(Long stationId) {
        try {
            String pattern = CONNECTOR_RESERVATION_KEY_PREFIX + "*";
            Set<String> reservationKeys = redisTemplate.keys(pattern);
            
            if (reservationKeys == null || reservationKeys.isEmpty()) {
                return 0;
            }
            
            int handledCount = 0;
            for (String key : reservationKeys) {
                try {
                    String connectorIdStr = key.substring(CONNECTOR_RESERVATION_KEY_PREFIX.length());
                    Long connectorId = Long.parseLong(connectorIdStr);
                    
                    if (handleReservationTimeout(connectorId)) {
                        handledCount++;
                    }
                    
                } catch (NumberFormatException e) {
                    logger.warn("无效的充电桩ID: {}", key);
                }
            }
            
            logger.info("批量处理预约超时完成: 处理了 {} 个超时预约", handledCount);
            return handledCount;
            
        } catch (Exception e) {
            logger.error("批量处理预约超时失败", e);
            return 0;
        }
    }
    
    @Override
    public void updateConnectorHeartbeat(Long connectorId, Instant heartbeatTime) {
        if (connectorId == null || heartbeatTime == null) {
            return;
        }
        
        try {
            String heartbeatKey = CONNECTOR_HEARTBEAT_KEY_PREFIX + connectorId;
            redisTemplate.opsForValue().set(heartbeatKey, heartbeatTime.toString(), 
                Duration.ofHours(2)); // 2小时过期
            
            logger.debug("更新充电桩心跳: connectorId={}, heartbeat={}", connectorId, heartbeatTime);
            
        } catch (Exception e) {
            logger.error("更新充电桩心跳失败: connectorId=" + connectorId, e);
        }
    }
    
    @Override
    public int checkAndHandleOfflineConnectors(int timeoutMinutes) {
        try {
            String pattern = CONNECTOR_HEARTBEAT_KEY_PREFIX + "*";
            Set<String> heartbeatKeys = redisTemplate.keys(pattern);
            
            if (heartbeatKeys == null || heartbeatKeys.isEmpty()) {
                return 0;
            }
            
            Instant timeoutThreshold = Instant.now().minusSeconds(timeoutMinutes * 60L);
            int offlineCount = 0;
            
            for (String key : heartbeatKeys) {
                try {
                    String heartbeatStr = (String) redisTemplate.opsForValue().get(key);
                    if (heartbeatStr == null) {
                        continue;
                    }
                    
                    Instant lastHeartbeat = Instant.parse(heartbeatStr);
                    if (lastHeartbeat.isBefore(timeoutThreshold)) {
                        // 充电桩离线
                        String connectorIdStr = key.substring(CONNECTOR_HEARTBEAT_KEY_PREFIX.length());
                        Long connectorId = Long.parseLong(connectorIdStr);
                        
                        // 检查当前状态，避免重复设置
                        Optional<ConnectorStatus> currentStatus = getCachedConnectorStatus(connectorId);
                        if (currentStatus.isEmpty() || currentStatus.get() != ConnectorStatus.OFFLINE) {
                            setCachedConnectorStatus(connectorId, ConnectorStatus.OFFLINE, DEFAULT_STATUS_CACHE_TTL);
                            recordStatusChange(connectorId, currentStatus.orElse(ConnectorStatus.IDLE), 
                                ConnectorStatus.OFFLINE, "心跳超时", null);
                            offlineCount++;
                        }
                    }
                    
                } catch (Exception e) {
                    logger.warn("处理心跳检查失败: key={}", key, e);
                }
            }
            
            if (offlineCount > 0) {
                logger.warn("检测到 {} 个充电桩离线", offlineCount);
            }
            
            return offlineCount;
            
        } catch (Exception e) {
            logger.error("检查离线充电桩失败", e);
            return 0;
        }
    }
    
    @Override
    public ConnectorStatusStatistics getConnectorStatusStatistics(Long stationId) {
        // 这里需要查询数据库获取充电站的充电桩列表，然后统计状态
        // 暂时返回模拟数据
        return new ConnectorStatusStatistics(
            stationId, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, Instant.now()
        );
    }
    
    @Override
    public ConnectorStatusStatistics getGlobalConnectorStatusStatistics() {
        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(GLOBAL_STATISTICS_KEY);
            
            if (stats.isEmpty()) {
                return new ConnectorStatusStatistics(
                    null, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, Instant.now()
                );
            }
            
            // 从缓存中构建统计信息
            int total = getIntValue(stats, "total");
            int idle = getIntValue(stats, "idle");
            int reserved = getIntValue(stats, "reserved");
            int occupied = getIntValue(stats, "occupied");
            int charging = getIntValue(stats, "charging");
            int fault = getIntValue(stats, "fault");
            int offline = getIntValue(stats, "offline");
            int maintenance = getIntValue(stats, "maintenance");
            
            double availabilityRate = total > 0 ? (double)(total - fault - offline - maintenance) / total * 100 : 0.0;
            double utilizationRate = total > 0 ? (double)(reserved + occupied + charging) / total * 100 : 0.0;
            
            return new ConnectorStatusStatistics(
                null, total, idle, reserved, occupied, charging, fault, offline, maintenance,
                availabilityRate, utilizationRate, Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("获取全局充电桩状态统计失败", e);
            return new ConnectorStatusStatistics(
                null, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, Instant.now()
            );
        }
    }
    
    @Override
    public void refreshConnectorStatusCache(Long connectorId) {
        // 这里需要从数据库查询最新状态并更新缓存
        // 暂时跳过实现
        logger.debug("刷新充电桩状态缓存: connectorId={}", connectorId);
    }
    
    @Override
    public void batchRefreshConnectorStatusCache(List<Long> connectorIds) {
        if (connectorIds == null || connectorIds.isEmpty()) {
            return;
        }
        
        for (Long connectorId : connectorIds) {
            refreshConnectorStatusCache(connectorId);
        }
        
        logger.info("批量刷新充电桩状态缓存完成: {} 个充电桩", connectorIds.size());
    }
    
    @Override
    public Optional<ConnectorStatus> getCachedConnectorStatus(Long connectorId) {
        if (connectorId == null) {
            return Optional.empty();
        }
        
        try {
            String statusKey = CONNECTOR_STATUS_KEY_PREFIX + connectorId;
            String statusStr = (String) redisTemplate.opsForValue().get(statusKey);
            
            if (statusStr == null) {
                return Optional.empty();
            }
            
            return Optional.of(ConnectorStatus.valueOf(statusStr));
            
        } catch (Exception e) {
            logger.error("获取缓存的充电桩状态失败: connectorId=" + connectorId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void setCachedConnectorStatus(Long connectorId, ConnectorStatus status, long ttlSeconds) {
        if (connectorId == null || status == null) {
            return;
        }
        
        try {
            String statusKey = CONNECTOR_STATUS_KEY_PREFIX + connectorId;
            redisTemplate.opsForValue().set(statusKey, status.name(), Duration.ofSeconds(ttlSeconds));
            
            logger.debug("设置充电桩状态缓存: connectorId={}, status={}, ttl={}s", 
                connectorId, status, ttlSeconds);
            
        } catch (Exception e) {
            logger.error("设置充电桩状态缓存失败: connectorId=" + connectorId, e);
        }
    }
    
    @Override
    public void clearConnectorStatusCache(Long connectorId) {
        if (connectorId == null) {
            return;
        }
        
        try {
            String statusKey = CONNECTOR_STATUS_KEY_PREFIX + connectorId;
            redisTemplate.delete(statusKey);
            
            logger.debug("清除充电桩状态缓存: connectorId={}", connectorId);
            
        } catch (Exception e) {
            logger.error("清除充电桩状态缓存失败: connectorId=" + connectorId, e);
        }
    }
    
    @Override
    public List<ConnectorStatusHistory> getConnectorStatusHistory(Long connectorId, int limit) {
        if (connectorId == null || limit <= 0) {
            return Collections.emptyList();
        }
        
        try {
            String historyKey = CONNECTOR_HISTORY_KEY_PREFIX + connectorId;
            List<Object> historyData = redisTemplate.opsForList().range(historyKey, 0, limit - 1);
            
            if (historyData == null || historyData.isEmpty()) {
                return Collections.emptyList();
            }
            
            return historyData.stream()
                .map(data -> {
                    try {
                        // 这里需要反序列化历史记录
                        // 暂时返回空记录
                        return new ConnectorStatusHistory(
                            connectorId, ConnectorStatus.IDLE, ConnectorStatus.IDLE, 
                            "", null, Instant.now()
                        );
                    } catch (Exception e) {
                        logger.warn("解析状态历史记录失败", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("获取充电桩状态历史失败: connectorId=" + connectorId, e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public void recordStatusChange(Long connectorId, ConnectorStatus oldStatus, ConnectorStatus newStatus, 
                                  String reason, Long userId) {
        if (connectorId == null || oldStatus == null || newStatus == null) {
            return;
        }
        
        try {
            ConnectorStatusHistory history = new ConnectorStatusHistory(
                connectorId, oldStatus, newStatus, reason, userId, Instant.now()
            );
            
            String historyKey = CONNECTOR_HISTORY_KEY_PREFIX + connectorId;
            
            // 将历史记录序列化并添加到列表头部
            Map<String, Object> historyData = Map.of(
                "oldStatus", oldStatus.name(),
                "newStatus", newStatus.name(),
                "reason", reason != null ? reason : "",
                "userId", userId != null ? userId.toString() : "",
                "changedAt", history.changedAt().toString()
            );
            
            redisTemplate.opsForList().leftPush(historyKey, historyData);
            
            // 限制历史记录数量
            redisTemplate.opsForList().trim(historyKey, 0, MAX_HISTORY_SIZE - 1);
            
            // 设置过期时间（30天）
            redisTemplate.expire(historyKey, Duration.ofDays(30));
            
            logger.debug("记录充电桩状态变更: connectorId={}, {} → {}, reason={}", 
                connectorId, oldStatus, newStatus, reason);
            
        } catch (Exception e) {
            logger.error("记录充电桩状态变更失败: connectorId=" + connectorId, e);
        }
    }
    
    // 私有辅助方法
    private int getIntValue(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
