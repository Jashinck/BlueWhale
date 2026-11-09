# -*- coding: utf-8 -*-
"""图 05 上下文供给模块架构图：读路径 + 三预算 + EMPTY 语义 + trace。"""
import bw_style as S

W, H = 1280, 900
d = S.Diagram('05-module-serve', W, H,
              title='上下文供给模块架构图 —— resolve() 读路径（默认 0 次 LLM 调用）',
              subtitle='权限在召回前生效；EMPTY 是正确答案；全链路 trace 可回放；多跳深检索是显式异步工具')

# ---- 左：主读路径 ----
d.box(60, 100, 300, 50, '入口 resolve(query, scope栈, budget, mode)', tint='blue', title_size=12)
d.box(60, 170, 300, 50, '权限前置', sub='scope 可见性 + AccessPolicy（召回前过滤，非召回后）',
      tint='teal', title_size=12.5, sub_size=10)
d.diamond(210, 272, 240, 76, '意图门控', sub='要不要检索？检索选择事件时间线、实体关系图还是精炼文档层？', tint='yellow')
d.box(60, 340, 300, 60, '多路召回（并发）', sub='dense + BM25 + 图遍历 + 时间过滤（bi-temporal）',
      tint='teal', title_size=12.5, sub_size=10)
d.box(60, 420, 300, 44, 'RRF 融合（k=60）', tint='teal', title_size=12.5)
d.box(60, 484, 300, 44, '重排 cross-encoder（重排/调和模型）', tint='teal', title_size=12.5)
d.box(60, 548, 300, 44, 'MMR 去冗（λ 多样性）', tint='teal', title_size=12.5)
d.box(60, 612, 300, 60, '预算分配', sub='CORE / ON_DEMAND 切分 + 摘要→概览 展开决策',
      tint='teal', title_size=12.5, sub_size=10)
d.box(60, 692, 300, 50, '出口 ContextBundle', sub='items + level + citations + trace',
      tint='blue', title_size=12.5, sub_size=10)

d.arrow([(210, 150), (210, 170)], 'main', 2.0)
d.arrow([(210, 220), (210, 234)], 'main', 2.0)
d.arrow([(210, 310), (210, 340)], 'main', 2.0)
d.arrow_label(220, 328, 'YES', 10, S.C_MAIN, anchor='start')
d.arrow([(330, 272), (420, 272)], 'red', 2.0)
d.arrow_label(375, 262, 'NO / 证据不足', 10, S.C_RED)
d.arrow([(210, 400), (210, 420)], 'main', 2.0)
d.arrow([(210, 464), (210, 484)], 'main', 2.0)
d.arrow([(210, 528), (210, 548)], 'main', 2.0)
d.arrow([(210, 592), (210, 612)], 'main', 2.0)
d.arrow([(210, 672), (210, 692)], 'main', 2.0)

d.box(420, 245, 380, 54, 'EMPTY + 原因码',
      lines=['NOT_RETRIEVAL_BEARING / NO_EVIDENCE', 'PERMISSION / CONFLICT_PENDING'],
      tint='red', title_size=12.5, sub_size=9.5)

# ---- 中：语义与约束 ----
d.box(420, 340, 380, 90, '装载三预算',
      lines=['CORE：身份/任务，总是满载', 'ON_DEMAND：长期记忆/知识，按需加载', 'LEDGER：事件账本，只写不读（供再加工与审计）'],
      tint='green', title_size=12.5, sub_size=10)
d.box(420, 450, 380, 80, 'EMPTY 是正确答案',
      lines=['证据不足 / 权限不足 / 冲突未决 → 返回空 + 原因码', '不凑数注入（堵 LoCoMo 对抗类幻觉）', 'CONFLICTED 单元成对返回，不单边裁决'],
      tint='red', title_size=12.5, sub_size=10)
d.box(420, 550, 380, 80, 'trace 全链路可回放',
      lines=['门控判定 · 各路命中与分数 · 融合权重', '重排分 · MMR 选择 · 预算分配 · 摘要→概览 决策', '无 trace 则归因与调参无从谈起'],
      tint='purple', title_size=12.5, sub_size=10)
d.box(420, 650, 380, 70, '0 次 LLM 约束',
      lines=['门控 = 规则 + 轻量模型；重排 = 重排/调和模型 或向量+词面混合', '多跳 agentic 深检索 = 显式工具 explore/expand（异步记账）'],
      tint='yellow', title_size=12.5, sub_size=10)

# ---- 右：索引与存储 ----
d.container(860, 340, 380, 90, '索引层（只读取摘要/概览）', tint='teal')
for i, t in enumerate(['向量 HNSW', '词法 BM25', '图遍历', '目录树']):
    d.pill(905 + i * 92, 398, 84, 24, t, tint='teal', title_size=10)
d.cylinder(1050, 470, 380, 60, 'ContextUnit 存储（PostgreSQL）', sub='bw_context_unit · bw_unit_edge', tint='blue', sub_size=10)
d.box(860, 560, 380, 60, '使用反馈接收', sub='feedback → bw_usage_event → unit 级归因',
      tint='purple', title_size=12.5, sub_size=10)

# ---- 跨列连线 ----
d.arrow([(360, 370), (390, 370), (390, 320), (1050, 320), (1050, 340)], 'gray', 1.6, curve=True)
d.arrow_label(700, 314, '并发查询', 10, S.C_GRAY)
d.arrow([(860, 400), (830, 400), (830, 446), (360, 446)], 'gray', 1.6, curve=True)
d.arrow_label(600, 440, '候选 摘要/概览 + 分数', 10, S.C_GRAY)
d.arrow([(860, 500), (830, 500), (830, 648), (360, 648)], 'data', 1.6, dash='5,3', curve=True)
d.arrow_label(600, 642, '概览→原文 按需展开', 10, S.C_DATA)
d.arrow([(210, 742), (210, 764), (1050, 764), (1050, 620)], 'async', 1.8, dash='5,3', curve=True)
d.arrow_label(630, 758, 'feedback(adopted / corrected / outcome / cost)', 10.5, S.C_ASYNC)

d.add_legend(60, 790, [
    ('main', '读路径主流程（同步 p95 < 300ms）'),
    ('gray', '索引委托'),
    ('data', '原文 按需展开', '5,3'),
    ('async', '使用反馈（异步）', '5,3'),
    ('red', 'EMPTY / 拒绝注入'),
], title='图例', col_w=300)

print(d.save())
