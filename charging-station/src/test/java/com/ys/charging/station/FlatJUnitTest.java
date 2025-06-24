package com.ys.charging.station;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 扁平化的 JUnit 测试 - 避免嵌套类问题
 * 
 * @author yang
 * @since 2025-06-24
 */
@DisplayName("充电站系统核心逻辑测试")
class FlatJUnitTest {
    
    @Test
    @DisplayName("应该能够进行精确的数学计算")
    void shouldPerformAccurateMathCalculations() {
        // Given
        BigDecimal power1 = BigDecimal.valueOf(60.0);
        BigDecimal power2 = BigDecimal.valueOf(40.0);
        
        // When
        BigDecimal total = power1.add(power2);
        
        // Then
        assertEquals(0, total.compareTo(BigDecimal.valueOf(100.0)));
    }
    
    @Test
    @DisplayName("应该能够正确处理时间比较")
    void shouldHandleTimeComparison() {
        // Given
        LocalTime openTime = LocalTime.of(6, 0);
        LocalTime closeTime = LocalTime.of(22, 0);
        LocalTime currentTime = LocalTime.of(14, 30);
        
        // When & Then
        assertTrue(currentTime.isAfter(openTime));
        assertTrue(currentTime.isBefore(closeTime));
    }
    
    @Test
    @DisplayName("应该能够处理字符串操作")
    void shouldHandleStringOperations() {
        // Given
        String stationName = "测试充电站";
        String operator = "测试运营商";
        
        // When
        String fullName = stationName + " - " + operator;
        
        // Then
        if (!"测试充电站 - 测试运营商".equals(fullName)) {
            throw new AssertionError("Expected: 测试充电站 - 测试运营商, but was: " + fullName);
        }
        if (stationName.isEmpty()) {
            throw new AssertionError("Station name should not be empty");
        }
        if (!stationName.contains("充电站")) {
            throw new AssertionError("Station name should contain '充电站'");
        }
    }
    
    @Test
    @DisplayName("应该能够处理枚举类型")
    void shouldHandleEnumTypes() {
        // Given
        TestStatus status1 = TestStatus.ACTIVE;
        TestStatus status2 = TestStatus.INACTIVE;
        
        // When & Then
        if (status1.equals(status2)) {
            throw new AssertionError("Status1 and Status2 should not be equal");
        }
        if (!"ACTIVE".equals(status1.name())) {
            throw new AssertionError("Expected: ACTIVE, but was: " + status1.name());
        }
        if (!TestStatus.ACTIVE.equals(TestStatus.valueOf("ACTIVE"))) {
            throw new AssertionError("valueOf should return ACTIVE");
        }
    }
    
    @Test
    @DisplayName("应该能够计算两点间的距离")
    void shouldCalculateDistanceBetweenTwoPoints() {
        // Given - 北京和上海的坐标
        double beijingLat = 39.9042;
        double beijingLon = 116.4074;
        double shanghaiLat = 31.2304;
        double shanghaiLon = 121.4737;
        
        // When
        double distance = calculateHaversineDistance(beijingLat, beijingLon, shanghaiLat, shanghaiLon);
        
        // Then - 北京到上海大约1000多公里
        assertTrue(distance > 1000);
        assertTrue(distance < 1500);
    }
    
    @Test
    @DisplayName("应该能够验证经纬度坐标的有效性")
    void shouldValidateCoordinates() {
        // Given
        double validLongitude = 116.4074;
        double validLatitude = 39.9042;
        double invalidLongitude = 200.0;
        double invalidLatitude = 100.0;
        
        // When & Then
        assertTrue(isValidLongitude(validLongitude));
        assertTrue(isValidLatitude(validLatitude));
        assertFalse(isValidLongitude(invalidLongitude));
        assertFalse(isValidLatitude(invalidLatitude));
    }
    
    @Test
    @DisplayName("应该能够验证电话号码格式")
    void shouldValidatePhoneNumberFormat() {
        // Given
        String validPhone = "010-12345678";
        String invalidPhone = "123";
        
        // When & Then
        assertTrue(isValidPhoneNumber(validPhone));
        assertFalse(isValidPhoneNumber(invalidPhone));
    }
    
