# 智慧充电服务平台 (Charging Service Platform)

## 简介

本平台是一个基于 Spring Cloud 的微服务架构充电服务系统，旨在提供高效、可靠的充电站管理、充电订单处理和充电流程控制功能。

## 技术栈

- **后端**:
  - Java 21
  - Spring Boot 3
  - Spring Cloud
- **构建工具**: Gradle

## 模块结构

项目采用多模块结构，核心模块如下：

- `charging-station`: **充电站管理模块** - 负责充电站、充电桩信息管理和状态维护。
- `charging-order`: **订单管理模块** - 负责充电订单的创建、支付、查询和管理。
- `charging-process`: **充电流程模块** - 负责处理实际的充电启动、停止、实时数据上报等业务流程。

(未来可能增加 `charging-gateway`, `charging-common` 等模块)

## 快速开始

> **先决条件**:
> - JDK 21
> - Gradle 8.x

1.  **克隆项目**:
    ```bash
    git clone [your-repo-url]
    cd charging
    ```

2.  **编译和打包**:
    ```bash
    mvn clean package
    ```

3.  **运行服务**:
    (详细运行方式待各模块建立后补充)

## 文档

### 📋 项目规划文档
- [开发计划](./docs/development-plan.md) - 详细的开发计划和时间安排
- [任务跟踪](./docs/task-tracking.md) - 开发进度跟踪表
- [技术架构](./docs/technical-architecture.md) - 技术选型和架构设计

### 📖 业务文档
- [用户故事](./docs/user_stories.yml) - 完整的用户需求和验收标准
- [业务流程](./docs/flowchart.md) - 充电业务流程图
- [统一语言](./docs/ubiquitous_language.md) - 领域统一语言定义
- [充电会话状态机](./docs/充电会话状态机详细设计.md) - 状态机详细设计

### 🛠 开发规范
- [项目结构规范](./docs/ai-code-rules/ai-project.md) - DDD 分层架构规范
- [领域驱动设计](./docs/ai-code-rules/ai-ddd.md) - DDD 实践指南
- [测试规范](./docs/ai-code-rules/ai-testing.md) - 测试策略和规范

更多详细信息，请参阅 [docs](./docs) 目录下的文档。

## 许可证

本项目遵循 [MIT](./LICENSE) 许可证。
