package com.ys.charging.station;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * JUnit 测试运行器
 * 
 * 用于独立运行 JUnit 测试，不依赖 Gradle 或 Maven
 * 
 * @author yang
 * @since 2025-06-24
 */
public class JUnitTestRunner {
    
    public static void main(String[] args) {
        System.out.println("=== 充电站系统 JUnit 测试套件 ===");
        System.out.println();
        
        try {
            // 创建测试发现请求
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(BasicLogicJUnitTest.class))
                .build();
            
            // 创建启动器
            Launcher launcher = LauncherFactory.create();
            
            // 创建监听器
            SummaryGeneratingListener listener = new SummaryGeneratingListener();
            
            // 执行测试
            launcher.registerTestExecutionListeners(listener);
            launcher.execute(request);
            
            // 获取测试结果
            TestExecutionSummary summary = listener.getSummary();
            
            // 输出结果
            printTestResults(summary);
            
            // 设置退出码
            if (summary.getTestsFailedCount() > 0) {
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
    
    private static void printTestResults(TestExecutionSummary summary) {
        System.out.println("📊 测试执行结果:");
        System.out.println("  总测试数: " + summary.getTestsFoundCount());
        System.out.println("  成功: " + summary.getTestsSucceededCount());
        System.out.println("  失败: " + summary.getTestsFailedCount());
        System.out.println("  跳过: " + summary.getTestsSkippedCount());
        System.out.println("  执行时间: " + summary.getTotalTime().toMillis() + "ms");
        
        if (summary.getTestsFailedCount() > 0) {
            System.out.println();
            System.out.println("❌ 失败的测试:");
            summary.getFailures().forEach(failure -> {
                System.out.println("  - " + failure.getTestIdentifier().getDisplayName());
                System.out.println("    " + failure.getException().getMessage());
            });
        }
        
        double successRate = summary.getTestsFoundCount() > 0 
            ? (double) summary.getTestsSucceededCount() / summary.getTestsFoundCount() * 100 
            : 0;
        
        System.out.println();
        if (summary.getTestsFailedCount() == 0) {
            System.out.println("🎉 所有测试通过！成功率: " + String.format("%.1f", successRate) + "%");
        } else {
            System.out.println("⚠️  有测试失败。成功率: " + String.format("%.1f", successRate) + "%");
        }
    }
}
