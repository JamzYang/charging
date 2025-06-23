# 领域驱动设计 (Domain-Driven Design - DDD)

## 概述 (Overview)

本规范定义了在微服务架构中应用领域驱动设计 (DDD) 的概念和实践。DDD 是一种以领域为核心的软件开发方法，旨在处理复杂的业务逻辑。

## 核心概念 (Core Concepts)

*   **领域 (Domain):** 业务所关注的特定范围。
*   **子域 (Subdomain):** 将领域进一步划分为更小的、更易于管理的部分。
*   **通用语言 (Ubiquitous Language):** 团队成员（包括开发人员、领域专家等）之间使用的通用词汇表，用于描述领域概念。
*   **实体 (Entity):** 具有唯一标识符的对象，其标识在整个生命周期中保持不变。
*   **值对象 (Value Object):** 没有唯一标识符的对象，通过其属性值来定义。值对象是不可变的。
*   **聚合 (Aggregate):** 一组相关对象的集合，它们被视为一个整体进行数据更改。
*   **聚合根 (Aggregate Root):** 聚合的入口点，负责协调聚合内对象的状态和行为。
*   **领域服务 (Domain Service):** 当某个操作不属于任何一个实体或值对象时，可以使用领域服务。领域服务是无状态的。
*   **仓储 (Repository):** 提供对聚合的持久化访问。
*   **领域事件 (Domain Event):** 表示领域中发生的有意义的事情。
*   **限界上下文 (Bounded Context):** 领域模型的边界，每个限界上下文都有自己的通用语言和领域模型。

## 实践 (Practices)

*   **识别限界上下文：** 将大型领域划分为多个限界上下文，每个限界上下文都有明确的职责和边界。
*   **定义通用语言：** 与领域专家合作，定义一套清晰、一致的通用语言。
*   **领域建模：** 使用实体、值对象、聚合等概念来构建领域模型。
*   **代码与模型一致：** 确保代码与领域模型保持一致。
*   **迭代式开发：** DDD 是一个迭代的过程，不断完善领域模型。
*   **战术设计模式:** 实体, 值对象, 聚合, 工厂, 仓储, 领域服务, 领域事件
*   **战略设计模式:** 限界上下文, 上下文映射, 防腐层

## 与 CQRS 的结合

*   DDD 和 CQRS (Command Query Responsibility Segregation) 可以很好地结合使用。
*   命令 (Command) 表示对领域模型的修改操作。
*   查询 (Query) 表示对领域模型的读取操作。
*   使用 CQRS 可以将读写操作分离，提高系统的性能和可扩展性。

# 实体类编写规范 (Entity Conventions)

## 概述 (Overview)

本规范定义了实体类的编写规则。

## 规则 (Rules)

*   **注解：**
    *   使用 `@Entity` 注解标记实体类。
    *   使用 `@Table` 注解指定表名（可选）。
    *   使用 `@Data` (Lombok) 或手动生成 getter/setter、`equals`、`hashCode`、`toString` 方法.
    *   使用 `@NoArgsConstructor`, `@AllArgsConstructor` 等 Lombok 注解简化构造函数.

*   **标识符：**
    *   使用 `@Id` 注解标记主键字段。
    *   使用 `@GeneratedValue` 注解指定主键生成策略（如 `GenerationType.IDENTITY`）。
    *   使用 Long 类型作为标识符.

*   **属性：**
    *   使用 `@Column` 注解映射属性到数据库列（可选，当列名与属性名不一致时需要）。
    *   使用 `@Transient` 注解标记不需要持久化的属性。
    *    字段尽量使用`private`

*   **关联关系：**
    *   使用 `@OneToOne`, `@OneToMany`, `@ManyToOne`, `@ManyToMany` 注解定义实体之间的关系。
    *   使用 `fetch = FetchType.LAZY` 进行延迟加载。
    *   使用 `cascade` 属性指定级联操作。
    *   使用 `mappedBy` 属性指定关系的维护方。
    *    避免双向关联, 尽量使用单向关联.

*   **业务方法：**
    *   在实体类中实现与实体相关的业务逻辑。
    *   方法名应该清晰地表达业务含义。
    *  尽量避免在实体类中直接操作集合属性，而是提供 addXXX, removeXXX 等方法。
    *  实体类应该尽量避免副作用 (Side Effect)，方法不应该修改除了自身以外的其它对象的状态

*   **继承：**
     *   可以创建一个 BaseEntity, 包含 id, createTime, updateTime 等通用的属性和方法.
     *   实体类可以继承 BaseEntity.

* **其他**
    * 避免在实体类中注入 Service, Repository 等组件。
    * 实体类不应该直接依赖于基础设施层的组件



