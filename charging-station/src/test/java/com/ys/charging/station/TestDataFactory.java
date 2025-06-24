package com.ys.charging.station;

import com.ys.charging.station.domain.model.*;
import com.ys.charging.station.domain.service.GeoSearchCriteria;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/**
 * 测试数据工厂
 * 
 * 提供各种测试场景所需的数据对象创建方法。
 * 
 * @author yang
 * @since 2025-06-23
 */
public class TestDataFactory {
    
    /**
     * 创建标准测试充电站
     */
    public static Station createTestStation() {
        return createTestStation("测试充电站", "测试运营商");
    }
    
    /**
     * 创建指定名称和运营商的测试充电站
     */
    public static Station createTestStation(String name, String operator) {
        StationInfo stationInfo = new StationInfo(
            name, operator, "010-12345678", "测试描述", "停车场,便利店"
        );
        
        Location location = Location.of(
            BigDecimal.valueOf(116.457),
            BigDecimal.valueOf(39.918),
            "北京市朝阳区建国门外大街1号",
            "北京市",
            "北京市"
        );
        
        BusinessHours businessHours = new BusinessHours(
            LocalTime.of(6, 0),
            LocalTime.of(22, 0),
            false
        );
        
        return Station.create(stationInfo, location, businessHours);
    }
    
    /**
     * 创建不同城市的测试充电站
     */
    public static Station createTestStationInCity(String city, double longitude, double latitude) {
        StationInfo stationInfo = new StationInfo(
            city + "充电站", "测试运营商", "010-12345678", "测试描述", "停车场"
        );
        
        Location location = Location.of(
            BigDecimal.valueOf(longitude),
            BigDecimal.valueOf(latitude),
            city + "测试地址",
            city,
            city.substring(0, 2) + "省"
        );
        
        BusinessHours businessHours = new BusinessHours(
            LocalTime.of(0, 0),
            LocalTime.of(23, 59),
            true
        );
        
        return Station.create(stationInfo, location, businessHours);
    }
    
    /**
     * 创建标准测试充电桩
     */
    public static Connector createTestConnector(Long stationId) {
        return createTestConnector(stationId, "A01", ConnectorInfo.ConnectorType.GB_T_DC, BigDecimal.valueOf(60.0));
    }
    
    /**
     * 创建指定参数的测试充电桩
     */
    public static Connector createTestConnector(Long stationId, String number, 
                                               ConnectorInfo.ConnectorType type, BigDecimal power) {
        ConnectorInfo connectorInfo = new ConnectorInfo(
            number, type, power, 500, 120, ConnectorInfo.Protocol.OCPP_16
        );
        
        return Connector.create(stationId, connectorInfo, 1L);
    }
    
    /**
     * 创建快充桩
     */
    public static Connector createFastCharger(Long stationId, String number) {
        return createTestConnector(stationId, number, ConnectorInfo.ConnectorType.GB_T_DC, BigDecimal.valueOf(120.0));
    }
    
    /**
     * 创建超充桩
     */
    public static Connector createSuperCharger(Long stationId, String number) {
        return createTestConnector(stationId, number, ConnectorInfo.ConnectorType.TESLA_SUPERCHARGER, BigDecimal.valueOf(250.0));
    }
    
    /**
     * 创建测试车位
     */
    public static ParkingSpot createTestParkingSpot(Long stationId, String number) {
        return new ParkingSpot(
            stationId,
            number,
            ParkingSpot.SpotType.STANDARD,
            true,
            ParkingSpot.LockStatus.UP
        );
    }
    
    /**
     * 创建地理搜索条件
     */
    public static GeoSearchCriteria createGeoSearchCriteria() {
        return GeoSearchCriteria.withLimit(116.457, 39.918, 5.0, 20);
    }
    
    /**
     * 创建快充搜索条件
     */
    public static GeoSearchCriteria createFastChargingCriteria() {
        return GeoSearchCriteria.fastCharging(116.457, 39.918, 10.0);
    }
    
    /**
     * 创建超充搜索条件
     */
    public static GeoSearchCriteria createSuperChargingCriteria() {
        return GeoSearchCriteria.superCharging(116.457, 39.918, 20.0);
    }
    
