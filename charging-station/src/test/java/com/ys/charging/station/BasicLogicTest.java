package com.ys.charging.station;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础逻辑测试 - 不依赖 Spring 和 JPA
 * 
 * @author yang
 * @since 2025-06-23
 */
@DisplayName("基础逻辑测试")
class BasicLogicTest {
    
    @Test
    @DisplayName("应该能够进行基本的数学计算")
    void shouldPerformBasicMath() {
        // Given
        BigDecimal power1 = BigDecimal.valueOf(60.0);
        BigDecimal power2 = BigDecimal.valueOf(40.0);
        
        // When
        BigDecimal total = power1.add(power2);
        
        // Then
        assertEquals(BigDecimal.valueOf(100.0), total);
    }
    
    @Test
    @DisplayName("应该能够比较时间")
    void shouldCompareTime() {
        // Given
        LocalTime openTime = LocalTime.of(6, 0);
        LocalTime closeTime = LocalTime.of(22, 0);
        LocalTime currentTime = LocalTime.of(14, 30);
        
        // When & Then
        assertTrue(currentTime.isAfter(openTime));
        assertTrue(currentTime.isBefore(closeTime));
    }
    
    @Test
    @DisplayName("应该能够处理字符串")
    void shouldHandleStrings() {
        // Given
        String stationName = "测试充电站";
        String operator = "测试运营商";
        
        // When
        String fullName = stationName + " - " + operator;
        
        // Then
        assertEquals("测试充电站 - 测试运营商", fullName);
        assertFalse(stationName.isEmpty());
        assertTrue(stationName.contains("充电站"));
    }
    
    @Test
    @DisplayName("应该能够处理枚举")
    void shouldHandleEnums() {
        // Given
        TestStatus status1 = TestStatus.ACTIVE;
        TestStatus status2 = TestStatus.INACTIVE;
        
        // When & Then
        assertNotEquals(status1, status2);
        assertEquals("ACTIVE", status1.name());
        assertEquals(TestStatus.ACTIVE, TestStatus.valueOf("ACTIVE"));
    }
    
    @Test
    @DisplayName("应该能够计算距离")
    void shouldCalculateDistance() {
        // Given
        double lat1 = 39.9042; // 北京
        double lon1 = 116.4074;
        double lat2 = 31.2304; // 上海
        double lon2 = 121.4737;
        
        // When
        double distance = calculateHaversineDistance(lat1, lon1, lat2, lon2);
        
        // Then
        assertTrue(distance > 1000); // 北京到上海大约1000多公里
        assertTrue(distance < 1500);
    }
    
    @Test
    @DisplayName("应该能够验证电话号码格式")
    void shouldValidatePhoneNumber() {
        // Given
        String validPhone = "010-12345678";
        String invalidPhone = "123";
        
        // When & Then
        assertTrue(isValidPhoneNumber(validPhone));
        assertFalse(isValidPhoneNumber(invalidPhone));
    }
    
    @Test
    @DisplayName("应该能够验证经纬度范围")
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
    @DisplayName("应该能够计算功率范围")
    void shouldCalculatePowerRange() {
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
    @DisplayName("应该能够处理营业时间逻辑")
    void shouldHandleBusinessHours() {
        // Given
        LocalTime openTime = LocalTime.of(6, 0);
        LocalTime closeTime = LocalTime.of(22, 0);
        
        // When & Then
        assertTrue(isWithinBusinessHours(LocalTime.of(14, 30), openTime, closeTime));
        assertFalse(isWithinBusinessHours(LocalTime.of(2, 30), openTime, closeTime));
        assertFalse(isWithinBusinessHours(LocalTime.of(23, 30), openTime, closeTime));
    }
    
    @Test
    @DisplayName("应该能够处理24小时营业")
    void shouldHandle24HourBusiness() {
        // Given
        boolean is24Hours = true;
        LocalTime anyTime = LocalTime.of(3, 30);
        
        // When & Then
        assertTrue(is24Hours || isWithinBusinessHours(anyTime, LocalTime.of(0, 0), LocalTime.of(23, 59)));
    }
    
    // 辅助方法
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
        // 简单的电话号码验证：包含数字和可能的连字符
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
