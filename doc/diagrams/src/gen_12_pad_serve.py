# -*- coding: utf-8 -*-
"""图 12 PAD：上下文供给的意图门控与多路融合排序算法。

默认 0 次 LLM 调用；EMPTY 是合法早退；预算决定 摘要→概览 展开。
"""
import bw_style as S

W, H = 1320, 900
d = S.Diagram('12-pad-serve', W, H,
              title='PAD 图 · 意图门控 + 多路融合排序（上下文供给）',
              subtitle='默认 0 次 LLM 调用；EMPTY 是合法早退；预算决定 摘要→概览 展开；全链路 trace 可回放')

L0, L1 = 60, 140


def pb(x, y, w, h, title, sub=None, tint='blue', ts=11.5, ss=9.5, lines=None):
    d.box(x, y, w, h, title, sub=sub, tint=tint, title_size=ts, sub_size=ss, lines=lines)


def spine(x, y0, y1, ticks):
    d.arrow([(x, y0), (x, y1)], 'ink', 1.2, marker=False)
    for ty in ticks:
        d.arrow([(x, ty), (x + 20, ty)], 'ink', 1.2, marker=False)


spine(48, 118, 862, [118, 178, 238, 408, 528, 588, 648, 708, 862])
pb(L0, 96, 520, 44, '输入', 'resolve(query, scope栈, budget, mode)', 'blue')
pb(L0, 156, 520, 44, '权限前置', '可见域 = scope ∩ AccessPolicy（召回前过滤，非召回后）', 'teal')
pb(L0, 216, 520, 44, 'SEL  意图门控', '规则 + 轻量模型（0 LLM）：要不要检索？选择事件时间线、实体关系图还是精炼文档层？', 'yellow')
spine(128, 296, 346, [296, 346])
pb(L1, 276, 520, 40, 'NO / 证据不足 / 权限不足', '→ EMPTY + 原因码（早退，不凑数注入）', 'red', ts=11, ss=9)
pb(L1, 326, 520, 40, 'YES', '→ 进入多路召回', 'green', ts=11, ss=9)
pb(L0, 386, 520, 44, 'LOOP  每条召回线并发', 'dense(HNSW) / BM25 / 图遍历', 'slate')
spine(128, 408, 466, [466])
pb(L1, 446, 520, 40, '取候选 + 分数', 'bi-temporal 过滤：仅 ACTIVE / STALE(降权) / CONFLICTED(成对)', 'teal', ts=11, ss=9)
pb(L0, 506, 520, 44, 'RRF 融合', 'k=60，多路名次倒数融合', 'teal')
pb(L0, 566, 520, 44, '重排', 'cross-encoder(重排/调和模型) 或 向量+词面混合', 'teal')
pb(L0, 626, 520, 44, 'MMR 去冗', 'λ 多样性，剔除高度重复候选', 'teal')
pb(L0, 686, 520, 44, 'SEL  预算分配', 'CORE / ON_DEMAND 切分 + 深度决策', 'yellow')
spine(128, 766, 810, [766, 810])
pb(L1, 746, 520, 40, 'budget=DETAIL / 多跳 explore', '→ 摘要→概览 按需展开（记 bw_usage_event，异步）', 'orange', ts=11, ss=9)
pb(L1, 790, 520, 40, '否则', '→ 只装 摘要/概览（同步路径，p95<300ms）', 'green', ts=11, ss=9)
pb(L0, 840, 520, 44, '组装 + 输出', 'ContextBundle(items+level+citations+trace) 或 EMPTY', 'green')

# ---- 右栏 ----
pb(760, 96, 520, 112, '意图门控 = 规则 + 轻量模型（0 LLM）', lines=[
    '查询类型判定：事实 / 程序 / 偏好 / 非检索承载',
    '检索线选择：事件时间线、实体关系图、精炼文档层',
    '置信不足 → EMPTY，不凑数（堵对抗类幻觉）',
], tint='teal', ts=12, ss=10)
pb(760, 228, 520, 96, '三预算装载', lines=[
    'CORE：身份/任务，总是满载',
    'ON_DEMAND：长期记忆/知识，按需加载',
    'LEDGER：事件账本，只写不读（供再加工与审计）',
], tint='green', ts=12, ss=10)
pb(760, 344, 520, 84, 'EMPTY 是正确答案', lines=[
    '证据不足 / 权限不足 / 冲突未决 → 空 + 原因码',
    'CONFLICTED 单元成对返回，不单边裁决',
], tint='red', ts=12, ss=10)
pb(760, 448, 520, 84, 'trace 全链路可回放', lines=[
    '门控判定 · 各路命中与分数 · 融合权重 · 重排分',
    'MMR 选择 · 预算分配 · 摘要→概览 决策',
], tint='purple', ts=12, ss=10)
d.notes(760, 560, [
    '同步读路径 p95 < 300ms，默认 0 次 LLM 调用；',
    '多跳 agentic 深检索 = 显式异步工具 explore/expand（记账）。',
    '无 trace 则归因与调参无从谈起，故 trace 为强制产出。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
