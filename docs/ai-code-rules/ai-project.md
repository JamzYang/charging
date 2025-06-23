# 项目结构规范 (Project Structure Conventions)
```
charging-service/
├── src/
│ ├── main/
│ │ ├── java/
│ │ │ └── com/
│ │ │ └── ys/
│ │ │ └── charging/
│ │ │ ├── application/ (应用层)
│ │ │ │ ├── dto
│ │ │ │ ├── service
│ │ │ │ ├── command
│ │ │ │ ├── query
│ │ │ │ └── event (系统事件)
│ │ │ ├── domain/ (领域层)
│ │ │ │ ├── model/ (实体、值对象、聚合根)
│ │ │ │ ├── service/ (领域服务)
│ │ │ │ ├── repository/ (仓储接口)
│ │ │ │ └── event/ (领域事件)
│ │ │ ├── infrastructure/ (基础设施层)
│ │ │ │ ├── config/ (配置)
│ │ │ │ ├── persistence/ (仓储实现)
│ │ │ │ └── external/ (外部服务适配器)
│ │ │ ├── adapter/ (展现层/接口层)
│ │ │ │ ├── controller/ (控制器)
│ │ │ │ ├── consumer/ (MQ消费者)
│ │ │ │ └── exception/
│ │ │ └── util/ (工具类)
│ │ └── resources/
│ │ └── application.yml
│ │ └── bootstrap.yml
│ │ └── logback-spring.xml
│ └── test/
│ └── ...
├── build.gradle
```

# 配置规范 (Spring Boot Configuration Conventions)

## 配置文件 (Configuration Files)

*   **格式：**
    *  使用 YAML 格式 (`application.yml`)。

## 配置方式 (Configuration Methods)

*   **@Value:**
    *   注入单个配置属性。
    ```java
    @Value("${my.property}")
    private String myProperty;
    ```

*   **@ConfigurationProperties:**
    *   将一组相关的配置属性绑定到一个对象。

    ```java
    @ConfigurationProperties(prefix = "my.config")
    public class MyConfig {
        private String property1;
        private int property2;
        // ... getters and setters ...
    }
    ```
   *   需要添加 `@Configuration` 或 `@Component` 注解

## 最佳实践

*   使用 `@ConfigurationProperties` 来绑定配置属性到对象。
*   将配置信息外部化，不要硬编码在代码中。
*   使用 Profile 来管理不同环境的配置。


# 编码规范 (Coding Conventions)

*   遵循 DRY (Don't Repeat Yourself) 原则，避免重复代码。
*   遵循 SOLID 原则。
*   遵循 KISS (Keep It Simple, Stupid) 原则。
*   遵循 YAGNI (You Ain't Gonna Need It) 原则。
*    及早失败(Fail-Fast), 尽早发现错误, 尽早处理异常
*   卫式语句(Guard Clause),尽早返回
*   遵循《阿里巴巴 Java 开发手册》