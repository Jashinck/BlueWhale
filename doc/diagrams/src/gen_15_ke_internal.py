# -*- coding: utf-8 -*-
"""图 15 知识工程 内部架构：七阶段流水线的组件、端口与存储。"""
import bw_style as S

W, H = 1400, 1000
d = S.Diagram('15-ke-internal', W, H,
              title='知识工程 · 内部架构',
              subtitle='七阶段知识处理流程 + Distill 萃取子包 + 横切（本体校验/血缘/传播/变更单闸门）+ 端口 + 存储；写路径异步，所有写入经 ChangeRequest')

d.container(40, 84, 1320, 880, '知识工程模块', tint='green')

# ---- 左列：管线 ----
d.box(60, 120, 680, 44, '输入 · SourceRef', sub='外部 URI · 上传 · 代码库快照（经 SsrfGuard + SecretResolver）', tint='gray', title_size=12.5, sub_size=10)
stages = [
    ('Ingest · 接入', '登记 bw_source，生成 CAPTURED 单元', 'orange'),
    ('Parser · 解析', '按 MIME 策略 → 结构化文档（标题树/表格/代码块）', 'orange'),
    ('Segmenter · 分段', 'SegTree + 摘要/概览（轻量模型）', 'orange'),
    ('Extractor · 抽取', '抽取/嵌入模型输出 WikiPatch（md diff）· claim 标注源文/推导/专家/复盘 · 源文级逐字校验', 'orange'),
    ('Normalizer · 规范化', '先合并后新增 · 实体链接 · 新类型进 PROPOSED 队列 · ontologyVersion', 'orange'),
    ('Reconciler · 调和', 'bi-temporal 写边 · 冲突≠覆盖 · 同属性（binds_property）跨页冲突检查', 'orange'),
    ('Publisher · 发布', '提交 ChangeRequest → 审核 → 回归闸门 → 批次 ATOMIC/CANARY/ROLLBACK', 'orange'),
]
y = 176
for t, sub, tint in stages:
    d.box(60, y, 680, 52, t, sub=sub, tint=tint, title_size=12.5, sub_size=10)
    y += 64
d.box(60, 624, 680, 44, '增量回流 · PropagationJob', sub='源变更 → needsReextract 子树重算（非全量重抽）', tint='teal', title_size=12.5, sub_size=10)

for i in range(8):
    y0 = 164 + i * 64
    d.arrow([(400, y0), (400, y0 + 12)], 'main', 1.6)
d.arrow([(400, 612), (400, 624)], 'main', 1.6)
# 增量回流 loop
d.arrow([(60, 646), (30, 646), (30, 330), (60, 330)], 'teal', 1.6, dash='4,3')

# ---- Distill 萃取子包 ----
d.note(60, 692, 'distill 子包（闭环：使用信号 → 归因 → 变更单草稿，author=SYSTEM 不得自批）', 12, weight='600', color=S.INK)
dist = [('SignalCollector', '收集 bw_signal'), ('Attributor', '按七类归因码定位'),
        ('Clusterer', '按（对象, 归因码）'), ('DraftWriter', '生成变更单草稿')]
for i, (tt, ss) in enumerate(dist):
    d.box(60 + i * 175, 702, 150, 58, tt, sub=ss, tint='yellow', title_size=12, sub_size=10)
    if i < 3:
        d.arrow([(210 + i * 175, 731), (235 + i * 175, 731)], 'ctrl', 1.6)
d.arrow([(735, 740), (756, 740), (756, 600), (740, 600)], 'ctrl', 1.6)
d.arrow_label(764, 680, '变更单草稿', 9.5, S.C_CTRL, anchor='start', bg=False)

# ---- 右列：横切 / 端口 / 存储 ----
d.note(800, 112, '横切组件', 12.5, weight='600', color=S.INK)
d.box(800, 128, 540, 44, 'OntologyValidator', sub='schema + evidenceSpan 校验；本体不得自扩', tint='slate', title_size=12, sub_size=10)
d.box(800, 180, 540, 44, 'LineageTracker', sub='source→segment→claim 血缘 + needsReextract 标记', tint='slate', title_size=12, sub_size=10)
d.box(800, 232, 540, 44, 'PropagationTrigger', sub='源变更事件 → 触发增量重算', tint='slate', title_size=12, sub_size=10)
d.box(800, 284, 540, 44, 'ChangeRequestGate + PublishBatchRepository', sub='变更单状态机 · 回归闸门 · 批次 ATOMIC/CANARY/ROLLBACK', tint='slate', title_size=12, sub_size=10)

d.note(800, 356, '端口（SPI，定义于 domain）', 12.5, weight='600', color=S.INK)
d.box(800, 372, 260, 44, 'ExtractionPort', sub='抽取、蒸馏与重排', tint='purple', title_size=12, sub_size=9.5)
d.box(1090, 372, 250, 44, 'EmbeddingPort', sub='向量化（抽取/嵌入模型）', tint='purple', title_size=12, sub_size=9.5)
d.box(800, 426, 260, 44, 'ObjectStorePort', sub='大对象 / 原文 / md', tint='purple', title_size=12, sub_size=9.5)
d.box(1090, 426, 250, 44, 'IndexPort', sub='HNSW/BM25/图 写入', tint='purple', title_size=12, sub_size=9.5)

d.note(800, 500, '存储（PostgreSQL + pgvector + 对象存储）', 12.5, weight='600', color=S.INK)
d.cylinder(900, 548, 150, 50, 'bw_source', tint='blue', title_size=11)
d.cylinder(1080, 548, 150, 50, 'bw_segment', tint='blue', title_size=11)
d.cylinder(900, 628, 150, 50, 'bw_context_unit', tint='blue', title_size=11)
d.cylinder(1080, 628, 150, 50, 'bw_unit_edge', tint='blue', title_size=11)
d.cylinder(900, 708, 150, 50, 'bw_lineage', tint='blue', title_size=11)
d.cylinder(1080, 708, 150, 50, 'bw_publish_batch', tint='blue', title_size=11)
d.cylinder(900, 788, 150, 50, 'bw_change_request', tint='blue', title_size=11)
d.cylinder(1080, 788, 150, 50, 'bw_signal', tint='blue', title_size=11)

# ---- 关键连线 ----
d.arrow([(740, 394), (770, 394), (770, 150), (800, 150)], 'ctrl', 1.6)
d.arrow_label(778, 270, '校验', 9.5, S.C_CTRL, anchor='start', bg=False)
d.arrow([(740, 586), (788, 586), (788, 306), (800, 306)], 'ctrl', 1.6)
d.arrow_label(792, 440, '提交', 9.5, S.C_CTRL, anchor='end', bg=False)
d.arrow([(935, 470), (935, 548)], 'gray', 1.6)
d.arrow_label(943, 512, '实现（adapter 委托）', 9.5, S.C_GRAY, anchor='start', bg=False)

# ---- 图例 ----
d.add_legend(60, 830, [
    ('main', '阶段流转'),
    ('ctrl', '校验/提交（横切依赖）'),
    ('teal', '增量回流', '4,3'),
    ('gray', '端口实现'),
    ('ctrl', '萃取 → 变更单草稿'),
], title='图例', col_w=220)

d.notes(560, 848, [
    '写路径异步：解析、分段与抽取经抽取 worker 池执行，幂等键 (sourceId, segmentId, extractorVersion)；',
    '调和与发布走批量去抖；失败整批回滚，不留半态。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
