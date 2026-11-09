# -*- coding: utf-8 -*-
"""图 06 生命周期状态机：12 态 · 仅 3 态可召回 · 2 条不可逆边 · 退场总线。"""
import bw_style as S

W, H = 1400, 790
d = S.Diagram('06-lifecycle-state-machine', W, H,
              title='ContextUnit 生命周期状态机 —— 12 态（知识工程/记忆工程 共用骨架）',
              subtitle='状态决定"能不能被 Agent 看到"；每次转移必写 bw_lifecycle_event（append-only）；不可逆转移仅 →SUPERSEDED 与 →ERASED')

RX = 25
# 起始/终止点
d.nodes.append('<circle cx="70" cy="145" r="7" fill="%s"/>' % S.INK)
d.nodes.append('<circle cx="70" cy="345" r="7" fill="%s"/>' % S.INK)
d.nodes.append('<circle cx="1320" cy="656" r="8" fill="%s"/>' % S.INK)
d.nodes.append('<circle cx="1320" cy="656" r="4" fill="#ffffff"/>')

# 状态
d.box(110, 120, 160, 50, 'CAPTURED', sub='原始信号/文档已登记', tint='gray', rx=RX, title_size=12.5, sub_size=10)
d.box(350, 120, 160, 50, 'EXTRACTED', sub='候选 unit 已抽出未校验', tint='gray', rx=RX, title_size=12.5, sub_size=10)
d.box(620, 120, 170, 50, 'QUARANTINED', sub='校验失败/无溯源/疑似注入', tint='red', rx=RX, title_size=12.5, sub_size=10)
d.box(110, 270, 150, 50, 'SHADOW', sub='重抽战役·只参与评测', tint='yellow', rx=RX, title_size=12.5, sub_size=10)
d.box(350, 270, 170, 50, 'PENDING_REVIEW', sub='待人审/双模型互检', tint='yellow', rx=RX, title_size=12.5, sub_size=10)
d.box(620, 270, 160, 50, 'SCHEDULED', sub='已批准·未到 validFrom', tint='yellow', rx=RX, title_size=12.5, sub_size=10)
d.box(880, 270, 160, 50, 'ACTIVE', sub='现行有效 · 默认进召回', tint='green', rx=RX, title_size=12.5, sub_size=10)
d.box(620, 420, 170, 50, 'STALE_SUSPECT', sub='可疑仍可能有效·降权+警示', tint='green', rx=RX, title_size=12.5, sub_size=10)
d.box(880, 420, 160, 50, 'CONFLICTED', sub='未消解冲突·成对返回', tint='green', rx=RX, title_size=12.5, sub_size=10)
d.box(620, 570, 160, 50, 'SUPERSEDED', sub='已被替代（软失效）', tint='red', rx=RX, title_size=12.5, sub_size=10)
d.box(860, 570, 150, 50, 'ARCHIVED', sub='主动归档留证', tint='red', rx=RX, title_size=12.5, sub_size=10)
d.box(1080, 570, 140, 50, 'EXPIRED', sub='保留期到·待销毁', tint='red', rx=RX, title_size=12.5, sub_size=10)
d.box(1260, 570, 120, 50, 'ERASED', sub='仅留墓碑', tint='red', rx=RX, title_size=12.5, sub_size=10)

