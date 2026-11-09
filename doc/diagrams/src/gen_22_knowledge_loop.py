# -*- coding: utf-8 -*-
"""图 22 知识端到端闭环：提取 → 更新(唯一写入口) → 知识库 → 使用 → 萃取 → 回到更新/提取。"""
import bw_style as S

W, H = 1370, 860
d = S.Diagram('22-knowledge-loop', W, H,
              title='知识端到端闭环 · 提取 → 使用 → 萃取 → 更新',
              subtitle='所有写入只经「变更闸门」一个入口；每次使用都留下可归因的信号；信号经归因与萃取回到闸门')

Y1, H1 = 100, 230   # 上排卡片
Y2, H2 = 480, 190   # 下排卡片

# ---- 上排：提取 → 更新闸门 → 知识库 → 使用 ----
d.box(40, Y1, 280, H1, '提取 Acquire', tint='blue', title_size=14, sub_size=10.5, align='start',
      lines=['接入、解析与分段 接入 · 解析 · 条款级分段',
             '抽取 Claim / 规则（LLM）',
             '本体校验 · 归并到已有页',
             '产物：Wiki Patch + evidenceSpan',
             '不直接写库，只提交补丁'])

d.box(410, Y1, 280, H1, '更新 Update · 变更检查', tint='teal', title_size=14, sub_size=10.5, align='start',
      lines=['ChangeRequest：唯一写入口',
             '本体前置校验 → 人审 diff',
             '回归门：评测集重放',
             '发布批次 ATOMIC / CANARY / 回滚',
             '血缘传播：依赖项降权并复核'])

d.box(780, Y1, 220, H1, '知识库', tint='green', title_size=14, sub_size=10.5, align='start',
      lines=['Wiki（md）= 真源',
             '确定性编译 → ContextUnit',
             '向量 · 词法 · 图 三索引',
             'Release 不可变快照',
             '12 态生命周期'])

d.box(1090, Y1, 240, H1, '使用 Use', tint='orange', title_size=14, sub_size=10.5, align='start',
      lines=['resolve()：权限前置',
             '按 releaseId 钉版本',
             'ContextBundle + citation',
             '消费方推理 / 成文',
             '记录 trace，上报 Signal'])

d.arrow([(320, 215), (410, 215)], 'main', 2.2)
d.arrow_label(365, 205, 'Patch', 10.5, S.C_MAIN, bg=False)
d.arrow([(690, 215), (780, 215)], 'ctrl', 2.2)
d.arrow_label(735, 205, '发布', 10.5, S.C_CTRL, bg=False)
d.arrow([(1000, 215), (1090, 215)], 'data', 2.2)
d.arrow_label(1045, 205, '召回', 10.5, S.C_DATA, bg=False)

# ---- 下排：萃取 ----
d.box(410, Y2, 590, H2, '萃取 Distill', tint='purple', title_size=14, sub_size=10.5, align='start',
      lines=['信号：点踩/改写 · 采纳/覆盖/忽略 · 评测失败 · 源变更 · 到期复审',
             '归因（程序推导，不由人填）：NO_HIT · WRONG_PICK · STALE · CONFLICT',
             '　　　　　　　　　　　　　 BAD_CLAIM · MISSING_QUALIFIER · CONSUMER_ERR',
             '聚类：同因同对象聚合，达阈值才立单',
             '萃取：纠正模式 → 候选规则；缺口簇 → 提取任务；记忆工程 蒸馏 → 晋升',
             '产物：ChangeRequest 草稿（带证据）· 回归用例'])

# ②→③ 信号
d.arrow([(1210, 330), (1210, 575), (1000, 575)], 'async', 2.0)
d.arrow_label(1222, 450, 'Signal', 10.5, S.C_ASYNC, anchor='start', bg=False)
# ③→闸门 草稿
d.arrow([(550, Y2), (550, 330)], 'ctrl', 2.0)
d.arrow_label(562, 410, '变更单草稿', 10.5, S.C_CTRL, anchor='start', bg=False)
# ③→① 缺口
d.arrow([(410, 575), (180, 575), (180, 330)], 'async', 2.0, dash='5,3')
d.arrow_label(192, 450, '提取任务', 10.5, S.C_ASYNC, anchor='start', bg=False)

# ---- 源 ----
d.box(40, 700, 280, 60, '外部知识源', sub='制度 / 文档 / 表格 / 代码；变更产生 source.changed', tint='gray',
      title_size=12.5, sub_size=10)
d.arrow([(90, 700), (90, 330)], 'main', 2.0)
d.arrow_label(102, 640, '首次接入 / 变更事件', 10.5, S.C_MAIN, anchor='start', bg=False)

# ---- 不变量 ----
d.note(410, 712, '闭环成立的四条不变量', 12.5, weight='600', color=S.INK)
d.notes(410, 734, [
    '统一写入口：  所有知识写入必经 ChangeRequest；记忆工程 晋升、人工编辑、重抽战役同样如此。',
    '绑定使用版本：  每次使用绑定 releaseId + 内容 hash，信号才能归因到具体版本。',
    '缺陷进入回归集：  每个已确认缺陷沉淀为回归用例，永久进入回归门。',
    '系统草稿必须人审：  萃取只产出草稿；发布权在闸门（人审 + 回归 + 可回滚）。',
], size=11, lh=19, color=S.INK2)

d.add_legend(1010, 712, [
    ('main', '提取/接入'),
    ('ctrl', '写入/发布/草稿'),
    ('data', '召回（读）'),
    ('async', '信号/任务（异步）', '5,3'),
], title='图例', col_w=250)

print(d.save())
