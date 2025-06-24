# 充电站管理模块测试指南

本文档描述了充电站管理模块的完整测试策略、测试结构和运行方法。

## 📋 目录

- [测试策略](#测试策略)
- [测试结构](#测试结构)
- [运行测试](#运行测试)
- [测试覆盖率](#测试覆盖率)
- [测试最佳实践](#测试最佳实践)
- [故障排除](#故障排除)

## 🎯 测试策略

### 测试金字塔

我们采用标准的测试金字塔策略：

```
    /\
   /  \     E2E Tests (端到端测试)
  /____\    Integration Tests (集成测试)  
 /______\   Unit Tests (单元测试)
```

- **单元测试 (70%)**：快速、独立、专注于单个组件
- **集成测试 (20%)**：测试组件间的交互
- **端到端测试 (10%)**：测试完整的业务流程

### 测试分层

1. **单元测试层**
   - 领域模型测试
   - 应用服务测试
   - 领域服务测试

2. **集成测试层**
   - 数据库集成测试
   - 缓存集成测试
   - REST API 集成测试

3. **端到端测试层**
   - 完整业务流程测试
   - 系统集成测试

## 🏗️ 测试结构

### 目录结构

```
src/test/java/
├── com/ys/charging/station/
│   ├── TestBase.java                    # 测试基础类
│   ├── TestDataFactory.java            # 测试数据工厂
│   ├── config/
│   │   └── TestConfig.java             # 测试配置
│   ├── domain/
│   │   ├── model/                      # 领域模型单元测试
│   │   │   ├── StationTest.java
│   │   │   ├── ConnectorTest.java
│   │   │   └── LocationTest.java
│   │   └── service/                    # 领域服务单元测试
│   │       ├── GeoLocationServiceTest.java
│   │       └── ConnectorStateServiceTest.java
│   ├── application/
│   │   └── service/                    # 应用服务单元测试
│   │       ├── StationServiceTest.java
│   │       ├── ConnectorServiceTest.java
│   │       └── GeoSearchServiceTest.java
│   ├── infrastructure/
│   │   ├── persistence/                # 仓储集成测试
│   │   │   ├── JpaStationRepositoryIntegrationTest.java
│   │   │   └── JpaConnectorRepositoryIntegrationTest.java
│   │   └── cache/                      # 缓存集成测试
│   │       └── RedisCacheServiceIntegrationTest.java
│   ├── interfaces/
│   │   └── rest/                       # REST API 集成测试
│   │       └── StationControllerIntegrationTest.java
│   └── ChargingStationEndToEndTest.java # 端到端测试
└── resources/
    └── application-test.yml             # 测试配置文件
```

### 测试基础设施

#### TestBase.java
提供通用的测试数据和工具方法：
- 测试常量定义
- 测试数据创建方法
- 反射工具方法
- 断言辅助方法

#### TestDataFactory.java
专门用于创建测试数据：
- 标准测试对象创建
- 不同状态的对象创建
- 批量数据创建
- 特殊场景数据创建

#### TestConfig.java
测试环境配置：
- 嵌入式 Redis 配置
- 测试数据库配置
- Mock Bean 配置

## 🚀 运行测试

### 快速开始

```bash
# 给脚本执行权限
chmod +x run-tests.sh

# 运行所有测试
./run-tests.sh

# 或使用 Gradle
./gradlew test
```

### 分类运行

```bash
# 只运行单元测试
./run-tests.sh unit

# 只运行集成测试
./run-tests.sh integration

# 只运行端到端测试
./run-tests.sh e2e

# 生成覆盖率报告
./run-tests.sh coverage
```

### Gradle 命令

```bash
# 运行所有测试
./gradlew test

# 运行特定测试类
./gradlew test --tests StationTest

# 运行特定包的测试
./gradlew test --tests "**/domain/model/**/*Test"

# 跳过测试
./gradlew build -x test

# 生成测试报告
./gradlew test jacocoTestReport
```

### IDE 中运行

在 IntelliJ IDEA 或 Eclipse 中：
1. 右键点击测试类或方法
2. 选择 "Run Test" 或 "Debug Test"
3. 查看测试结果和覆盖率

## 📊 测试覆盖率

### 覆盖率目标

- **整体覆盖率**: ≥ 85%
- **领域模型**: ≥ 90%
- **应用服务**: ≥ 85%
- **REST API**: ≥ 80%

### 生成覆盖率报告

```bash
# 使用脚本生成
./run-tests.sh coverage

# 使用 Gradle 生成
./gradlew clean test jacocoTestReport

# 查看报告
open build/reports/jacoco/test/html/index.html
```

### 覆盖率分析

覆盖率报告包含：
- **行覆盖率**: 执行的代码行百分比
- **分支覆盖率**: 执行的分支百分比
- **方法覆盖率**: 调用的方法百分比
- **类覆盖率**: 涉及的类百分比

## 🎯 测试最佳实践

### 命名规范

```java
// 测试类命名
public class StationTest { }
public class StationServiceTest { }
public class StationControllerIntegrationTest { }

// 测试方法命名
@Test
@DisplayName("应该能够创建有效的充电站")
void shouldCreateValidStation() { }

@Test
@DisplayName("创建充电站时不能传入空的充电站信息")
void shouldThrowExceptionWhenStationInfoIsNull() { }
```

### 测试结构

使用 AAA 模式（Arrange-Act-Assert）：

```java
@Test
void shouldCreateStation() {
    // Arrange (Given)
    StationInfo stationInfo = new StationInfo(...);
    Location location = Location.of(...);
    
    // Act (When)
    Station station = Station.create(stationInfo, location, businessHours);
    
    // Assert (Then)
    assertNotNull(station);
    assertEquals(stationInfo, station.getStationInfo());
}
```

### 测试数据管理

```java
// 使用 TestDataFactory 创建测试数据
Station station = TestDataFactory.createTestStation();
Connector connector = TestDataFactory.createTestConnector(stationId);

// 使用 @Nested 组织相关测试
@Nested
@DisplayName("充电站创建测试")
class StationCreationTest {
    // 相关测试方法
}
```

### Mock 使用

```java
@ExtendWith(MockitoExtension.class)
class StationServiceTest {
    @Mock
    private StationRepository stationRepository;
    
    @Test
    void shouldCreateStation() {
        // Given
        when(stationRepository.save(any(Station.class))).thenReturn(expectedStation);
        
        // When & Then
        verify(stationRepository).save(any(Station.class));
    }
}
```

## 🔧 故障排除

### 常见问题

#### 1. Java 版本问题
```
错误: 需要 Java 21 或更高版本
解决: 确保使用 Java 21，设置 JAVA_HOME 环境变量
```

#### 2. Redis 连接失败
```
错误: Redis connection failed
解决: 检查嵌入式 Redis 配置，确保端口未被占用
```

#### 3. 数据库连接问题
```
错误: H2 database connection failed
解决: 检查 application-test.yml 配置，确保数据库 URL 正确
```

#### 4. 测试超时
```
错误: Test timeout
解决: 增加测试超时时间，检查异步操作
```

### 调试技巧

1. **启用详细日志**：
   ```yaml
   logging:
     level:
       com.ys.charging.station: DEBUG
   ```

2. **使用测试切片**：
   ```java
   @DataJpaTest  // 只加载 JPA 相关配置
   @WebMvcTest   // 只加载 Web 层配置
   ```

3. **条件测试**：
   ```java
   @EnabledIf("#{environment.acceptsProfiles('integration')}")
   @DisabledOnOs(OS.WINDOWS)
   ```

### 性能优化

1. **并行测试**：
   ```xml
   <plugin>
     <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-surefire-plugin</artifactId>
     <configuration>
       <parallel>methods</parallel>
       <threadCount>4</threadCount>
     </configuration>
   </plugin>
   ```

2. **测试分组**：
   ```java
   @Tag("unit")
   @Tag("integration")
   @Tag("slow")
   ```

3. **资源清理**：
   ```java
   @AfterEach
   void cleanup() {
       // 清理测试数据
   }
   ```

## 📈 持续集成

### GitHub Actions 配置

```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: ./gradlew test
      - uses: codecov/codecov-action@v3
```

### 测试报告

- **Gradle 测试报告**: `build/reports/tests/test/`
- **JaCoCo 覆盖率**: `build/reports/jacoco/test/html/`
- **测试结果**: `build/test-results/test/`

## 📚 参考资料

- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot 测试指南](https://spring.io/guides/gs/testing-web/)
- [AssertJ 断言库](https://assertj.github.io/doc/)
- [Testcontainers](https://www.testcontainers.org/)

---

**注意**: 确保在提交代码前运行完整的测试套件，保持高质量的代码覆盖率。
