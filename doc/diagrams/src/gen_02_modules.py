# -*- coding: utf-8 -*-
"""图 02 模块架构图：Client 端 + Server 端 Maven 模块 + core 包内分层 + 工程纪律。"""
import bw_style as S

W, H = 1280, 950
d = S.Diagram('02-module-architecture', W, H,
              title='BlueWhale — 模块架构图（Client 端 + Server 端 · 依赖方向 · 包内分层）',
              subtitle='Server 为默认部署，内部沿用 Langur DDD；client 端 = Agent runtime + 管理端（独立应用，知识工程与管理端），非 BlueWhale Maven 模块')

# ================= Client 端 =================
d.container(40, 84, 1200, 72, 'Client 端（独立，非 BlueWhale Maven 模块）', tint='blue')
d.box(60, 104, 320, 44, 'Langur Agent runtime', sub='SDK/DI · 消费 ContextBundle · recordFeedback', tint='blue', title_size=12, sub_size=10)
d.box(400, 104, 320, 44, '管理端 · 独立 client 应用（知识工程与管理端）', sub='知识治理 · 记忆管理 · 生命周期看板', tint='red', title_size=12, sub_size=10)
d.box(740, 104, 240, 44, '第三方 / 跨语言消费者', sub='REST · MCP', tint='blue', title_size=12, sub_size=10)
d.box(1000, 104, 220, 44, 'Embedded 库模式', sub='单机测试 / 小规模（同进程 DI）', tint='gray', title_size=12, sub_size=10)

# ================= 左：Server 端 Maven 模块 =================
d.note(60, 168, 'Server 端 Maven 模块与编译依赖方向（箭头 = depends on）', 13, weight='600', color=S.INK)

d.box(60, 180, 1160, 40, 'bluewhale-starter —— Server 默认装配入口（REST + MCP + 管理端 API）；embedded 仅单机测试/小规模',
      tint='yellow', title_size=11.5)

d.box(60, 248, 320, 60, 'bluewhale-interfaces', sub='REST · MCP · 管理端 API · DTO', tint='blue', title_size=13)
d.box(420, 248, 300, 60, 'bluewhale-client', sub='远程薄 SDK（Agent runtime 与 管理端共用）', tint='gray', title_size=13)

d.box(60, 336, 660, 60, 'bluewhale-app', sub='Ingest/Capture/Resolve/Feedback/Maintenance/Govern + ChangeRequest 用例 · distill · 周期 job',
      tint='green', title_size=13, sub_size=10)

d.box(60, 424, 660, 72, 'bluewhale-core —— domain（零框架依赖）',
      lines=['ContextUnit 聚合根 · UnitEdge 双时态图 · Lifecycle · Ownership · Lineage · ChangeRequest',
             '端口全部在此：IndexPort / RetrievalPort / ExtractionPort / EmbeddingPort / ObjectStorePort / ClockPort / PolicyPort'],
      tint='purple', title_size=13.5, sub_size=10.5)

d.box(60, 528, 200, 60, 'bluewhale-index-adapter', sub='IndexPort → skylark-index', tint='orange', title_size=11.5, sub_size=10)
d.box(280, 528, 210, 60, 'bluewhale-store-postgres', sub='元数据 + pgvector + Flyway', tint='orange', title_size=11.5, sub_size=10)
d.box(510, 528, 210, 60, 'bluewhale-extract', sub='ExtractionPort → 抽取、重排和推理模型', tint='orange', title_size=11.5, sub_size=10)

d.box(60, 616, 200, 60, 'skylark-index（共享库）', sub='9 端口 · 4 实现 · RRF', tint='slate', title_size=11.5, sub_size=10)
d.cylinder(385, 616, 210, 60, 'PostgreSQL + pgvector', sub='HNSW · 事件/血缘/审核表', tint='blue', sub_size=10)
d.box(510, 616, 210, 60, 'Langur LlmGateway', sub='模型矩阵统一出口', tint='slate', title_size=11.5, sub_size=10)

