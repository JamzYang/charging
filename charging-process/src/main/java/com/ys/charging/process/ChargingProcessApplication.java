package com.ys.charging.process;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 充电流程模块主应用类
 * 
 * 负责充电会话状态机的管理，包括：
 * - 充电会话全流程状态管理
 * - 领域事件驱动的副作用处理
 * - 基于DDD的聚合根设计
 * 
 * @author yang
 * @since 2025-06-23
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
public class ChargingProcessApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargingProcessApplication.class, args);
    }
}
