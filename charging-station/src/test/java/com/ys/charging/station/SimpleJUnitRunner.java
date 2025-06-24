package com.ys.charging.station;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 简单的 JUnit 测试运行器
 * 
 * 使用反射直接调用测试方法，不依赖 JUnit Platform
 * 
 * @author yang
 * @since 2025-06-24
 */
public class SimpleJUnitRunner {
    
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static List<String> failures = new ArrayList<>();
    
    public static void main(String[] args) {
        System.out.println("=== 充电站系统 JUnit 测试套件 ===");
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 运行测试类
            runTestClass(FlatJUnitTest.class);
            
            long endTime = System.currentTimeMillis();
            
            // 输出结果
            printTestResults(endTime - startTime);
            
            // 设置退出码
            if (failedTests > 0) {
                System.exit(1);
            } else {
                System.exit(0);
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试执行失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void runTestClass(Class<?> testClass) throws Exception {
        System.out.println("🧪 运行测试类: " + testClass.getSimpleName());
        System.out.println();
        
        // 创建测试实例
        Object testInstance = testClass.getDeclaredConstructor().newInstance();
        
        // 查找所有测试方法
        Method[] methods = testClass.getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(Test.class)) {
                runTestMethod(testInstance, method);
            }
        }
        
        // 查找嵌套测试类
        Class<?>[] nestedClasses = testClass.getDeclaredClasses();
        for (Class<?> nestedClass : nestedClasses) {
            if (hasTestMethods(nestedClass)) {
                System.out.println();
                System.out.println("📁 " + getDisplayName(nestedClass));
                runTestClass(nestedClass);
            }
        }
    }
    
    private static void runTestMethod(Object testInstance, Method method) {
        totalTests++;
        String testName = getDisplayName(method);
        
        try {
            method.setAccessible(true);
            method.invoke(testInstance);
            passedTests++;
            System.out.println("  ✅ " + testName);
        } catch (Exception e) {
            failedTests++;
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            failures.add(testName + ": " + errorMessage);
            System.out.println("  ❌ " + testName + " - " + errorMessage);
        }
    }
    
    private static boolean hasTestMethods(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Test.class)) {
                return true;
            }
        }
        return false;
    }
    
    private static String getDisplayName(Method method) {
        DisplayName displayName = method.getAnnotation(DisplayName.class);
        return displayName != null ? displayName.value() : method.getName();
    }
    
    private static String getDisplayName(Class<?> clazz) {
        DisplayName displayName = clazz.getAnnotation(DisplayName.class);
        return displayName != null ? displayName.value() : clazz.getSimpleName();
    }
    
    private static void printTestResults(long executionTime) {
        System.out.println();
        System.out.println("=== 测试结果汇总 ===");
        System.out.println("📊 测试执行结果:");
        System.out.println("  总测试数: " + totalTests);
        System.out.println("  成功: " + passedTests);
        System.out.println("  失败: " + failedTests);
        System.out.println("  执行时间: " + executionTime + "ms");
        
        if (failedTests > 0) {
            System.out.println();
            System.out.println("❌ 失败的测试:");
            for (String failure : failures) {
                System.out.println("  - " + failure);
            }
        }
        
        double successRate = totalTests > 0 ? (double) passedTests / totalTests * 100 : 0;
        
        System.out.println();
        if (failedTests == 0) {
            System.out.println("🎉 所有测试通过！成功率: " + String.format("%.1f", successRate) + "%");
            System.out.println();
            System.out.println("📋 测试覆盖范围:");
            System.out.println("  ✅ 数据处理和计算逻辑");
            System.out.println("  ✅ 地理位置计算");
            System.out.println("  ✅ 数据验证规则");
            System.out.println("  ✅ 业务时间逻辑");
            System.out.println("  ✅ 核心业务计算");
            System.out.println();
            System.out.println("🏆 JUnit 测试框架验证成功！");
        } else {
            System.out.println("⚠️  有测试失败。成功率: " + String.format("%.1f", successRate) + "%");
        }
    }
}
