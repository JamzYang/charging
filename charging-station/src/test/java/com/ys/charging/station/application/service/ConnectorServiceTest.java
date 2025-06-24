package com.ys.charging.station.application.service;

import com.ys.charging.station.TestBase;
import com.ys.charging.station.TestDataFactory;
import com.ys.charging.station.domain.model.*;
import com.ys.charging.station.domain.repository.ConnectorRepository;
import com.ys.charging.station.domain.repository.StationRepository;
import com.ys.charging.station.domain.service.ConnectorStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ConnectorService 单元测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("充电桩应用服务测试")
class ConnectorServiceTest extends TestBase {
    
    @Mock
    private ConnectorRepository connectorRepository;
    
    @Mock
    private StationRepository stationRepository;
    
    @Mock
    private ConnectorStateService stateService;
    
    private ConnectorService connectorService;
    
    @BeforeEach
    void setUp() {
        connectorService = new ConnectorService(connectorRepository, stationRepository, stateService);
    }
    
    @Nested
    @DisplayName("充电桩查询测试")
    class ConnectorQueryTest {
        
        @Test
        @DisplayName("应该能够根据ID获取充电桩")
        void shouldGetConnectorById() {
            // Given
            Connector expectedConnector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            setFieldValue(expectedConnector, "id", TEST_CONNECTOR_ID);
            
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(expectedConnector));
            
            // When
            Connector result = connectorService.getConnectorById(TEST_CONNECTOR_ID);
            
            // Then
            assertNotNull(result);
            assertEquals(TEST_CONNECTOR_ID, result.getId());
            
            verify(connectorRepository).findById(TEST_CONNECTOR_ID);
        }
        