# 值对象类编写规范 (Value Object Conventions)

## 概述

本规范定义值对象类的编写规则。

## 规则

*   **不可变性:**
    *   值对象应该是不可变的。
    *   将所有属性设置为 `final` (Java) 或 `val` (Kotlin)。
    *   不提供 setter 方法。
    *    可以使用 `@Value` (Lombok) 注解。

*   **相等性:**
    *   重写 `equals()` 和 `hashCode()` 方法，基于所有属性值进行比较。
    *  可以使用Objects.equals 和 Objects.hash

*   **构造函数:**
    *   提供一个包含所有属性的构造函数。
    *   可以在构造函数中进行参数校验。
    *   可以提供多个构造函数，以支持不同的创建方式.

*   **业务方法:**
    *   可以包含一些简单的、与值对象相关的业务逻辑。
    *  避免复杂的业务逻辑



# 聚合根类编写规范 (Aggregate Root Conventions)

## 概述 (Overview)

本规范定义了聚合根类的编写规则。

## 规则 (Rules)

*   **聚合根是一个实体：** 聚合根类应该遵循实体类的所有规则。
*   **唯一入口点：** 聚合根是聚合的唯一入口点，所有对聚合内对象的操作都必须通过聚合根进行。
*   **协调者：** 聚合根负责协调聚合内对象的状态和行为。
*   **维护一致性：** 聚合根负责维护聚合的完整性和一致性。
*   **发布领域事件：** 聚合根可以发布领域事件，通知其他聚合或外部系统。
*   **标识符：** 聚合根的 ID 是全局唯一的。
*   **生命周期:** 聚合根负责管理聚合内所有对象的生命周期。
*   **接口(可选):** 可以定义一个 `IAggregateRoot` 接口, 用于标记聚合根.
*   **大小:** 聚合根不应该过大， 应该尽量保持小而简单。

# 仓储接口编写规范 (Repository Interface Conventions)

## 概述 (Overview)

本规范定义了仓储接口的编写规则。

## 规则 (Rules)

*   **接口：** 仓储应该是一个接口。
*   **命名：** 仓储接口名通常以 `Repository` 结尾 (例如：`OrderRepository`)。
*   **泛型：** 使用泛型来指定聚合根的类型和 ID 类型。
*   **继承：** 继承 Spring Data JPA 的 `JpaRepository` 接口（或其他 ORM 框架提供的类似接口）。
    *   可以自定义一个 BaseRepository, 包含通用的方法，如 `findById`, `save`, `delete` 等
    *   具体的 Repository 接口可以继承 BaseRepository.
*   **方法：**
    *   提供对聚合的 CRUD 操作。
    *   可以使用方法名查询或 JPQL 查询。
    *   方法名应该清晰地表达查询的意图。
    *   避免在仓储接口中定义业务逻辑。
    *   可以使用 `@Query` 注解自定义查询。
    *   可以使用 `@EntityGraph` 注解来避免 N+1 问题。
    *   对于复杂的查询, 可以返回 DTO 或自定义的投影接口.

*   **注解：** 使用 `@Repository` 注解标记仓储接口。

# 仓储实现类编写规范 (Repository Implementation Conventions)

## 概述 (Overview)

本规范定义了仓储实现类的编写规则，通常使用 Spring Data JPA 实现。

## 规则 (Rules)

*   **实现接口：** 仓储实现类应该实现相应的仓储接口。
*   **注解：**
    *   使用 `@Repository` 注解标记仓储实现类。
*   **依赖注入：**
    *   使用构造函数注入或 `@Autowired` 注解注入 `EntityManager` 或 `JpaRepository`。
*   **方法实现：**
    *   使用 Spring Data JPA 提供的方法（如 `findById`, `save`, `delete`, `findAll` 等）。
    *   对于复杂查询，可以使用 JPQL (`@Query`) 或 Criteria API。
    *   避免在仓储实现类中包含业务逻辑。
*   **异常处理：**
    *   让 Spring Data JPA 处理底层数据库异常。
*  **事务管理**
    *  Repository 的方法默认是事务性的, 不需要显式添加 `@Transactional` 注解.
* **其他**
    *  避免直接使用原生的 SQL 语句, 除非 JPA/JPQL 无法实现.


# 领域服务接口编写规范 (Domain Service Interface Conventions)

## 概述

本规范定义领域服务接口的编写规则。

## 规则

*   **接口:** 领域服务通常是一个接口.

*   **命名:** 领域服务接口名通常以 `Service` 结尾, 例如: `OrderService`。

*   **无状态:** 领域服务应该是无状态的。

