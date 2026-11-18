# BlueWhale 知识构建自动化设计：Ontology × LLMWiki × 内嵌编排

本文把知识构建从"人工填 `KnowledgePatch`"升级为**自动化的端到端闭环**：构建 → 检索使用 → 萃取更新。
它是 `CONTEXT-FRAMEWORK-DESIGN.md`（总体）与 `KNOWLEDGE-CLOSED-LOOP-DESIGN.md`（闭环）的**自动化实施细化**，不重复其已定结论，只补齐"如何自动跑起来"。

> 现状锚点：`ChangeRequest` 治理闸门、`GovernReviewUseCase`、`ChangeApplier`（仅 ADD）、反馈/economy 已落地；
> 知识自动构建管线、`knowledge/` 子域、萃取闭环、REVISE/DEPRECATE 应用尚未实现。本设计补齐这些。

---

## 0. 决策记录（2026-11-12，已拍板）

| # | 决策点 | 选定 | 说明 |
|---|---|---|---|
| ① | 编排架构 | **BlueWhale 内嵌编排** | BlueWhale 自带知识工程编排器，拥有构建/萃取工作流控制 |
| ② | Wiki 真源载体 | **Git 仓库** | md 版本化 + 天然 diff + 审计 + 回滚；WikiPatch 即 git diff |
| ③ | Ontology 存储 | **YAML 同仓 + 启动校验** | 与 Wiki 同仓库版本化，启动 fail-fast + CI 检查 |
| ④ | 抽取模型 | **复用 Langur 矩阵经 `ExtractionPort`** | 编排器不重做模型网关，LLM 推理委托 Langur |
| ⑤ | 首个验证实例 | **租赁合同审查 Pack** | 以保证金误报事故为首个回归用例 |
| ⑥ | 首期切入 | **P1 构建自动化**（P0 数据前置随附） | 先跑通"源 → Wiki → ContextUnit" |

**①+④ 的合取含义**：BlueWhale 内嵌编排器负责**工作流控制 + 确定性校验 + 状态闸门**；LLM 推理经 `ExtractionPort` 委托 Langur 模型矩阵（不重做网关）；Langur 仍负责消费侧装配。知识工程因此在 BlueWhale 内自包含，可被任意消费方复用。这落在 BlueWhale 既有的写路径职责内（总体设计 §3），不违背"机制/编排"大边界，只是把**知识工程侧的编排**收归 BlueWhale。

两条铁律（继承闭环设计，不动摇）：
1. **Wiki(md) 是真源**，ContextUnit/索引是编译产物；编译产物绑 `sourceHash` + 漂移检测；运行时数据不回写 md。
2. **一切写入（含 LLM 草稿）经 `ChangeRequest`**；`author=SYSTEM` 的草稿必须人审，不能自批。

---

## 1. 核心架构

```
                 ┌────────────────────── BlueWhale ──────────────────────┐
 源(文档/库/事件) │  知识工程编排器(内嵌)                                    │
   │ Connector   │   工作流控制 · 预算/重试 · 人审断点 · 状态持久化           │
   └────────────▶│   ├─ 确定性步骤: 解析/分段/Ontology校验/逐字比对/编译/闸门 │
                 │   └─ LLM 步骤: 抽取/蒸馏/起草 ─ExtractionPort─┐          │
                 │  Git Wiki(md 真源) ─编译─▶ ContextUnit + 三索引 │          │
                 │  Ontology(YAML) ─校验─▶ 属性轴/证据等级/冲突   │          │
                 └──────────────────────────────────────────────┼──────────┘
                                                                 ▼
                                              Langur 模型矩阵(推理 + 降级)
 消费侧: Langur 装配 ─resolve─▶ ContextBundle(releaseId+contentHash) ─reportSignal─▶ 闭环
```

分工：**编排器管"怎么走流程"，BlueWhale 代码管"确定性验收"，Langur 管"模型推理与消费装配"**。LLM 只生成候选，不信任其自证——校验由 BlueWhale 强制执行。

---

## 2. Ontology：语义契约（不只是类型系统）

- **三层**：框架内置 / 领域 Pack / 项目术语表。YAML schema 与 Wiki 同仓版本化；启动校验 + CI。
- **属性轴 `binds_property` 是发布硬门**：每个规则页必须声明。由此得到三件事：
  1. 确定性取规则 `resolve(by=PROPERTY_SET, props, asOf)`；
  2. 评测期望集；
  3. 跨页冲突检测（同属性同维度多口径）。
