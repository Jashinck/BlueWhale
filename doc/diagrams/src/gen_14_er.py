# -*- coding: utf-8 -*-
"""图 14 ER：物理数据模型（11 表 + 1 派生视图）。

append-only 事件表是状态机与归因的可执行证据；双时态边承载冲突调和。
"""
import bw_style as S

W, H = 1600, 1000
d = S.Diagram('14-er-data-model', W, H,
              title='ER 图 · 物理数据模型（PostgreSQL + pgvector）',
              subtitle='11 张物理表 + 1 派生视图；append-only 事件表是状态机与归因的可执行证据')


def ent(x, y, w, title, cols, tint='blue', dashed=False):
    f, s = S.TINT[tint]
    h = 26 + len(cols) * 15 + 8
    dash = ' stroke-dasharray="6,4"' if dashed else ''
    d.nodes.append('<rect x="%s" y="%s" width="%s" height="%s" rx="8" ry="8" fill="%s" '
                   'stroke="%s" stroke-width="1.5"%s/>' % (x, y, w, h, f, s, dash))
    d.nodes.append('<rect x="%s" y="%s" width="%s" height="26" rx="8" ry="8" fill="%s" '
                   'opacity="0.45"/>' % (x, y, w, s))
    d.nodes.append('<rect x="%s" y="%s" width="%s" height="13" fill="%s" opacity="0.45"/>'
                   % (x, y + 13, w, s))
    d.texts.append('<text x="%s" y="%s" font-size="12" font-weight="700" fill="%s" '
                   'class="mono">%s</text>' % (x + 10, y + 17.5, S.INK, S.esc(title)))
    for i, c in enumerate(cols):
        d.texts.append('<text x="%s" y="%s" font-size="9.5" fill="%s" class="mono">%s</text>'
                       % (x + 10, y + 40 + i * 15, S.INK2, S.esc(c)))
    return y + h


# ---- 中心：聚合根 ----
ent(600, 90, 400, 'bw_context_unit', [
    'id UUID  PK',
    'tenant · scope_path（前缀匹配作用域）',
    'kind · origin · state',
    'l0_abstract / l1_overview / l2_inline / l2_ref',
    'confidence · authority · importance',
    'valid_from / valid_to 现实轴 · learned_at 系统轴',
    'source_published_at / captured_at 采集轴',
    'review_due_at / retention_until / erased_at 治理轴',
    'superseded_by → bw_context_unit.id',
    'cost_tokens · n_seen · n_cited · n_adopted · n_corrected',
    'embedding vector(1536) HNSW · metadata JSONB GIN',
    'ontology_version · batch_id → bw_publish_batch.id',
], tint='blue')

# ---- 左列：溯源 / 供给族 ----
ent(140, 90, 300, 'bw_episode', [
    'id UUID  PK',
    'tenant · scope_path',
    'occurred_at · source',
    'payload JSONB',
    '只增不改（LEDGER 预算，供再加工与审计）',
], tint='purple')
ent(140, 230, 300, 'bw_segment', [
    'id UUID  PK',
    'doc_id · parent_id（目录树自引用）',
    'ordinal · path',
    'abstract · overview',
    'content_ref',
    '层级 = 摘要/概览/原文 三级表征的结构前提',
], tint='purple')
ent(140, 384, 300, 'bw_lineage', [
    'parent_id + child_id + derivation  PK',
    'derivation: PARSED | SEGMENTED |',
    '    EXTRACTED | DISTILLED | SKILLIZED',
    'derived_at · extractor_version · ontology_version',
    'needs_reextract BOOLEAN',
    '变更传播与影响分析的前提',
], tint='purple')
ent(140, 538, 300, 'bw_usage_event', [
    'id BIGSERIAL  PK',
    'unit_id → bw_context_unit.id',
    'trace_id · event · at',
    'tokens · outcome',
    'append-only，归因与成本账原料',
], tint='orange')
ent(140, 678, 300, 'bw_review', [
    'id UUID  PK',
    'unit_id → bw_context_unit.id',
    'gate · status',
    'reviewer · at',
    'note（晋升 / 废弃闸门留痕）',
], tint='orange')