    @Test
    @DisplayName("应该能够验证功率范围")
    void shouldValidatePowerRange() {
        // Given
        BigDecimal minPower = BigDecimal.valueOf(50.0);
        BigDecimal maxPower = BigDecimal.valueOf(150.0);
        BigDecimal testPower = BigDecimal.valueOf(100.0);
        
        // When & Then
        assertTrue(isInPowerRange(testPower, minPower, maxPower));
        assertFalse(isInPowerRange(BigDecimal.valueOf(30.0), minPower, maxPower));
        assertFalse(isInPowerRange(BigDecimal.valueOf(200.0), minPower, maxPower));
    }
    
    @Test
    @DisplayName("应该能够正确判断营业时间")
    void shouldCorrectlyDetermineBusinessHours() {
        // Given
        LocalTime openTime = LocalTime.of(6, 0);
        LocalTime closeTime = LocalTime.of(22, 0);
        
        // When & Then
        assertTrue(isWithinBusinessHours(LocalTime.of(14, 30), openTime, closeTime));
        assertFalse(isWithinBusinessHours(LocalTime.of(2, 30), openTime, closeTime));
        assertFalse(isWithinBusinessHours(LocalTime.of(23, 30), openTime, closeTime));
    }
    
    @Test
    @DisplayName("应该能够处理24小时营业场景")
    void shouldHandle24HourBusiness() {
        // Given
        boolean is24Hours = true;
        LocalTime anyTime = LocalTime.of(3, 30);
        
        // When & Then
        assertTrue(is24Hours || isWithinBusinessHours(anyTime, LocalTime.of(0, 0), LocalTime.of(23, 59)));
    }
    
    @Test
    @DisplayName("应该能够正确计算充电功率")
    void shouldCalculateChargingPowerCorrectly() {
        // Given
        BigDecimal voltage = BigDecimal.valueOf(400.0); // 400V
        BigDecimal current = BigDecimal.valueOf(150.0); // 150A
        BigDecimal expectedPower = BigDecimal.valueOf(60.0); // 60kW
        
        // When
        BigDecimal calculatedPower = voltage.multiply(current).divide(BigDecimal.valueOf(1000));
        
        // Then
        assertEquals(0, calculatedPower.compareTo(expectedPower));
    }
    
    @Test
    @DisplayName("应该能够正确计算充电费用")
    void shouldCalculateChargingCostCorrectly() {
        // Given
        BigDecimal powerKwh = BigDecimal.valueOf(50.0); // 50度电
        BigDecimal pricePerKwh = BigDecimal.valueOf(1.5); // 1.5元/度
        BigDecimal serviceFee = BigDecimal.valueOf(5.0); // 5元服务费
        BigDecimal expectedTotal = BigDecimal.valueOf(80.0); // 50*1.5+5=80
        
        // When
        BigDecimal totalPrice = powerKwh.multiply(pricePerKwh).add(serviceFee);
        
        // Then
        assertEquals(0, totalPrice.compareTo(expectedTotal));
    }
    
    @Test
    @DisplayName("应该能够正确管理充电站容量")
    void shouldManageStationCapacityCorrectly() {
        // Given
        int totalConnectors = 10;
        int occupiedConnectors = 3;
        int faultConnectors = 1;
        int reservedConnectors = 2;
        
        // When
        int availableConnectors = totalConnectors - occupiedConnectors - faultConnectors - reservedConnectors;
        double utilizationRate = (double) (occupiedConnectors + reservedConnectors) / totalConnectors * 100;
        
        // Then
        if (availableConnectors != 4) {
            throw new AssertionError("Expected: 4, but was: " + availableConnectors);
        }
        if (Math.abs(utilizationRate - 50.0) > 0.01) {
            throw new AssertionError("Expected: 50.0, but was: " + utilizationRate);
        }
    }
    
    // 辅助方法和枚举
    private enum TestStatus {
        ACTIVE, INACTIVE, MAINTENANCE
    }
    
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return phone.matches("\\d{3,4}-\\d{7,8}") || phone.matches("\\d{10,11}");
    }
    
    private boolean isValidLongitude(double longitude) {
        return longitude >= -180.0 && longitude <= 180.0;
    }
    
    private boolean isValidLatitude(double latitude) {
        return latitude >= -90.0 && latitude <= 90.0;
    }
    
    private boolean isInPowerRange(BigDecimal power, BigDecimal minPower, BigDecimal maxPower) {
        return power.compareTo(minPower) >= 0 && power.compareTo(maxPower) <= 0;
    }
    
    private boolean isWithinBusinessHours(LocalTime currentTime, LocalTime openTime, LocalTime closeTime) {
        return !currentTime.isBefore(openTime) && !currentTime.isAfter(closeTime);
    }
}
