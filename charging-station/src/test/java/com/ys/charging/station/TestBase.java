package com.ys.charging.station;

import com.ys.charging.station.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

/**
 * 测试基础类
 * 
 * 提供通用的测试数据和工具方法。
 * 
 * @author yang
 * @since 2025-06-23
 */
@ActiveProfiles("test")
public abstract class TestBase {
    
    // 测试常量
    protected static final Long TEST_STATION_ID = 1L;
    protected static final Long TEST_CONNECTOR_ID = 1L;
    protected static final Long TEST_USER_ID = 1001L;
    protected static final Long TEST_PARKING_SPOT_ID = 1L;
    
    // 测试数据
    protected StationInfo testStationInfo;
    protected Location testLocation;
    protected BusinessHours testBusinessHours;
    protected ConnectorInfo testConnectorInfo;
    protected ParkingSpot testParkingSpot;
    protected Station testStation;
    protected Connector testConnector;
    
    @BeforeEach
    void setUpTestData() {
        setupTestStationInfo();
        setupTestLocation();
        setupTestBusinessHours();
        setupTestConnectorInfo();
        setupTestParkingSpot();
        setupTestStation();
        setupTestConnector();
    }
    
    /**
     * 创建测试用充电站信息
     */
    protected void setupTestStationInfo() {
        testStationInfo = new StationInfo(
            "测试充电站",
            "测试运营商",
            "010-12345678",
            "测试充电站描述",
            "停车场,便利店"
        );
    }
    
    /**
     * 创建测试用地理位置
     */
    protected void setupTestLocation() {
        testLocation = Location.of(
            BigDecimal.valueOf(116.457),
            BigDecimal.valueOf(39.918),
            "北京市朝阳区建国门外大街1号",
            "北京市",
            "北京市"
        );
    }
    
    /**
     * 创建测试用营业时间
     */
    protected void setupTestBusinessHours() {
        testBusinessHours = new BusinessHours(
            LocalTime.of(6, 0),
            LocalTime.of(22, 0),
            false
        );
    }
    
    /**
     * 创建测试用充电桩信息
     */
    protected void setupTestConnectorInfo() {
        testConnectorInfo = new ConnectorInfo(
            "A01",
            ConnectorInfo.ConnectorType.GB_T_DC,
            BigDecimal.valueOf(60.0),
            500,
            120,
            ConnectorInfo.Protocol.OCPP_16
        );
    }
    
    /**
     * 创建测试用车位
     */
    protected void setupTestParkingSpot() {
        testParkingSpot = new ParkingSpot(
            TEST_STATION_ID,
            "P01",
            ParkingSpot.SpotType.STANDARD,
            true,
            ParkingSpot.LockStatus.UP
        );
    }
    
    /**
     * 创建测试用充电站
     */
    protected void setupTestStation() {
        testStation = Station.create(testStationInfo, testLocation, testBusinessHours);
        // 使用反射设置ID（模拟数据库保存后的状态）
        setFieldValue(testStation, "id", TEST_STATION_ID);
        setFieldValue(testStation, "createdAt", Instant.now());
        setFieldValue(testStation, "updatedAt", Instant.now());
    }
    
    /**
     * 创建测试用充电桩
     */
    protected void setupTestConnector() {
        testConnector = Connector.create(TEST_STATION_ID, testConnectorInfo, TEST_PARKING_SPOT_ID);
        // 使用反射设置ID
        setFieldValue(testConnector, "id", TEST_CONNECTOR_ID);
        setFieldValue(testConnector, "createdAt", Instant.now());
        setFieldValue(testConnector, "updatedAt", Instant.now());
    }
    
    /**
     * 创建不同状态的充电站
     */
    protected Station createStationWithStatus(StationStatus status) {
        Station station = Station.create(testStationInfo, testLocation, testBusinessHours);
        setFieldValue(station, "status", status);
        return station;
    }
    
    /**
     * 创建不同状态的充电桩
     */
    protected Connector createConnectorWithStatus(ConnectorStatus status) {
        Connector connector = Connector.create(TEST_STATION_ID, testConnectorInfo, TEST_PARKING_SPOT_ID);
        setFieldValue(connector, "status", status);
        return connector;
    }
    
    /**
     * 创建预约的充电桩
     */
    protected Connector createReservedConnector(Long userId, int reservationMinutes) {
        Connector connector = Connector.create(TEST_STATION_ID, testConnectorInfo, TEST_PARKING_SPOT_ID);
        connector.reserve(userId, reservationMinutes);
        return connector;
    }
    
    /**
     * 创建不同类型的充电桩信息
     */
    protected ConnectorInfo createConnectorInfo(ConnectorInfo.ConnectorType type, BigDecimal power) {
        return new ConnectorInfo(
            "TEST-" + type.name(),
            type,
            power,
            500,
            120,
            ConnectorInfo.Protocol.OCPP_16
        );
    }
    
    /**
     * 创建不同城市的地理位置
     */
    protected Location createLocation(String city, double longitude, double latitude) {
        return Location.of(
            BigDecimal.valueOf(longitude),
            BigDecimal.valueOf(latitude),
            city + "测试地址",
            city,
            city.substring(0, 2) + "省"
        );
    }
    
    /**
     * 验证充电站基本属性
     */
    protected void assertStationBasicProperties(Station station) {
        assert station != null;
        assert station.getStationInfo() != null;
        assert station.getLocation() != null;
        assert station.getBusinessHours() != null;
        assert station.getStatus() != null;
    }
    
    /**
     * 验证充电桩基本属性
     */
    protected void assertConnectorBasicProperties(Connector connector) {
        assert connector != null;
        assert connector.getStationId() != null;
        assert connector.getConnectorInfo() != null;
        assert connector.getStatus() != null;
    }
    
    /**
     * 使用反射设置私有字段值
     */
    protected void setFieldValue(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("设置字段值失败: " + fieldName, e);
        }
    }
    
    /**
     * 使用反射获取私有字段值
     */
    @SuppressWarnings("unchecked")
    protected <T> T getFieldValue(Object target, String fieldName) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("获取字段值失败: " + fieldName, e);
        }
    }
    
    /**
     * 等待异步操作完成
     */
    protected void waitForAsyncOperation(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待异步操作被中断", e);
        }
    }
    
    /**
     * 断言异常消息包含指定文本
     */
    protected void assertExceptionMessage(Exception exception, String expectedMessage) {
        assert exception.getMessage() != null;
        assert exception.getMessage().contains(expectedMessage) : 
            "期望异常消息包含: " + expectedMessage + ", 实际: " + exception.getMessage();
    }
}
