package com.ys.charging.station.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 事务管理配置
 * 
 * 配置事务管理、JPA 审计和 AOP 代理。
 * 支持声明式事务和编程式事务。
 * 
 * @author yang
 * @since 2025-06-23
 */
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
@EnableAspectJAutoProxy
public class TransactionConfig {
    
    /**
     * 编程式事务模板
     * 
     * @param transactionManager 事务管理器
     * @return 事务模板
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        
        // 设置事务超时时间（30秒）
        template.setTimeout(30);
        
        // 设置事务传播行为
        template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
        
        // 设置事务隔离级别
        template.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);
        
        return template;
    }
}
