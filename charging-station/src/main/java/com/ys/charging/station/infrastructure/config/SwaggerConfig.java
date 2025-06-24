package com.ys.charging.station.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger API 文档配置
 * 
 * 配置 OpenAPI 3.0 文档生成，提供完整的 API 接口文档。
 * 
 * @author yang
 * @since 2025-06-23
 */
@Configuration
public class SwaggerConfig {
    
    /**
     * 配置 OpenAPI 文档
     * 
     * @return OpenAPI 配置
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(apiServers());
    }
    
    /**
     * API 基本信息
     */
    private Info apiInfo() {
        return new Info()
            .title("充电站管理系统 API")
            .description("""
                充电站管理系统的 RESTful API 接口文档
                
                ## 功能模块
                
                ### 充电站管理
                - 充电站的创建、查询、更新、删除
                - 充电站状态管理和营业时间设置
                - 充电站搜索和分页查询
                
                ### 充电桩管理
                - 充电桩状态管理和控制
                - 预约管理和充电会话控制
                - 故障管理和心跳监控
                
                ### 地理搜索
                - 附近充电站搜索
                - 距离计算和路径规划
                - 地理位置数据同步
                
                ## 技术特性
                
                - **高性能**: Redis GEO 地理搜索，微秒级响应
                - **高可用**: 多级缓存，降级容错机制
                - **实时性**: WebSocket 状态推送，事件驱动架构
                - **可扩展**: 微服务架构，支持水平扩展
                
                ## 认证方式
                
                API 使用 JWT Token 进行认证，请在请求头中添加：
                ```
                Authorization: Bearer <your-jwt-token>
                ```
                """)
            .version("1.0.0")
            .contact(apiContact())
            .license(apiLicense());
    }
    
    /**
     * API 联系信息
     */
    private Contact apiContact() {
        return new Contact()
            .name("充电站管理系统开发团队")
            .email("dev@charging-station.com")
            .url("https://github.com/charging-station/api");
    }
    
    /**
     * API 许可证信息
     */
    private License apiLicense() {
        return new License()
            .name("MIT License")
            .url("https://opensource.org/licenses/MIT");
    }
    
    /**
     * API 服务器列表
     */
    private List<Server> apiServers() {
        return List.of(
            new Server()
                .url("http://localhost:8080")
                .description("本地开发环境"),
            new Server()
                .url("https://api-dev.charging-station.com")
                .description("开发测试环境"),
            new Server()
                .url("https://api.charging-station.com")
                .description("生产环境")
        );
    }
}
