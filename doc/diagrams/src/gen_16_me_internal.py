# -*- coding: utf-8 -*-
"""图 16 记忆工程 内部架构：六阶段组件 + 反馈 + 周期 job + 端口 + 存储。"""
import bw_style as S

W, H = 1400, 1000
d = S.Diagram('16-me-internal', W, H,
              title='记忆工程 · 内部架构',
              subtitle='六阶段记忆处理组件 + 反馈回写 + 三个周期 job（失败域隔离、各自幂等）+ 端口 + 存储')

d.container(40, 84, 1320, 880, '记忆工程模块', tint='purple')

# ---- 左列：主链路 ----
d.box(60, 120, 680, 44, '输入 · CaptureSignal', sub='会话 · 工具调用 · 用户反馈信号', tint='gray', title_size=12.5, sub_size=10)
stages = [
    ('CaptureService · 捕获', '写 bw_episode（LEDGER，只增不改）', 'purple'),
    ('MemoryExtractor · 抽取', '与既有记忆比对 → ADD / UPDATE / DELETE / NOOP', 'purple'),
    ('EntityLinker · 挂链', '实体链接 + ABOUT / DERIVED_FROM 边', 'purple'),
    ('Consolidator · 巩固蒸馏', '相似聚类 → 模型蒸馏（推理模型）→ DISTILLED', 'purple'),
    ('DecayEngine · 衰减强化', 'salience = f(importance, Δt, 四计数)', 'purple'),
    ('Reflector · 复盘', '轨迹反思 → SKILL → 晋升闸门（人审）', 'purple'),
]
y = 176
for t, sub, tint in stages:
    d.box(60, y, 680, 52, t, sub=sub, tint=tint, title_size=12.5, sub_size=10)
    y += 64
d.box(60, 560, 680, 44, '输出 · MaintenanceReport', sub='每条决策 append-only 写 bw_lifecycle_event', tint='green', title_size=12.5, sub_size=10)

for i in range(7):
    y0 = 164 + i * 64
    d.arrow([(400, y0), (400, y0 + 12)], 'main', 1.6)
d.arrow([(400, 548), (400, 560)], 'main', 1.6)

# ---- 右列：反馈 / job / 端口 / 存储 ----
d.note(800, 112, '反馈与周期 job（失败域隔离 · 各自幂等）', 12.5, weight='600', color=S.INK)
d.box(800, 128, 540, 44, 'FeedbackRecorder', sub='recordApplied(ADOPTED|CORRECTED) → economy 四计数', tint='teal', title_size=12, sub_size=10)
d.box(800, 180, 540, 44, 'ConsolidateJob', sub='巩固/抽象，分钟级；幂等键 (jobRunId, unitId)', tint='teal', title_size=12, sub_size=10)
d.box(800, 232, 540, 44, 'DecayJob', sub='衰减/强化，高频定时；技能只降权不遗忘', tint='teal', title_size=12, sub_size=10)
d.box(800, 284, 540, 44, 'ReflectionJob', sub='会话/任务结束事件驱动', tint='teal', title_size=12, sub_size=10)

d.note(800, 356, '端口（SPI，定义于 domain）', 12.5, weight='600', color=S.INK)
d.box(800, 372, 260, 44, 'ExtractionPort', sub='抽取、蒸馏与重排', tint='purple', title_size=12, sub_size=9.5)
d.box(1090, 372, 250, 44, 'EmbeddingPort', sub='向量化', tint='purple', title_size=12, sub_size=9.5)
d.box(800, 426, 260, 44, 'IndexPort', sub='HNSW/BM25/图', tint='purple', title_size=12, sub_size=9.5)
d.box(1090, 426, 250, 44, 'ClockPort', sub='时间源（衰减/双时态）', tint='purple', title_size=12, sub_size=9.5)

d.note(800, 500, '存储', 12.5, weight='600', color=S.INK)
d.cylinder(900, 548, 150, 50, 'bw_episode', tint='blue', title_size=11)
d.cylinder(1080, 548, 150, 50, 'bw_context_unit', tint='blue', title_size=11)
d.cylinder(900, 628, 150, 50, 'bw_unit_edge', tint='blue', title_size=11)
d.cylinder(1080, 628, 150, 50, 'bw_usage_event', tint='blue', title_size=11)
d.cylinder(900, 708, 150, 50, 'bw_lineage', tint='blue', title_size=11)

# ---- 关键连线 ----
d.arrow([(740, 266), (770, 266), (770, 394), (800, 394)], 'ctrl', 1.6)
d.arrow_label(778, 320, '调用', 9.5, S.C_CTRL, anchor='start', bg=False)
d.arrow([(800, 150), (788, 150), (788, 458), (740, 458)], 'teal', 1.6, dash='4,3')
d.arrow_label(792, 300, '反馈', 9.5, S.C_TEAL, anchor='end', bg=False)
d.arrow([(935, 470), (935, 548)], 'gray', 1.6)
d.arrow_label(943, 512, '实现（adapter 委托）', 9.5, S.C_GRAY, anchor='start', bg=False)

# ---- 图例 ----
d.add_legend(60, 830, [
    ('main', '阶段流转'),
    ('ctrl', '依赖（抽取/蒸馏）'),
    ('teal', '反馈信号（异步）', '4,3'),
    ('gray', '端口实现'),
], title='图例', col_w=220)

d.notes(560, 848, [
    '记忆抽取的 ADD/UPDATE/DELETE/NOOP 是写入核心决策，非无条件 append；',
    'episode 原文永远保留做溯源；退场候选 ≠ 删除，销毁走受控销毁（legal hold 优先）。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
