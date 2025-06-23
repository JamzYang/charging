# 任务：充电站管理模块开发 (charging-station)

> DN 开发过程记录

## 一、任务元数据

*   **任务 ID**: `charging-station-module`
*   **文件名**: `2025-06-23_2`
*   **创建时间**: `2025-06-23_16:45:00`
*   **创建者**: `AI Assistant`
*   **关联项目/模块**: charging-station
*   **主分支**: `dev`
*   **任务分支**: `task/charging-station-module_2025-06-23_2`

## 二、任务状态与概览

*   **当前状态**: 规划中
*   **任务描述 (原始需求)**:
    ```
    开发充电站管理模块（charging-station），实现充电站、充电桩、车位信息管理和状态维护功能。
    按照 DDD 分层架构设计，包含核心领域建模、基础设施层、应用服务层和接口层。
    使用 Java 21 特性，遵循项目技术规范。
    ```
*   **项目概览 (相关背景)**:
    ```
    智慧充电服务平台的核心模块之一，为其他模块（charging-order、charging-process）提供基础数据支持。
    需要管理充电站基本信息、充电桩状态、车位和地锁管理、地理位置信息等。
    支持实时状态同步、地理位置搜索、缓存优化等技术特性。
    ```

⚠️ **核心协议提醒**: 
- 严格遵循 DDD 分层架构原则
- 使用 Java 21 特性（sealed interface、record、pattern matching）
- 聚合根封装业务规则，确保数据一致性
- 事件驱动架构，模块间通过领域事件解耦
- 测试驱动开发，确保代码质量
- 渐进式开发，每个阶段都有可交付成果
⚠️ **核心协议提醒** ⚠️

## 三、研究日志 (RESEARCH Mode)

*   **目标**:
    *   分析 charging-station 模块的业务需求和技术要求
    *   研究现有代码结构和依赖关系
    *   理解与其他模块的集成接口需求
    *   确定核心领域模型和技术架构

*   **发现与观察**:
    *   charging-station 模块当前仅有基础框架，src/main/java 目录下已有部分枚举类（StationStatus、ConnectorStatus）
    *   build.gradle.kts 已配置基础依赖，但缺少 JPA、数据库、缓存等状态管理所需依赖
    *   根据业务需求分析，需要管理四个核心实体：Station（充电站）、Connector（充电桩）、ParkingSpot（车位）、Location（地理位置）
    *   从 user_stories.yml 中识别出关键业务场景：地图浏览、筛选、预约、地锁控制、实时状态查询等
    *   从 ubiquitous_language.md 中确认了核心概念：场站、充电桩、车位、地锁、地理围栏等
    *   技术架构文档中明确了模块间的 Feign 调用关系：charging-order 和 charging-process 都需要调用 charging-station 的接口
    *   充电桩状态管理复杂：7个状态（IDLE、RESERVED、OCCUPIED、CHARGING、FAULT、OFFLINE、MAINTENANCE）
    *   地锁控制是关键业务功能：支持远程降锁、5分钟超时自动升锁机制

*   **提出的问题**:
    *   充电站聚合根应该包含哪些业务不变量和规则？（营业时间、服务范围、充电桩数量限制等）
    *   充电桩状态管理的复杂度如何处理（7个状态的流转，并发更新控制）？
    *   地理位置搜索功能需要什么样的技术支持（空间索引、距离计算、地理围栏）？
    *   与 charging-order 和 charging-process 模块的集成接口如何设计（Feign 客户端、DTO 转换）？
    *   缓存策略如何设计以优化查询性能（充电站列表、充电桩状态、地理位置数据）？
    *   地锁控制的技术实现方案（远程控制、超时机制、状态同步）？
    *   如何处理充电桩的实时状态同步（WebSocket、SSE、轮询）？

