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
| REST API | 提供记忆存储、召回、查询、删除和演化接口 |

## 代码结构

当前服务采用 DDD 分层，领域层不依赖外部基础设施：

```text
src/main/java/org/skylark/bluewhale/
├── domain/                      # 领域模型、领域服务、仓储接口和领域事件
│   ├── model/
│   │   ├── memory/              # Memory 聚合根及值对象
│   │   ├── graph/               # 记忆节点和关联关系
│   │   └── evolution/           # 记忆演化记录
│   ├── service/                 # 记忆管理与演化服务
│   ├── repository/              # 记忆和图谱仓储接口
│   └── event/                   # 存储、召回和演化事件
├── application/                 # 用例、命令和 DTO 转换
├── infrastructure/              # Embedding、向量存储和持久化实现
└── interfaces/                  # REST 控制器及请求、响应 DTO
```

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
java -jar target/bluewhale.jar
```

服务默认监听 `http://localhost:8082`。

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
