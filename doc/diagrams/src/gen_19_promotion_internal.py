# -*- coding: utf-8 -*-
"""图 19 晋升桥内部架构：晋升判定链 + 降权/冷启动 + 治理闸门。"""
import bw_style as S

W, H = 1200, 800
d = S.Diagram('19-promotion-internal', W, H,
              title='知识工程⇄记忆工程 晋升桥 · 内部架构',
              subtitle='晋升五步判定链（任一 NO 即留在记忆工程）+ 降权/冷启动旁路 + 人审/双模型互检闸门')

d.container(40, 84, 1120, 660, '晋升桥模块', tint='orange')

# ---- 左列：晋升判定链 ----
d.box(60, 120, 560, 44, '输入 · DISTILLED 单元', sub='来自记忆工程蒸馏产物', tint='gray', title_size=12.5, sub_size=10)
steps = [
    ('检查证据是否充分', 'lineage / provenance / extractorVersion 完整可回溯', 'orange'),
    ('检查能否跨主体泛化', '≥N 个 agent/user 反复召回（非单一会话偶然）', 'orange'),
    ('检查采纳率是否超过阈值', 'nAdopted / nSeen 达标（bw_usage_event 归因）', 'orange'),
    ('检查纠正率是否低于阈值', 'nCorrected / nSeen 低于红线（超则走降权）', 'orange'),
]
y = 176
for t, sub, tint in steps:
    d.box(60, y, 560, 52, t, sub=sub, tint=tint, title_size=12.5, sub_size=10)
    y += 64
d.box(60, 432, 560, 52, 'ReviewGate · 治理闸门', sub='人审 / 双模型互检 → 带 authority 发布', tint='red', title_size=12.5, sub_size=10)
d.box(60, 496, 560, 44, '输出 · 候选知识单元', sub='origin=DISTILLED, scope=project/workspace', tint='green', title_size=12.5, sub_size=10)

for i in range(5):
    y0 = 164 + i * 64
    d.arrow([(340, y0), (340, y0 + 12)], 'main', 1.6)
d.arrow([(340, 484), (340, 496)], 'main', 1.6)

# ---- 右列：旁路 + 存储 ----
d.note(720, 112, '旁路（非晋升正门）', 12.5, weight='600', color=S.INK)
d.box(720, 128, 420, 52, 'DemotionTrigger', sub='频繁纠正/命中冲突/源变更 → STALE_SUSPECT（只降权不删）', tint='teal', title_size=12, sub_size=10)
d.box(720, 192, 420, 52, 'BootstrapLinker', sub='知识库实体/本体挂载点 → 实体链接（无治理负担）', tint='teal', title_size=12, sub_size=10)

d.note(720, 272, '存储', 12.5, weight='600', color=S.INK)
d.cylinder(800, 320, 150, 50, 'bw_context_unit', tint='blue', title_size=11)
d.cylinder(1000, 320, 150, 50, 'bw_unit_edge', tint='blue', title_size=11)
d.cylinder(800, 400, 150, 50, 'bw_usage_event', tint='blue', title_size=11)
d.cylinder(1000, 400, 150, 50, 'bw_lifecycle_event', tint='blue', title_size=10.5)

# ---- 关键连线 ----
d.arrow([(620, 394), (690, 394), (690, 154), (720, 154)], 'teal', 1.6, dash='4,3')
d.arrow_label(698, 270, '纠正率检查未通过 → 降权', 9.5, S.C_TEAL, anchor='end', bg=False)
d.arrow([(620, 458), (690, 458), (690, 345), (725, 345)], 'ctrl', 1.6)
d.arrow_label(698, 408, '发布', 9.5, S.C_CTRL, anchor='end', bg=False)

# ---- 图例 ----
d.add_legend(60, 596, [
    ('main', '判定链流转'),
    ('ctrl', '发布'),
    ('teal', '旁路/降权（异步）', '4,3'),
], title='图例', col_w=240)

d.notes(420, 612, [
    '所有动作 append-only 写 bw_lifecycle_event；',
    '未经 ReviewGate 的 DISTILLED 单元不得进入 ACTIVE。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
