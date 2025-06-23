package com.ys.charging.station;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 充电站管理模块主应用类
 * 
 * 负责充电站、充电桩、车位信息管理和状态维护，包括：
 * - 充电站基本信息管理和状态控制
 * - 充电桩状态管理和预约功能
 * - 基于 Redis GEO 的地理位置搜索
 * - 车位和地锁控制功能
 * - 实时状态同步和缓存优化
 * 
 * 技术特性：
 * - DDD 领域驱动设计架构
 * - Redis GEO 高性能地理查询
 * - 多级缓存策略优化
 * - Feign 客户端服务集成
 * - 分布式链路追踪支持
 * 
 * @author yang
 * @since 2025-06-23
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
@EnableCaching
@EnableFeignClients
public class ChargingStationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargingStationApplication.class, args);
    }
}
