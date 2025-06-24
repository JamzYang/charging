package com.ys.charging.station;

import com.ys.charging.station.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单的基础测试
 * 
 * @author yang
 * @since 2025-06-23
 */
@DisplayName("基础功能测试")
class SimpleTest {
    
    @Test
    @DisplayName("应该能够创建充电站信息")
    void shouldCreateStationInfo() {
        // Given
        String name = "测试充电站";
        String operator = "测试运营商";
        String phone = "010-12345678";
        String description = "测试描述";
        String facilities = "停车场,便利店";
        
        // When
        StationInfo stationInfo = new StationInfo(name, operator, phone, description, facilities);
        
        // Then
        assertNotNull(stationInfo);
        assertEquals(name, stationInfo.name());
        assertEquals(operator, stationInfo.operator());
        assertEquals(phone, stationInfo.contactPhone());
        assertEquals(description, stationInfo.description());
        assertEquals(facilities, stationInfo.facilities());
    }
    
    @Test
    @DisplayName("应该能够创建地理位置")
    void shouldCreateLocation() {
        // Given
        BigDecimal longitude = BigDecimal.valueOf(116.457);
        BigDecimal latitude = BigDecimal.valueOf(39.918);
        String address = "北京市朝阳区建国门外大街1号";
        String city = "北京市";
        String province = "北京市";
        
        // When
        Location location = Location.of(longitude, latitude, address, city, province);
        
        // Then
        assertNotNull(location);
        assertEquals(longitude, location.longitude());
        assertEquals(latitude, location.latitude());
        assertEquals(address, location.address());
        assertEquals(city, location.city());
        assertEquals(province, location.province());
    }
    
    @Test
    @DisplayName("应该能够创建营业时间")
    void shouldCreateBusinessHours() {
        // Given
        LocalTime openTime = LocalTime.of(6, 0);
        LocalTime closeTime = LocalTime.of(22, 0);
        boolean is24Hours = false;
        
        // When
        BusinessHours businessHours = new BusinessHours(openTime, closeTime, is24Hours);
        
        // Then
        assertNotNull(businessHours);
        assertEquals(openTime, businessHours.openTime());
        assertEquals(closeTime, businessHours.closeTime());
        assertEquals(is24Hours, businessHours.is24Hours());
    }
    
    @Test
    @DisplayName("应该能够创建充电桩信息")
    void shouldCreateConnectorInfo() {
        // Given
        ConnectorInfo.ConnectorType type = ConnectorInfo.ConnectorType.GB_T_DC;
        BigDecimal maxPower = BigDecimal.valueOf(60.0);
        
        // When
        ConnectorInfo connectorInfo = new ConnectorInfo(type, maxPower);
        
        // Then
        assertNotNull(connectorInfo);
        assertEquals(type, connectorInfo.connectorType());
        assertEquals(maxPower, connectorInfo.maxPower());
    }
    
    @Test
    @DisplayName("应该能够创建停车位信息")
    void shouldCreateParkingSpot() {
        // Given
        String spotNumber = "A01";
        boolean isAccessible = false;
        boolean isCovered = true;
        
        // When
        ParkingSpot parkingSpot = new ParkingSpot(spotNumber, isAccessible, isCovered);
        
        // Then
        assertNotNull(parkingSpot);
        assertEquals(spotNumber, parkingSpot.spotNumber());
        assertEquals(isAccessible, parkingSpot.isAccessible());
        assertEquals(isCovered, parkingSpot.isCovered());
    }
    
    @Test
    @DisplayName("应该能够创建充电站")
    void shouldCreateStation() {
        // Given
        StationInfo stationInfo = new StationInfo(
            "测试充电站", "测试运营商", "010-12345678", "测试描述", "停车场"
        );
        Location location = Location.of(
            BigDecimal.valueOf(116.457), BigDecimal.valueOf(39.918),
            "北京市朝阳区", "北京市", "北京市"
        );
        BusinessHours businessHours = new BusinessHours(
            LocalTime.of(6, 0), LocalTime.of(22, 0), false
        );
        
        // When
        Station station = new Station(stationInfo, location, businessHours);
        
        // Then
        assertNotNull(station);
        assertEquals(stationInfo, station.getStationInfo());
        assertEquals(location, station.getLocation());
        assertEquals(businessHours, station.getBusinessHours());
        assertEquals(StationStatus.OPERATING, station.getStatus());
        assertTrue(station.getConnectors().isEmpty());
    }
    
