# BlueWhale

BlueWhale 是 [Skylark](https://github.com/Jashinck/Skylark) 生态中知识与记忆统一的 Context 框架。知识与记忆共用 `ContextUnit` 聚合、生命周期、索引与供给入口，按 origin / scope / 治理强度区分治理方式；提供捕获、解析、反馈、生命周期与知识变更闭环能力。

## 设计文档

- [总体设计](CONTEXT-FRAMEWORK-DESIGN.md)：模块分工、核心概念、技术决策和实施计划。
- [详细设计](ARCHITECTURE-DETAILED-DESIGN.md)：接口、数据结构、任务调度、异常处理和验收方式。
- [知识更新闭环](KNOWLEDGE-CLOSED-LOOP-DESIGN.md)：使用反馈如何归因、修订、回归验证并发布。

上述文档描述目标架构，不代表当前代码已经全部实现。

## 当前代码提供的能力

| 能力 | 说明 |
|------|------|
| 多层记忆 | 支持情节、语义、程序和工作四种记忆类型（`UnitKind`） |
| 记忆关联 | 捕获时按向量相似度写入 `UnitEdge`（SIMILAR_TO），蒸馏写 DERIVED_FROM |
| 记忆遗忘 | 维护用例按衰减后重要度软退场（ACTIVE→ARCHIVED），程序性记忆豁免 |
| 记忆巩固 | 重要度由 economy 的采纳/纠正信号驱动重算 |
| 语义抽象 | 高采纳情节记忆经 `ExtractionPort` 蒸馏为 DISTILLED 单元，挂 DERIVED_FROM 边并待人审 |
| 向量召回 | dense + 词法多路召回，RRF 融合、状态重排、MMR 去冗 |
| Context 读取 | 意图门控、权限前置、状态过滤、预算装配、trace 与 citation |
| 生命周期 | 统一状态转移入口，所有状态变化写入生命周期事件 |
| 知识写入 | ChangeRequest 统一写入口，经审核、回归、发布后进入索引 |
| REST API | 提供上下文捕获/解析/反馈、生命周期与知识变更单接口 |

## 代码结构

工程采用 Maven 多模块 DDD 分层，依赖方向固定为
`starter → interfaces → app → core`，所有适配器只依赖 core：

| 模块 | 职责 |
|------|------|
| `bluewhale-core` | ContextUnit 聚合、生命周期、治理规则、领域端口 |
| `bluewhale-app` | 捕获、解析、生命周期、反馈等用例编排 |
| `bluewhale-store-sqlite` | ContextUnit、事件、episode 和 SQLite schema |
| `bluewhale-index-adapter` | 内存与 SQLite 索引实现 |
| `bluewhale-extract` | Embedding 及后续抽取、蒸馏、重排适配 |
| `bluewhale-interfaces` | REST 控制器、请求响应协议转换 |
| `bluewhale-client` | 不依赖 Spring 的远程薄 SDK |
| `bluewhale-starter` | Spring Boot 启动入口和运行时装配 |

`RunMaintenanceUseCase` 在 `ContextUnit` 上执行维护周期：

```text
遗忘：衰减后重要度 < 0.05 软退场（ACTIVE→ARCHIVED），PROCEDURAL 豁免
巩固：重要度由 economy 的采纳/纠正信号重算（幂等）
抽象：≥3 条高采纳情节记忆经 ExtractionPort 蒸馏为 DISTILLED 单元，挂 DERIVED_FROM 边，进入 PENDING_REVIEW 待人审
记录：状态转移写入 LifecycleEvent
```

抽象默认关闭，需装配 `ExtractionPort` 适配器后启用；遗忘/巩固阈值待历史数据标定。

## 构建与运行

环境要求：

- Java 17 或更高版本
- Maven 3.8 或更高版本

```bash
mvn clean package -DskipTests
java -jar bluewhale-starter/target/bluewhale.jar
```

服务默认监听 `http://localhost:8082`，默认使用 SQLite 文件存储，数据库位于 `./data/bluewhale.db`；启动时自动创建表和索引。

```bash
export BLUEWHALE_STORE=sqlite
export BLUEWHALE_DB_URL=jdbc:sqlite:./data/bluewhale.db
java -jar bluewhale-starter/target/bluewhale.jar
```

开发和单元测试可切换为内存模式：

```bash
export BLUEWHALE_STORE=memory
java -jar bluewhale-starter/target/bluewhale.jar
```

## REST API

### 捕获记忆

```http
POST /api/context/memories
Content-Type: application/json

{
  "traceId": "trace-001",
  "tenant": "acme",
  "agentId": "agent-001",
  "source": "conversation",
  "text": "用户偏好深色主题界面",
  "kind": "EPISODIC",
  "confidence": 0.9
}
```

### 解析上下文

```http
POST /api/context/resolve
Content-Type: application/json

{
  "traceId": "trace-002",
  "tenant": "acme",
  "agentId": "agent-001",
  "intent": "TASK",
  "query": "用户界面偏好",
  "maxTokens": 2000
}
```

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/context/memories` | 捕获信号并生成可召回的上下文单元 |
| `POST` | `/api/context/resolve` | 按作用域、意图和预算解析上下文 |
| `POST` | `/api/context/feedback` | 记录采纳、纠正等使用反馈并回写 economy |
| `POST` | `/api/change-requests` | 提交知识变更单（统一写入口） |
| `POST` | `/api/change-requests/{id}/decision` | 审核、回归、发布或回滚变更单 |
| `POST` | `/api/context/units/{id}/transitions` | 执行生命周期状态转移 |
| `GET` | `/api/context/units/{id}/lifecycle-events` | 查询生命周期事件证据链 |

## 替换向量服务

实现 `EmbeddingPort` 接口即可接入其他 Embedding 服务：

```java
public interface EmbeddingPort {
    float[] embed(String text);
    List<float[]> embedBatch(List<String> texts);
    int getDimension();
}
```

## 相关项目

- [Skylark](https://github.com/Jashinck/Skylark)：实时 AI 语音对话系统。
- [Langur](https://github.com/Jashinck/Langur)：与 BlueWhale 配套的 Agent 框架。

## 许可证

Apache License 2.0
