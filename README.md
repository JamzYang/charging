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

更多详细信息，请参阅 [docs](./docs) 目录下的文档。

## 许可证

本项目遵循 [MIT](./LICENSE) 许可证。