# 转移
d.arrow([(77, 145), (110, 145)], 'main', 2.0)
d.arrow([(270, 145), (350, 145)], 'main', 2.0)
d.arrow_label(310, 137, '抽取完成 ≤1h', 10, S.C_MAIN)
d.arrow([(510, 145), (620, 145)], 'red', 2.0)
d.arrow_label(565, 137, '校验失败/疑似注入', 10, S.C_RED)
d.arrow([(390, 170), (390, 220), (185, 220), (185, 270)], 'ctrl', 1.6, curve=True)
d.arrow_label(290, 214, '重抽战役产物', 10, S.C_CTRL)
d.arrow([(450, 170), (450, 270)], 'main', 1.8)
d.arrow_label(458, 262, 'DECLARED / DISTILLED', 10, S.C_MAIN, anchor='start')
d.arrow([(510, 158), (560, 158), (560, 250), (960, 250), (960, 270)], 'main', 1.8, curve=True)
d.arrow_label(760, 262, 'OBSERVED 且 confidence ≥ 阈值（自动发布）', 10, S.C_MAIN)
d.arrow([(705, 170), (705, 205), (480, 205), (480, 170)], 'teal', 1.6, curve=True)
d.arrow_label(592, 199, '人工修复重抽', 10, S.C_TEAL)
d.arrow([(260, 295), (350, 295)], 'main', 1.8)
d.arrow_label(305, 287, '影子评测达标', 10, S.C_MAIN)
d.arrow([(185, 320), (185, 345), (78, 345)], 'gray', 1.6, curve=True)
d.arrow_label(130, 337, '不达标丢弃', 10, S.C_GRAY)
d.arrow([(520, 295), (620, 295)], 'main', 1.8)
d.arrow_label(570, 287, '批准·validFrom>now', 10, S.C_MAIN)
d.arrow([(435, 320), (435, 352), (800, 352), (800, 315), (880, 315)], 'main', 1.8, curve=True)
d.arrow_label(620, 346, '批准且已生效 ≤3 工作日', 10, S.C_MAIN)
d.arrow([(435, 270), (435, 235), (705, 235), (705, 170)], 'red', 1.6, curve=True)
d.arrow_label(575, 246, '驳回', 10, S.C_RED)
d.arrow([(780, 285), (880, 285)], 'main', 1.8)
d.arrow_label(830, 277, '到达 validFrom ≤5min', 10, S.C_MAIN)
d.arrow([(940, 320), (940, 372), (705, 372), (705, 420)], 'ctrl', 1.8, curve=True)
d.arrow_label(948, 398, '源变更 / 纠正超阈 / 复审逾期 ≤15min', 10, S.C_CTRL, anchor='start')
d.arrow([(1000, 320), (1000, 420)], 'ctrl', 1.8)
d.arrow_label(1008, 372, '检出未消解冲突', 10, S.C_CTRL, anchor='start')
d.arrow([(790, 445), (830, 445), (830, 300), (880, 300)], 'teal', 1.6, curve=True)
d.arrow_label(838, 410, '复核确认仍有效', 10, S.C_TEAL, anchor='start')
d.arrow([(1040, 445), (1090, 445), (1090, 300), (1040, 300)], 'teal', 1.6, curve=True)
d.arrow_label(1098, 380, '冲突消解', 10, S.C_TEAL, anchor='start')
d.arrow([(660, 470), (660, 570)], 'red', 2.0)
d.arrow_label(652, 522, '新版本替代', 10, S.C_RED, anchor='end')
d.arrow([(900, 320), (900, 362), (545, 362), (545, 595), (620, 595)], 'red', 2.0, curve=True)
d.arrow_label(537, 430, '新版本替代（不可逆）', 10, S.C_RED, anchor='end')

# 退场总线
d.edges.append('<line x1="560" y1="535" x2="1220" y2="535" stroke="%s" stroke-width="1.6"/>' % S.C_GRAY)
d.arrow([(740, 470), (740, 535)], 'gray', 1.6)
d.arrow([(1040, 310), (1120, 310), (1120, 535)], 'gray', 1.6, curve=True)
d.arrow([(935, 570), (935, 535)], 'gray', 1.6)
d.arrow([(700, 570), (700, 535)], 'gray', 1.6)
d.arrow([(1150, 535), (1150, 570)], 'gray', 1.8)
d.arrow_label(1142, 556, '保留期到 retentionUntil', 10, S.C_GRAY, anchor='end')
d.arrow([(1220, 595), (1260, 595)], 'red', 2.0)
d.arrow_label(1240, 562, '无 legal hold ≤30d', 9.5, S.C_RED)
d.arrow([(1080, 605), (1010, 605)], 'teal', 1.6)
d.arrow_label(1045, 621, 'legal hold 冻结退场', 9.5, S.C_TEAL)
d.arrow([(1320, 620), (1320, 648)], 'red', 2.0)

# 召回行为与不变式
d.note(60, 700, '召回行为：', 12, weight='600', color=S.INK)
d.pill(180, 696, 150, 26, 'ACTIVE → 默认进召回', tint='green', title_size=10.5)
d.pill(360, 696, 210, 26, 'STALE_SUSPECT → 降权 + 带警示', tint='green', title_size=10.5)
d.pill(600, 696, 230, 26, 'CONFLICTED → 成对返回 + 进 notes', tint='green', title_size=10.5)
d.pill(860, 696, 200, 26, 'QUARANTINED → 永不进召回', tint='red', title_size=10.5)
d.notes(60, 736, [
    '不变式 1：默认软失效；硬删只能由 erasure 流程触发，必须留墓碑、必须级联、必须可验证',
    '不变式 2：状态转移必须写事件（bw_lifecycle_event，append-only），无事件的状态变化视为 bug',
    '不变式 3：不可逆转移只有两个 —— →SUPERSEDED 与 →ERASED；其余全部可回退以支持误判恢复',
], size=11, lh=17, color=S.INK2)

d.add_legend(1090, 680, [
    ('main', '正常推进'),
    ('ctrl', '风险触发（源变更/冲突）'),
    ('teal', '人工/复核回退'),
    ('red', '不可逆 / 拒绝'),
    ('gray', '退场总线（保留期）'),
], title='图例', col_w=150)

print(d.save())
