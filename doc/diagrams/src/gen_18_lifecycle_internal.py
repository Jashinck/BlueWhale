# -*- coding: utf-8 -*-
"""图 18 生命周期与治理内部架构：状态机/时间轴/调和/擦除/影响分析 + 周期 job + 端口 + 存储。"""
import bw_style as S

W, H = 1400, 1000
d = S.Diagram('18-lifecycle-internal', W, H,
              title='生命周期与治理 · 内部架构',
              subtitle='状态机唯一入口 + 四时间轴 + 冲突调和 + 受控销毁 + 血缘影响分析 + 五个周期 job + 端口 + 存储')

d.container(40, 84, 1320, 880, '生命周期与治理模块', tint='red')

# ---- 左列：核心引擎 ----
d.box(60, 120, 680, 44, '触发 · Transition / Actor', sub='源变更 · nCorrected 超阈 · 复审逾期 · 合规请求', tint='gray', title_size=12.5, sub_size=10)
stages = [
    ('StateMachine', 'transition() 唯一入口：每次转移必写 lifecycle event', 'red'),
    ('TimelineService', '四时间轴（现实/系统/采集/治理）读写', 'red'),
    ('ConflictResolver', '调和 PAD：裁决顺序 + bi-temporal 写边', 'red'),
    ('ErasureEngine', '受控销毁：legal hold 判定 + 级联 + 召回探针验证', 'red'),
    ('ImpactService', '血缘影响分析 impact(unitId, depth)', 'red'),
]
y = 176
for t, sub, tint in stages:
    d.box(60, y, 680, 52, t, sub=sub, tint=tint, title_size=12.5, sub_size=10)
    y += 64
d.box(60, 496, 680, 44, '输出 · TransitionResult', sub='状态转移结果 + 事件证据（append-only）', tint='green', title_size=12.5, sub_size=10)

for i in range(5):
    y0 = 164 + i * 64
    d.arrow([(400, y0), (400, y0 + 12)], 'main', 1.6)
d.arrow([(400, 484), (400, 496)], 'main', 1.6)

# ---- 右列：job / 端口 / 存储 ----
d.note(800, 112, '周期 job（各自独立幂等、可重入、可单跑）', 12.5, weight='600', color=S.INK)
jobs = [
    ('ReconcileJob', '冲突调和，源变更触发 + 兜底轮询'),
    ('ReviewJob', '复审逾期扫描 + 自动续期，日级'),
    ('RetentionJob', '保留期到期 → EXPIRED，日级'),
    ('ErasureJob', 'EXPIRED → ERASED，须校验 legal hold'),
    ('PropagationJob', '血缘传播，事件触发'),
]
y = 128
for t, sub in jobs:
    d.box(800, y, 540, 44, t, sub=sub, tint='teal', title_size=12, sub_size=10)
    y += 52

d.note(800, 404, '端口（SPI，定义于 domain）', 12.5, weight='600', color=S.INK)
d.box(800, 420, 540, 44, 'ClockPort', sub='时间源（四时间轴 / 衰减 / 复审到期）', tint='purple', title_size=12, sub_size=10)
d.box(800, 474, 540, 44, 'PolicyPort', sub='保留策略 / 衰减策略 / 失效规则', tint='purple', title_size=12, sub_size=10)

d.note(800, 548, '存储', 12.5, weight='600', color=S.INK)
d.cylinder(900, 596, 150, 50, 'bw_lifecycle_event', tint='blue', title_size=10.5)
d.cylinder(1080, 596, 150, 50, 'bw_lineage', tint='blue', title_size=11)
d.cylinder(900, 676, 150, 50, 'bw_asset', tint='blue', title_size=11)
d.cylinder(1080, 676, 150, 50, 'bw_erasure', tint='blue', title_size=11)
d.cylinder(900, 756, 150, 50, 'bw_review', tint='blue', title_size=11)

# ---- 关键连线 ----
d.arrow([(740, 202), (770, 202), (770, 621), (825, 621)], 'ctrl', 1.6)
d.arrow_label(792, 400, '写事件', 9.5, S.C_CTRL, anchor='end', bg=False)
d.arrow([(800, 306), (788, 306), (788, 394), (740, 394)], 'teal', 1.6, dash='4,3')
d.arrow_label(792, 348, '销毁', 9.5, S.C_TEAL, anchor='end', bg=False)
d.arrow([(1070, 518), (1070, 596)], 'gray', 1.6)
d.arrow_label(1078, 556, '实现', 9.5, S.C_GRAY, anchor='start', bg=False)

# ---- 图例 ----
d.add_legend(60, 830, [
    ('main', '引擎流转'),
    ('ctrl', '事件写入（append-only）'),
    ('teal', 'job 触发（异步）', '4,3'),
    ('gray', '端口实现'),
], title='图例', col_w=220)

d.notes(560, 848, [
    '三不变式：默认软失效；状态转移必写事件；不可逆转移仅 →SUPERSEDED 与 →ERASED。',
    'legal hold 优先于一切擦除；ERASED 只留墓碑（bw_erasure），召回探针验证不可见。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
