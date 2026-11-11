# BlueWhale

BlueWhale 是 [Skylark](https://github.com/Jashinck/Skylark) 生态中的记忆与上下文服务。当前代码主要提供记忆存储、向量召回、记忆关联和记忆演化能力；下一步会扩展为同时管理知识与记忆的 Context 框架。

## 设计文档

- [总体设计](CONTEXT-FRAMEWORK-DESIGN.md)：模块分工、核心概念、技术决策和实施计划。
- [详细设计](ARCHITECTURE-DETAILED-DESIGN.md)：接口、数据结构、任务调度、异常处理和验收方式。
- [知识更新闭环](KNOWLEDGE-CLOSED-LOOP-DESIGN.md)：使用反馈如何归因、修订、回归验证并发布。

上述文档描述目标架构，不代表当前代码已经全部实现。

## 当前代码提供的能力

| 能力 | 说明 |
|------|------|
| 多层记忆 | 支持情节、语义、程序和工作四种记忆类型 |
| 记忆关联 | 通过 MemoryGraph 保存和查询记忆节点之间的关系 |
| 记忆遗忘 | 自动淘汰重要度低于 0.05 的记忆 |
| 记忆巩固 | 访问次数超过 3 次时提高重要度 |
| 语义抽象 | 从多条高重要度情节记忆中生成语义记忆 |
| 向量召回 | 根据 Embedding 的语义相似度检索记忆 |
| Context 读取 | 意图门控、权限前置、状态过滤、预算装配、trace 与 citation |
| 生命周期 | 统一状态转移入口，所有状态变化写入生命周期事件 |
| 知识写入 | ChangeRequest 统一写入口，经审核、回归、发布后进入索引 |
| REST API | 提供记忆存取、上下文解析、反馈和知识变更单接口 |

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

`MemoryEvolutionService` 当前实现以下规则：

```text
遗忘：淘汰 importance < 0.05 的记忆
巩固：访问次数 > 3 时提高重要度
抽象：将 3 条以上高重要度情节记忆提炼为语义记忆
记录：每次演化写入 EvolutionRecord
```

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

### 存储记忆

```http
POST /api/memories
Content-Type: application/json

{
  "agentId": "agent-001",
  "content": "用户偏好深色主题界面",
  "memoryType": "EPISODIC",
  "importance": 0.8
}
```

### 召回记忆

```http
POST /api/memories/recall
Content-Type: application/json

{
  "agentId": "agent-001",
  "query": "用户界面偏好",
  "topK": 5
}
```

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/memories` | 存储记忆 |
| `POST` | `/api/memories/recall` | 语义召回 |
| `GET` | `/api/memories` | 按 agentId 查询记忆 |
| `DELETE` | `/api/memories/{id}` | 删除单条记忆 |
| `DELETE` | `/api/memories` | 清空 Agent 的所有记忆 |
| `POST` | `/api/memories/evolve` | 执行一次记忆演化 |
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