    @Test
    @DisplayName("应该能够创建充电桩")
    void shouldCreateConnector() {
        // Given
        Long stationId = 1L;
        String connectorNumber = "A01";
        ConnectorInfo connectorInfo = new ConnectorInfo(
            ConnectorInfo.ConnectorType.GB_T_DC, BigDecimal.valueOf(60.0)
        );
        ParkingSpot parkingSpot = new ParkingSpot("A01", false, true);
        
        // When
        Connector connector = new Connector(stationId, connectorNumber, connectorInfo, parkingSpot);
        
        // Then
        assertNotNull(connector);
        assertEquals(stationId, connector.getStationId());
        assertEquals(connectorNumber, connector.getConnectorNumber());
        assertEquals(connectorInfo, connector.getConnectorInfo());
        assertEquals(parkingSpot, connector.getParkingSpot());
        assertEquals(ConnectorStatus.IDLE, connector.getStatus());
    }
    
    @Test
    @DisplayName("应该能够预约充电桩")
    void shouldReserveConnector() {
        // Given
        Connector connector = createTestConnector();
        Long userId = 1001L;
        int reservationMinutes = 30;
        
        // When
        connector.reserve(userId, reservationMinutes);
        
        // Then
        assertEquals(ConnectorStatus.RESERVED, connector.getStatus());
        assertEquals(userId, connector.getReservedByUserId());
        assertNotNull(connector.getReservationExpiresAt());
    }
    
    @Test
    @DisplayName("应该能够占用充电桩")
    void shouldOccupyConnector() {
        // Given
        Connector connector = createTestConnector();
        Long userId = 1001L;
        
        // When
        connector.occupy(userId);
        
        // Then
        assertEquals(ConnectorStatus.OCCUPIED, connector.getStatus());
        assertEquals(userId, connector.getOccupiedByUserId());
    }
    
    @Test
    @DisplayName("应该能够开始充电")
    void shouldStartCharging() {
        // Given
        Connector connector = createTestConnector();
        connector.occupy(1001L);
        
        // When
        connector.startCharging();
        
        // Then
        assertEquals(ConnectorStatus.CHARGING, connector.getStatus());
        assertNotNull(connector.getChargingStartedAt());
    }
    
    @Test
    @DisplayName("应该能够停止充电")
    void shouldStopCharging() {
        // Given
        Connector connector = createTestConnector();
        connector.occupy(1001L);
        connector.startCharging();
        
        // When
        connector.stopCharging();
        
        // Then
        assertEquals(ConnectorStatus.OCCUPIED, connector.getStatus());
        assertNull(connector.getChargingStartedAt());
    }
    
    @Test
    @DisplayName("应该能够释放充电桩")
    void shouldReleaseConnector() {
        // Given
        Connector connector = createTestConnector();
        connector.occupy(1001L);
        
        // When
        connector.release();
        
        // Then
        assertEquals(ConnectorStatus.IDLE, connector.getStatus());
        assertNull(connector.getOccupiedByUserId());
    }
    
    @Test
    @DisplayName("应该能够设置充电桩故障")
    void shouldSetConnectorFault() {
        // Given
        Connector connector = createTestConnector();
        String faultReason = "电源模块故障";
        
        // When
        connector.setFault(faultReason);
        
        // Then
        assertEquals(ConnectorStatus.FAULT, connector.getStatus());
        assertEquals(faultReason, connector.getFaultReason());
    }
    
    @Test
    @DisplayName("应该能够修复充电桩故障")
    void shouldRepairConnector() {
        // Given
        Connector connector = createTestConnector();
        connector.setFault("电源模块故障");
        
        // When
        connector.repair();
        
        // Then
        assertEquals(ConnectorStatus.IDLE, connector.getStatus());
        assertNull(connector.getFaultReason());
    }
    
    // 辅助方法
    private Connector createTestConnector() {
        ConnectorInfo connectorInfo = new ConnectorInfo(
            ConnectorInfo.ConnectorType.GB_T_DC, BigDecimal.valueOf(60.0)
        );
        ParkingSpot parkingSpot = new ParkingSpot("A01", false, true);
        return new Connector(1L, "A01", connectorInfo, parkingSpot);
    }
}
