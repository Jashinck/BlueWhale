# -*- coding: utf-8 -*-
"""BlueWhale 架构图共享绘图库（Style-1 Flat Icon）。

所有生成脚本 import 本模块，保证 12 张图的字体、配色、线宽、图例风格一致。
"""
import os
import unicodedata

OUT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))

FONT = ("'Helvetica Neue', Helvetica, Arial, 'PingFang SC', 'Hiragino Sans GB', "
        "'Microsoft YaHei', 'SimHei', sans-serif")

# ---- 语义色 ----
C_MAIN = '#2563eb'      # 主数据流 / 请求
C_DATA = '#16a34a'      # 数据写入 / 记忆读
C_CTRL = '#ea580c'      # 控制 / 触发
C_ASYNC = '#9333ea'     # 异步 / 反馈 / 蒸馏
C_GRAY = '#6b7280'      # 非阻塞 / 事件
C_RED = '#dc2626'       # 冲突 / 拒绝 / 擦除
C_TEAL = '#0d9488'      # 治理 / 审核

INK = '#111827'
INK2 = '#6b7280'
LINE = '#d1d5db'
BG = '#ffffff'

# ---- 淡色底 ----
TINT = {
    'blue':   ('#eff6ff', '#bfdbfe'),
    'green':  ('#f0fdf4', '#bbf7d0'),
    'orange': ('#fff7ed', '#fed7aa'),
    'purple': ('#faf5ff', '#ddd6fe'),
    'teal':   ('#f0fdfa', '#99f6e4'),
    'red':    ('#fef2f2', '#fecaca'),
    'gray':   ('#f9fafb', '#e5e7eb'),
    'yellow': ('#fefce8', '#fde68a'),
    'white':  ('#ffffff', '#d1d5db'),
    'slate':  ('#f1f5f9', '#cbd5e1'),
}

ARROW_COLORS = {
    'main': C_MAIN, 'data': C_DATA, 'ctrl': C_CTRL, 'async': C_ASYNC,
    'gray': C_GRAY, 'red': C_RED, 'teal': C_TEAL, 'ink': INK,
}