    /**
     * 创建带有充电桩的完整测试充电站
     */
    public static Station createStationWithConnectors(int connectorCount) {
        Station station = createTestStation();
        
        for (int i = 1; i <= connectorCount; i++) {
            ConnectorInfo connectorInfo = new ConnectorInfo(
                "A" + String.format("%02d", i),
                ConnectorInfo.ConnectorType.GB_T_DC,
                BigDecimal.valueOf(60.0),
                500,
                120,
                ConnectorInfo.Protocol.OCPP_16
            );
            
            ParkingSpot parkingSpot = createTestParkingSpot(station.getId(), "P" + String.format("%02d", i));
            station.addConnector(connectorInfo, parkingSpot);
        }
        
        return station;
    }
    
    /**
     * 创建不同状态的充电站列表
     */
    public static List<Station> createStationsWithDifferentStatuses() {
        return List.of(
            createStationWithStatus(StationStatus.OPERATING),
            createStationWithStatus(StationStatus.MAINTENANCE),
            createStationWithStatus(StationStatus.FAULT),
            createStationWithStatus(StationStatus.CLOSED)
        );
    }
    
    /**
     * 创建指定状态的充电站
     */
    public static Station createStationWithStatus(StationStatus status) {
        Station station = createTestStation();
        // 使用反射设置状态
        try {
            var field = Station.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(station, status);
        } catch (Exception e) {
            throw new RuntimeException("设置充电站状态失败", e);
        }
        return station;
    }
    
    /**
     * 创建不同类型的充电桩列表
     */
    public static List<Connector> createConnectorsWithDifferentTypes(Long stationId) {
        return List.of(
            createTestConnector(stationId, "A01", ConnectorInfo.ConnectorType.GB_T_DC, BigDecimal.valueOf(60.0)),
            createTestConnector(stationId, "A02", ConnectorInfo.ConnectorType.GB_T_AC, BigDecimal.valueOf(7.0)),
            createTestConnector(stationId, "A03", ConnectorInfo.ConnectorType.TESLA_SUPERCHARGER, BigDecimal.valueOf(250.0)),
            createTestConnector(stationId, "A04", ConnectorInfo.ConnectorType.CCS_COMBO, BigDecimal.valueOf(150.0)),
            createTestConnector(stationId, "A05", ConnectorInfo.ConnectorType.CHADEMO, BigDecimal.valueOf(50.0))
        );
    }
    
    /**
     * 创建不同状态的充电桩列表
     */
    public static List<Connector> createConnectorsWithDifferentStatuses(Long stationId) {
        List<Connector> connectors = List.of(
            createTestConnector(stationId, "A01"),
            createTestConnector(stationId, "A02"),
            createTestConnector(stationId, "A03"),
            createTestConnector(stationId, "A04"),
            createTestConnector(stationId, "A05"),
            createTestConnector(stationId, "A06"),
            createTestConnector(stationId, "A07")
        );
        
        // 设置不同状态
        setConnectorStatus(connectors.get(0), ConnectorStatus.IDLE);
        setConnectorStatus(connectors.get(1), ConnectorStatus.RESERVED);
        setConnectorStatus(connectors.get(2), ConnectorStatus.OCCUPIED);
        setConnectorStatus(connectors.get(3), ConnectorStatus.CHARGING);
        setConnectorStatus(connectors.get(4), ConnectorStatus.FAULT);
        setConnectorStatus(connectors.get(5), ConnectorStatus.OFFLINE);
        setConnectorStatus(connectors.get(6), ConnectorStatus.MAINTENANCE);
        
        return connectors;
    }
    
    /**
     * 创建预约的充电桩
     */
    public static Connector createReservedConnector(Long stationId, Long userId, int reservationMinutes) {
        Connector connector = createTestConnector(stationId);
        connector.reserve(userId, reservationMinutes);
        return connector;
    }
    
    /**
     * 创建过期预约的充电桩
     */
    public static Connector createExpiredReservationConnector(Long stationId, Long userId) {
        Connector connector = createTestConnector(stationId);
        connector.reserve(userId, 30); // 30分钟预约
        
        // 设置过期时间为过去
        try {
            var field = Connector.class.getDeclaredField("reservationExpiresAt");
            field.setAccessible(true);
            field.set(connector, Instant.now().minusSeconds(3600)); // 1小时前过期
        } catch (Exception e) {
            throw new RuntimeException("设置预约过期时间失败", e);
        }
        
        return connector;
    }
    
    // 私有辅助方法
    private static void setConnectorStatus(Connector connector, ConnectorStatus status) {
        try {
            var field = Connector.class.getDeclaredField("status");
            field.setAccessible(true);
            field.set(connector, status);
        } catch (Exception e) {
            throw new RuntimeException("设置充电桩状态失败", e);
        }
    }
}
