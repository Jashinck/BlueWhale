# -*- coding: utf-8 -*-
"""图 04 记忆工程模块架构图：六段管线 + 晋升桥 + 反馈驱动。"""
import bw_style as S

W, H = 1280, 780
d = S.Diagram('04-module-me', W, H,
              title='记忆工程模块架构图 —— 六段式管线（写入是决策，不是 append）',
              subtitle='记忆抽取的 ADD/UPDATE/DELETE/NOOP 是核心；用模型蒸馏替换现状 substring 拼接实现；程序性记忆只降权不删')

XS = [50 + i * 202 for i in range(6)]
stages = [
    ('捕获', ['对话轮 · 工具调用 · 成败', '用户纠正 · token/美元成本', '产物：CaptureSignal']),
    ('抽取', ['候选事实 + 写入决策', 'ADD / UPDATE / DELETE / NOOP', '与既有记忆比对（Mem0 式）']),
    ('挂链', ['episode 原文溯源', '实体链接：挂在实体上', '而非漂浮的孤立事实']),
    ('巩固', ['去重合并 · 相似聚类', '情景→语义 模型蒸馏（推理模型）', '多条→技能 SkillCandidate']),
    ('自适应', ['衰减 exp(-λ·Δt) · 强化', '采纳/纠正信号驱动权重', '程序性记忆不参与遗忘']),
    ('复盘', ['任务后轨迹反思', '产出可复用程序性记忆', '从"记住"到"学会"']),
]
for i, (t, subs) in enumerate(stages):
    d.box(XS[i], 110, 180, 110, t, lines=subs, tint='purple', title_size=13, sub_size=9.5)
    if i < 5:
        d.arrow([(XS[i] + 180, 165), (XS[i + 1], 165)], 'main', 2.0)

arts = ['CaptureSignal', '候选+决策', 'linked fact', 'DISTILLED', '权重更新']
for i, a in enumerate(arts):
    gx = XS[i] + 191
    d.pill(gx, 245, 110, 22, a, tint='white', title_size=10)
    d.arrow([(gx, 175), (gx, 234)], 'gray', 1.2, dash='3,3')

# 横切：生命周期 + 晋升桥
d.container(50, 290, 1190, 70, '横切：生命周期引擎 + 知识工程⇄记忆工程 晋升桥（统一存储后支持的能力）', tint='purple')
gov = ['状态迁移 lifecycle_event', '晋升 Promotion：跨≥N agent 采纳>阈值 → 审核 → scope=project',
       '降权 Demotion：频繁纠正/命中冲突 → STALE_SUSPECT', 'Bootstrap：知识本体为记忆提供挂载点']
gw = [220, 380, 290, 230]
gx = 62
for t, w in zip(gov, gw):
    d.pill(gx + w / 2.0, 338, w, 24, t, tint='purple', title_size=10)
    gx += w + 12

# 存储 / 源 / 端口
d.box(50, 400, 380, 70, '捕获源（事件只增不改，写满不读回）',
      lines=['Langur trace · RSI MemoryDistiller', '工具调用结果 · 用户纠正 · 成本账'],
      tint='gray', title_size=12, sub_size=10)
d.box(450, 400, 380, 70, '存储表（PostgreSQL）',
      lines=['bw_episode · bw_context_unit · bw_usage_event', 'bw_lineage · bw_lifecycle_event'],
      tint='blue', title_size=12, sub_size=10)
d.box(850, 400, 390, 70, '端口与策略（domain 定义）',
      lines=['CapturePort · ExtractionPort（推理模型） · ConsolidationPort', 'DecayPolicy · RetentionPolicy · PromotionRule'],
      tint='purple', title_size=12, sub_size=10)

# 反馈驱动
d.arrow([(560, 400), (560, 360)], 'async', 1.6, dash='5,3')
d.arrow_label(570, 384, 'usage_event', 10, S.C_ASYNC, anchor='start')
d.arrow([(948, 290), (948, 262)], 'async', 1.8, dash='5,3')
d.arrow_label(958, 280, '驱动衰减/强化', 10, S.C_ASYNC, anchor='start')

# 晋升桥落地
d.box(50, 510, 700, 84, '晋升桥闸门（私有经验 → 共享知识）',
      lines=['触发：DISTILLED 单元跨 ≥N 个 agent/user 被召回，采纳率 > 阈值，纠正率 < 阈值',
             '动作：生成 origin=DISTILLED, scope=project/workspace 候选知识单元',
             '治理：必须过审核闸门（人审或双模型互检），带 authority 标记发布'],
      tint='yellow', title_size=12.5, sub_size=10)
d.box(790, 510, 450, 84, '知识工程 侧接收（同一 ContextUnit 存储）',
      lines=['调和：与既有知识做冲突检测', '发布：走知识审核档，不绕过审核', '降权反向：知识被纠正 → 回写 记忆工程 权重'],
      tint='orange', title_size=12.5, sub_size=10)
d.arrow([(750, 552), (790, 552)], 'main', 2.2)
d.arrow_label(770, 542, '候选', 10, S.C_MAIN)
d.arrow([(100, 360), (100, 380), (30, 380), (30, 552), (50, 552)], 'teal', 1.6, dash='4,3', curve=True)
d.texts.append('<text transform="rotate(-90 22 470)" x="22" y="470" text-anchor="middle" '
               'font-size="10.5" fill="%s">晋升判定</text>' % S.C_TEAL)
d.arrow([(900, 510), (900, 492), (820, 492), (820, 470)], 'red', 1.6, dash='4,3', curve=True)
d.arrow_label(860, 486, '纠正回写', 10, S.C_RED, anchor='start')

d.add_legend(50, 630, [
    ('main', '管线主数据流 / 晋升候选'),
    ('async', '使用反馈驱动（异步）', '5,3'),
    ('teal', '晋升判定（治理）', '4,3'),
    ('red', '纠正 / 降权回写', '4,3'),
    ('gray', '产物落点', '3,3'),
], title='图例', col_w=260)

d.notes(700, 656, [
    '现状修复：MemoryEvolutionService:81 的 substring(0,30) 字符串拼接 → 模型蒸馏',
    '现状修复：importance 不再靠 accessCount>3 硬编码 → economy 采纳/纠正信号',
    '保留正确决策：PROCEDURAL 不参与遗忘（同文件:37），技能只降权不删',
], size=11, lh=20, color=S.INK2)

print(d.save())
