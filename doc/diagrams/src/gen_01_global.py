# -*- coding: utf-8 -*-
"""图 01 全局架构图：Client / Server 两分（server 内部沿用 Langur DDD，详见 图 02）。"""
import bw_style as S

W, H = 1280, 1000
d = S.Diagram('01-global-architecture', W, H,
              title='BlueWhale Context 基座 — 全局架构图（Client / Server）',
              subtitle='Server 为默认部署；Server 内部沿用 Langur DDD（domain→application→interfaces，详见 图 02）；Client = Agent runtime + 管理端（独立应用，知识工程与管理端）')

# ============ Band A · Client 端 ============
d.container(40, 84, 1200, 112, 'Client 端（消费 + 管理）—— 经契约层调用，不直接碰存储与索引',
            tint='blue')
d.box(60, 116, 300, 56, 'Langur Agent Harness · 上下文装配组件',
      sub='SDK/DI · ContextAssembler', tint='blue')
d.box(380, 116, 260, 56, 'Skylark 语音 Agent',
      sub='MCP client', tint='blue')
d.box(660, 116, 220, 56, '第三方 IDE Agent',
      sub='REST · MCP', tint='blue')
d.box(900, 116, 320, 56, '管理端 · 独立 client 应用（知识工程与管理端）',
      sub='知识治理 · 记忆管理 · 生命周期看板', tint='red')

# ============ Band B · 契约层 ============
d.container(40, 224, 1200, 64, '契约层（client ↔ server 唯一边界，禁止绕过）', tint='slate')
d.pill(150, 268, 150, 26, 'ingest(source)', tint='white', title_size=11)
d.pill(470, 268, 140, 26, 'capture(episode)', tint='white', title_size=11)
d.pill(650, 268, 170, 26, 'reportSignal / feedback', tint='white', title_size=11)
d.pill(890, 268, 150, 26, 'resolve(query, scope栈, budget)', tint='white', title_size=11)
d.pill(1090, 268, 200, 26, 'govern / decide / erasure', tint='white', title_size=11)

# ============ Band C · Server 端 ============
d.container(40, 304, 1200, 304, 'Server 端 · BlueWhale 基座 · 机制层（内部 DDD：domain 端口 → application 用例 → interfaces 协议）',
            tint='green')

# --- Row C1: KE / ME / Serve ---
d.container(60, 326, 360, 88, '知识工程处理流程', tint='orange', dash='4,3', label_size=12)
ke_r1 = ['接入', '解析', '分段', '抽取patch']
ke_r2 = ['规范化', '调和', '变更单闸门', 'Distill萃取']
for i, t in enumerate(ke_r1):
    d.pill(108 + i * 88, 370, 80, 22, t, tint='orange', title_size=10.5)
for i, t in enumerate(ke_r2):
    d.pill(108 + i * 88, 398, 80, 22, t, tint='orange', title_size=10.5)

d.container(440, 326, 360, 88, '记忆工程处理流程', tint='purple', dash='4,3', label_size=12)
me_r1 = ['捕获', '抽取', '挂链', '巩固']
me_r2 = ['衰减', '复盘', '晋升桥 ⇄知识工程', '反馈调权']
for i, t in enumerate(me_r1):
    d.pill(488 + i * 88, 370, 80, 22, t, tint='purple', title_size=10.5)
for i, t in enumerate(me_r2):
    d.pill(488 + i * 88, 398, 80, 22, t, tint='purple', title_size=10.5)

d.container(880, 326, 340, 88, '上下文供给（读路径默认 0 次 LLM）', tint='teal', dash='4,3', label_size=12)
sv_r1 = ['门控', '多路', 'RRF', '重排']
sv_r2 = ['MMR', '预算', 'EMPTY', 'trace']
for i, t in enumerate(sv_r1):
    d.pill(922 + i * 84, 370, 76, 22, t, tint='teal', title_size=10.5)
for i, t in enumerate(sv_r2):
    d.pill(922 + i * 84, 398, 76, 22, t, tint='teal', title_size=10.5)