*   **方法:**
    *   方法名通常以动词开头，表示一个领域操作。
    *   方法参数和返回值应该是领域对象（实体、值对象）或 DTO。
    *    避免在领域服务中直接操作数据库。
    *   领域服务可以协调多个聚合根或实体来完成一个业务操作。

*    **注解:** 可以使用 `@Service` 注解, 将领域服务注册为 Spring 的 Bean.


# 领域事件类编写规范 (Domain Event Conventions)

## 概述

本规范定义领域事件类的编写规则。

## 规则

*   **不可变性:** 领域事件应该是不可变的。
*   **命名:** 类名使用过去时态动词 (例如：`OrderCreatedEvent`, `ProductPriceChangedEvent`)。
*   **属性:**
    *   包含事件发生的时间戳。
    *   包含与事件相关的所有必要数据。
*   **方法:**
    *   通常只包含 getter 方法。
    *   不应该包含任何业务逻辑。
*    **基类:** 可以创建一个 DomainEvent 基类, 包含 timestamp 等通用的属性和方法.
*    **序列化:** 领域事件通常需要序列化, 可以实现 Serializable 接口, 或者使用 JSON 序列化.


# 应用服务编写规范 (Application Service Conventions)

## 概述 (Overview)

本规范定义了应用服务接口和实现类的编写规则。

## 规则 (Rules)

*   **接口和实现类：**
    *   定义应用服务接口。
    *   创建应用服务实现类，实现应用服务接口。
    *   使用 `@Service` 注解标记应用服务实现类。

*   **命名：**
    *   应用服务接口名通常以 `Service` 结尾 (例如：`OrderService`, `ProductService`)。
    *   应用服务实现类名通常以 `ServiceImpl` 结尾 (例如：`OrderServiceImpl`, `ProductServiceImpl`)。

*   **方法：**
    *   方法名通常以动词开头，表示一个用例或用户操作。
    *   方法参数应该是 DTO 或基本类型。
    *   返回值应该是 DTO 或基本类型。
    *   方法内部应该调用领域层来执行业务逻辑。
    *   方法内部应该处理事务 (`@Transactional`)。
    *   方法内部可以发布或订阅领域事件。
    *   应用服务不应该包含业务逻辑，只负责协调和编排。
*   **依赖注入:**
    *   使用构造函数注入或 `@Autowired` 注解注入依赖项（如仓储、领域服务）。
*    **异常处理:**
     *    应用服务应该捕获领域层抛出的异常，并转换为应用层异常或错误响应。

# 命令和命令处理器编写规范 (Command and Command Handler Conventions)

## 概述 (Overview)

本规范定义了命令类和命令处理器类的编写规则。

## 命令 (Command)

*   **定义：** 命令表示执行某个操作的请求。
*   **规则：**
    *   命令类应该是不可变的。
    *   类名使用祈使语气动词 (例如：`CreateOrderCommand`, `UpdateProductCommand`)。
    *   包含执行命令所需的所有数据。
    *   可以使用 record 类型 (Java 14+) 或 Lombok 的 `@Value` 注解。
    *   可以在构造函数中进行参数校验。
    *   避免包含业务逻辑。

## 命令处理器 (Command Handler)

*   **定义：** 命令处理器负责处理命令，调用领域层执行业务逻辑。
*   **规则：**
    *   命令处理器通常是无状态的。
    *   类名通常以 `CommandHandler` 结尾 (例如：`CreateOrderCommandHandler`)。
    *   实现一个通用的命令处理接口 (例如：`ICommandHandler<T>`，其中 `T` 是命令类型)。
    *   使用 `@Component` 或 `@Service` 注解将其注册为 Spring Bean。
    *   使用构造函数注入或 `@Autowired` 注解注入依赖项（如仓储、领域服务）。
    *   `handle` 方法：
        *   接收命令对象作为参数。
        *   调用领域层执行业务逻辑。
        *   可以发布领域事件。
        *   不应该包含业务逻辑，而是委托给领域对象。
        *   处理异常。
        *   可以使用 `@Transactional` 注解来管理事务。


# 查询和查询处理器编写规范 (Query and Query Handler Conventions)

## 概述 (Overview)

本规范定义了查询类和查询处理器类的编写规则。

## 查询 (Query)

*   **定义：** 查询表示获取数据的请求。
*   **规则：**
    *   查询类应该是不可变的。
    *   类名使用名词或名词短语 (例如：`GetOrderByIdQuery`, `FindProductsByNameQuery`)。
    *   包含执行查询所需的所有参数。
    *   可以使用 record 类型 (Java 14+) 或 Lombok 的 `@Value` 注解。
    *   避免包含业务逻辑.