*   **初步识别的风险/约束**:
    *   需要添加 JPA、数据库、缓存等依赖，确保与项目版本兼容（参考 charging-process 模块的依赖配置）
    *   充电桩状态的并发更新需要考虑乐观锁机制（@Version 字段）
    *   地理位置搜索可能需要专门的 GIS 数据库支持或空间索引（MySQL 空间索引或 PostGIS）
    *   模块间的 Feign 调用需要考虑服务可用性和熔断机制（Spring Cloud LoadBalancer + Resilience4j）
    *   实时状态同步可能需要 WebSocket 或 SSE 技术支持（充电桩状态变更通知）
    *   地锁控制需要与外部硬件系统集成，可能存在网络延迟和失败重试问题
    *   缓存一致性问题：充电桩状态变更时需要及时更新缓存
    *   数据库设计需要支持地理位置查询和空间索引优化

## 四、创新与构思 (INNOVATE Mode)

*   **探索的解决方案**:

    1.  **方案 A：传统 DDD + JPA 方案**
        *   优点:
            - 严格遵循 DDD 分层架构，与 charging-process 模块保持一致
            - JPA 成熟稳定，开发效率高
            - Spring Boot 生态完整，集成简单
            - 团队熟悉度高，维护成本低
        *   缺点:
            - 地理位置搜索性能可能不够优化
            - 大量充电桩状态查询时可能存在性能瓶颈
            - 缓存策略需要额外设计
        *   初步评估: 适合 MVP 版本，技术风险低，开发周期短

    2.  **方案 B：DDD + Redis 地理查询优化方案**
        *   优点:
            - Redis GEO 命令支持高性能地理位置查询（GEORADIUS、GEOSEARCH）
            - 内存存储，查询速度极快（微秒级响应）
            - 支持复杂的地理查询（距离、围栏、附近搜索）
            - 技术栈简单，无需引入额外的地理数据库
            - 与现有 Redis 缓存方案完美融合
            - 支持实时位置更新和查询
        *   缺点:
            - 内存占用较大，成本相对较高
            - 数据持久化需要额外考虑
            - 复杂的空间分析功能有限
        *   初步评估: 非常适合当前场景，性能优异且技术复杂度适中

    3.  **方案 C：事件驱动 + CQRS 方案**
        *   优点:
            - 读写分离，查询性能优化
            - 事件驱动架构，模块解耦度高
            - 支持复杂的业务规则和状态管理
            - 扩展性和可维护性好
        *   缺点:
            - 架构复杂度高，开发难度大
            - 数据一致性处理复杂
            - 调试和排错困难
        *   初步评估: 适合大型系统，但当前规模可能不需要

*   **关键决策点与理由**:
    *   **技术栈选择**: 重新评估后，推荐采用 **方案 A + 方案 B 混合方案**
    *   **地理位置处理**: 使用 **Redis GEO** 作为地理查询的主要方案，MySQL 作为数据持久化
    *   **缓存策略**: Redis 既作为缓存又作为地理查询引擎，一举两得
    *   **状态同步**: 使用 Spring 事件机制 + WebSocket 实现实时状态推送
    *   **模块集成**: 通过 Feign 客户端提供同步接口，通过消息队列处理异步事件

*   **Redis GEO 方案的具体优势**:
    *   **性能卓越**: 内存查询，响应时间在微秒级别
    *   **功能丰富**: 支持按距离搜索、按区域搜索、获取距离、获取坐标等
    *   **易于集成**: 项目已计划使用 Redis，无需额外引入组件
    *   **实时性强**: 支持充电站位置的实时更新和查询
    *   **成本合理**: 充电站数据量相对较小，内存成本可控

