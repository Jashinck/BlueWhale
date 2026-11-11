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

## 未完成

### 知识闭环深化

- [ ] `REVISE/DEPRECATE/REBIND/ONTOLOGY/RECLASS` 的变更应用逻辑。
- [ ] `bw_regression_case` 回归用例闸门接线（无新增用例不得 READY）。
- [ ] distill 子包：`SignalCollector → Attributor → Clusterer → DraftWriter` 自动起草变更单。
- [ ] 影响面重放、防震荡冻结、强制 CANARY 发布。

### 生命周期治理

- [ ] `ConsolidateJob`：记忆巩固与蒸馏。
- [ ] `DecayJob`：衰减与强化。
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
