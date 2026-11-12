# BlueWhale Roadmap

本文档维护 BlueWhale 的完成情况与未完成事项，随实现进展更新。

## 已完成

- 总体设计、详细设计、知识闭环三份核心文档与架构图。
- 地基：`ContextUnit` 聚合、生命周期状态机、四时间轴、领域端口、ArchUnit 分层守卫。
- 读路径：意图门控、多路召回、RRF、状态重排、MMR、内容去重、EMPTY 语义、trace、citation。
- 持久化：SQLite 元数据、索引、FTS5 词法检索；事务与乐观锁；重启可恢复。
- 使用闭环：usage 异步埋点、`RecordFeedbackUseCase`、economy 计数回写。
- 工程：Maven 多模块（core/app/store-sqlite/index-adapter/extract/interfaces/starter/client）。
- 知识写入：ChangeRequest 统一写入口，ADD 类型经审核、回归、发布后进入索引。
- 记忆轨收敛：legacy `Memory`/`MemoryGraph` 模型与 `/api/memories` 面整体退役，统一到 `ContextUnit`；图关联（`UnitEdge`）与维护用例（`RunMaintenanceUseCase`：遗忘/巩固/抽象）在 ContextUnit 上重建；`AccessPolicy` 归入 context 内核、`CaptureSignal` 归入 domain/memory；ArchUnit 升级为 legacy 包禁回归 + 内核自洽硬规则。

## 记忆轨收敛决策（2026-11-11）

知识与记忆不拆成两个工程，而是同一 Context 限界上下文内按子域分包；收敛已落地，关键决策：

1. `/api/memories` 无外部调用方（Langur 仅 README 互链、Skylark 零引用、client 只用 `/api/context`），直接删除而非兼容 shim。
2. 相似度关联边用受控类型 `EdgeType.SIMILAR_TO`（不沿用裸字符串 `RELATED_TO`）。
3. 抽象走 `ExtractionPort` 模型蒸馏生成 `DISTILLED` + `DERIVED_FROM`，严禁字符串拼接；未装配适配器时默认关闭。
4. 遗忘为软失效（ACTIVE→ARCHIVED），不硬删；`PROCEDURAL` 豁免。
5. `AccessPolicy` 迁入 `context` 内核，消除内核→治理反向依赖。
6. `skylark-index` 下沉本次不动，`IndexPort` 现状够用。

目标包边界：`context`（共享内核）/ `memory` / `graph` / `knowledge`（待建）/ `lifecycle` / `governance` / `feedback` / `port`；依赖方向为子域→内核、端口→内核，内核自洽。

## 未完成

### 知识闭环深化

- [ ] `REVISE/DEPRECATE/REBIND/ONTOLOGY/RECLASS` 的变更应用逻辑。
- [ ] `bw_regression_case` 回归用例闸门接线（无新增用例不得 READY）。
- [ ] distill 子包：`SignalCollector → Attributor → Clusterer → DraftWriter` 自动起草变更单。
- [ ] 影响面重放、防震荡冻结、强制 CANARY 发布。

### 生命周期治理

- [ ] `ConsolidateJob`/`DecayJob` 定时调度：维护逻辑已由 `RunMaintenanceUseCase` 提供（遗忘/巩固/抽象），待接调度、阈值标定与 `ExtractionPort` 适配器。
- [ ] `ReviewJob`：复审逾期扫描。
- [ ] `RetentionJob`：保留期到期处理。
- [ ] `ErasureJob`：受控销毁与 legal hold。
- [ ] `ReconcileJob`：冲突调和。
- [ ] `PropagationJob`：血缘变更传播。

### 管理端

- [ ] 服务端管理 API 面：审核队列、记忆管理、看板聚合、erasure 台。
- [ ] 独立管理端应用。

### 共享索引与规模化

- [ ] `skylark-index` 从 Langur 下沉（跨仓库）。
- [ ] dense 召回接 HNSW 或外部向量索引。

### 评测与本体

- [ ] 30 题评测集与 CI 评测基线。
- [ ] 意图门控题库。
- [ ] 本体三层分层、schema YAML、`binds_property` 发布硬门。

### 文档同步

- [ ] 三份设计文档按 SQLite + 多模块现状修订，修复旧源码路径链接。