# --- Row C2: 统一存储 ---
d.box(60, 442, 1160, 56,
      '统一上下文存储 ContextUnit —— 七 facet × 三条表征线 × 三索引',
      sub='facet：kind · origin · scope · lifecycle · resolution · economy · lineage　｜　表征线：事件时间线 · 实体-关系图 · 精炼文档层　｜　索引：向量(HNSW) + 词法(BM25) + 图遍历',
      tint='green', title_size=13.5, sub_size=10.5)

# --- Row C3: 生命周期 / 治理 API 面 / 使用闭环 ---
d.box(60, 528, 640, 56, '生命周期与治理（贯穿读写路径）',
      sub='12 态状态机 · 四时间轴 · 血缘 impact() · Owner & ReviewJob · 发布批次/灰度/回滚 · ErasureJob（受控擦除）',
      tint='red', title_size=13, sub_size=10.5)
d.box(720, 528, 300, 56, '使用闭环', sub='信号 · 归因 · 变更单草稿 · 回归闸门', tint='purple', title_size=12.5, sub_size=10.5)
d.box(1040, 528, 200, 56, '治理 API 面', sub='REST/MCP 治理接口（UI 在 client 管理端）', tint='red', title_size=12, sub_size=10)

# ============ Band D 索引与持久化 ============
d.container(40, 636, 1200, 112, '索引与持久化层（store 不可用 → fail-fast，禁止静默降级为内存）', tint='gray')
d.box(60, 664, 240, 64, 'skylark-index（共享 SPI）',
      sub='9 端口类型 · 4 实现 · RRF 融合 · namespace 隔离', tint='gray', title_size=12.5, sub_size=10.5)
d.cylinder(410, 664, 180, 64, 'PostgreSQL', sub='pgvector · Flyway', tint='blue')
d.cylinder(600, 664, 150, 64, '对象存储', sub='原文 / md', tint='blue')
d.box(700, 664, 160, 64, 'Redis', sub='抽取队列 · 分布式锁 · 去抖', tint='gray', title_size=12.5, sub_size=10.5)
d.cylinder(950, 664, 150, 64, 'ES / Milvus', sub='词法 · 向量', tint='blue')
d.box(1050, 664, 170, 64, 'Git 仓库', sub='md 单一事实源 · PR 审核闸门', tint='gray', title_size=12.5, sub_size=10.5)

# ============ Band E 模型依赖 ============
d.container(40, 764, 1200, 84, '外部依赖 · 模型矩阵（写路径异步调用，经 Langur LlmGateway 统一出口）', tint='yellow')
pills = ['轻量模型 · 门控', '抽取/嵌入模型', '重排/调和模型', '推理模型 · 抽取/蒸馏',
         '长上下文模型', 'LlmGateway 出口', 'SecretResolver', 'SsrfGuard']
for i, t in enumerate(pills):
    d.pill(128 + i * 146, 814, 136, 26, t, tint='yellow', title_size=10.5)

# ============ 连线 ============
# A → B
d.arrow([(210, 172), (210, 224)], 'main', 2.0)
d.arrow_label(220, 196, 'SDK / DI', 10.5, S.C_MAIN, anchor='start')
d.arrow([(510, 172), (510, 224)], 'main', 2.0)
d.arrow_label(520, 196, 'MCP context_search', 10.5, S.C_MAIN, anchor='start')
d.arrow([(770, 172), (770, 224)], 'main', 2.0)
d.arrow_label(780, 196, 'REST / SDK', 10.5, S.C_MAIN, anchor='start')
d.arrow([(1060, 172), (1060, 224)], 'main', 2.0)
d.arrow_label(1070, 196, '管理端 API', 10.5, S.C_MAIN, anchor='start')