- **LLM 不得自扩本体**：新类型/字段进 `PROPOSED` 队列人裁决；`ontologyVersion` 写进 provenance；升级走 SHADOW 重抽战役。
- **机读分档**：判定式（代码求值）/ 检索式（LLM 选条 + 代码门）/ 生成式。规则页对可判定部分带机读条件字段。

YAML 骨架（示意）：
```yaml
ontologyVersion: "1.0"
properties:
  - id: deposit_refund_condition
    type: enum
    values: [normal, breach, mutual]
    binds_required: true
entityTypes: [Contract, Clause, Party]
relationTypes: [GOVERNS, REFERENCES, SUPERSEDES]
grades: [SOURCE, DERIVED, EXPERT, RETRO]
```

## 3. LLMWiki：真源 + 编译产物

- **页结构**：frontmatter（`binds_property` / `grade` / `effective_from` / `applies_to` / `source_version` / `needs` / `triggers`）+ claim 段落（带 `grade` + `evidenceSpan`）。
- **证据四档**：源文（evidenceSpan + 逐字校验）/ 推导（指向源文依据）/ 专家 / 复盘（后两者不得自动改写）。
- **变更单位 = WikiPatch（md diff）**：新建页/改页/废弃页，带 `sourceHash`；运行时数据不回写 md。
- **编译**：Wiki → 分段 → ContextUnit（l0 摘要 / l1 概览 / l2 原文）+ 三索引；lineage 绑 `sourceHash`，源变 → 漂移检测 → `needsReextract`。

**Ontology × LLMWiki 咬合**：Ontology 既约束抽取（抽什么类型/属性），又校验 Wiki 页（属性闭集、`binds_property`、同属性冲突）。LLM 产出 WikiPatch，BlueWhale 用 Ontology 做确定性验收。

## 4. 内嵌编排器设计

- **工作流 = 步骤序列/DAG**；每步是"确定性 BlueWhale 操作"或"经 `ExtractionPort` 的 LLM 步骤"。
- **状态持久化**：工作流实例落库，支持人审断点暂停/恢复；幂等、可重入、可单跑。
- **人审断点**：workflow 在 `ChangeRequest` 闸门暂停，管理端批准后继续回归 + 发布。
- **预算/重试/降级**：编排器管 token 预算与步骤重试；模型降级经 `ExtractionPort` 由 Langur 处理。
- **异步**：构建/萃取走队列 + 批量 + 去抖（写路径贵）；读路径低成本。

构建工作流（P1）：
```
ingest → parse → segment → extract(LLM, Ontology约束) → normalize/disambiguate
       → draftWikiPatch(LLM) → validate(确定性门) → ChangeRequest → [人审断点]
       → regression → release → compile(Wiki→ContextUnit+索引)
```

## 5. 端到端闭环（三段，全部经 ChangeRequest）

- **构建**：源 → Wiki → ContextUnit（第 4 节工作流）。抽取输出是 WikiPatch 不是 unit；校验不过 → 降级或 `QUARANTINED`，不静默入库。
- **使用**：`resolve(query)` 开放问答 / `resolve(by=PROPERTY_SET)` 确定性取规则组；`ContextBundle` 带 `releaseId + contentHash + requiredFacts`；`trace + reportSignal(traceId) → bw_signal`。默认读路径 0 LLM。
- **萃取更新**：信号 → 归因决策树（程序推导，码 `MISSING/NO_HIT/WRONG_PICK/BAD_CLAIM/STALE/CONFLICT/CONSUMER_ERR`）→ 聚类立单（达阈值）→ LLM 起草补丁（+回归用例）→ 人审 → 回归门 → 发布 → 血缘传播。隐式信号只降权；防振荡（频繁变更冻结、影响超阈强制 CANARY）。

## 6. 模块 / 包 / 端口 / 表 增量

