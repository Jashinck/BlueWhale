# -*- coding: utf-8 -*-
"""图 11 PAD：调和 / 冲突消解算法。嵌套缩进 + 左侧脊线（PAD 记法）。

核心：冲突 ≠ 覆盖；优先级裁决；不可裁决→CONFLICTED 成对；全程 bi-temporal 软失效。
"""
import bw_style as S

W, H = 1320, 860
d = S.Diagram('11-pad-reconcile', W, H,
              title='PAD 图 · 调和 / 冲突消解算法',
              subtitle='嵌套层级 = 控制结构（SEQ/SEL/LOOP）；冲突 ≠ 覆盖，败者软失效留 supersededBy')

L0, L1, L2 = 60, 140, 240


def pb(x, y, w, h, title, sub=None, tint='blue', ts=11.5, ss=9.5, lines=None):
    d.box(x, y, w, h, title, sub=sub, tint=tint, title_size=ts, sub_size=ss, lines=lines)


def spine(x, y0, y1, ticks):
    d.arrow([(x, y0), (x, y1)], 'ink', 1.2, marker=False)
    for ty in ticks:
        d.arrow([(x, ty), (x + 20, ty)], 'ink', 1.2, marker=False)


# ---- 左脊 L0 ----
spine(48, 118, 748, [118, 178, 628, 688, 748])
pb(L0, 96, 520, 44, '输入', '新候选 C + 同指称现有集 E（按 entity/QID 聚类）', 'blue')
pb(L0, 156, 520, 44, 'LOOP  对 E 中每个 e', 'SEQ 内层逐条调和', 'slate')
# ---- L1 ----
spine(128, 238, 606, [238, 458])
pb(L1, 216, 520, 44, 'SEL  rel = relate(C, e)', 'ENTAILS / CONTRADICTS / OVERLAPS / UNRELATED', 'yellow')
# ---- L2（rel 分支）----
spine(228, 296, 396, [296, 346, 396])
pb(L2, 276, 460, 40, 'CONTRADICTS', '→ 冲突消解（见下 SEL）', 'red', ts=11, ss=9)
pb(L2, 326, 460, 40, 'ENTAILS / OVERLAPS', '→ 挂边合并，不覆盖旧值', 'green', ts=11, ss=9)
pb(L2, 376, 460, 40, 'UNRELATED', '→ C 直接新增为独立 unit', 'gray', ts=11, ss=9)
# ---- L1 冲突消解 ----
pb(L1, 436, 520, 44, '冲突消解 · 优先级裁决', 'scope>origin>时间>confidence（见右）', 'yellow')
spine(228, 516, 566, [516, 566])
pb(L2, 496, 460, 40, '可裁决', '胜 ACTIVE / 败 SUPERSEDED(soft) + 写 edge', 'green', ts=11, ss=9)
pb(L2, 546, 460, 40, '不可裁决', '双方 CONFLICTED + 进人审 + 成对召回', 'orange', ts=11, ss=9)
# ---- L0 收尾 ----
pb(L0, 606, 520, 44, '写 bi-temporal', 'validFrom / validTo / supersededBy / bw_unit_edge', 'blue')
pb(L0, 666, 520, 44, '写 bw_lifecycle_event', 'append-only，无事件的状态变化 = bug', 'blue')
pb(L0, 726, 520, 44, '输出', '调和后 unit 集 + 边 + 事件（供 发布）', 'green')

# ---- 右栏：裁决规则与不变式 ----
pb(760, 96, 520, 132, '优先级裁决顺序（高→低）', lines=[
    '先比较 scope：更具体者胜（子 scope > 父 scope）',
    '再比较 origin：更可信者胜（OBSERVED > DECLARED > DISTILLED）',
    '再比较采集时间：更新者胜（source_published_at）',
    '最后比较 confidence：更高者胜',
    '同级仍平 → 判「不可裁决」，转 CONFLICTED',
], tint='teal', ts=12, ss=10)
pb(760, 248, 520, 108, '冲突 ≠ 覆盖（三纪律）', lines=[
    '不物理覆盖旧值；败者软失效留 supersededBy',
    'CONFLICTED 召回必须成对返回，不单边裁决',
    '时间旅行可查：validTo 之前的版本仍可见',
], tint='red', ts=12, ss=10)
pb(760, 376, 520, 84, 'bi-temporal 双时间轴', lines=[
    '事件时间（源何时生效）vs 系统时间（何时入库/失效）',
    '纠正历史不改写，只追加新版本',
], tint='blue', ts=12, ss=10)
d.notes(760, 492, [
    '不变式：默认软失效（留墓碑）；状态转移必写 bw_lifecycle_event；',
    '仅 →SUPERSEDED / →ERASED 不可逆，其余可回退以支持误判恢复。',
    '调和是增量优先：只重算受影响的同指称簇，不做全量重扫。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
