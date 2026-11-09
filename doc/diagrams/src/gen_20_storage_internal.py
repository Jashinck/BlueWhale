# -*- coding: utf-8 -*-
"""图 20 存储与索引内部架构：三索引 + 元数据仓储 + 对象存储 + 部分索引/视图。"""
import bw_style as S

W, H = 1400, 900
d = S.Diagram('20-storage-internal', W, H,
              title='存储与索引 · 内部架构',
              subtitle='三索引分工（HNSW 语义 / BM25 词法 / 边表图）+ PG 元数据 + 对象存储 + 部分索引/派生视图')

d.container(40, 84, 1320, 780, '存储与索引模块', tint='blue')

# ---- 左列：读写路径与优化 ----
d.box(60, 120, 680, 44, '写路径', sub='upsert unit / edge / lineage / episode（append-only 三表）', tint='blue', title_size=12.5, sub_size=10)
d.box(60, 176, 680, 44, '读路径', sub='hybrid search / 图遍历 / 时间旅行查询', tint='blue', title_size=12.5, sub_size=10)
d.box(60, 232, 680, 44, '部分索引 bw_current_idx', sub='"当前为真"最高频谓词，不让每次召回扫全表', tint='teal', title_size=12.5, sub_size=10)
d.box(60, 288, 680, 44, '派生视图 bw_current_truth', sub='valid_to IS NULL AND superseded_by IS NULL AND state=ACTIVE', tint='teal', title_size=12.5, sub_size=10)
d.box(60, 344, 680, 44, 'namespace 租户隔离', sub='scope_path 前缀匹配 + tenant 过滤', tint='teal', title_size=12.5, sub_size=10)

# ---- 右列：索引实现与存储 ----
d.note(800, 112, '索引实现（skylark-index · 4 实现）', 12.5, weight='600', color=S.INK)
d.box(800, 128, 540, 44, 'HNSWIndex', sub='pgvector vector_cosine_ops · 语义', tint='slate', title_size=12, sub_size=10)
d.box(800, 180, 540, 44, 'BM25Index', sub='PG tsvector / ES · 词法', tint='slate', title_size=12, sub_size=10)
d.box(800, 232, 540, 44, 'GraphIndex', sub='bw_unit_edge · 图遍历 + 时间旅行', tint='slate', title_size=12, sub_size=10)
d.box(800, 284, 540, 44, 'RepositoryPort', sub='PG 元数据（Flyway 建表）', tint='slate', title_size=12, sub_size=10)
d.box(800, 336, 540, 44, 'ObjectStorePort', sub='大对象 / 原文 / md（对象存储）', tint='slate', title_size=12, sub_size=10)

d.note(800, 412, '物理存储', 12.5, weight='600', color=S.INK)
d.cylinder(900, 460, 170, 56, 'PostgreSQL', sub='pgvector · Flyway', tint='blue')
d.cylinder(1080, 460, 170, 56, '对象存储', sub='原文 / md', tint='blue')
d.cylinder(990, 560, 170, 56, 'ES / Milvus', sub='词法 · 向量（可选）', tint='blue')

# ---- 关键连线 ----
d.arrow([(740, 198), (764, 198), (764, 150), (800, 150)], 'ctrl', 1.6)
d.arrow_label(772, 172, '召回', 9.5, S.C_CTRL, anchor='start', bg=False)
d.arrow([(740, 142), (782, 142), (782, 306), (800, 306)], 'ctrl', 1.6)
d.arrow_label(782, 220, '写入', 9.5, S.C_CTRL, anchor='end', bg=False)
d.arrow([(985, 388), (985, 460)], 'gray', 1.6)
d.arrow_label(993, 420, '实现', 9.5, S.C_GRAY, anchor='start', bg=False)

# ---- 图例 ----
d.add_legend(60, 620, [
    ('ctrl', '读/写委托'),
    ('gray', '端口实现'),
    ('main', '数据流'),
], title='图例', col_w=240)

d.notes(560, 638, [
    '地基阶段不引入 Neo4j：图先做关系表 + 递归 CTE，量级证明需要再换；',
    'store 不可用 fail-fast，禁止静默降级为内存实现。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