# ---- 右列：关系 / 治理族 ----
ent(1160, 90, 300, 'bw_unit_edge', [
    'from_id + to_id + edge_type + valid_from  PK',
    'edge_type: SUPPORTS / CONTRADICTS /',
    '    SUPERSEDES / DERIVED_FROM / ABOUT …',
    'valid_from / valid_to（双时态边）· weight',
    '冲突调和的落点，时间旅行的依据',
], tint='teal')
ent(1160, 230, 300, 'bw_lifecycle_event', [
    'id BIGSERIAL  PK',
    'unit_id → bw_context_unit.id',
    'from_state · to_state · trigger',
    'actor · at · reason',
    'batch_id → bw_publish_batch.id',
    'append-only，状态机的可执行证据',
], tint='teal')
ent(1160, 384, 300, 'bw_asset', [
    'unit_id  PK → bw_context_unit.id（1:1）',
    'owner_id · owner_org',
    'review_cycle_days · review_due_at · reviewed_at',
    'retention_until',
    'legal_hold · hold_reason（优先于擦除）',
], tint='teal')
ent(1160, 524, 300, 'bw_erasure', [
    'id UUID  PK',
    'unit_id → bw_context_unit.id',
    'request_type · legal_basis',
    'requested_by · executed_at',
    'cascade JSONB · residual_note（墓碑）',
], tint='red')
ent(1160, 664, 300, 'bw_publish_batch', [
    'id UUID  PK',
    'ontology_version · scope_filter',
    'mode: ATOMIC | CANARY | ROLLBACK',
    'status · effective_at',
    'created_by',
], tint='green')

# ---- 派生视图（底带） ----
ent(620, 860, 360, 'VIEW bw_current_truth', [
    'SELECT * FROM bw_context_unit',
    'WHERE valid_to IS NULL AND superseded_by IS NULL',
    "  AND state = 'ACTIVE'",
], tint='gray', dashed=True)

# ---- 关系连线（箭头指向 1 侧） ----
d.arrow([(440, 145), (600, 145)], 'main', 1.6)
d.arrow([(440, 292), (600, 292)], 'main', 1.6)
d.arrow([(1160, 145), (1000, 145)], 'main', 1.6)
d.arrow([(1160, 292), (1000, 292)], 'main', 1.6)
d.arrow([(440, 446), (640, 446), (640, 304)], 'main', 1.6)
d.arrow([(440, 593), (680, 593), (680, 304)], 'main', 1.6)
d.arrow([(440, 733), (720, 733), (720, 304)], 'main', 1.6)
d.arrow([(1160, 439), (960, 439), (960, 304)], 'main', 1.6)
d.arrow([(1160, 579), (920, 579), (920, 304)], 'main', 1.6)
d.arrow([(1160, 719), (880, 719), (880, 304)], 'main', 1.6)
d.arrow([(800, 304), (800, 860)], 'gray', 1.4, dash='4,2')
# 自引用
d.arrow([(700, 90), (700, 76), (860, 76), (860, 90)], 'gray', 1.3)
d.arrow([(200, 230), (200, 216), (340, 216), (340, 230)], 'gray', 1.3)

# ---- 关系标注 ----
d.arrow_label(448, 137, 'provenance.episodeId', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(592, 137, 'N:1', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(448, 284, 'abstract / overview 物化为单元', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(592, 284, 'N:1', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(1152, 137, 'from_id / to_id（双端）', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(1008, 137, 'N:1 ×2', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(1152, 284, 'unit_id', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(1008, 284, '1:N', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(448, 438, 'parent_id / child_id', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(648, 324, 'N:1 ×2', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(448, 585, 'unit_id · trace_id', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(688, 324, '1:N', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(448, 725, 'unit_id', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(728, 324, '1:N', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(1152, 431, 'unit_id PK', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(952, 324, '1:1', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(1152, 571, 'unit_id', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(912, 324, '1:N', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(1152, 711, 'unit.batch_id', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(872, 324, '1:N', 9, S.INK2, bg=False, anchor='end')
d.arrow_label(808, 324, '派生', 9, S.INK2, bg=False, anchor='start')
d.arrow_label(780, 70, 'superseded_by 自引用（软失效）', 9, S.INK2, bg=False)
d.arrow_label(348, 224, 'parent_id 目录树自引用', 9, S.INK2, bg=False, anchor='start')

# ---- 设计纪律 ----
d.notes(60, 860, [
    'append-only 三表只增不改：状态机、归因、审计的可执行证据；',
    'bw_unit_edge 双时态：调和写新边 + valid_to，不覆盖旧边；',
    '图遍历先做递归 CTE，地基阶段 不引入 Neo4j；三索引：HNSW + BM25 + 边表；',
    '"当前为真"走部分索引，不让每次召回扫全表；',
    'bw_source DDL 随 地基阶段 补齐；生命周期五表必须一次建齐。',
], size=10, lh=17, color=S.INK2)

d.add_legend(1040, 860, [
    ('main', '外键 / 强引用（箭头指向 1 侧）'),
    ('gray', '派生 / 自引用（虚线或无箭头）', '4,2'),
], col_w=260, size=10)

print(d.save())