# B → C
d.arrow([(150, 288), (150, 326)], 'data', 1.8, dash='5,3')
d.arrow_label(160, 306, 'ingest', 10.5, S.C_DATA, anchor='start')
d.arrow([(470, 288), (470, 326)], 'data', 1.8, dash='5,3')
d.arrow_label(480, 306, 'capture', 10.5, S.C_DATA, anchor='start')
d.arrow([(890, 288), (890, 326)], 'main', 1.8)
d.arrow_label(900, 306, 'resolve', 10.5, S.C_MAIN, anchor='start')
d.arrow([(1050, 326), (1050, 288)], 'data', 2.2)
d.arrow_label(1060, 306, 'ContextBundle ↑', 10.5, S.C_DATA, anchor='start')
# govern：经右缘走线到治理 API 面
d.arrow([(1090, 281), (1090, 288), (1256, 288), (1256, 556), (1240, 556)], 'teal', 1.8)
d.arrow_label(1256, 420, '审核/发布/擦除', 10, S.C_TEAL, anchor='end', bg=False)

# Row C1 → Row C2 (写入 / 读取)
d.arrow([(240, 414), (240, 442)], 'data', 1.8, dash='5,3')
d.arrow_label(250, 432, 'upsert unit', 10, S.C_DATA, anchor='start')
d.arrow([(620, 414), (620, 442)], 'data', 1.8, dash='5,3')
d.arrow_label(630, 432, 'upsert unit', 10, S.C_DATA, anchor='start')
d.arrow([(1050, 442), (1050, 414)], 'gray', 1.8)
d.arrow_label(1060, 432, 'hybrid search', 10, S.C_GRAY, anchor='start')

# Row C2 ↔ Row C3
d.arrow([(380, 528), (380, 498)], 'teal', 1.8)
d.arrow_label(390, 517, 'lifecycle_event', 10, S.C_TEAL, anchor='start')
d.arrow([(870, 498), (870, 528)], 'async', 1.8, dash='4,3')
d.arrow_label(862, 517, 'usage_event', 10, S.C_ASYNC, anchor='end')
d.arrow([(870, 584), (870, 596), (380, 596), (380, 584)], 'async', 1.8, curve=True)
d.arrow_label(880, 612, '调权 / 降权 / reconcile', 10, S.C_ASYNC, anchor='start')

# C → D
d.arrow([(640, 584), (640, 636)], 'data', 2.6)
d.arrow_label(655, 615, 'IndexPort · RepositoryPort · ObjectStorePort · ClockPort', 10.5, S.C_DATA, anchor='start')

# C → E （左缘走线，避免穿越 D 层）
d.arrow([(60, 368), (24, 368), (24, 806), (40, 806)], 'async', 1.8, dash='6,4', curve=True)
d.texts.append('<text transform="rotate(-90 18 590)" x="18" y="590" text-anchor="middle" '
               'font-size="11" font-weight="500" fill="%s">LLM 调用：门控 / 抽取 / 蒸馏 / 重排（仅写路径，读路径 0 次）</text>' % S.C_ASYNC)

# ============ 图例 + 决策注记 ============
d.add_legend(60, 864, [
    ('main', '读请求 / 供给（同步，p95 < 300ms）'),
    ('data', '写入 / 召回（数据面）'),
    ('teal', '治理 / 审核 / 状态迁移'),
    ('async', '异步 / 反馈 / LLM 调用', '6,4'),
    ('gray', '检索后端委托'),
    ('red', '冲突 / 擦除（不可逆）'),
], title='图例 · 箭头语义', col_w=380)

d.notes(880, 888, [
    '边界纪律：BlueWhale 不拼 prompt，Langur 不做加工与生命周期',
    'Server 为默认部署（REST + MCP + 管理端 API）；embedded 降级为单机测试/小规模',
    '管理端 = 独立 client 应用（知识工程与管理端）：知识治理 · 记忆管理 · 生命周期看板',
    '严禁双写：VectorMemoryService / Knowledge*Tool 过渡为 BlueWhale 客户端',
    '闭环设计：写入统一经 ChangeRequest 闸门，使用信号回流为变更单草稿',
], size=11, lh=19, color=S.INK2)

print(d.save())
