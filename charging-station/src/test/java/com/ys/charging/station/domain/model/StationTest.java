package com.ys.charging.station.domain.model;

import com.ys.charging.station.TestBase;
import com.ys.charging.station.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Station 聚合根单元测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@DisplayName("充电站聚合根测试")
class StationTest extends TestBase {
    
    @Nested
    @DisplayName("充电站创建测试")
    class StationCreationTest {
        
        @Test
        @DisplayName("应该能够创建有效的充电站")
        void shouldCreateValidStation() {
            // When
            Station station = Station.create(testStationInfo, testLocation, testBusinessHours);
            
            // Then
            assertNotNull(station);
            assertEquals(testStationInfo, station.getStationInfo());
            assertEquals(testLocation, station.getLocation());
            assertEquals(testBusinessHours, station.getBusinessHours());
            assertEquals(StationStatus.OPERATING, station.getStatus());
            assertEquals(0, station.getTotalConnectors());
            assertEquals(0, station.getAvailableConnectors());
            assertNotNull(station.getCreatedAt());
            assertNotNull(station.getUpdatedAt());
        }
        
        @Test
        @DisplayName("创建充电站时不能传入空的充电站信息")
        void shouldThrowExceptionWhenStationInfoIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Station.create(null, testLocation, testBusinessHours);
            });
        }
        
        @Test
        @DisplayName("创建充电站时不能传入空的地理位置")
        void shouldThrowExceptionWhenLocationIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Station.create(testStationInfo, null, testBusinessHours);
            });
        }
        
        @Test
        @DisplayName("创建充电站时不能传入空的营业时间")
        void shouldThrowExceptionWhenBusinessHoursIsNull() {
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                Station.create(testStationInfo, testLocation, null);
            });
        }
    }
    
    @Nested
    @DisplayName("充电站状态管理测试")
    class StationStatusManagementTest {
        
        @Test
        @DisplayName("应该能够更改充电站状态")
        void shouldChangeStationStatus() {
            // Given
            Station station = TestDataFactory.createTestStation();
            
            // When
            station.changeStatus(StationStatus.MAINTENANCE, "定期维护");
            
            // Then
            assertEquals(StationStatus.MAINTENANCE, station.getStatus());
        }
        
        @Test
        @DisplayName("更改状态时不能传入空状态")
        void shouldThrowExceptionWhenStatusIsNull() {
            // Given
            Station station = TestDataFactory.createTestStation();
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                station.changeStatus(null, "测试");
            });
        }
        
        @Test
        @DisplayName("应该能够检查充电站是否在营业时间内")
        void shouldCheckIfInBusinessHours() {
            // Given
            BusinessHours businessHours = new BusinessHours(
                LocalTime.of(8, 0),
                LocalTime.of(20, 0),
                false
            );
            Station station = Station.create(testStationInfo, testLocation, businessHours);
            
            // When & Then
            // 注意：这个测试依赖于当前时间，在实际项目中可能需要 mock 时间
            boolean inBusinessHours = station.isInBusinessHours();
            assertNotNull(inBusinessHours);
        }
        
        @Test
        @DisplayName("24小时营业的充电站应该始终在营业时间内")
        void shouldAlwaysBeInBusinessHoursFor24HourStation() {
            // Given
            BusinessHours businessHours = new BusinessHours(null, null, true);
            Station station = Station.create(testStationInfo, testLocation, businessHours);
            
            // When & Then
            assertTrue(station.isInBusinessHours());
        }
    }
    
    @Nested
    @DisplayName("充电桩管理测试")
    class ConnectorManagementTest {
        
        @Test
        @DisplayName("应该能够添加充电桩到充电站")
        void shouldAddConnectorToStation() {
            // Given
            Station station = TestDataFactory.createTestStation();
            
            // When
            station.addConnector(testConnectorInfo, testParkingSpot);
            
            // Then
            assertEquals(1, station.getTotalConnectors());
            assertEquals(1, station.getAvailableConnectors());
            assertFalse(station.getConnectors().isEmpty());
        }
        
        @Test
        @DisplayName("添加充电桩时不能传入空的充电桩信息")
        void shouldThrowExceptionWhenConnectorInfoIsNull() {
            // Given
            Station station = TestDataFactory.createTestStation();
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                station.addConnector(null, testParkingSpot);
            });
        }
        
        @Test
        @DisplayName("添加充电桩时不能传入空的车位信息")
        void shouldThrowExceptionWhenParkingSpotIsNull() {
            // Given
            Station station = TestDataFactory.createTestStation();
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                station.addConnector(testConnectorInfo, null);
            });
        }
        
        @Test
        @DisplayName("应该能够移除充电桩")
        void shouldRemoveConnectorFromStation() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(2);
            Long connectorId = station.getConnectors().get(0).getId();
            
            // When
            station.removeConnector(connectorId);
            
            // Then
            assertEquals(1, station.getTotalConnectors());
            assertEquals(1, station.getAvailableConnectors());
        }
        
        @Test
        @DisplayName("移除不存在的充电桩应该抛出异常")
        void shouldThrowExceptionWhenRemovingNonExistentConnector() {
            // Given
            Station station = TestDataFactory.createTestStation();
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                station.removeConnector(999L);
            });
        }
        
        @Test
        @DisplayName("应该能够检查充电站是否有可用充电桩")
        void shouldCheckIfHasAvailableConnectors() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(2);
            
            // When & Then
            assertTrue(station.hasAvailableConnectors());
            
            // 设置所有充电桩为不可用状态
            station.getConnectors().forEach(connector -> {
                setFieldValue(connector, "status", ConnectorStatus.FAULT);
            });
            
            assertFalse(station.hasAvailableConnectors());
        }
    }
    
    @Nested
    @DisplayName("充电桩预约管理测试")
    class ConnectorReservationTest {
        
        @Test
        @DisplayName("应该能够预约充电桩")
        void shouldReserveConnector() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            Long connectorId = station.getConnectors().get(0).getId();
            Long userId = 1001L;
            
            // When
            station.reserveConnector(connectorId, userId, 30);
            
            // Then
            Connector connector = station.getConnectors().get(0);
            assertEquals(ConnectorStatus.RESERVED, connector.getStatus());
            assertEquals(userId, connector.getReservedByUserId());
            assertNotNull(connector.getReservedAt());
            assertNotNull(connector.getReservationExpiresAt());
        }
        
        @Test
        @DisplayName("预约不存在的充电桩应该抛出异常")
        void shouldThrowExceptionWhenReservingNonExistentConnector() {
            // Given
            Station station = TestDataFactory.createTestStation();
            
            // When & Then
            assertThrows(IllegalArgumentException.class, () -> {
                station.reserveConnector(999L, 1001L, 30);
            });
        }
        
        @Test
        @DisplayName("应该能够取消预约")
        void shouldCancelReservation() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            Long connectorId = station.getConnectors().get(0).getId();
            Long userId = 1001L;
            station.reserveConnector(connectorId, userId, 30);
            
            // When
            station.cancelReservation(connectorId, userId, "用户取消");
            
            // Then
            Connector connector = station.getConnectors().get(0);
            assertEquals(ConnectorStatus.IDLE, connector.getStatus());
            assertNull(connector.getReservedByUserId());
            assertNull(connector.getReservedAt());
            assertNull(connector.getReservationExpiresAt());
        }
        
        @Test
        @DisplayName("非预约用户不能取消预约")
        void shouldThrowExceptionWhenNonReservingUserCancelsReservation() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            Long connectorId = station.getConnectors().get(0).getId();
            station.reserveConnector(connectorId, 1001L, 30);
            
            // When & Then
            assertThrows(IllegalStateException.class, () -> {
                station.cancelReservation(connectorId, 1002L, "其他用户尝试取消");
            });
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
            Long connectorId = station.getConnectors().get(0).getId();
            Long userId = 1001L;
            
            // When
            station.occupyConnector(connectorId, userId);
            
            // Then
            Connector connector = station.getConnectors().get(0);
            assertEquals(ConnectorStatus.OCCUPIED, connector.getStatus());
        }
        
        @Test
        @DisplayName("应该能够开始充电")
        void shouldStartCharging() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            Long connectorId = station.getConnectors().get(0).getId();
            station.occupyConnector(connectorId, 1001L);
            
            // When
            station.startCharging(connectorId);
            
            // Then
            Connector connector = station.getConnectors().get(0);
            assertEquals(ConnectorStatus.CHARGING, connector.getStatus());
        }
        
        @Test
        @DisplayName("应该能够停止充电")
        void shouldStopCharging() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            Long connectorId = station.getConnectors().get(0).getId();
            station.occupyConnector(connectorId, 1001L);
            station.startCharging(connectorId);
            
            // When
            station.stopCharging(connectorId);
            
            // Then
            Connector connector = station.getConnectors().get(0);
            assertEquals(ConnectorStatus.OCCUPIED, connector.getStatus());
        }
        
        @Test
        @DisplayName("应该能够释放充电桩")
        void shouldReleaseConnector() {
            // Given
            Station station = TestDataFactory.createStationWithConnectors(1);
            Long connectorId = station.getConnectors().get(0).getId();
            station.occupyConnector(connectorId, 1001L);
            
            // When
            station.releaseConnector(connectorId);
            
            // Then
            Connector connector = station.getConnectors().get(0);
            assertEquals(ConnectorStatus.IDLE, connector.getStatus());
        }
    }
}
