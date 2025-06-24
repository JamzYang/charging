package com.ys.charging.station.domain.model;

import com.ys.charging.station.TestBase;
import com.ys.charging.station.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Connector 实体单元测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@DisplayName("充电桩实体测试")
class ConnectorTest extends TestBase {
    
    @Nested
    @DisplayName("充电桩创建测试")
    class ConnectorCreationTest {
        
        @Test
        @DisplayName("应该能够创建有效的充电桩")
        void shouldCreateValidConnector() {
            // When
            Connector connector = Connector.create(TEST_STATION_ID, testConnectorInfo, TEST_PARKING_SPOT_ID);
            
            // Then
            assertNotNull(connector);
            assertEquals(TEST_STATION_ID, connector.getStationId());
            assertEquals(testConnectorInfo, connector.getConnectorInfo());
            assertEquals(TEST_PARKING_SPOT_ID, connector.getParkingSpotId());
            assertEquals(ConnectorStatus.IDLE, connector.getStatus());
            assertNull(connector.getReservedByUserId());
            assertNull(connector.getReservedAt());
            assertNull(connector.getReservationExpiresAt());
            assertNull(connector.getFaultReason());
            assertNotNull(connector.getCreatedAt());
            assertNotNull(connector.getUpdatedAt());
        }
        