# Client → starter
d.arrow([(220, 148), (220, 180)], 'main', 2.0)
d.arrow([(560, 148), (560, 180)], 'main', 2.0)
d.arrow([(860, 148), (860, 180)], 'main', 2.0)
d.arrow([(1110, 148), (1110, 180)], 'gray', 1.6, dash='4,3')
d.arrow_label(1130, 166, '同进程 DI', 10, S.C_GRAY, anchor='start', bg=False)

# 依赖箭头
d.arrow([(220, 220), (220, 248)], 'main', 2.0)
d.arrow([(570, 220), (570, 248)], 'main', 2.0)
d.arrow([(220, 308), (220, 336)], 'main', 2.0)
d.arrow([(220, 396), (220, 424)], 'main', 2.0)
d.arrow([(160, 528), (160, 496)], 'data', 1.8)
d.arrow_label(170, 516, '实现 IndexPort', 10, S.C_DATA, anchor='start')
d.arrow([(385, 528), (385, 496)], 'data', 1.8)
d.arrow_label(395, 516, '实现 RepositoryPort', 10, S.C_DATA, anchor='start')
d.arrow([(615, 528), (615, 496)], 'data', 1.8)
d.arrow_label(625, 516, '实现 ExtractionPort', 10, S.C_DATA, anchor='start')
d.arrow([(160, 588), (160, 616)], 'gray', 1.6)
d.arrow([(385, 588), (385, 616)], 'gray', 1.6)
d.arrow([(615, 588), (615, 616)], 'gray', 1.6)

d.notes(60, 700, [
    '依赖倒置：core 只定义端口，三个 adapter 实现端口；app 不 import 任何 infrastructure 包',
    '现状缺陷修复：MemoryDomainService 反向 import infrastructure.embedding/vectorstore → 端口下沉 domain',
], size=11, lh=18, color=S.INK2)

# ================= 右：包内分层 + 纪律 =================
d.container(800, 180, 440, 306, 'bluewhale-core 包内分层（ArchUnit 层守卫，沿用 Langur DDD）', tint='purple')
d.box(820, 214, 400, 44, 'interfaces 包 —— REST / MCP / 管理端 API / DTO 转换', tint='blue', title_size=12)
d.box(820, 274, 400, 44, 'application 包 —— 用例编排 · 事务边界 · 事件发布', tint='green', title_size=12)
d.box(820, 334, 400, 60, 'domain 包 —— 聚合根 · 状态机 · 领域服务 · 端口',
      sub='context / knowledge / memory / graph / lifecycle / governance / feedback / port',
      tint='purple', title_size=12, sub_size=10)
d.box(820, 414, 400, 44, 'infrastructure 包 —— 端口实现（store / index / extract / mcp / rest）', tint='orange', title_size=12)
d.arrow([(1020, 258), (1020, 274)], 'main', 1.8)
d.arrow([(1020, 318), (1020, 334)], 'main', 1.8)
d.arrow([(1020, 414), (1020, 394)], 'data', 1.8)
d.arrow_label(1030, 406, '实现端口', 10, S.C_DATA, anchor='start')

d.container(800, 504, 440, 236, '工程纪律（沿用 Langur 既有约束）', tint='gray')
d.notes(820, 540, [
    '· domain 零框架依赖：禁 import spring / infra / 第三方 SDK',
    '· application 只依赖 domain 端口，禁直接 new 适配器',
    '· interfaces 只做 DTO 转换与参数校验，禁含业务规则',
    '· SecretResolver 取密钥，密钥永不落日志；SsrfGuard 校验外部 URI',
    '· namespace 强制租户隔离；嵌入维度一致性启动 fail-fast',
    '· store 不可用 fail-fast，禁止静默降级为内存实现',
    '· 权限在召回前生效（不把不该见的摘要送进排序器）',
    '· 检索内容按不可信输入处理：trust tier + quarantine 两道闸',
    '· 写入统一经 ChangeRequest；写路径异步，读路径默认 0 次 LLM',
], size=11, lh=21, color=S.INK)

# ================= 图例 =================
d.add_legend(60, 770, [
    ('main', '编译依赖（depends on）'),
    ('data', '端口实现（依赖倒置）'),
    ('gray', '运行时 / 网络委托', '4,3'),
    ('async', '装配注入（starter）', '6,4'),
], title='图例', col_w=300)

print(d.save())