## 查询处理器 (Query Handler)

*   **定义：** 查询处理器负责处理查询，从数据源获取数据。
*   **规则：**
    *   查询处理器通常是无状态的。
    *   类名通常以 `QueryHandler` 结尾 (例如：`GetOrderByIdQueryHandler`)。
    *   实现一个通用的查询处理接口 (例如：`IQueryHandler<Q, R>`，其中 `Q` 是查询类型，`R` 是返回类型)。
    *    使用 `@Component` 或 `@Repository` 注解将其注册为 Spring Bean。
    *   使用构造函数注入或 `@Autowired` 注解注入依赖项（如数据源、仓储）。
    *   `handle` 方法：
        *   接收查询对象作为参数。
        *   从数据源获取数据。
        *   可以将数据转换为 DTO。
        *   返回数据。
        *   不应该包含业务逻辑。
        *    处理异常。


# DTO 编写规范 (DTO Conventions)

## 概述 (Overview)

本规范定义了数据传输对象 (Data Transfer Object, DTO) 类的编写规则。

## 规则 (Rules)

*   **定义：** DTO 用于在层之间传输数据。
*   **命名：**
    *   类名通常以 `Dto` 结尾 (例如：`OrderDto`, `ProductDto`)。
    *   根据用途，也可以使用其他后缀，例如 `OrderSummaryDto`, `CreateOrderRequestDto`。

*   **属性：**
    *   包含需要传输的数据。
    *   属性类型应该是基本类型、包装类型、String、Date、BigDecimal 等，或者是其他 DTO。
    *   避免包含领域对象（实体、值对象）。
    *    字段尽量使用`private`
*   **方法：**
    *   通常只包含 getter 和 setter 方法。
    *   可以使用 Lombok 的 `@Data`、`@Getter`、`@Setter` 注解。
    *   可以包含一些简单的、与数据转换相关的逻辑 (比如类型转换, 格式化).
    *  避免包含复杂的业务逻辑
*   **不可变性：**
    *   根据需要，DTO 可以是可变的或不可变的。
    *   对于请求 DTO，通常是可变的。
    *   对于响应 DTO，可以是不可变的 (推荐) 或可变的。
    *    如果使用不可变的DTO, 可以使用record (Java 14+), 或者 Lombok 的 `@Value` 注解。

*    **校验:**
     *   可以使用 Bean Validation 注解 (如 `@NotNull`, `@NotBlank`, `@Size`, `@Email`) 进行参数校验 (通常用于请求 DTO)。


# 外部服务适配器编写规范 (External Service Adapter Conventions)

## 概述

本规范定义外部服务适配器 (External Service Adapter) 的编写规则。外部服务适配器负责与外部系统 (例如第三方 API、消息队列、遗留系统等) 进行交互。

## 规则

*   **接口定义:**
    *   定义一个接口来表示外部服务的功能。
    *   接口方法应该与外部服务的 API 相对应。

*   **适配器实现:**
    *   创建一个实现类来实现外部服务接口。
    *   使用 `@Component` 注解将适配器注册为 Spring Bean。

*   **依赖注入:**
    *   使用`@Autowired` 注解注入所需的依赖项。

*   **错误处理:**
    *   捕获外部服务可能抛出的异常。
    *   将外部服务的错误转换为应用程序内部的异常或错误码。
    *   可以考虑使用重试、熔断等机制来提高系统的弹性。

*   **隔离:**
    *   将外部服务适配器与领域逻辑隔离。
    *   适配器应该只负责数据转换和协议转换，不应该包含业务逻辑。


### 绘制领域建模的类图建议

1. 突出聚合边界: 清晰地显示哪些类是聚合根 (<<Aggregate>>)。
2. 显示聚合内部的关键关系: 例如 Station 包含 Connector。
3. 显示最重要的跨聚合关联: 只绘制那些对理解核心领域逻辑至关重要的聚合根之间的关系。通常通过 ID 引用来体现。
4. 省略次要关系或细节:
   - 可以省略指向基础类型或简单值对象（如 Money, String, Location）的线。
   - 可以省略一些可以通过其他关系推断出来的间接关联。
   - 服务与聚合之间的依赖关系通常不在静态类图上画全，除非是为了说明特定的交互模式。
5. 使用注释: 对于一些省略的关系，可以用注释（note right of Class: ...）来补充说明。
6. 分层/分视图: 对于复杂的域，可以创建多个图：
   - 一个高层的聚合关系图。
   - 每个复杂聚合的内部结构图。
   - 特定用例或流程相关的对象交互图。