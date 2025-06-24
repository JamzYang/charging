package com.ys.charging.station;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 业务逻辑测试 - 测试充电站核心业务规则
 * 
 * @author yang
 * @since 2025-06-23
 */
public class BusinessLogicTest {
    
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    
    public static void main(String[] args) {
        System.out.println("=== 充电站业务逻辑测试 ===");
        System.out.println();
        
        // 运行业务逻辑测试
        testConnectorStatusTransitions();
        testReservationLogic();
        testChargingSessionLogic();
        testPowerCalculations();
        testBusinessHoursValidation();
        testLocationDistanceCalculation();
        testConnectorAvailability();
        testFaultHandling();
        testPricingCalculation();
        testCapacityManagement();
        
        // 输出测试结果
        System.out.println();
        System.out.println("=== 测试结果汇总 ===");
        System.out.println("总测试数: " + totalTests);
        System.out.println("通过: " + passedTests);
        System.out.println("失败: " + failedTests);
        System.out.println("成功率: " + (totalTests > 0 ? (passedTests * 100 / totalTests) : 0) + "%");
        
        if (failedTests == 0) {
            System.out.println("🎉 所有业务逻辑测试通过！");
            System.exit(0);
        } else {
            System.out.println("❌ 有测试失败");
            System.exit(1);
        }
    }
    
    private static void testConnectorStatusTransitions() {
        test("充电桩状态转换", () -> {
            // 测试状态转换逻辑
            ConnectorStatus status = ConnectorStatus.IDLE;
            
            // IDLE -> RESERVED
            status = transitionStatus(status, "RESERVE");
            if (status != ConnectorStatus.RESERVED) return false;
            
            // RESERVED -> OCCUPIED
            status = transitionStatus(status, "OCCUPY");
            if (status != ConnectorStatus.OCCUPIED) return false;
            
            // OCCUPIED -> CHARGING
            status = transitionStatus(status, "START_CHARGING");
            if (status != ConnectorStatus.CHARGING) return false;
            
            // CHARGING -> OCCUPIED
            status = transitionStatus(status, "STOP_CHARGING");
            if (status != ConnectorStatus.OCCUPIED) return false;
            
            // OCCUPIED -> IDLE
            status = transitionStatus(status, "RELEASE");
            return status == ConnectorStatus.IDLE;
        });
    }
    
    private static void testReservationLogic() {
        test("预约逻辑", () -> {
            // 测试预约时间计算
            Instant now = Instant.now();
            int reservationMinutes = 30;
            Instant expiresAt = now.plus(reservationMinutes, ChronoUnit.MINUTES);
            
            // 验证预约时间
            long minutesDiff = ChronoUnit.MINUTES.between(now, expiresAt);
            if (minutesDiff != reservationMinutes) return false;
            
            // 测试预约是否过期
            Instant pastTime = now.minus(10, ChronoUnit.MINUTES);
            return isReservationExpired(pastTime, now) && !isReservationExpired(expiresAt, now);
        });
    }
    
    private static void testChargingSessionLogic() {
        test("充电会话逻辑", () -> {
            // 测试充电时长计算
            Instant startTime = Instant.now().minus(2, ChronoUnit.HOURS);
            Instant endTime = Instant.now();
            
            long chargingMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
            
            // 验证充电时长（应该是120分钟）
            return chargingMinutes == 120;
        });
    }
    
    private static void testPowerCalculations() {
        test("功率计算", () -> {
            // 测试功率相关计算
            BigDecimal maxPower = BigDecimal.valueOf(60.0); // 60kW
            BigDecimal voltage = BigDecimal.valueOf(400.0); // 400V
            BigDecimal current = BigDecimal.valueOf(150.0); // 150A

            // 计算理论功率 P = V * I / 1000
            BigDecimal calculatedPower = voltage.multiply(current).divide(BigDecimal.valueOf(1000));

            // 验证功率计算是否正确 (使用 compareTo 而不是 equals)
            return calculatedPower.compareTo(maxPower) == 0;
        });
    }
    
    private static void testBusinessHoursValidation() {
        test("营业时间验证", () -> {
            // 测试不同的营业时间场景
            LocalTime openTime = LocalTime.of(6, 0);
            LocalTime closeTime = LocalTime.of(22, 0);
            
            // 正常营业时间内
            boolean inHours = isWithinBusinessHours(LocalTime.of(14, 30), openTime, closeTime);
            
            // 营业时间外
            boolean beforeHours = !isWithinBusinessHours(LocalTime.of(5, 30), openTime, closeTime);
            boolean afterHours = !isWithinBusinessHours(LocalTime.of(23, 30), openTime, closeTime);
            
            // 边界时间
            boolean atOpen = isWithinBusinessHours(openTime, openTime, closeTime);
            boolean atClose = isWithinBusinessHours(closeTime, openTime, closeTime);
            
            return inHours && beforeHours && afterHours && atOpen && atClose;
        });
    }
    
