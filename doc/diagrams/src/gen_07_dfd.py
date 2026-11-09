# -*- coding: utf-8 -*-
"""图 07 数据流转图 DFD：知识工程 / 记忆工程 / 上下文供给 三段泳道，每一跳标注数据形态与目标表。

跨泳道闭环：读通道（记忆工程 存储↓→上下文供给 召回）、反馈通道（usage_event↑右通道 x1508→知识工程/记忆工程 存储）、
晋升通道（记忆工程 复盘→知识工程/记忆工程 间隙 y352→知识工程 规范化 x908 净空列）。所有连线避开流程盒与数据药丸带。
"""
import bw_style as S

W, H = 1600, 1000
d = S.Diagram('07-data-flow-dfd', W, H,
              title='BlueWhale Context 数据流转图（DFD）—— 每一跳的数据形态与目标存储',
              subtitle='知识写入、记忆写入、上下文读取与反馈通过三条通道连接')

BOX_W, BOX_H = 152, 54
XS6 = [184, 400, 616, 832, 1048, 1264]
XS7 = [184, 376, 568, 760, 952, 1144, 1336]


def gaps(xs):
    return [(xs[i] + BOX_W + xs[i + 1]) / 2.0 for i in range(len(xs) - 1)]


def procs(xs, y, items, tint):
    for x, (t, s) in zip(xs, items):
        d.box(x, y, BOX_W, BOX_H, t, sub=s, tint=tint, title_size=11, sub_size=9)


def chain(xs, y, color):
    yc = y + BOX_H / 2.0
    d.arrow([(168, yc), (xs[0], yc)], color, 1.6)
    for i in range(len(xs) - 1):
        d.arrow([(xs[i] + BOX_W, yc), (xs[i + 1], yc)], color, 1.6)


def pills(gx, cy, labels, tint, w=184, h=48):
    for x, (l1, l2) in zip(gx, labels):
        d.pill(x, cy, w, h, '', tint=tint)
        d.note(x, cy - 3, l1, 9.3, anchor='middle', color=S.INK)
        d.note(x, cy + 13, l2, 9.3, anchor='middle', color=S.INK2)


def ticks(xs, y0, y1):
    for gx in gaps(xs):
        d.arrow([(gx, y0), (gx, y1)], 'gray', 1.0, dash='2,2', marker=False)


# ===== 泳道容器 =====
d.container(40, 84, 1520, 262, '知识工程写入流', tint='blue')
d.container(40, 358, 1520, 242, '记忆工程写入流', tint='green')
d.container(40, 612, 1520, 242, '上下文读取流与使用反馈', tint='teal')

# ---------- ① KE ----------
d.box(56, 112, 112, BOX_H, '外部源', sub='md·doc·ppt·xls·web', tint='gray', title_size=12, sub_size=9)
procs(XS6, 112, [
    ('接入与解析', 'Connector+DocTree'),
    ('分段·建目录', 'SegTree+摘要/概览'),
    ('抽取', 'Claim/Entity/Rel'),
    ('规范化·消歧', '本体对齐 QID'),
    ('调和', 'bi-temporal 冲突'),
    ('发布·建索引', '三索引+权限'),
], 'blue')
chain(XS6, 112, 'main')
ticks(XS6, 166, 172)
pills(gaps(XS6), 196, [
    ('SourceRef + contentHash', '→ bw_source'),
    ('DocTree + SegTree + 摘要/概览', '→ bw_segment'),
    ('Claim/Entity/Relation/Rule', '+ evidenceSpan·qualifier'),
    ('消歧候选 (QID 归一)', '+ confidence'),
    ('bi-temporal 单元', 'validFrom/To·冲突标记'),
], 'blue')
d.cylinder(1340, 250, 200, 56, 'bw_context_unit', sub='kind=KNOWLEDGE + 三索引', tint='blue', sub_size=9)
d.arrow([(1340, 166), (1340, 250)], 'main', 1.6)
d.arrow_label(1352, 210, '发布写入', 9.5, S.C_MAIN, anchor='start')
d.arrow([(1240, 290), (240, 290), (240, 166)], 'gray', 1.4, dash='5,3', curve=True)
d.arrow_label(740, 284, '源变更 / 复审逾期 → 增量重算（只重跑受影响子图）', 9.5, S.C_GRAY)

# ---------- ② ME ----------
d.box(56, 386, 112, BOX_H, '交互轨迹', sub='对话·工具·结果', tint='gray', title_size=12, sub_size=9)
procs(XS6, 386, [
    ('捕获', 'episode 登记'),
    ('抽取', 'ADD/UPD/DEL/NOOP'),
    ('挂链', '时间线 prev/next'),
    ('巩固·蒸馏', '跨 episode 归纳'),
    ('衰减·强化', '自适应 score'),
    ('复盘', '纠偏·晋升判定'),
], 'green')
chain(XS6, 386, 'data')
ticks(XS6, 440, 446)
pills(gaps(XS6), 470, [
    ('Episode (append-only)', '→ bw_episode'),
    ('记忆操作 + 目标 unit', 'ADD/UPDATE/DELETE/NOOP'),
    ('MemoryUnit + 时间链', '→ bw_context_unit(MEMORY)'),
    ('蒸馏产物 (跨 episode)', '+ 晋升候选'),
    ('衰减 / 强化后 score', '→ bw_context_unit'),
], 'green')
d.cylinder(1340, 524, 200, 56, 'bw_context_unit', sub='kind=MEMORY + bw_episode 账本', tint='green', sub_size=9)
d.arrow([(1340, 440), (1340, 524)], 'data', 1.6)
d.arrow_label(1352, 484, '写入', 9.5, S.C_DATA, anchor='start')

