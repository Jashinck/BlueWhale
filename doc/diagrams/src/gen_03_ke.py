# -*- coding: utf-8 -*-
"""图 03 知识工程模块架构图：七段管线 + 横切治理 + 索引层 + 端口存储。"""
import bw_style as S

W, H = 1280, 770
d = S.Diagram('03-module-ke', W, H,
              title='知识工程模块架构图 —— 七段式管线（增量优先 · 摘要先行 · 冲突≠覆盖）',
              subtitle='每段只依赖 domain 端口；抽取/调和/发布为高风险步骤，限定 scope 隔离并按 origin 分档审核')

XS = [55 + i * 170 for i in range(7)]
GAPS = [x + 160 for x in XS]  # 间隙中心

stages = [
    ('接入', ['Connector SPI', '源：md/doc/ppt/xls/git', '产物：SourceRef + 指纹'], 'orange'),
    ('解析归一', ['OCR · 表格 · 代码结构', '格式统一为 DocTree', '保留标题层级'], 'orange'),
    ('结构分段', ['章节/函数/表 切分', '生成目录树', '产物：SegTree + 摘要/概览'], 'orange'),
    ('抽取', ['Claim·Entity·Relation·Rule', '输出 WikiPatch(md diff)', 'claim 标等级 源文/推导/专家/复盘'], 'orange'),
    ('规范化', ['先合并后新增 · 消歧', '本体对齐 · 新类型待裁决', '产物：linked Claims'], 'orange'),
    ('调和', ['冲突检测 · authority 优先', '同属性跨页冲突检查', '产物：Unit 候选 + 矛盾边'], 'orange'),
    ('发布治理', ['提交 ChangeRequest 闸门', '审核 · 回归 · 批次发布', '产物：ACTIVE Unit + md 回写'], 'orange'),
]
for i, (t, subs, tint) in enumerate(stages):
    d.box(XS[i], 110, 160, 110, t, lines=subs, tint=tint, title_size=13, sub_size=9.5)
    if i < 6:
        d.arrow([(XS[i] + 160, 165), (XS[i + 1], 165)], 'main', 2.0)

arts = ['SourceRef', 'DocTree', 'SegTree', 'Claims', 'Linked', 'Unit 候选', '']
for i, a in enumerate(arts[:6]):
    d.pill(GAPS[i], 245, 110, 22, a, tint='white', title_size=10)
    d.arrow([(GAPS[i], 175), (GAPS[i], 234)], 'gray', 1.2, dash='3,3')

# 横切治理带
d.container(55, 290, 1170, 70, '横切：生命周期与治理引擎（贯穿七段，每段必写事件）', tint='red')
gov = ['状态迁移 lifecycle_event', '血缘登记 lineage / DERIVED_FROM',
       '审核闸门：DECLARED 人审 · OBSERVED 抽检 · DISTILLED 双模型互检',
       'quarantine：注入嫌疑 → QUARANTINED']
gw = [240, 260, 360, 250]
gx = 70
for t, w in zip(gov, gw):
    d.pill(gx + w / 2.0, 338, w, 24, t, tint='red', title_size=10)
    gx += w + 14
d.arrow([(640, 290), (640, 262)], 'teal', 1.8)
d.arrow_label(650, 280, '每段写事件', 10, S.C_TEAL, anchor='start')

# 索引层
d.note(55, 392, '索引层（发布后构建，检索只读取 摘要/概览）', 12.5, weight='600', color=S.INK)
idx = [('向量索引', 'pgvector HNSW · 摘要/概览 embed'), ('词法索引', 'BM25 / ES'),
       ('图索引', 'UnitEdge 双时态遍历'), ('目录树', '.abstract(摘要) / .overview(概览)')]
for i, (t, s) in enumerate(idx):
    d.box(55 + i * 296, 404, 280, 62, t, sub=s, tint='teal', title_size=12.5, sub_size=10)
d.arrow([(1235, 165), (1256, 165), (1256, 435), (1223, 435)], 'data', 1.8, curve=True)
d.texts.append('<text transform="rotate(-90 1248 300)" x="1248" y="300" text-anchor="middle" '
               'font-size="10.5" fill="%s">发布 → 建索引</text>' % S.C_DATA)

# 端口与存储
d.box(55, 500, 380, 70, '端口族（domain 定义）',
      lines=['ConnectorPort · ParserPort · SplitterPort', 'ExtractionPort（推理模型） · OntologyPort · IndexPort'],
      tint='purple', title_size=12.5, sub_size=10)
d.box(455, 500, 380, 70, '存储表（PostgreSQL）',
      lines=['bw_asset · bw_segment(树) · bw_context_unit', 'bw_unit_edge · bw_lifecycle_event · bw_review'],
      tint='blue', title_size=12.5, sub_size=10)
d.box(855, 500, 370, 70, '外部依赖',
      lines=['Git md 单一事实源 + PR 审核', '对象存储原文 · Redis 抽取队列'],
      tint='gray', title_size=12.5, sub_size=10)
d.arrow([(760, 466), (760, 500)], 'gray', 1.8)
d.arrow_label(770, 484, '持久化', 10, S.C_GRAY, anchor='start')

# 增量回流
d.arrow([(1040, 500), (1040, 492), (30, 492), (30, 165), (55, 165)], 'ctrl', 1.6, dash='6,4', curve=True)
d.texts.append('<text transform="rotate(-90 22 330)" x="22" y="330" text-anchor="middle" '
               'font-size="10.5" fill="%s">源变更事件 → 增量重算（不全库重算）</text>' % S.C_CTRL)

d.add_legend(55, 610, [
    ('main', '管线主数据流'),
    ('data', '建索引 / 持久化'),
    ('teal', '治理事件写入'),
    ('ctrl', '增量触发（异步）', '6,4'),
    ('gray', '产物落点 / 委托', '3,3'),
], title='图例', col_w=250)

d.notes(700, 636, [
    '关键实践：摘要先行（先读 摘要 判相关 → 概览 判细节 → 才读 原文）',
    '关键实践：冲突时旧边打 expired_at + invalid_at，不删除（bi-temporal）',
    '关键实践：图谱克制 —— 仅知识密集型查询启用 GraphRAG 路由',
    '闭环设计：所有写入经 ChangeRequest；使用信号经 distill 回流为 变更单草稿（见图 22）',
], size=11, lh=20, color=S.INK2)

print(d.save())