        @Test
        @DisplayName("创建充电桩时不能传入空的充电站ID")
        void shouldThrowExceptionWhenStationIdIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Connector.create(null, testConnectorInfo, TEST_PARKING_SPOT_ID);
            });
        }
        
        @Test
        @DisplayName("创建充电桩时不能传入空的充电桩信息")
        void shouldThrowExceptionWhenConnectorInfoIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Connector.create(TEST_STATION_ID, null, TEST_PARKING_SPOT_ID);
            });
        }
    }
    
    @Nested
    @DisplayName("充电桩状态管理测试")
    class ConnectorStatusManagementTest {
        
        @Test
        @DisplayName("应该能够检查充电桩是否可用")
        void shouldCheckIfConnectorIsAvailable() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When & Then
            assertTrue(connector.isAvailable());
            
            // 设置为故障状态
            connector.setFault("测试故障");
            assertFalse(connector.isAvailable());
        }
        
        @Test
        @DisplayName("应该能够检查充电桩是否可预约")
        void shouldCheckIfConnectorIsReservable() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When & Then
            assertTrue(connector.isReservable());
            
            // 预约后不可再预约
            connector.reserve(TEST_USER_ID, 30);
            assertFalse(connector.isReservable());
        }
        
        @Test
        @DisplayName("应该能够检查充电桩是否使用中")
        void shouldCheckIfConnectorIsInUse() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When & Then
            assertFalse(connector.isInUse());
            
            // 预约后为使用中
            connector.reserve(TEST_USER_ID, 30);
            assertTrue(connector.isInUse());
            
            // 占用后仍为使用中
            connector.occupy(TEST_USER_ID);
            assertTrue(connector.isInUse());
            
            // 开始充电后仍为使用中
            connector.startCharging();
            assertTrue(connector.isInUse());
        }
    }
    
    @Nested
    @DisplayName("充电桩预约测试")
    class ConnectorReservationTest {
        
        @Test
        @DisplayName("应该能够预约充电桩")
        void shouldReserveConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When
            connector.reserve(TEST_USER_ID, 30);
            
            // Then
            assertEquals(ConnectorStatus.RESERVED, connector.getStatus());
            assertEquals(TEST_USER_ID, connector.getReservedByUserId());
            assertNotNull(connector.getReservedAt());
            assertNotNull(connector.getReservationExpiresAt());
            assertTrue(connector.getReservationExpiresAt().isAfter(Instant.now()));
        }
        
        @Test
        @DisplayName("预约时不能传入空的用户ID")
        void shouldThrowExceptionWhenUserIdIsNull() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                connector.reserve(null, 30);
            });
        }
        
        @Test
        @DisplayName("预约时间必须大于0")
        void shouldThrowExceptionWhenReservationTimeIsInvalid() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                connector.reserve(TEST_USER_ID, 0);
            });
            
            assertThrows(IllegalArgumentException.class, () -> {
                connector.reserve(TEST_USER_ID, -10);
            });
        }
        
        @Test
        @DisplayName("已预约的充电桩不能再次预约")
        void shouldThrowExceptionWhenReservingAlreadyReservedConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.reserve(TEST_USER_ID, 30);
            
            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                connector.reserve(TEST_USER_ID + 1, 30);
            });
        }
        
        @Test
        @DisplayName("应该能够取消预约")
        void shouldCancelReservation() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.reserve(TEST_USER_ID, 30);
            
            // When
            connector.cancelReservation(TEST_USER_ID, "用户取消");
            
            // Then
            assertEquals(ConnectorStatus.IDLE, connector.getStatus());
            assertNull(connector.getReservedByUserId());
            assertNull(connector.getReservedAt());
            assertNull(connector.getReservationExpiresAt());
        }
        
        @Test
        @DisplayName("非预约用户不能取消预约")
        void shouldThrowExceptionWhenNonReservingUserCancelsReservation() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.reserve(TEST_USER_ID, 30);
            
            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                connector.cancelReservation(TEST_USER_ID + 1, "其他用户尝试取消");
            });
        }
        
        @Test
        @DisplayName("应该能够检查预约是否过期")
        void shouldCheckIfReservationIsExpired() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.reserve(TEST_USER_ID, 30);
            
            // When & Then
            assertFalse(connector.isReservationExpired());
            
            // 设置过期时间为过去
            setFieldValue(connector, "reservationExpiresAt", Instant.now().minusSeconds(3600));
            assertTrue(connector.isReservationExpired());
        }
    }
    
    @Nested
    @DisplayName("充电会话测试")
    class ChargingSessionTest {
        
        @Test
        @DisplayName("应该能够占用充电桩")
        void shouldOccupyConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When
            connector.occupy(TEST_USER_ID);
            
            // Then
            assertEquals(ConnectorStatus.OCCUPIED, connector.getStatus());
        }
        
        @Test
        @DisplayName("预约用户可以占用充电桩")
        void shouldAllowReservingUserToOccupyConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.reserve(TEST_USER_ID, 30);
            
            // When
            connector.occupy(TEST_USER_ID);
            
            // Then
            assertEquals(ConnectorStatus.OCCUPIED, connector.getStatus());
            assertNull(connector.getReservedByUserId());
            assertNull(connector.getReservedAt());
            assertNull(connector.getReservationExpiresAt());
        }
        
        @Test
        @DisplayName("应该能够开始充电")
        void shouldStartCharging() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.occupy(TEST_USER_ID);
            
            // When
            connector.startCharging();
            
            // Then
            assertEquals(ConnectorStatus.CHARGING, connector.getStatus());
        }
        
        @Test
        @DisplayName("只有占用状态的充电桩才能开始充电")
        void shouldOnlyStartChargingFromOccupiedStatus() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                connector.startCharging();
            });
        }
        
        @Test
        @DisplayName("应该能够停止充电")
        void shouldStopCharging() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.occupy(TEST_USER_ID);
            connector.startCharging();
            
            // When
            connector.stopCharging();
            
            // Then
            assertEquals(ConnectorStatus.OCCUPIED, connector.getStatus());
        }
        
        @Test
        @DisplayName("应该能够释放充电桩")
        void shouldReleaseConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.occupy(TEST_USER_ID);
            
            // When
            connector.release();
            
            // Then
            assertEquals(ConnectorStatus.IDLE, connector.getStatus());
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
            String faultReason = "电源故障";
            
            // When
            connector.setFault(faultReason);
            
            // Then
            assertEquals(ConnectorStatus.FAULT, connector.getStatus());
            assertEquals(faultReason, connector.getFaultReason());
        }
        
        @Test
        @DisplayName("设置故障时不能传入空的故障原因")
        void shouldThrowExceptionWhenFaultReasonIsNull() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                connector.setFault(null);
            });
            
            assertThrows(IllegalArgumentException.class, () -> {
                connector.setFault("");
            });
        }
        
        @Test
        @DisplayName("应该能够修复充电桩故障")
        void shouldRepairConnectorFault() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.setFault("电源故障");
            
            // When
            connector.repair();
            
            // Then
            assertEquals(ConnectorStatus.IDLE, connector.getStatus());
            assertNull(connector.getFaultReason());
        }
        
        @Test
        @DisplayName("只有故障状态的充电桩才能修复")
        void shouldOnlyRepairFaultyConnector() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            
            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                connector.repair();
            });
        }
    }
    
    @Nested
    @DisplayName("心跳管理测试")
    class HeartbeatManagementTest {
        
        @Test
        @DisplayName("应该能够更新心跳时间")
        void shouldUpdateHeartbeat() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            Instant beforeUpdate = Instant.now();
            
            // When
            connector.updateHeartbeat();
            
            // Then
            assertNotNull(connector.getLastHeartbeatAt());
            assertTrue(connector.getLastHeartbeatAt().isAfter(beforeUpdate) || 
                      connector.getLastHeartbeatAt().equals(beforeUpdate));
        }
        
        @Test
        @DisplayName("应该能够检查心跳是否超时")
        void shouldCheckIfHeartbeatTimeout() {
            // Given
            Connector connector = TestDataFactory.createTestConnector(TEST_STATION_ID);
            connector.updateHeartbeat();
            
            // When & Then
            assertFalse(connector.isHeartbeatTimeout(5)); // 5分钟超时
            
            // 设置心跳时间为过去
            setFieldValue(connector, "lastHeartbeatAt", Instant.now().minusSeconds(600)); // 10分钟前
            assertTrue(connector.isHeartbeatTimeout(5));
        }
    }
}