*   **核心架构设计思路**:

    **1. 领域模型设计**
    ```
    Station (聚合根)
    ├── StationInfo (值对象) - 基本信息
    ├── Location (值对象) - 地理位置
    ├── BusinessHours (值对象) - 营业时间
    ├── List<Connector> (实体集合) - 充电桩列表
    └── StationStatus (枚举) - 充电站状态

    Connector (实体)
    ├── ConnectorInfo (值对象) - 充电桩信息
    ├── ConnectorStatus (枚举) - 充电桩状态
    ├── ParkingSpot (值对象) - 关联车位
    └── ReservationInfo (值对象) - 预约信息
    ```

    **2. 分层架构设计**
    ```
    adapter/controller     - REST API 控制器
    application/service    - 应用服务层
    domain/model          - 领域模型
    domain/repository     - 仓储接口
    domain/service        - 领域服务
    infrastructure/       - 基础设施层
    ├── persistence/      - JPA 实现
    ├── cache/           - 缓存实现
    ├── external/        - 外部服务适配器
    └── config/          - 配置管理
    ```

    **3. 关键技术决策（更新后）**
    - **Java 21 特性**: 使用 record 定义值对象，sealed interface 定义领域事件
    - **状态管理**: 乐观锁 + 状态机模式处理充电桩状态流转
    - **地理搜索**: **Redis GEO** 作为主要地理查询引擎
    - **缓存策略**: Caffeine 本地缓存 + Redis（缓存 + 地理查询）
    - **实时通信**: WebSocket + Spring 事件机制

    **4. Redis GEO 技术方案详细设计**
    ```java
    // 充电站地理位置存储结构
    Key: "stations:geo"
    Value: GEOADD stations:geo longitude latitude station_id

    // 主要查询命令
    GEORADIUS stations:geo longitude latitude radius km WITHDIST WITHCOORD
    GEOSEARCH stations:geo BYRADIUS longitude latitude radius km
    GEOPOS stations:geo station_id
    GEODIST stations:geo station1_id station2_id km
    ```

    **5. 数据同步策略**
    - **写入**: MySQL 主存储 → Redis GEO 同步更新
    - **查询**: 地理位置查询走 Redis，详细信息查询走缓存或数据库
    - **一致性**: 使用事务确保 MySQL 和 Redis 的数据一致性

## 五、详细规划 (PLAN Mode)

*   **选定方案**: 方案 A + 方案 B 混合方案（DDD + Redis GEO 地理查询优化）

*   **技术规格与实施清单**:

    1.  **依赖配置与项目基础设施**
        1.1. 修改 `charging-station/build.gradle.kts`，添加 JPA、Redis、缓存等必需依赖
        1.2. 创建 `src/main/resources/application.yml` 配置文件
        1.3. 创建主应用类 `ChargingStationApplication.java`
        1.4. 配置 Redis 连接和 GEO 功能支持

    2.  **DDD 分层包结构创建**
        2.1. 创建基础包结构 `com.ys.charging.station`
        2.2. 创建 `domain.model` 包（聚合根、实体、值对象、枚举）
        2.3. 创建 `domain.event` 包（领域事件）
        2.4. 创建 `domain.service` 包（领域服务）
        2.5. 创建 `domain.repository` 包（仓储接口）
        2.6. 创建 `application.service` 包（应用服务）
        2.7. 创建 `infrastructure.persistence` 包（JPA 仓储实现）
        2.8. 创建 `infrastructure.cache` 包（Redis 缓存和 GEO 实现）
        2.9. 创建 `adapter.controller` 包（REST API）

    3.  **核心领域模型设计**
        3.1. 完善 `StationStatus` 和 `ConnectorStatus` 枚举
        3.2. 创建 `Station` 聚合根（包含充电桩集合管理）
        3.3. 创建 `Connector` 实体（充电桩状态管理）
        3.4. 创建 `ParkingSpot` 实体（车位和地锁管理）
        3.5. 创建值对象：`Location`、`BusinessHours`、`ConnectorInfo`、`StationInfo`
        3.6. 创建领域事件：`StationStatusChangedEvent`、`ConnectorStatusChangedEvent`

    4.  **地理位置服务设计（Redis GEO）**
        4.1. 创建 `GeoLocationService` 领域服务
        4.2. 实现充电站地理位置的 Redis GEO 存储
        4.3. 实现附近充电站搜索功能（GEORADIUS）
        4.4. 实现距离计算功能（GEODIST）
        4.5. 实现地理位置数据同步机制

    5.  **状态管理与缓存设计**
        5.1. 创建 `ConnectorStateService` 领域服务
        5.2. 实现充电桩状态机逻辑
        5.3. 实现 Redis 缓存策略（充电站信息、充电桩状态）
        5.4. 实现缓存与数据库的数据一致性机制
        5.5. 实现乐观锁并发控制

    6.  **数据持久化层实现**
        6.1. 设计数据库表结构（stations、connectors、parking_spots）
        6.2. 创建 JPA 实体映射
        6.3. 实现 Repository 接口和 JPA 实现
        6.4. 配置数据源和事务管理
        6.5. 创建数据库初始化脚本

    7.  **应用服务层实现**
        7.1. 创建 `StationService` 应用服务
        7.2. 创建 `ConnectorService` 应用服务
        7.3. 创建 `GeoSearchService` 应用服务
        7.4. 实现业务逻辑协调和事务管理
        7.5. 实现领域事件发布

    8.  **REST API 接口设计**
        8.1. 创建 `StationController` 控制器
        8.2. 创建 `ConnectorController` 控制器
        8.3. 创建 `GeoSearchController` 控制器
        8.4. 实现 Feign 客户端所需的接口
        8.5. 实现 API 文档和错误处理

    9.  **外部集成接口**
        9.1. 实现地锁控制外部接口适配器
        9.2. 实现充电桩硬件状态同步接口
        9.3. 实现 WebSocket 实时状态推送
        9.4. 实现消息队列事件发布

    10. **测试实现**
        10.1. 单元测试（领域模型、服务层）
        10.2. 集成测试（数据库、Redis、API）
        10.3. 地理位置查询性能测试
        10.4. 并发状态更新测试
        10.5. 端到端测试

