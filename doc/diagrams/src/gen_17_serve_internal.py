# -*- coding: utf-8 -*-
"""图 17 上下文供给 内部架构：读路径 七步装配组件 + 横切 + 端口 + 存储。"""
import bw_style as S

W, H = 1400, 1000
d = S.Diagram('17-serve-internal', W, H,
              title='上下文供给 · 内部架构',
              subtitle='读路径 七步装配组件（默认 0 次 LLM）+ 横切（trace/状态过滤）+ 端口 + 存储；同步 p95 < 300ms')

d.container(40, 84, 1320, 880, '上下文供给模块', tint='teal')

# ---- 左列：七步装配 ----
d.box(60, 120, 680, 44, '输入 · ResolveQuery', sub='traceId · scope栈 · intent · query · budget · lines · depth', tint='gray', title_size=12, sub_size=10)
stages = [
    ('PermissionFilter · 权限前置', '可见域 = scope ∩ AccessPolicy（召回前过滤）', 'teal'),
    ('IntentGate · 意图门控', '规则 + 轻量模型：要不要检索？选择事件时间线、实体关系图还是精炼文档层？', 'teal'),
    ('MultiRecall · 多路召回', 'dense(HNSW) / BM25 / 图遍历 并发', 'teal'),
    ('RRFFusion · 融合', 'k=60 名次倒数融合', 'teal'),
    ('Reranker · 重排', 'cross-encoder(重排/调和模型) 或 向量+词面混合', 'teal'),
    ('MMRDeduplicator · 去冗', 'λ 多样性，剔除高度重复候选', 'teal'),
    ('BudgetAllocator · 预算', 'CORE/ON_DEMAND 切分 + 摘要→概览 展开决策', 'teal'),
]
y = 176
for t, sub, tint in stages:
    d.box(60, y, 680, 52, t, sub=sub, tint=tint, title_size=12.5, sub_size=10)
    y += 64
d.box(60, 624, 680, 44, '输出 · ContextBundle', sub='items+level+citations+trace 或 EMPTY + 原因码', tint='green', title_size=12.5, sub_size=10)

for i in range(8):
    y0 = 164 + i * 64
    d.arrow([(400, y0), (400, y0 + 12)], 'main', 1.6)
d.arrow([(400, 612), (400, 624)], 'main', 1.6)

# ---- 右列：横切 / 端口 / 存储 ----
d.note(800, 112, '横切', 12.5, weight='600', color=S.INK)
d.box(800, 128, 540, 44, 'TraceRecorder', sub='全链路 trace：门控/分数/融合权重/MMR/预算 可回放', tint='slate', title_size=12, sub_size=10)
d.box(800, 180, 540, 44, 'StateFilter', sub='bi-temporal 状态过滤：仅 ACTIVE/STALE(降权)/CONFLICTED(成对)', tint='slate', title_size=12, sub_size=10)

d.note(800, 256, '端口（SPI，定义于 domain）', 12.5, weight='600', color=S.INK)
d.box(800, 272, 540, 44, 'IndexPort', sub='dense(HNSW) / BM25 / 图遍历 三路', tint='purple', title_size=12, sub_size=10)
d.box(800, 326, 540, 44, 'RetrievalPort', sub='候选 + 分数（路级 deadline）', tint='purple', title_size=12, sub_size=10)
d.box(800, 380, 540, 44, 'EmbeddingPort', sub='重排/查询向量化', tint='purple', title_size=12, sub_size=10)

d.note(800, 456, '存储', 12.5, weight='600', color=S.INK)
d.cylinder(900, 504, 150, 50, 'bw_context_unit', tint='blue', title_size=11)
d.cylinder(1080, 504, 150, 50, 'bw_unit_edge', tint='blue', title_size=11)
d.cylinder(900, 584, 150, 50, 'bw_usage_event', tint='blue', title_size=11)

# ---- 关键连线 ----
d.arrow([(740, 330), (770, 330), (770, 294), (800, 294)], 'ctrl', 1.6)
d.arrow_label(778, 316, '检索', 9.5, S.C_CTRL, anchor='start', bg=False)
d.arrow([(740, 142), (770, 142), (770, 150), (800, 150)], 'gray', 1.6, dash='4,3')
d.arrow_label(778, 140, '埋点', 9.5, S.C_GRAY, anchor='start', bg=False)
d.arrow([(1070, 424), (1070, 504)], 'gray', 1.6)
d.arrow_label(1078, 466, '实现', 9.5, S.C_GRAY, anchor='start', bg=False)

# ---- 图例 ----
d.add_legend(60, 830, [
    ('main', '装配流转'),
    ('ctrl', '检索委托'),
    ('gray', '埋点 / 端口实现', '4,3'),
], title='图例', col_w=220)

d.notes(560, 848, [
    'EMPTY 是合法答案（证据不足/权限不足/冲突未决 → 空 + 原因码）；',
    '重排模型调用是默认路径唯一的 LLM 点，且必须记 bw_usage_event（异步）。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