        @Test
        @DisplayName("获取不存在的充电桩应该抛出异常")
        void shouldThrowExceptionWhenConnectorNotFound() {
            // Given
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.empty());
            
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                connectorService.getConnectorById(TEST_CONNECTOR_ID);
            });
            
            assertTrue(exception.getMessage().contains("充电桩不存在"));
        }
        
        @Test
        @DisplayName("应该能够获取充电站的所有充电桩")
        void shouldGetConnectorsByStationId() {
            // Given
            List<Connector> expectedConnectors = TestDataFactory.createConnectorsWithDifferentTypes(TEST_STATION_ID);
            
            when(connectorRepository.findByStationId(TEST_STATION_ID)).thenReturn(expectedConnectors);
            
            // When
            List<Connector> result = connectorService.getConnectorsByStationId(TEST_STATION_ID);
            
            // Then
            assertNotNull(result);
            assertEquals(5, result.size());
            
            verify(connectorRepository).findByStationId(TEST_STATION_ID);
        }
        
        @Test
        @DisplayName("应该能够获取可用的充电桩")
        void shouldGetAvailableConnectors() {
            // Given
            List<Connector> availableConnectors = List.of(
                TestDataFactory.createTestConnector(TEST_STATION_ID, "A01"),
                TestDataFactory.createTestConnector(TEST_STATION_ID, "A02")
            );
            
            when(connectorRepository.findAvailableConnectors(TEST_STATION_ID)).thenReturn(availableConnectors);
            
            // When
            List<Connector> result = connectorService.getAvailableConnectors(TEST_STATION_ID);
            
            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            
            verify(connectorRepository).findAvailableConnectors(TEST_STATION_ID);
        }
        
        @Test
        @DisplayName("应该能够根据状态分页查询充电桩")
        void shouldGetConnectorsByStatus() {
            // Given
            List<Connector> connectors = List.of(TestDataFactory.createTestConnector(TEST_STATION_ID));
            Page<Connector> expectedPage = new PageImpl<>(connectors);
            Pageable pageable = PageRequest.of(0, 10);
            
            when(connectorRepository.findByStatus(ConnectorStatus.IDLE, pageable)).thenReturn(expectedPage);
            
            // When
            Page<Connector> result = connectorService.getConnectorsByStatus(ConnectorStatus.IDLE, pageable);
            
            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            
            verify(connectorRepository).findByStatus(ConnectorStatus.IDLE, pageable);
        }
        
        @Test
        @DisplayName("应该能够获取用户预约的充电桩")
        void shouldGetUserReservedConnectors() {
            // Given
            List<Connector> reservedConnectors = List.of(
                TestDataFactory.createReservedConnector(TEST_STATION_ID, TEST_USER_ID, 30)
            );
            
            when(connectorRepository.findByReservedByUserId(TEST_USER_ID)).thenReturn(reservedConnectors);
            
            // When
            List<Connector> result = connectorService.getUserReservedConnectors(TEST_USER_ID);
            
            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            
            verify(connectorRepository).findByReservedByUserId(TEST_USER_ID);
        }
    }
    
    @Nested
    @DisplayName("充电桩预约测试")
    class ConnectorReservationTest {
        
        @Test
        @DisplayName("应该能够预约充电桩")
        void shouldReserveConnector() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            setFieldValue(station, "id", TEST_STATION_ID);
            Connector connector = station.getConnectors().get(0);
            setFieldValue(connector, "id", TEST_CONNECTOR_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(station));
            when(stationRepository.save(station)).thenReturn(station);
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(connector));
            
            // When
            Connector result = connectorService.reserveConnector(TEST_STATION_ID, TEST_CONNECTOR_ID, TEST_USER_ID, 30);
            
            // Then
            assertNotNull(result);
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(station);
            verify(stateService).setCachedConnectorStatus(TEST_CONNECTOR_ID, connector.getStatus(), 3600);
            verify(stateService).recordStatusChange(eq(TEST_CONNECTOR_ID), eq(ConnectorStatus.IDLE), 
                eq(ConnectorStatus.RESERVED), eq("用户预约"), eq(TEST_USER_ID));
        }
        
        @Test
        @DisplayName("应该能够取消预约")
        void shouldCancelReservation() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            setFieldValue(station, "id", TEST_STATION_ID);
            Connector connector = station.getConnectors().get(0);
            setFieldValue(connector, "id", TEST_CONNECTOR_ID);
            connector.reserve(TEST_USER_ID, 30);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(station));
            when(stationRepository.save(station)).thenReturn(station);
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(connector));
            
            // When
            Connector result = connectorService.cancelReservation(TEST_STATION_ID, TEST_CONNECTOR_ID, TEST_USER_ID, "用户取消");
            
            // Then
            assertNotNull(result);
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(station);
            verify(stateService).setCachedConnectorStatus(TEST_CONNECTOR_ID, connector.getStatus(), 3600);
            verify(stateService).recordStatusChange(eq(TEST_CONNECTOR_ID), eq(ConnectorStatus.RESERVED), 
                eq(ConnectorStatus.IDLE), eq("用户取消"), eq(TEST_USER_ID));
        }
    }
    
    @Nested
    @DisplayName("充电会话管理测试")
    class ChargingSessionTest {
        
        @Test
        @DisplayName("应该能够占用充电桩")
        void shouldOccupyConnector() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            setFieldValue(station, "id", TEST_STATION_ID);
            Connector connector = station.getConnectors().get(0);
            setFieldValue(connector, "id", TEST_CONNECTOR_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(station));
            when(stationRepository.save(station)).thenReturn(station);
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(connector));
            
            // When
            Connector result = connectorService.occupyConnector(TEST_STATION_ID, TEST_CONNECTOR_ID, TEST_USER_ID);
            
            // Then
            assertNotNull(result);
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(station);
            verify(stateService).setCachedConnectorStatus(TEST_CONNECTOR_ID, connector.getStatus(), 3600);
        }
        
        @Test
        @DisplayName("应该能够开始充电")
        void shouldStartCharging() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            setFieldValue(station, "id", TEST_STATION_ID);
            Connector connector = station.getConnectors().get(0);
            setFieldValue(connector, "id", TEST_CONNECTOR_ID);
            connector.occupy(TEST_USER_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(station));
            when(stationRepository.save(station)).thenReturn(station);
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(connector));
            
            // When
            Connector result = connectorService.startCharging(TEST_STATION_ID, TEST_CONNECTOR_ID);
            
            // Then
            assertNotNull(result);
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(station);
            verify(stateService).setCachedConnectorStatus(TEST_CONNECTOR_ID, connector.getStatus(), 3600);
        }
        
        @Test
        @DisplayName("应该能够停止充电")
        void shouldStopCharging() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            setFieldValue(station, "id", TEST_STATION_ID);
            Connector connector = station.getConnectors().get(0);
            setFieldValue(connector, "id", TEST_CONNECTOR_ID);
            connector.occupy(TEST_USER_ID);
            connector.startCharging();
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(station));
            when(stationRepository.save(station)).thenReturn(station);
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(connector));
            
            // When
            Connector result = connectorService.stopCharging(TEST_STATION_ID, TEST_CONNECTOR_ID);
            
            // Then
            assertNotNull(result);
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(station);
            verify(stateService).setCachedConnectorStatus(TEST_CONNECTOR_ID, connector.getStatus(), 3600);
        }
        
        @Test
        @DisplayName("应该能够释放充电桩")
        void shouldReleaseConnector() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            setFieldValue(station, "id", TEST_STATION_ID);
            Connector connector = station.getConnectors().get(0);
            setFieldValue(connector, "id", TEST_CONNECTOR_ID);
            connector.occupy(TEST_USER_ID);
            
            when(stationRepository.findById(TEST_STATION_ID)).thenReturn(Optional.of(station));
            when(stationRepository.save(station)).thenReturn(station);
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(connector));
            
            // When
            Connector result = connectorService.releaseConnector(TEST_STATION_ID, TEST_CONNECTOR_ID);
            
            // Then
            assertNotNull(result);
            
            verify(stationRepository).findById(TEST_STATION_ID);
            verify(stationRepository).save(station);
            verify(stateService).setCachedConnectorStatus(TEST_CONNECTOR_ID, connector.getStatus(), 3600);
        }
    }
    
    @Nested
    @DisplayName("故障管理测试")
    class FaultManagementTest {
        
        @Test
        @DisplayName("应该能够设置充电桩故障")
        void shouldSetConnectorFault() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            setFieldValue(connector, "id", TEST_CONNECTOR_ID);
            String faultReason = "电源故障";
            
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(connector));
            when(connectorRepository.save(connector)).thenReturn(connector);
            
            // When
            Connector result = connectorService.setConnectorFault(TEST_CONNECTOR_ID, faultReason);
            
            // Then
            assertNotNull(result);
            assertEquals(ConnectorStatus.FAULT, result.getStatus());
            assertEquals(faultReason, result.getFaultReason());
            
            verify(connectorRepository).findById(TEST_CONNECTOR_ID);
            verify(connectorRepository).save(connector);
            verify(stateService).setCachedConnectorStatus(TEST_CONNECTOR_ID, ConnectorStatus.FAULT, 3600);
            verify(stateService).recordStatusChange(eq(TEST_CONNECTOR_ID), eq(ConnectorStatus.IDLE), 
                eq(ConnectorStatus.FAULT), eq(faultReason), isNull());
        }
        
        @Test
        @DisplayName("应该能够修复充电桩故障")
        void shouldRepairConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            setFieldValue(connector, "id", TEST_CONNECTOR_ID);
            connector.setFault("电源故障");
            
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(connector));
            when(connectorRepository.save(connector)).thenReturn(connector);
            
            // When
            Connector result = connectorService.repairConnector(TEST_CONNECTOR_ID);
            
            // Then
            assertNotNull(result);
            assertEquals(ConnectorStatus.IDLE, result.getStatus());
            assertNull(result.getFaultReason());
            
            verify(connectorRepository).findById(TEST_CONNECTOR_ID);
            verify(connectorRepository).save(connector);
            verify(stateService).setCachedConnectorStatus(TEST_CONNECTOR_ID, ConnectorStatus.IDLE, 3600);
            verify(stateService).recordStatusChange(eq(TEST_CONNECTOR_ID), eq(ConnectorStatus.FAULT), 
                eq(ConnectorStatus.IDLE), eq("故障修复"), isNull());
        }
    }
    
    @Nested
    @DisplayName("心跳管理测试")
    class HeartbeatManagementTest {
        
        @Test
        @DisplayName("应该能够更新充电桩心跳")
        void shouldUpdateConnectorHeartbeat() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            setFieldValue(connector, "id", TEST_CONNECTOR_ID);
            
            when(connectorRepository.findById(TEST_CONNECTOR_ID)).thenReturn(Optional.of(connector));
            when(connectorRepository.save(connector)).thenReturn(connector);
            
            // When
            connectorService.updateConnectorHeartbeat(TEST_CONNECTOR_ID);
            
            // Then
            verify(connectorRepository).findById(TEST_CONNECTOR_ID);
            verify(connectorRepository).save(connector);
            verify(stateService).updateConnectorHeartbeat(eq(TEST_CONNECTOR_ID), any(Instant.class));
        }
    }
    
    @Nested
    @DisplayName("超时处理测试")
    class TimeoutHandlingTest {
        
        @Test
        @DisplayName("应该能够处理预约超时")
        void shouldHandleReservationTimeouts() {
            // Given
            int expectedHandledCount = 3;
            
            when(stateService.batchHandleReservationTimeouts(TEST_STATION_ID)).thenReturn(expectedHandledCount);
            
            // When
            int result = connectorService.handleReservationTimeouts(TEST_STATION_ID);
            
            // Then
            assertEquals(expectedHandledCount, result);
            
            verify(stateService).batchHandleReservationTimeouts(TEST_STATION_ID);
        }
        
        @Test
        @DisplayName("应该能够处理离线充电桩")
        void shouldHandleOfflineConnectors() {
            // Given
            int timeoutMinutes = 10;
            int expectedOfflineCount = 2;
            
            when(stateService.checkAndHandleOfflineConnectors(timeoutMinutes)).thenReturn(expectedOfflineCount);
            
            // When
            int result = connectorService.handleOfflineConnectors(timeoutMinutes);
            
            // Then
            assertEquals(expectedOfflineCount, result);
            
            verify(stateService).checkAndHandleOfflineConnectors(timeoutMinutes);
        }
    }
    
    @Nested
    @DisplayName("统计信息测试")
    class StatisticsTest {
        
        @Test
        @DisplayName("应该能够获取充电桩统计信息")
        void shouldGetConnectorStatistics() {
            // Given
            ConnectorRepository.ConnectorStatistics expectedStats = new ConnectorRepository.ConnectorStatistics(
                TEST_STATION_ID, 10L, 5L, 1L, 2L, 1L, 1L, 0L, 0L, 3L, 1L, 90.0, 40.0, 10.0
            );
            
            when(connectorRepository.getStatistics(TEST_STATION_ID)).thenReturn(expectedStats);
            
            // When
            ConnectorRepository.ConnectorStatistics result = connectorService.getConnectorStatistics(TEST_STATION_ID);
            
            // Then
            assertNotNull(result);
            assertEquals(TEST_STATION_ID, result.stationId());
            assertEquals(10L, result.totalConnectors());
            assertEquals(5L, result.idleConnectors());
            
            verify(connectorRepository).getStatistics(TEST_STATION_ID);
        }
    }
}
