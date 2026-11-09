# -*- coding: utf-8 -*-
"""图 13 PAD：记忆工程的巩固、衰减与复盘维护算法。

三个 job 失败域隔离、各自幂等；蒸馏不删原件；衰减由信号驱动而非仅时间。
"""
import bw_style as S

W, H = 1320, 1250
d = S.Diagram('13-pad-consolidate', W, H,
              title='PAD 图 · 巩固 / 衰减 / 复盘维护算法',
              subtitle='三 job 失败域隔离且各自幂等；蒸馏保留原件并挂 DERIVED_FROM；衰减由采纳/纠正信号驱动，不只看时间')

L0, L1, L2 = 60, 140, 220


def pb(x, y, w, h, title, sub=None, tint='blue', ts=11.5, ss=9.5, lines=None):
    d.box(x, y, w, h, title, sub=sub, tint=tint, title_size=ts, sub_size=ss, lines=lines)


def spine(x, y0, y1, ticks):
    d.arrow([(x, y0), (x, y1)], 'ink', 1.2, marker=False)
    for ty in ticks:
        d.arrow([(x, ty), (x + 20, ty)], 'ink', 1.2, marker=False)


# ---- 主干 ----
spine(48, 113, 1169, [113, 169, 377, 715, 993, 1169])

pb(L0, 92, 520, 42, '输入', 'MaintenanceTrigger(trigger, scope, jobRunId)', 'blue')
pb(L0, 148, 520, 42, 'SEL  按触发源分派 job', '三 job 独立调度 / 独立重入，不用一个用例全包', 'yellow')

spine(128, 190, 319, [223, 271, 319])
pb(L1, 204, 520, 38, 'ConsolidationJob', '定时 或 相似簇规模达阈值', 'purple', ts=11, ss=9)
pb(L1, 252, 520, 38, 'DecayJob', '高频定时（每日），只算不删', 'teal', ts=11, ss=9)
pb(L1, 300, 520, 38, 'ReflectionJob', '会话 / 任务结束事件驱动', 'orange', ts=11, ss=9)

# ---- M4 ----
pb(L0, 356, 520, 42, 'LOOP  每个候选簇（巩固·蒸馏）', '相似聚类 → 判定可否抽象 → 蒸馏 → 挂血缘', 'slate')
spine(128, 377, 653, [433, 489, 653])
pb(L1, 412, 520, 42, '相似聚类 + 去重合并', '向量近邻 ∧ 同 entity ∧ 同 scope 才可合并（MERGE 边）', 'purple', ts=11, ss=9)
pb(L1, 470, 520, 38, 'SEL  簇内能否抽象为更高层语义？', '证据是否充分、是否跨 ≥N 个 episode 重复出现', 'yellow', ts=11, ss=9)
spine(208, 489, 593, [543, 593])
pb(L2, 524, 480, 38, 'NO', '仅合并，不造新单元（宁缺勿滥，堵幻觉抽象）', 'red', ts=11, ss=9)
pb(L2, 572, 480, 42, 'YES', '模型蒸馏（推理模型 + 结构化输出）→ 新建 DISTILLED 单元', 'green', ts=11, ss=9)
pb(L1, 632, 520, 42, '挂 DERIVED_FROM 边 + lineage.parents', '原单元保留并降权，不删（软失效）；产物必带 provenance', 'purple', ts=11, ss=9)

# ---- M5 ----
pb(L0, 694, 520, 42, 'LOOP  每个可衰减单元（衰减·强化）', 'salience 计算 → 类型豁免 → 分档处置', 'slate')
spine(128, 715, 827, [771, 827])
pb(L1, 750, 520, 42, 'salience = f(importance, Δt, nSeen, nCited, nAdopted, nCorrected)',
   '采纳提权 / 纠正降权：行为改变记忆强度，而非仅访问时间', 'teal', ts=10.5, ss=9)
pb(L1, 808, 520, 38, 'SEL  kind == PROCEDURAL / SKILL？', '程序性记忆是"学会的"，不是"记住的"', 'yellow', ts=11, ss=9)
spine(208, 827, 931, [881, 931])
pb(L2, 862, 480, 38, 'YES', '只降权，永不遗忘（保留既有正确决策）', 'green', ts=11, ss=9)
pb(L2, 910, 480, 42, 'NO', '高档→强化提权 / 中档→保持 / 低档→STALE_SUSPECT / 极低且保留期到→退场候选',
   'red', ts=10.5, ss=8.5)

# ---- M6 ----
pb(L0, 972, 520, 42, 'LOOP  每条采纳 / 纠正信号（复盘·回写）',
   '计数回写 → 超阈降权 → 反思产出技能', 'slate')
spine(128, 993, 1107, [1049, 1107])
pb(L1, 1028, 520, 42, 'recordApplied(ADOPTED | CORRECTED) → economy 计数',
   '纠正率超阈 → Demotion(STALE_SUSPECT) 或送 reconcile，不直接删', 'orange', ts=10.5, ss=9)
pb(L1, 1086, 520, 42, '轨迹反思 → 产出程序性记忆（SKILL）',
   '提交 BlueWhale 走晋升闸门（人审），不在 Langur 侧自存', 'orange', ts=11, ss=9)

pb(L0, 1148, 520, 42, '输出', 'MaintenanceReport + 每条决策 append-only 写 bw_lifecycle_event', 'green')

# ---- 右栏 ----
RX = 760
pb(RX, 92, 520, 112, '蒸馏 ≠ 摘要拼接', lines=[
    '聚类先行：不同 entity / 不同 scope 的经验不可混蒸',
    '蒸馏产物是新的 ContextUnit，带 provenance 与 extractorVersion',
    '原件保留 + DERIVED_FROM 可回溯到"哪几段输入产生的"',
], tint='purple', ts=12, ss=10)
pb(RX, 224, 520, 96, '衰减的输入是信号，不是时间', lines=[
    '仅 exp(-decay·天) 是伪闭环：无"被采纳/被纠正"即断链',
    'importance 不再由 accessCount>3 硬编码，改由 economy 驱动',
    '衰减只改 salience / state，从不物理删除',
], tint='teal', ts=12, ss=10)
pb(RX, 340, 520, 84, '两条不可违背的纪律', lines=[
    '巩固不删原件：软失效 + 墓碑，冲突≠覆盖',
    '程序性记忆不参与遗忘：技能只降权',
], tint='red', ts=12, ss=10)
pb(RX, 444, 520, 96, '幂等与失败域隔离', lines=[
    '幂等键 (jobRunId, unitId)，重跑不重复计数、不重复产物',
    '单 job 失败不影响其余 job，更不影响同步读路径',
    '维护全程异步，禁止阻塞 resolve()',
], tint='orange', ts=12, ss=10)
pb(RX, 560, 520, 84, '可观测产出（无它则不可调参）', lines=[
    '合并数 / 蒸馏数 / 强化数 / 降权数 / 退场候选数',
    '每条处置写 lifecycle event，可按 unit 回放前后状态',
], tint='green', ts=12, ss=10)
d.notes(RX, 676, [
    '维护是写路径的"新陈代谢"：缺了它，记忆只增不减、只堆不炼，',
    '召回质量随规模单调劣化，成本账也永远算不清。',
    '退场候选不等于删除——是否销毁由 知识工程 侧受控销毁流程裁决',
    '（legal hold 优先，见 10-erasure-flow）。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
