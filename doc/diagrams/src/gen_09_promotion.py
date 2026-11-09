# -*- coding: utf-8 -*-
"""图 09 晋升桥流程图：记忆工程 蒸馏产物 → 晋升判定 → 知识工程 候选 → 审核 → ACTIVE / 驳回。

含反向 Demotion 与 Bootstrap 旁路；每个闸门标注守卫条件与失败去向。
"""
import bw_style as S

W, H = 1500, 860
d = S.Diagram('09-promotion-bridge', W, H,
              title='知识工程⇄记忆工程 晋升桥流程图 —— 记忆如何升级为知识（及反向降级）',
              subtitle='Promotion：蒸馏产物过门槛→知识工程 候选→溯源/冲突/审核闸门→ACTIVE；Demotion/Bootstrap 为旁路')

CX, DW, BW = 640, 380, 400
LX, RX = 240, 1070


def sbox(cy, title, sub, tint='blue'):
    d.box(CX - BW / 2.0, cy - 22, BW, 44, title, sub=sub, tint=tint, title_size=12, sub_size=9.5)


def sdia(cy, title, sub, tint='yellow', h=68):
    d.diamond(CX, cy, DW, h, title, sub=sub, tint=tint, title_size=11.5, sub_size=9)


def v(y0, y1):
    d.arrow([(CX, y0), (CX, y1)], 'main', 1.8)


def lbranch(cy, title, sub, tint):
    d.box(LX - 150, cy - 26, 300, 52, title, sub=sub, tint=tint, title_size=11.5, sub_size=9.5)
    d.arrow([(CX - DW / 2.0, cy), (LX + 150, cy)], 'red', 1.6)
    d.arrow_label((CX - DW / 2.0 + LX + 150) / 2.0, cy - 8, 'NO', 10, S.C_RED)


def rbranch(cy, title, sub, tint, tag):
    d.box(RX - 150, cy - 26, 300, 52, title, sub=sub, tint=tint, title_size=11.5, sub_size=9.5)
    d.arrow([(CX + DW / 2.0, cy), (RX - 150, cy)], 'ctrl', 1.6)
    d.arrow_label((CX + DW / 2.0 + RX - 150) / 2.0, cy - 8, tag, 10, S.C_CTRL)


# ---- 主脊 ----
d.pill(CX, 100, 430, 40, '记忆工程 巩固·蒸馏 产出蒸馏产物（跨 episode 归纳）', tint='green', title_size=11.5, bold=True)
v(120, 132)
sdia(166, '晋升门槛?', '频次≥N · 跨 episode 一致 · conf≥θ')
lbranch(166, '留在 记忆工程', 'kind=MEMORY 继续衰减/巩固', 'gray')
v(200, 212)
sbox(234, '生成晋升候选', 'origin=DISTILLED + lineage 指向源 episodes', 'green')
v(256, 268)
sbox(290, '知识工程 侧接收', '进入 抽取 / 规范化（作为候选 unit）', 'blue')
v(312, 324)
sdia(358, '可溯源?', '有 evidenceSpan · 可回指源文档')
lbranch(358, 'QUARANTINED', '无溯源 → 人工修复或丢弃', 'red')
v(392, 404)
sdia(438, '与现有 知识工程 unit 冲突?', '调和引擎检 CONTRADICTS')
rbranch(438, 'CONFLICTED', '成对返回 + 人审裁决', 'orange', '冲突')
v(472, 484)
sbox(506, '审核闸门', 'origin=DISTILLED → PENDING_REVIEW（SLA ≤ 3 工作日）', 'teal')
v(528, 540)
sdia(574, '人审 / 双模型互检 通过?', '')
lbranch(574, '驳回', '→ QUARANTINED / 丢弃', 'red')
v(608, 618)
sdia(650, 'validFrom > now?', '', h=64)
rbranch(650, 'SCHEDULED', '定时 job 到点 → ACTIVE', 'yellow', 'YES')
v(682, 694)
sbox(716, 'ACTIVE（可召回）', '写 bw_lifecycle_event（append-only）', 'green')
v(738, 750)
sbox(772, '记忆工程 侧原 memory 处置', 'supersededBy 指向新 unit / Demotion 降权', 'purple')

for y in (200, 392, 608, 682):
    d.arrow_label(CX + 14, y + 5, 'YES', 9.5, S.C_MAIN, anchor='start')

# ---- 旁路：Demotion / Bootstrap ----
d.note(1262, 118, '旁路（非晋升正门）', 11, anchor='start', color=S.INK2, weight='600')
d.box(1262, 130, 206, 84, 'Demotion 降级', lines=['知识工程 unit 长期零使用 /', '纠正超阈 → 降回 记忆工程', 'kind=MEMORY 重新巩固'],
      tint='purple', title_size=11.5, sub_size=9)
d.box(1262, 248, 206, 84, 'Bootstrap 冷启动', lines=['全新领域无 知识工程 时，', '记忆工程 蒸馏直接搭', '知识工程 骨架（低 conf）'],
      tint='purple', title_size=11.5, sub_size=9)
d.arrow([(1468, 172), (1484, 172), (1484, 772), (840, 772)], 'async', 1.5, dash='5,3', curve=True)
d.arrow_label(1160, 766, 'Demotion 降权回写', 9.5, S.C_ASYNC)
d.arrow([(1262, 290), (840, 290)], 'async', 1.5, dash='5,3')
d.arrow_label(1050, 284, 'Bootstrap 冷启动注入', 9.5, S.C_ASYNC)

# ---- 注记 ----
d.notes(60, 812, [
    '晋升是「记忆→知识」的唯一写入路径：蒸馏产物必须过门槛 + 溯源 + 冲突 + 审核四道闸，不直接写 ACTIVE。',
    '每次晋升/降级都写 bw_lifecycle_event 与 bw_lineage（候选←源 episodes），保证可回放、可回滚。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
