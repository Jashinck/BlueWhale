# -*- coding: utf-8 -*-
"""图 08 resolve() 时序图：Langur 上下文装配组件 ↔ 上下文供给 ↔ 索引层 ↔ 存储 ↔ 使用反馈。

默认读路径 0 次 LLM 调用；EMPTY 是合法早退；摘要→概览 展开为 opt；feedback 为异步闭环。
"""
import bw_style as S

W, H = 1320, 950
d = S.Diagram('08-sequence-resolve', W, H,
              title='resolve() 供给时序图 —— 默认 0 次 LLM 调用的同步读路径 + 异步反馈闭环',
              subtitle='权限在召回前生效；不检索 / 证据不足 → EMPTY+原因码 早退；摘要→概览 展开为显式 opt')

LX = {'C': 150, 'S': 430, 'I': 720, 'D': 980, 'F': 1200}
TOP, BOT = 132, 872

# ---- 生命线头部 ----
d.box(LX['C'] - 90, 88, 180, 44, 'Langur 上下文装配', sub='调用方 / Agent', tint='blue', title_size=12, sub_size=9.5)
d.box(LX['S'] - 100, 88, 200, 44, '上下文供给 · ResolveContextUseCase', sub='同步读路径', tint='teal', title_size=11, sub_size=9.5)
d.box(LX['I'] - 90, 88, 180, 44, '索引层 skylark-index', sub='dense/BM25/图', tint='teal', title_size=11, sub_size=9.5)
d.box(LX['D'] - 90, 88, 180, 44, 'ContextUnit 存储', sub='PostgreSQL', tint='blue', title_size=11, sub_size=9.5)
d.box(LX['F'] - 80, 88, 160, 44, '使用反馈', sub='bw_usage_event', tint='purple', title_size=11, sub_size=9.5)

for x in LX.values():
    d.arrow([(x, TOP), (x, BOT)], 'gray', 1.2, dash='4,4', marker=False)


def msg(y, a, b, text, color='main', dash=None, w=1.8):
    d.arrow([(LX[a], y), (LX[b], y)], color, w, dash=dash)
    d.arrow_label((LX[a] + LX[b]) / 2.0, y - 7, text, 10, S.ARROW_COLORS.get(color, color))


def selfmsg(y, a, text, color='main'):
    x = LX[a]
    d.arrow([(x, y), (x + 46, y), (x + 46, y + 16), (x, y + 16)], color, 1.6)
    d.note(x + 54, y + 10, text, 10, anchor='start', color=S.ARROW_COLORS.get(color, color))


# ---- 消息序列 ----
msg(170, 'C', 'S', 'resolve(query, scope栈, budget, mode)')
selfmsg(214, 'S', '权限前置（召回前过滤）', 'teal')
selfmsg(258, 'S', '意图门控（检索?哪条线）', 'teal')
msg(310, 'S', 'C', 'alt[不检索 / 证据不足 / 权限不足] → EMPTY + 原因码', 'red', dash='5,3')
msg(354, 'S', 'I', '并发召回 dense+BM25+图（bi-temporal）')
msg(398, 'I', 'D', '读 bw_context_unit（仅 ACTIVE/STALE/CONFLICTED）')
msg(442, 'D', 'I', '候选行', 'gray', dash='5,3')
msg(486, 'I', 'S', '多路候选 + 分数', 'gray', dash='5,3')
selfmsg(530, 'S', 'RRF→重排→MMR', 'teal')
msg(582, 'S', 'D', 'opt[budget=DETAIL] 摘要→概览 按需展开', 'data', dash='5,3')
msg(626, 'D', 'S', '概览/原文 全文', 'gray', dash='5,3')
selfmsg(670, 'S', '预算分配→ContextBundle', 'teal')
msg(722, 'S', 'C', 'ContextBundle（items+level+citations+trace）', 'main', dash=None)
selfmsg(766, 'C', '注入 prompt→LLM/Agent', 'main')
msg(818, 'C', 'F', 'feedback(adopted / corrected / outcome / cost)', 'async', dash='5,3')
msg(856, 'F', 'D', '归因回写：unit 级调权 / 纠正 / reconcile', 'async', dash='5,3')

# ---- 约束注记 ----
d.notes(60, 900, [
    '同步读路径 p95 < 300ms，默认 0 次 LLM 调用（门控=规则+轻量模型，重排=重排/调和模型 或混合）；多跳深检索 = 显式异步工具 explore/expand。',
    'EMPTY 是正确答案：证据不足 / 权限不足 / 冲突未决 → 返回空 + 原因码，不凑数注入；CONFLICTED 单元成对返回。',
    '全链路 trace 可回放：门控判定 · 各路命中与分数 · 融合权重 · 重排分 · MMR · 预算分配 · 摘要→概览 决策。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
