# -*- coding: utf-8 -*-
"""图 10 受控擦除流程图：legal hold 优先 → 级联范围 → 墓碑 → 删索引 → 验证 → ERASED。

硬删只能走本流程；默认软失效；→ERASED 不可逆且必留墓碑与 bw_erasure 审计行。
"""
import bw_style as S

W, H = 1200, 880
d = S.Diagram('10-erasure-flow', W, H,
              title='受控擦除流程图 —— legal hold 优先 · 级联清理 · 墓碑留证',
              subtitle='硬删只能由本流程触发；默认软失效；→ERASED 不可逆，必留墓碑 + bw_erasure 审计行')

CX, DW, BW = 600, 380, 460


def sbox(cy, title, sub, tint='blue'):
    d.box(CX - BW / 2.0, cy - 22, BW, 44, title, sub=sub, tint=tint, title_size=11.5, sub_size=9.5)


def sdia(cy, title, sub, h=68):
    d.diamond(CX, cy, DW, h, title, sub=sub, tint='yellow', title_size=11.5, sub_size=9)


def v(y0, y1, color='main'):
    d.arrow([(CX, y0), (CX, y1)], color, 1.8)


d.pill(CX, 100, 470, 40, '触发：retentionUntil 到期 / 合规请求（被遗忘权）/ 制度废止', tint='red', title_size=11.5, bold=True)
v(120, 138)
sbox(160, '定位目标 unit 集合', '读 bw_lineage 找出下游派生（蒸馏/汇总）', 'blue')
v(182, 198)
sdia(232, '存在 legal hold?', '诉讼/审计冻结优先于一切擦除')
d.box(860, 232 - 26, 300, 52, '冻结退场 → ARCHIVED', sub='不擦除 · 留证 · 待 hold 解除', tint='teal', title_size=11.5, sub_size=9.5)
d.arrow([(CX + DW / 2.0, 232), (860, 232)], 'teal', 1.6)
d.arrow_label((CX + DW / 2.0 + 860) / 2.0, 224, 'YES', 10, S.C_TEAL)
v(266, 290)
sbox(312, '计算级联范围', 'embeddings · 摘要/概览 目录项 · 三索引条目 · 派生蒸馏产物', 'blue')
v(334, 354)
sdia(388, '级联清单确认?', '治理 job 自动 / 高风险人工')
d.box(60, 388 - 26, 300, 52, '挂起', sub='→ 人工复核级联范围', tint='gray', title_size=11.5, sub_size=9.5)
d.arrow([(CX - DW / 2.0, 388), (360, 388)], 'gray', 1.6)
d.arrow_label((CX - DW / 2.0 + 360) / 2.0, 380, 'NO', 10, S.C_GRAY)
v(422, 442)
sbox(464, '写 bw_erasure 审计行', '删了什么 · 依据 · 时间 · 执行者 · 级联清单 · 残留说明', 'red')
v(486, 502)
sbox(524, '置墓碑', 'unit 行保留 id + erasedAt + reason，其余字段置空', 'red')
v(546, 562)
sbox(584, '删索引条目', '向量 HNSW · BM25 · 图边；清 摘要/概览 目录项', 'red')
v(606, 622)
sbox(644, '级联处理派生产物', '下游蒸馏 unit 标记 stale / 触发重算', 'red')
v(666, 684)
sdia(718, '验证通过?', '召回返回空 · 墓碑可查 · 级联无残留')
d.box(60, 718 - 26, 300, 52, '回滚 / 告警', sub='→ 人工介入', tint='red', title_size=11.5, sub_size=9.5)
d.arrow([(CX - DW / 2.0, 718), (360, 718)], 'red', 1.6)
d.arrow_label((CX - DW / 2.0 + 360) / 2.0, 710, 'NO', 10, S.C_RED)
v(752, 772)
d.pill(CX, 792, 430, 40, 'ERASED（不可逆）· 仅留墓碑 + bw_erasure 审计行', tint='red', title_size=11.5, bold=True)

d.arrow_label(CX + 14, 271, 'NO', 9.5, S.C_MAIN, anchor='start')
d.arrow_label(CX + 14, 427, 'YES', 9.5, S.C_MAIN, anchor='start')
d.arrow_label(CX + 14, 757, 'YES', 9.5, S.C_MAIN, anchor='start')

d.notes(60, 836, [
    '三不变式之一：默认软失效；硬删只能由本流程触发，且必须留墓碑（§14.6）。legal hold 优先于擦除与保留期。',
    'md 源文件是否删除取决于源保留策略（Git 历史默认保留）；bw_erasure 只记录「系统内」擦除范围与残留说明。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
