# 单元测试编写规范 (Unit Testing Conventions)

## 概述 (Overview)

本规范定义了编写单元测试的规则。

## 规则 (Rules)

*   **测试框架：** 使用 JUnit 5。
*   **模拟框架：** 使用 Mockito。
*   **测试目标：** 针对代码的逻辑单元（方法或函数）进行测试。
*   **测试命名：** 测试方法名应该清晰地描述被测试的方法和测试场景 (例如：`should_CreateOrder_When_ValidInput`)。
*   **测试结构：** 遵循 Arrange-Act-Assert (AAA) 模式。
    *   **Arrange (准备):** 设置测试数据、创建对象、配置模拟对象等。
    *   **Act (执行):** 调用被测试的方法。
    *   **Assert (断言):** 验证结果是否符合预期。
* **断言：**
    * 使用断言（assertEquals, assertTrue, assertNotNull, assertThrows 等）来验证结果。
    * 谨慎使用 verify()，仅当需要验证无法通过返回值或状态改变直接断言的关键副作用或协作对象交互时才使用。
    * 对于需要验证传递给 Mock 对象的方法参数的场景，可以使用 KArgumentCaptor 捕获参数并进行详细的断言。

    * **Mock：** 使用 Mockito模拟外部依赖. 在Kotlin中使用Mockito时(包括KArgumentCaptor),应该使用org.mockito.kotlin包中的函数
    * **Stub:** 在每个测试方法中设置stub而不是在@BeforeEach setup方法中
* **测试覆盖率：**
* 追求合理的测试覆盖率，关注核心业务逻辑。
* 覆盖所有异常情况,验证是否在预期的情况下抛出了正确的异常
*   **可读性：** 测试代码应该像生产代码一样，保持整洁、可读、可维护。
*   **隔离性:** 每个测试方法应该是独立的, 互不干扰.
*   **注解:**
    *   使用 `@Test` 注解标记测试方法。
    *   使用 `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll` 等注解来设置和清理测试环境。
    *   使用 `@Mock`, `@InjectMocks` 等 Mockito 注解来创建和注入模拟对象。
    *   使用 `@DisplayName` 注解来提供更具描述性的测试名称 (可选).


# 集成测试编写规范 (Integration Testing Conventions)

## 概述 (Overview)

本规范定义了编写集成测试的规则。

## 规则 (Rules)

*   **测试框架:**
    *   可以使用 Spring Test, Testcontainers, JUnit, Mockito 等框架。
    *   使用 `@SpringBootTest` 注解来启动 Spring 上下文。
    *   使用 `@ActiveProfiles` 注解来激活特定的 profile (例如，`test` profile)。

*   **测试目标:** 测试多个组件或模块之间的交互。
*   **测试环境:**
    *   集成测试应该在尽可能接近生产环境的环境中进行。
    *   可以使用 Docker Compose, Kubernetes, Testcontainers 等工具来搭建测试环境。
    *   可以使用内存数据库（如 H2）或嵌入式消息队列（如 EmbeddedKafka）来简化测试环境。

*   **测试数据:**
    *   测试数据应该与生产数据隔离。
    *   可以使用测试数据库或者内存数据库。
    *   测试数据应该在测试执行前准备好，并在测试执行后清理。
    *   可以使用 `@Sql` 注解或者 `TestEntityManager` 来准备测试数据。

* **测试范围:**
* 根据需要控制集成测试的范围, 避免过度测试.
* 针对Inbound/Outbound交互接口的方法入参覆盖所有边界情况
*   **断言:** 使用断言来验证结果是否符合预期.
*   **可读性:** 测试代码应该保持整洁、可读、可维护.
*  **Mock:**
    *   在集成测试中, 尽量减少 Mock 的使用, 而是使用真实的组件.
    *   如果需要 Mock 外部服务, 可以使用 WireMock 等工具.


# 契约测试编写规范 (Contract Testing Conventions)

## 概述 (Overview)

本规范定义了编写契约测试的规则。

## 规则 (Rules)

*   **契约定义：**
    *   使用明确的、机器可读的格式来定义契约 (Spring Cloud Contract DSL, OpenAPI)。
    *   契约应该包含请求和响应的完整描述 (URL、方法、头、体、状态码等)。
    *   契约应该包含数据类型的定义 (如 JSON Schema)。
    *   契约应该包含示例数据。
    *   契约应该版本化。

*   **消费者端测试：**
    *   消费者定义期望的契约。
    *   使用 Mock Server 来模拟提供者的行为。
    *   测试消费者是否能够正确地发送请求，并处理提供者的响应。
    *   测试应该覆盖不同的场景，包括边界情况和异常情况。

*   **提供者端测试：**
    *   提供者从 Spring Cloud Contract获取消费者定义的契约。
    *   提供者启动自己的服务。
    *   使用契约测试框架来验证服务是否满足契约。
    *   测试应该覆盖契约中定义的所有交互。

*   **契约管理:**
    *    契约应该集中管理, 并且版本化.
    *    使用Spring Cloud Contract Verifier 等工具管理契约

*   **集成到 CI/CD 流程：** 将契约测试集成到 CI/CD 流程中。