*   **预期依赖变更**:
    *   Spring Boot Starter Data JPA - 数据持久化
    *   Spring Boot Starter Data Redis - Redis 缓存和 GEO 查询
    *   Spring Boot Starter Cache - 缓存抽象
    *   Spring Cloud Starter OpenFeign - 服务间调用
    *   Spring Cloud Starter Sleuth - 分布式链路追踪
    *   MySQL Connector - 数据库驱动
    *   H2 Database - 测试数据库
    *   Caffeine - 本地缓存
    *   TestContainers - 集成测试

*   **测试策略**:
    *   **单元测试**:
        - 领域模型业务规则测试
        - 状态机逻辑测试
        - 地理位置计算测试
        - 缓存策略测试
    *   **集成测试**:
        - Redis GEO 功能测试
        - 数据库事务测试
        - REST API 接口测试
        - Feign 客户端测试
    *   **性能测试**:
        - 地理位置查询性能测试
        - 并发状态更新测试
        - 缓存命中率测试

*   **关键技术实现细节**:

    **Redis GEO 数据结构设计**:
    ```
    Key: "stations:geo" - 存储所有充电站的地理位置
    Key: "station:{id}:connectors" - 存储充电站的充电桩列表
    Key: "connector:{id}:status" - 存储充电桩状态信息
    Key: "station:{id}:info" - 存储充电站详细信息
    ```

    **数据库表结构设计**:
    ```sql
    stations: id, name, operator, address, longitude, latitude, status, business_hours, created_at, updated_at, version
    connectors: id, station_id, connector_number, connector_type, power, status, parking_spot_id, created_at, updated_at, version
    parking_spots: id, station_id, spot_number, has_lock, lock_status, created_at, updated_at
    ```

    **核心接口设计**:
    ```java
    // Feign 客户端接口
    GET /api/v1/stations/{stationId}
    GET /api/v1/stations/{stationId}/connectors/{connectorId}
    POST /api/v1/stations/{stationId}/connectors/{connectorId}/reserve
    PUT /api/v1/connectors/{connectorId}/status
    POST /api/v1/connectors/{connectorId}/lock/down
    POST /api/v1/connectors/{connectorId}/lock/up
    GET /api/v1/stations/nearby?lat={lat}&lng={lng}&radius={radius}
    ```

## 六、执行日志 (EXECUTE Mode)

## 七、审查日志 (REVIEW Mode)

## 八、任务总结与产出 (完成后填写)

## 九、遇到的障碍与解决方案
