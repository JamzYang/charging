package com.ys.charging.station.domain.service;

import com.ys.charging.station.TestBase;
import com.ys.charging.station.domain.model.ConnectorStatus;
import com.ys.charging.station.domain.repository.ConnectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ConnectorStateService 单元测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("充电桩状态服务测试")
class ConnectorStateServiceTest extends TestBase {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ConnectorRepository connectorRepository;
    
    private ConnectorStateService stateService;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        stateService = new RedisConnectorStateService(redisTemplate, connectorRepository);
    }
    
    @Nested
    @DisplayName("状态缓存管理测试")
    class StateCacheManagementTest {
        
        @Test
        @DisplayName("应该能够设置充电桩状态缓存")
        void shouldSetConnectorStatusCache() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            ConnectorStatus status = ConnectorStatus.CHARGING;
            int ttlSeconds = 3600;
            
            // When
            stateService.setCachedConnectorStatus(connectorId, status, ttlSeconds);
            
            // Then
            verify(valueOperations).set(eq("connector:status:" + connectorId), eq(status.name()), 
                eq(java.time.Duration.ofSeconds(ttlSeconds)));
        }
        
        @Test
        @DisplayName("应该能够获取缓存的充电桩状态")
        void shouldGetCachedConnectorStatus() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            ConnectorStatus expectedStatus = ConnectorStatus.IDLE;
            
            when(valueOperations.get("connector:status:" + connectorId)).thenReturn(expectedStatus.name());
            
            // When
            Optional<ConnectorStatus> result = stateService.getCachedConnectorStatus(connectorId);
            
            // Then
            assertTrue(result.isPresent());
            assertEquals(expectedStatus, result.get());
            
            verify(valueOperations).get("connector:status:" + connectorId);
        }
        
        @Test
        @DisplayName("获取不存在的缓存状态应该返回空")
        void shouldReturnEmptyForNonExistentCachedStatus() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            
            when(valueOperations.get("connector:status:" + connectorId)).thenReturn(null);
            
            // When
            Optional<ConnectorStatus> result = stateService.getCachedConnectorStatus(connectorId);
            
            // Then
            assertFalse(result.isPresent());
        }
        
        @Test
        @DisplayName("应该能够清除充电桩状态缓存")
        void shouldClearConnectorStatusCache() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            
            when(redisTemplate.delete("connector:status:" + connectorId)).thenReturn(true);
            
            // When
            boolean result = stateService.clearCachedConnectorStatus(connectorId);
            
            // Then
            assertTrue(result);
            
            verify(redisTemplate).delete("connector:status:" + connectorId);
        }
    }
    
    @Nested
    @DisplayName("状态变更记录测试")
    class StatusChangeRecordTest {
        
        @Test
        @DisplayName("应该能够记录状态变更")
        void shouldRecordStatusChange() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            ConnectorStatus fromStatus = ConnectorStatus.IDLE;
            ConnectorStatus toStatus = ConnectorStatus.RESERVED;
            String reason = "用户预约";
            Long userId = TEST_USER_ID;
            
            // When
            stateService.recordStatusChange(connectorId, fromStatus, toStatus, reason, userId);
            
            // Then
            verify(valueOperations).set(eq("connector:history:" + connectorId), anyString(), 
                eq(java.time.Duration.ofDays(30)));
        }
        
        @Test
        @DisplayName("应该能够获取状态变更历史")
        void shouldGetStatusChangeHistory() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            String mockHistory = "2025-06-23T10:00:00Z|IDLE|RESERVED|用户预约|1001";
            
            when(valueOperations.get("connector:history:" + connectorId)).thenReturn(mockHistory);
            
            // When
            List<String> result = stateService.getStatusChangeHistory(connectorId);
            
            // Then
            assertNotNull(result);
            assertFalse(result.isEmpty());
            
            verify(valueOperations).get("connector:history:" + connectorId);
        }
    }
    
    @Nested
    @DisplayName("心跳管理测试")
    class HeartbeatManagementTest {
        
        @Test
        @DisplayName("应该能够更新充电桩心跳时间")
        void shouldUpdateConnectorHeartbeat() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            Instant heartbeatTime = Instant.now();
            
            // When
            stateService.updateConnectorHeartbeat(connectorId, heartbeatTime);
            
            // Then
            verify(valueOperations).set(eq("connector:heartbeat:" + connectorId), 
                eq(heartbeatTime.toString()), eq(java.time.Duration.ofHours(1)));
        }
        
        @Test
        @DisplayName("应该能够获取充电桩最后心跳时间")
        void shouldGetLastHeartbeatTime() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            Instant expectedTime = Instant.now();
            
            when(valueOperations.get("connector:heartbeat:" + connectorId)).thenReturn(expectedTime.toString());
            
            // When
            Optional<Instant> result = stateService.getLastHeartbeatTime(connectorId);
            
            // Then
            assertTrue(result.isPresent());
            assertEquals(expectedTime, result.get());
            
            verify(valueOperations).get("connector:heartbeat:" + connectorId);
        }
        
        @Test
        @DisplayName("应该能够检查心跳超时")
        void shouldCheckHeartbeatTimeout() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            int timeoutMinutes = 5;
            Instant oldHeartbeat = Instant.now().minusSeconds(600); // 10分钟前
            
            when(valueOperations.get("connector:heartbeat:" + connectorId)).thenReturn(oldHeartbeat.toString());
            
            // When
            boolean result = stateService.isHeartbeatTimeout(connectorId, timeoutMinutes);
            
            // Then
            assertTrue(result); // 10分钟前的心跳，5分钟超时，应该超时
        }
        
        @Test
        @DisplayName("正常心跳不应该超时")
        void shouldNotTimeoutForRecentHeartbeat() {
            // Given
            Long connectorId = TEST_CONNECTOR_ID;
            int timeoutMinutes = 5;
            Instant recentHeartbeat = Instant.now().minusSeconds(60); // 1分钟前
            
            when(valueOperations.get("connector:heartbeat:" + connectorId)).thenReturn(recentHeartbeat.toString());
            
            // When
            boolean result = stateService.isHeartbeatTimeout(connectorId, timeoutMinutes);
            
            // Then
            assertFalse(result); // 1分钟前的心跳，5分钟超时，不应该超时
        }
    }
    
    @Nested
    @DisplayName("批量超时处理测试")
    class BatchTimeoutHandlingTest {
        
        @Test
        @DisplayName("应该能够批量处理预约超时")
        void shouldBatchHandleReservationTimeouts() {
            // Given
            Long stationId = TEST_STATION_ID;
            
            // Mock 数据库查询返回过期预约
            when(connectorRepository.findExpiredReservations(any(Instant.class)))
                .thenReturn(List.of(
                    createExpiredReservationConnector(1L),
                    createExpiredReservationConnector(2L)
                ));
            
            when(connectorRepository.batchUpdateStatus(anyList(), eq(ConnectorStatus.IDLE)))
                .thenReturn(2);
            
            // When
            int result = stateService.batchHandleReservationTimeouts(stationId);
            
            // Then
            assertEquals(2, result);
            
            verify(connectorRepository).findExpiredReservations(any(Instant.class));
            verify(connectorRepository).batchUpdateStatus(anyList(), eq(ConnectorStatus.IDLE));
        }
        
        @Test
        @DisplayName("应该能够检查并处理离线充电桩")
        void shouldCheckAndHandleOfflineConnectors() {
            // Given
            int timeoutMinutes = 10;
            
            // Mock 数据库查询返回心跳超时的充电桩
            when(connectorRepository.findHeartbeatTimeoutConnectors(any(Instant.class)))
                .thenReturn(List.of(
                    createTimeoutConnector(1L),
                    createTimeoutConnector(2L)
                ));
            
            when(connectorRepository.batchUpdateStatus(anyList(), eq(ConnectorStatus.OFFLINE)))
                .thenReturn(2);
            
            // When
            int result = stateService.checkAndHandleOfflineConnectors(timeoutMinutes);
            
            // Then
            assertEquals(2, result);
            
            verify(connectorRepository).findHeartbeatTimeoutConnectors(any(Instant.class));
            verify(connectorRepository).batchUpdateStatus(anyList(), eq(ConnectorStatus.OFFLINE));
        }
    }
    
    @Nested
    @DisplayName("状态统计测试")
    class StatusStatisticsTest {
        
        @Test
        @DisplayName("应该能够获取充电桩状态统计")
        void shouldGetConnectorStatusStatistics() {
            // Given
            Long stationId = TEST_STATION_ID;
            
            // Mock 仓储返回统计数据
            when(connectorRepository.getStatistics(stationId))
                .thenReturn(new ConnectorRepository.ConnectorStatistics(
                    stationId, 10L, 5L, 1L, 2L, 1L, 1L, 0L, 0L, 3L, 1L, 90.0, 40.0, 10.0
                ));
            
            // When
            ConnectorStateService.ConnectorStatusStatistics result = 
                stateService.getConnectorStatusStatistics(stationId);
            
            // Then
            assertNotNull(result);
            assertEquals(stationId, result.stationId());
            assertEquals(10L, result.totalConnectors());
            assertEquals(5L, result.idleConnectors());
            
            verify(connectorRepository).getStatistics(stationId);
        }
        
        @Test
        @DisplayName("应该能够检查状态流转的有效性")
        void shouldValidateStatusTransition() {
            // When & Then
            // 有效的状态流转
            assertTrue(stateService.isValidStatusTransition(ConnectorStatus.IDLE, ConnectorStatus.RESERVED));
            assertTrue(stateService.isValidStatusTransition(ConnectorStatus.RESERVED, ConnectorStatus.OCCUPIED));
            assertTrue(stateService.isValidStatusTransition(ConnectorStatus.OCCUPIED, ConnectorStatus.CHARGING));
            assertTrue(stateService.isValidStatusTransition(ConnectorStatus.CHARGING, ConnectorStatus.OCCUPIED));
            assertTrue(stateService.isValidStatusTransition(ConnectorStatus.OCCUPIED, ConnectorStatus.IDLE));
            
            // 无效的状态流转
            assertFalse(stateService.isValidStatusTransition(ConnectorStatus.IDLE, ConnectorStatus.CHARGING));
            assertFalse(stateService.isValidStatusTransition(ConnectorStatus.CHARGING, ConnectorStatus.RESERVED));
            assertFalse(stateService.isValidStatusTransition(ConnectorStatus.FAULT, ConnectorStatus.CHARGING));
        }
    }
    
    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTest {
        
        @Test
        @DisplayName("Redis 异常时应该优雅处理")
        void shouldHandleRedisException() {
            // Given
            when(valueOperations.set(anyString(), any(), any(java.time.Duration.class)))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("Redis连接失败"));
            
            // When & Then
            assertDoesNotThrow(() -> {
                stateService.setCachedConnectorStatus(TEST_CONNECTOR_ID, ConnectorStatus.IDLE, 3600);
            });
        }
        
        @Test
        @DisplayName("无效的状态转换应该被拒绝")
        void shouldRejectInvalidStatusTransition() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                stateService.recordStatusChange(TEST_CONNECTOR_ID, ConnectorStatus.IDLE, 
                    ConnectorStatus.CHARGING, "无效转换", TEST_USER_ID);
            });
        }
    }
    
    // 辅助方法
    private com.ys.charging.station.domain.model.Connector createExpiredReservationConnector(Long id) {
        var connector = com.ys.charging.station.TestDataFactory.createTestConnector(TEST_STATION_ID);
        setFieldValue(connector, "id", id);
        setFieldValue(connector, "status", ConnectorStatus.RESERVED);
        setFieldValue(connector, "reservationExpiresAt", Instant.now().minusSeconds(3600));
        return connector;
    }
    
    private com.ys.charging.station.domain.model.Connector createTimeoutConnector(Long id) {
        var connector = com.ys.charging.station.TestDataFactory.createTestConnector(TEST_STATION_ID);
        setFieldValue(connector, "id", id);
        setFieldValue(connector, "lastHeartbeatAt", Instant.now().minusSeconds(1200)); // 20分钟前
        return connector;
    }
}