# ---------- ③ Serve ----------
d.box(56, 640, 112, BOX_H, 'resolve()', sub='query·scope·budget', tint='gray', title_size=11.5, sub_size=9)
procs(XS7, 640, [
    ('权限前置', 'AccessPolicy 裁剪'),
    ('意图门控', '检索?·哪条线'),
    ('召回与融合', 'dense+BM25+图 k=60'),
    ('重排与去冗', 'cross-encoder·去冗'),
    ('预算装载', 'CORE/ON_DEMAND'),
    ('ContextBundle', '+citations+trace'),
    ('注入 prompt', '→ LLM / Agent'),
], 'teal')
chain(XS7, 640, 'main')
ticks(XS7, 694, 720)
pills(gaps(XS7), 744, [
    ('权限裁剪后的可见域', 'scope ∩ AccessPolicy'),
    ('意图 + 事件时间线、实体关系图或精炼文档层', '或 EMPTY + 原因码'),
    ('多路候选 + 分数', 'dense / BM25 / 图遍历'),
    ('融合重排去冗 topN', 'RRF→rerank→MMR'),
    ('CORE/ON_DEMAND 切分', '+ 摘要→概览 展开决策'),
    ('ContextBundle', 'items+citations+trace'),
], 'teal', w=168, h=46)
d.cylinder(1340, 782, 200, 52, 'bw_usage_event', sub='adopted·corrected·outcome·cost', tint='purple', sub_size=8.5)
d.arrow([(1412, 694), (1412, 772), (1340, 772), (1340, 782)], 'async', 1.6, dash='5,3', curve=True)
d.arrow_label(1352, 730, '采集', 9.5, S.C_ASYNC, anchor='start')

# ===== 三闭环通道 =====
# 读通道（gray）：ME 存储 → Serve 召回（走 x1340↓ + y628 顶部净空）
d.arrow([(1340, 580), (1340, 628), (644, 628), (644, 640)], 'gray', 1.8, dash='4,3', curve=True)
d.arrow_label(1000, 622, '读取召回：bw_context_unit 三索引 → 候选（知识工程/记忆工程 同一张表）', 9.5, S.C_GRAY)
# 反馈通道（async）：usage_event → 右通道 x1508 → KE / ME 存储
d.arrow([(1440, 808), (1508, 808), (1508, 270)], 'async', 1.6, dash='5,3', marker=False, curve=True)
d.arrow([(1508, 270), (1440, 270)], 'async', 1.8, dash='5,3')
d.arrow_label(1444, 262, '归因→知识工程', 9.3, S.C_ASYNC, bg=False, anchor='start')
d.arrow([(1508, 540), (1440, 540)], 'async', 1.8, dash='5,3')
d.arrow_label(1444, 532, '归因→记忆工程', 9.3, S.C_ASYNC, bg=False, anchor='start')
# 晋升通道（ctrl）：ME 复盘 → KE/ME 间隙 y352 → KE 规范化（x908 净空列）
d.arrow([(1340, 386), (1340, 352), (908, 352), (908, 166)], 'ctrl', 1.8, dash='6,3', curve=True)
d.arrow_label(1120, 346, '晋升桥 Promotion（复盘判定 → 知识工程 规范化·消歧）', 9.5, S.C_CTRL)

# ===== 图例 + 记法说明 =====
d.add_legend(56, 872, [
    ('main', '知识工程 / 上下文供给 主流程'),
    ('data', '记忆工程 写入主流程'),
    ('gray', '读取 / 增量重算', '4,3'),
    ('async', '使用反馈归因（异步）', '5,3'),
    ('ctrl', '晋升桥 记忆工程→知识工程', '6,3'),
], title='图例 · 箭头语义', col_w=250, size=10.5)
d.notes(1000, 890, [
    '数据药丸 = 该跳的数据形态；「→ 表名」= 落库目标。',
    '知识工程/记忆工程 两个 bw_context_unit 是同一张表的不同 kind；存储只画关键终端表。',
    '反馈通道 = usage_event 归因 → 知识工程 调权/reconcile、记忆工程 纠正回写（异步）。',
    '三不变式：默认软失效（留墓碑）· 状态转移必写 bw_lifecycle_event · 仅 →SUPERSEDED/→ERASED 不可逆。',
], size=10, lh=17, color=S.INK2)

print(d.save())