def esc(s):
    return (str(s).replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;'))


def measure(text, size=13):
    """估算文本渲染宽度（CJK 全宽 = size，拉丁 ≈ 0.56*size）。"""
    w = 0.0
    for ch in str(text):
        if unicodedata.east_asian_width(ch) in ('W', 'F'):
            w += size * 1.02
        elif ch == ' ':
            w += size * 0.30
        else:
            w += size * 0.56
    return w


class Diagram(object):
    """按 Z 序分层收集 SVG 元素：背景 → 容器 → 连线 → 节点 → 文字 → 图例。"""

    def __init__(self, name, w=960, h=600, title=None, subtitle=None):
        self.name = name
        self.w = w
        self.h = h
        self.title = title
        self.subtitle = subtitle
        self.bg = []
        self.containers = []
        self.edges = []
        self.edge_labels = []
        self.nodes = []
        self.texts = []
        self.legend = []

    # ---------- 基础 ----------
    def container(self, x, y, w, h, label=None, tint='gray', dash='6,4',
                  label_size=13, rx=12, lw=1.5):
        fill, stroke = TINT.get(tint, TINT['gray'])
        self.containers.append(
            '<rect x="%s" y="%s" width="%s" height="%s" rx="%s" ry="%s" fill="%s" '
            'stroke="%s" stroke-width="%s" stroke-dasharray="%s" opacity="0.55"/>'
            % (x, y, w, h, rx, rx, fill, stroke, lw, dash))
        if label:
            self.containers.append(
                '<text x="%s" y="%s" font-size="%s" font-weight="600" fill="%s" '
                'letter-spacing="0.4">%s</text>'
                % (x + 14, y + 22, label_size, INK2, esc(label)))
        return self

    def box(self, x, y, w, h, title, sub=None, tint='white', rx=8, lw=1.5,
            title_size=13, sub_size=11, bold=True, stroke=None, fill=None,
            align='middle', lines=None):
        f, s = TINT.get(tint, TINT['white'])
        self.nodes.append(
            '<rect x="%s" y="%s" width="%s" height="%s" rx="%s" ry="%s" fill="%s" '
            'stroke="%s" stroke-width="%s"/>'
            % (x, y, w, h, rx, rx, fill or f, stroke or s, lw))
        anchor = 'middle' if align == 'middle' else 'start'
        tx = x + w / 2.0 if align == 'middle' else x + 12
        n_sub = len(lines) if lines else (1 if sub else 0)
        if n_sub == 0:
            ty = y + h / 2.0 + title_size * 0.36
            self.texts.append('<text x="%s" y="%.1f" text-anchor="%s" font-size="%s" '
                              'font-weight="%s" fill="%s">%s</text>'
                              % (tx, ty, anchor, title_size,
                                 '600' if bold else '400', INK, esc(title)))
            return self
        block = title_size * 1.25 + n_sub * (sub_size * 1.5)
        ty = y + (h - block) / 2.0 + title_size * 0.95
        self.texts.append('<text x="%s" y="%.1f" text-anchor="%s" font-size="%s" '
                          'font-weight="%s" fill="%s">%s</text>'
                          % (tx, ty, anchor, title_size, '600' if bold else '400',
                             INK, esc(title)))
        subs = lines if lines else [sub]
        for i, sl in enumerate(subs):
            self.texts.append('<text x="%s" y="%.1f" text-anchor="%s" font-size="%s" '
                              'fill="%s">%s</text>'
                              % (tx, ty + (i + 1) * sub_size * 1.5, anchor,
                                 sub_size, INK2, esc(sl)))
        return self

    def cylinder(self, cx, top_y, w, h, title, sub=None, tint='blue',
                 title_size=12.5, sub_size=10.5):
        fill, stroke = TINT.get(tint, TINT['blue'])
        x = cx - w / 2.0
        ry = min(12, w * 0.12)
        body = ('<path d="M %s %s L %s %s A %s %s 0 0 0 %s %s L %s %s A %s %s 0 0 0 %s %s Z" '
                'fill="%s" stroke="%s" stroke-width="1.5"/>'
                % (x, top_y + ry, x, top_y + h - ry, w / 2.0, ry, x + w, top_y + h - ry,
                   x + w, top_y + ry, w / 2.0, ry, x, top_y + ry, fill, stroke))
        cap = ('<ellipse cx="%s" cy="%s" rx="%s" ry="%s" fill="%s" stroke="%s" '
               'stroke-width="1.5"/>'
               % (cx, top_y + ry, w / 2.0, ry, '#ffffff', stroke))
        self.nodes.append(body)
        self.nodes.append(cap)
        avail_top = top_y + ry * 1.6
        block = title_size * 1.2 + (sub_size * 1.5 if sub else 0)
        ty = avail_top + (h - ry * 1.6 - block) / 2.0 + title_size * 0.95
        self.texts.append('<text x="%s" y="%.1f" text-anchor="middle" font-size="%s" '
                          'font-weight="600" fill="%s">%s</text>'
                          % (cx, ty, title_size, INK, esc(title)))
        if sub:
            self.texts.append('<text x="%s" y="%.1f" text-anchor="middle" font-size="%s" '
                              'fill="%s">%s</text>'
                              % (cx, ty + sub_size * 1.6, sub_size, INK2, esc(sub)))
        return self

    def diamond(self, cx, cy, w, h, title, sub=None, tint='yellow',
                title_size=12, sub_size=10):
        fill, stroke = TINT.get(tint, TINT['yellow'])
        pts = '%s,%s %s,%s %s,%s %s,%s' % (cx, cy - h / 2.0, cx + w / 2.0, cy,
                                          cx, cy + h / 2.0, cx - w / 2.0, cy)
        self.nodes.append('<polygon points="%s" fill="%s" stroke="%s" stroke-width="1.5"/>'
                          % (pts, fill, stroke))
        if sub:
            self.texts.append('<text x="%s" y="%.1f" text-anchor="middle" font-size="%s" '
                              'font-weight="600" fill="%s">%s</text>'
                              % (cx, cy - 1, title_size, INK, esc(title)))
            self.texts.append('<text x="%s" y="%.1f" text-anchor="middle" font-size="%s" '
                              'fill="%s">%s</text>'
                              % (cx, cy + sub_size * 1.5, sub_size, INK2, esc(sub)))
        else:
            self.texts.append('<text x="%s" y="%.1f" text-anchor="middle" font-size="%s" '
                              'font-weight="600" fill="%s">%s</text>'
                              % (cx, cy + title_size * 0.36, title_size, INK, esc(title)))
        return self

    def pill(self, cx, cy, w, h, title, tint='gray', title_size=11.5, fill=None,
             stroke=None, bold=False):
        f, s = TINT.get(tint, TINT['gray'])
        self.nodes.append('<rect x="%s" y="%s" width="%s" height="%s" rx="%s" ry="%s" '
                          'fill="%s" stroke="%s" stroke-width="1.2"/>'
                          % (cx - w / 2.0, cy - h / 2.0, w, h, h / 2.0, h / 2.0,
                             fill or f, stroke or s))
        self.texts.append('<text x="%s" y="%.1f" text-anchor="middle" font-size="%s" '
                          'font-weight="%s" fill="%s">%s</text>'
                          % (cx, cy + title_size * 0.36, title_size,
                             '600' if bold else '400', INK, esc(title)))
        return self

    def note(self, x, y, text, size=11, fill=None, anchor='start', weight='400',
             color=None):
        self.texts.append('<text x="%s" y="%s" text-anchor="%s" font-size="%s" '
                          'font-weight="%s" fill="%s">%s</text>'
                          % (x, y, anchor, size, weight, color or (fill or INK2),
                             esc(text)))
        return self

    def notes(self, x, y, lines, size=11, lh=None, color=None, anchor='start',
              weight='400'):
        lh = lh or size * 1.65
        for i, ln in enumerate(lines):
            self.note(x, y + i * lh, ln, size=size, color=color, anchor=anchor,
                      weight=weight)
        return self

    # ---------- 连线 ----------
    def arrow(self, pts, color='main', width=1.8, dash=None, marker=True,
              curve=False):
        """pts: [(x,y), ...]，正交折线（curve=True 时用平滑折角）。"""
        c = ARROW_COLORS.get(color, color)
        mid = 'arrow-%s' % color if marker else None
        d = _path_d(pts, curve)
        extra = ' stroke-dasharray="%s"' % dash if dash else ''
        me = ' marker-end="url(#%s)"' % mid if mid else ''
        self.edges.append('<path d="%s" fill="none" stroke="%s" stroke-width="%s"%s%s '
                          'stroke-linejoin="round"/>' % (d, c, width, extra, me))
        return self

    def arrow_label(self, x, y, text, size=11, color=None, bg=True, anchor='middle',
                    weight='500'):
        col = color or INK2
        if bg:
            w = measure(text, size)
            self.edge_labels.append(
                '<rect x="%.1f" y="%.1f" width="%.1f" height="%.1f" rx="3" ry="3" '
                'fill="#ffffff" opacity="0.94"/>'
                % (x - w / 2.0 - 4 if anchor == 'middle' else x - 3,
                   y - size * 0.85, w + 8, size * 1.5))
        self.edge_labels.append('<text x="%s" y="%s" text-anchor="%s" font-size="%s" '
                                'font-weight="%s" fill="%s">%s</text>'
                                % (x, y, anchor, size, weight, col, esc(text)))
        return self

    # ---------- 图例 ----------
    def add_legend(self, x, y, items, title='图例', col_w=200, size=11):
        """items: [(color_key, '说明', dash|None), ...]"""
        n = len(items)
        cols = 1 if n <= 4 else (2 if n <= 8 else 3)
        rows = (n + cols - 1) // cols
        w = col_w * cols + 24
        h = rows * 20 + 34
        self.legend.append('<rect x="%s" y="%s" width="%s" height="%s" rx="8" ry="8" '
                           'fill="#ffffff" stroke="%s" stroke-width="1"/>'
                           % (x, y, w, h, LINE))
        self.legend.append('<text x="%s" y="%s" font-size="%s" font-weight="600" '
                           'fill="%s">%s</text>' % (x + 12, y + 18, size, INK2,
                                                    esc(title)))
        for i, it in enumerate(items):
            ck, desc = it[0], it[1]
            dash = it[2] if len(it) > 2 else None
            c = ARROW_COLORS.get(ck, ck)
            cx = x + 14 + (i // rows) * col_w
            cy = y + 32 + (i % rows) * 20
            de = ' stroke-dasharray="%s"' % dash if dash else ''
            self.legend.append('<line x1="%s" y1="%s" x2="%s" y2="%s" stroke="%s" '
                               'stroke-width="2"%s marker-end="url(#arrow-%s)"/>'
                               % (cx, cy, cx + 26, cy, c, de, ck))
            self.legend.append('<text x="%s" y="%.1f" font-size="%s" fill="%s">%s</text>'
                               % (cx + 34, cy + 4, size, INK, esc(desc)))
        return self

    # ---------- 输出 ----------
    def svg(self):
        L = []
        L.append('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" '
                 'width="%d" height="%d">' % (self.w, self.h, self.w, self.h))
        L.append('<style>text{font-family:%s;}'
                 '.mono{font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;}'
                 '</style>' % FONT)
        L.append('<defs>')
        for k, c in ARROW_COLORS.items():
            L.append('<marker id="arrow-%s" markerWidth="10" markerHeight="7" refX="9" '
                     'refY="3.5" orient="auto"><polygon points="0 0, 10 3.5, 0 7" '
                     'fill="%s"/></marker>' % (k, c))
        L.append('<filter id="soft" x="-20%%" y="-20%%" width="140%%" height="140%%">'
                 '<feDropShadow dx="0" dy="1.5" stdDeviation="3" '
                 'flood-color="#0f172a" flood-opacity="0.10"/></filter>')
        L.append('</defs>')
        L.append('<rect width="%d" height="%d" fill="%s"/>' % (self.w, self.h, BG))
        if self.title:
            L.append('<text x="40" y="42" font-size="19" font-weight="700" fill="%s">'
                     '%s</text>' % (INK, esc(self.title)))
        if self.subtitle:
            L.append('<text x="40" y="64" font-size="12.5" fill="%s">%s</text>'
                     % (INK2, esc(self.subtitle)))
        for group in (self.containers, self.edges, self.edge_labels, self.nodes,
                      self.texts, self.legend):
            L.extend(group)
        L.append('</svg>')
        return '\n'.join(L)

    def save(self):
        path = os.path.join(OUT_DIR, self.name + '.svg')
        with open(path, 'w', encoding='utf-8') as f:
            f.write(self.svg())
        return path


def _path_d(pts, curve=False):
    if len(pts) < 2:
        raise ValueError('need >= 2 points')
    if not curve or len(pts) == 2:
        return 'M ' + ' L '.join('%.1f,%.1f' % p for p in pts)
    # 圆角折线
    r = 8.0
    d = 'M %.1f,%.1f' % pts[0]
    for i in range(1, len(pts) - 1):
        px, py = pts[i - 1]
        cx, cy = pts[i]
        nx, ny = pts[i + 1]
        v1 = (0 if cx == px else (1 if cx > px else -1), 0 if cy == py else (1 if cy > py else -1))
        v2 = (0 if nx == cx else (1 if nx > cx else -1), 0 if ny == cy else (1 if ny > cy else -1))
        l1 = ((cx - px) ** 2 + (cy - py) ** 2) ** 0.5
        l2 = ((nx - cx) ** 2 + (ny - cy) ** 2) ** 0.5
        rr = min(r, l1 / 2.0, l2 / 2.0)
        d += ' L %.1f,%.1f Q %.1f,%.1f %.1f,%.1f' % (cx - v1[0] * rr, cy - v1[1] * rr,
                                                     cx, cy, cx + v2[0] * rr,
                                                     cy + v2[1] * rr)
    d += ' L %.1f,%.1f' % pts[-1]
    return d
