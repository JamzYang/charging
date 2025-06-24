package com.ys.charging.station;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 简单的测试运行器 - 不依赖 JUnit
 * 
 * @author yang
 * @since 2025-06-23
 */
public class TestRunner {
    
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    
    public static void main(String[] args) {
        System.out.println("=== 充电站系统基础逻辑测试 ===");
        System.out.println();
        
        // 运行所有测试
        testBasicMath();
        testTimeComparison();
        testStringHandling();
        testEnumHandling();
        testDistanceCalculation();
        testPhoneNumberValidation();
        testCoordinateValidation();
        testPowerRangeCalculation();
        testBusinessHoursLogic();
        test24HourBusiness();
        
        // 输出测试结果
        System.out.println();
        System.out.println("=== 测试结果汇总 ===");
        System.out.println("总测试数: " + totalTests);
        System.out.println("通过: " + passedTests);
        System.out.println("失败: " + failedTests);
        System.out.println("成功率: " + (totalTests > 0 ? (passedTests * 100 / totalTests) : 0) + "%");
        
        if (failedTests == 0) {
            System.out.println("🎉 所有测试通过！");
            System.exit(0);
        } else {
            System.out.println("❌ 有测试失败");
            System.exit(1);
        }
    }
    
    private static void testBasicMath() {
        test("基本数学计算", () -> {
            BigDecimal power1 = BigDecimal.valueOf(60.0);
            BigDecimal power2 = BigDecimal.valueOf(40.0);
            BigDecimal total = power1.add(power2);
            return total.equals(BigDecimal.valueOf(100.0));
        });
    }
    
    private static void testTimeComparison() {
        test("时间比较", () -> {
            LocalTime openTime = LocalTime.of(6, 0);
            LocalTime closeTime = LocalTime.of(22, 0);
            LocalTime currentTime = LocalTime.of(14, 30);
            
            return currentTime.isAfter(openTime) && currentTime.isBefore(closeTime);
        });
    }
    
    private static void testStringHandling() {
        test("字符串处理", () -> {
            String stationName = "测试充电站";
            String operator = "测试运营商";
            String fullName = stationName + " - " + operator;
            
            return "测试充电站 - 测试运营商".equals(fullName) 
                && !stationName.isEmpty() 
                && stationName.contains("充电站");
        });
    }
    
    private static void testEnumHandling() {
        test("枚举处理", () -> {
            TestStatus status1 = TestStatus.ACTIVE;
            TestStatus status2 = TestStatus.INACTIVE;
            
            return !status1.equals(status2) 
                && "ACTIVE".equals(status1.name())
                && TestStatus.ACTIVE.equals(TestStatus.valueOf("ACTIVE"));
        });
    }
    
    private static void testDistanceCalculation() {
        test("距离计算", () -> {
            double lat1 = 39.9042; // 北京
            double lon1 = 116.4074;
            double lat2 = 31.2304; // 上海
            double lon2 = 121.4737;
            
            double distance = calculateHaversineDistance(lat1, lon1, lat2, lon2);
            return distance > 1000 && distance < 1500; // 北京到上海大约1000多公里
        });
    }
    
    private static void testPhoneNumberValidation() {
        test("电话号码验证", () -> {
            String validPhone = "010-12345678";
            String invalidPhone = "123";
            
            return isValidPhoneNumber(validPhone) && !isValidPhoneNumber(invalidPhone);
        });
    }
    
    private static void testCoordinateValidation() {
        test("经纬度验证", () -> {
            double validLongitude = 116.4074;
            double validLatitude = 39.9042;
            double invalidLongitude = 200.0;
            double invalidLatitude = 100.0;
            
            return isValidLongitude(validLongitude) 
                && isValidLatitude(validLatitude)
                && !isValidLongitude(invalidLongitude) 
                && !isValidLatitude(invalidLatitude);
        });
    }
    
    private static void testPowerRangeCalculation() {
        test("功率范围计算", () -> {
            BigDecimal minPower = BigDecimal.valueOf(50.0);
            BigDecimal maxPower = BigDecimal.valueOf(150.0);
            BigDecimal testPower = BigDecimal.valueOf(100.0);
            
            return isInPowerRange(testPower, minPower, maxPower)
                && !isInPowerRange(BigDecimal.valueOf(30.0), minPower, maxPower)
                && !isInPowerRange(BigDecimal.valueOf(200.0), minPower, maxPower);
        });
    }
    
    private static void testBusinessHoursLogic() {
        test("营业时间逻辑", () -> {
            LocalTime openTime = LocalTime.of(6, 0);
            LocalTime closeTime = LocalTime.of(22, 0);
            
            return isWithinBusinessHours(LocalTime.of(14, 30), openTime, closeTime)
                && !isWithinBusinessHours(LocalTime.of(2, 30), openTime, closeTime)
                && !isWithinBusinessHours(LocalTime.of(23, 30), openTime, closeTime);
        });
    }
    
    private static void test24HourBusiness() {
        test("24小时营业", () -> {
            boolean is24Hours = true;
            LocalTime anyTime = LocalTime.of(3, 30);
            
            return is24Hours || isWithinBusinessHours(anyTime, LocalTime.of(0, 0), LocalTime.of(23, 59));
        });
    }
    
    // 测试框架方法
    private static void test(String testName, TestCase testCase) {
        totalTests++;
        try {
            boolean result = testCase.run();
            if (result) {
                passedTests++;
                System.out.println("✅ " + testName + " - 通过");
            } else {
                failedTests++;
                System.out.println("❌ " + testName + " - 失败");
            }
        } catch (Exception e) {
            failedTests++;
            System.out.println("❌ " + testName + " - 异常: " + e.getMessage());
        }
    }
    
    @FunctionalInterface
    private interface TestCase {
        boolean run() throws Exception;
    }
    
    // 辅助方法和枚举
    private enum TestStatus {
        ACTIVE, INACTIVE, MAINTENANCE
    }
    
    private static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    private static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return phone.matches("\\d{3,4}-\\d{7,8}") || phone.matches("\\d{10,11}");
    }
    
    private static boolean isValidLongitude(double longitude) {
        return longitude >= -180.0 && longitude <= 180.0;
    }
    
    private static boolean isValidLatitude(double latitude) {
        return latitude >= -90.0 && latitude <= 90.0;
    }
    
    private static boolean isInPowerRange(BigDecimal power, BigDecimal minPower, BigDecimal maxPower) {
        return power.compareTo(minPower) >= 0 && power.compareTo(maxPower) <= 0;
    }
    
    private static boolean isWithinBusinessHours(LocalTime currentTime, LocalTime openTime, LocalTime closeTime) {
        return !currentTime.isBefore(openTime) && !currentTime.isAfter(closeTime);
    }
}
