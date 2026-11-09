# -*- coding: utf-8 -*-
"""图 21 管理端内部架构：client 前端三模块 + 权限门面 + server 管理端 API 面。"""
import bw_style as S

W, H = 1400, 900
d = S.Diagram('21-admin-internal', W, H,
              title='管理端 · 内部架构（Client 应用）',
              subtitle='前端三功能域（知识治理/记忆管理/看板）+ 权限门面 + API client ↔ server 管理端 API 面')

# ---- Client 端 ----
d.container(40, 84, 760, 780, 'Client 端 · 独立前端应用（知识工程与管理端）', tint='blue')
d.box(60, 120, 720, 56, '知识治理 UI', sub='审核队列 · 发布管理 · 降权/废弃 · 本体管理 · 血缘视图', tint='blue', title_size=12.5, sub_size=10.5)
d.box(60, 188, 720, 56, '记忆管理 UI', sub='我的记忆（查看/编辑/撤销）· 纠正回炉 · 晋升候选', tint='blue', title_size=12.5, sub_size=10.5)
d.box(60, 256, 720, 56, '生命周期看板 UI', sub='12 态分布 · 冲突积压 · 无主资产 · 复审逾期 · erasure 台', tint='blue', title_size=12.5, sub_size=10.5)
d.box(60, 324, 720, 44, '权限门面', sub='用户仅见自己记忆；治理者按 scope 授权（复用 AccessPolicy）', tint='teal', title_size=12.5, sub_size=10)
d.box(60, 380, 720, 44, 'API Client', sub='REST / MCP（管理端 API）；薄客户端，无业务规则', tint='gray', title_size=12.5, sub_size=10)

d.arrow([(420, 176), (420, 188)], 'main', 1.6)
d.arrow([(420, 244), (420, 256)], 'main', 1.6)
d.arrow([(420, 312), (420, 324)], 'main', 1.6)
d.arrow([(420, 368), (420, 380)], 'main', 1.6)

# ---- Server 端 ----
d.container(840, 84, 520, 780, 'Server 端 · 管理端 API 面', tint='green')
d.box(860, 120, 480, 56, 'bluewhale-interfaces', sub='REST + MCP + 管理端 API（DTO 转换 · 参数校验）', tint='green', title_size=12.5, sub_size=10.5)
d.box(860, 188, 480, 56, 'GovernReviewUseCase', sub='审核决策（通过/驳回/批注）→ 状态转移', tint='green', title_size=12.5, sub_size=10.5)
d.box(860, 256, 480, 56, '本体 / 发布 / 血缘用例', sub='本体 PROPOSED 裁决 · 批次发布 · impact() 查询', tint='green', title_size=12.5, sub_size=10.5)

d.note(860, 344, '留痕存储（append-only）', 12.5, weight='600', color=S.INK)
d.cylinder(940, 392, 150, 50, 'bw_review', tint='blue', title_size=11)
d.cylinder(1120, 392, 150, 50, 'bw_lifecycle_event', tint='blue', title_size=10.5)
d.cylinder(940, 472, 150, 50, 'bw_asset', tint='blue', title_size=11)

d.arrow([(860, 176), (860, 188)], 'main', 1.6)
d.arrow([(860, 244), (860, 256)], 'main', 1.6)
d.arrow([(1015, 312), (1015, 392)], 'gray', 1.6)
d.arrow_label(1023, 350, '留痕', 9.5, S.C_GRAY, anchor='start', bg=False)

# ---- client ↔ server ----
d.arrow([(780, 402), (840, 402)], 'main', 2.0)
d.arrow_label(810, 392, '管理端 API', 10.5, S.C_MAIN)

# ---- 图例 ----
d.add_legend(60, 620, [
    ('main', '模块流转 / 网络调用'),
    ('gray', '留痕写入'),
], title='图例', col_w=240)

d.notes(420, 640, [
    '管理端是薄客户端：所有业务规则留在 server 的 application/domain 层；',
    '所有管理动作 append-only 写 bw_review / bw_lifecycle_event，可审计；',
    '权限前置：不该见的 unit 不出现在任何视图。',
], size=10.5, lh=17, color=S.INK2)

print(d.save())