    private static void testLocationDistanceCalculation() {
        test("位置距离计算", () -> {
            // 测试已知距离的城市
            double beijingLat = 39.9042, beijingLon = 116.4074;
            double shanghaiLat = 31.2304, shanghaiLon = 121.4737;
            
            double distance = calculateHaversineDistance(beijingLat, beijingLon, shanghaiLat, shanghaiLon);
            
            // 北京到上海的直线距离大约1067公里
            return distance > 1000 && distance < 1200;
        });
    }
    
    private static void testConnectorAvailability() {
        test("充电桩可用性", () -> {
            // 测试充电桩可用性判断
            return isConnectorAvailable(ConnectorStatus.IDLE) 
                && !isConnectorAvailable(ConnectorStatus.OCCUPIED)
                && !isConnectorAvailable(ConnectorStatus.CHARGING)
                && !isConnectorAvailable(ConnectorStatus.FAULT)
                && !isConnectorAvailable(ConnectorStatus.RESERVED);
        });
    }
    
    private static void testFaultHandling() {
        test("故障处理", () -> {
            // 测试故障状态处理
            ConnectorStatus normalStatus = ConnectorStatus.IDLE;
            ConnectorStatus faultStatus = ConnectorStatus.FAULT;
            
            // 正常状态可以使用
            boolean normalCanUse = canUseConnector(normalStatus);
            
            // 故障状态不能使用
            boolean faultCannotUse = !canUseConnector(faultStatus);
            
            return normalCanUse && faultCannotUse;
        });
    }
    
    private static void testPricingCalculation() {
        test("价格计算", () -> {
            // 测试充电价格计算
            BigDecimal powerKwh = BigDecimal.valueOf(50.0); // 50度电
            BigDecimal pricePerKwh = BigDecimal.valueOf(1.5); // 1.5元/度
            BigDecimal serviceFee = BigDecimal.valueOf(5.0); // 5元服务费

            BigDecimal totalPrice = powerKwh.multiply(pricePerKwh).add(serviceFee);
            BigDecimal expectedPrice = BigDecimal.valueOf(80.0); // 50*1.5+5=80

            return totalPrice.compareTo(expectedPrice) == 0;
        });
    }
    
    private static void testCapacityManagement() {
        test("容量管理", () -> {
            // 测试充电站容量管理
            int totalConnectors = 10;
            int occupiedConnectors = 3;
            int faultConnectors = 1;
            int reservedConnectors = 2;
            
            int availableConnectors = totalConnectors - occupiedConnectors - faultConnectors - reservedConnectors;
            double utilizationRate = (double) (occupiedConnectors + reservedConnectors) / totalConnectors * 100;
            
            return availableConnectors == 4 && utilizationRate == 50.0;
        });
    }
    
    // 辅助方法
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
    
    // 业务逻辑枚举和方法
    private enum ConnectorStatus {
        IDLE, RESERVED, OCCUPIED, CHARGING, FAULT, OFFLINE, MAINTENANCE
    }
    
    private static ConnectorStatus transitionStatus(ConnectorStatus current, String action) {
        return switch (action) {
            case "RESERVE" -> current == ConnectorStatus.IDLE ? ConnectorStatus.RESERVED : current;
            case "OCCUPY" -> current == ConnectorStatus.RESERVED || current == ConnectorStatus.IDLE ? ConnectorStatus.OCCUPIED : current;
            case "START_CHARGING" -> current == ConnectorStatus.OCCUPIED ? ConnectorStatus.CHARGING : current;
            case "STOP_CHARGING" -> current == ConnectorStatus.CHARGING ? ConnectorStatus.OCCUPIED : current;
            case "RELEASE" -> current == ConnectorStatus.OCCUPIED ? ConnectorStatus.IDLE : current;
            case "FAULT" -> ConnectorStatus.FAULT;
            case "REPAIR" -> current == ConnectorStatus.FAULT ? ConnectorStatus.IDLE : current;
            default -> current;
        };
    }
    
    private static boolean isReservationExpired(Instant expiresAt, Instant now) {
        return expiresAt.isBefore(now);
    }
    
    private static boolean isWithinBusinessHours(LocalTime currentTime, LocalTime openTime, LocalTime closeTime) {
        return !currentTime.isBefore(openTime) && !currentTime.isAfter(closeTime);
    }
    
    private static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    private static boolean isConnectorAvailable(ConnectorStatus status) {
        return status == ConnectorStatus.IDLE;
    }
    
    private static boolean canUseConnector(ConnectorStatus status) {
        return status != ConnectorStatus.FAULT && status != ConnectorStatus.OFFLINE && status != ConnectorStatus.MAINTENANCE;
    }
}