| 层 | 增量 |
|---|---|
| `domain/knowledge/` | Source · Document · Segment · Claim · WikiPage · WikiPatch · PublishState（新建子域） |
| `domain/ontology/` | OntologySchema · PropertyAxis · TermBinding · ProposedType |
| `application/orchestration/` | KnowledgeWorkflowEngine · WorkflowInstance · Step（内嵌编排器） |
| `application` | `CompileWikiUseCase` · `AttributionService` · `DistillUseCase` · `RegressionUseCase`；`IngestContextUseCase` 升级为编排入口 |
| `port` | `WikiStorePort`(Git) · `OntologyPort` · `ConnectorPort` · `ExtractionPort`(已有) |
| `ChangeApplier` | 补 REVISE/DEPRECATE/REBIND/ONTOLOGY/RECLASS 差异化应用（当前仅 ADD） |
| 表 | 复用 `bw_source/bw_segment/bw_signal/bw_attribution/bw_change_request/bw_regression_case/bw_publish_batch`；补 `bw_wiki_page`（page↔sourceHash↔unit 绑定）、`bw_workflow_instance` |

内核 vs Pack：内核 = Signal/Attribution/ChangeRequest/回归/发布/传播 + 决策树 + reportSignal；Pack = 本体/规则模板/阈值/评测集/消费方适配器。

## 7. 校验门（确定性验收，不信任 LLM 自证）

| 门 | 规则 | 不过的动作 |
|---|---|---|
| 逐字比对 | 源文级 claim 的 evidenceSpan 必须与原文逐字一致 | 降为推导/专家级，人确认 |
| 属性闭集 | claim 属性 ∈ Ontology 定义 | 拒绝 / 进 PROPOSED |
| binds_property | 规则页必须声明且属性存在 | 发布硬门拦截 |
| 同属性冲突 | 同属性同维度多口径 | CONFLICTED + 人裁决 |
| 溯源 | 无 evidenceSpan/来源 | QUARANTINED |

## 8. 分期落地（P1 为首期）

- **P0 数据与契约前置**（随 P1 附）：补 `binds_property/grade/effective_from`；Wiki 真源 + sourceHash 绑定 + 漂移检测；Ontology YAML + 启动校验。
- **P1 构建自动化**：Connector → 分段 → Ontology 约束抽取 → WikiPatch → 校验门 → ChangeRequest(ADD)；CompileWiki → 索引；内嵌编排器接入。
- **P2 使用绑版本**：`releaseId+contentHash` 贯穿；`resolve(by=PROPERTY_SET)`；`reportSignal + bw_signal`。
- **P3 萃取闭环**：归因决策树 + 聚类立单 + 草稿起草 + 回归门；REVISE/DEPRECATE 应用；一条真实纠正走完全链路。
- **P4 影响面/防震荡/本体演进**：传播复核、CANARY、振荡刹车、SHADOW 重抽。
- **P5 Pack 化**：第二领域不改内核接入。

### P1 任务卡（首期实施）

| # | 任务 | 验收 |
|---|---|---|
| 1.1 | `domain/knowledge` 值对象与聚合（Source/Segment/Claim/WikiPage/WikiPatch） | 单测覆盖不变式 |
| 1.2 | `domain/ontology` + YAML 加载 + 启动校验 | 非法 schema 启动 fail-fast |
| 1.3 | `WikiStorePort`(Git) + `OntologyPort` + `ConnectorPort` 端口与适配 | Git 读写 md + diff 可用 |
| 1.4 | 校验门（逐字/属性闭集/binds_property/冲突/溯源） | 各门有拒绝/降级测试 |
| 1.5 | `CompileWikiUseCase`（Wiki→ContextUnit+索引，绑 sourceHash） | 编译产物可召回、漂移可检测 |
| 1.6 | 内嵌编排器骨架（工作流实例持久化 + 人审断点） | 构建工作流可暂停/恢复 |
| 1.7 | `IngestContextUseCase` 接编排器，抽取经 `ExtractionPort` | 一份源文档自动走完到 ChangeRequest(ADD) |

## 9. 验证实例：租赁合同审查 Pack

以保证金误报事故为首个回归用例：发布前应被"同属性口径冲突检查"拦下。Pack 提供租赁本体（属性轴/规则模板/阈值/评测集）+ 消费方适配器；内核不改。

## 10. 与既有文档/边界的关系

- 不改动总体设计的"BlueWhale=机制 / Langur=消费装配"大边界；仅把**知识工程侧编排**收归 BlueWhale（决策①）。
- 闭环四约束、归因码、防振荡、证据等级、属性轴硬门均继承 `KNOWLEDGE-CLOSED-LOOP-DESIGN.md`，本文不重复定义。
- 生命周期（12 态/四时间轴/血缘/SHADOW）继承总体设计 §14，构建/萃取产物一律走同一状态机与 `ChangeRequest`。