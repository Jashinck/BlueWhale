# -*- coding: utf-8 -*-
"""把 ARCHITECTURE-DETAILED-DESIGN.md 中的图引用内联为 base64 data URI（自包含单文件）。

用法：python3 embed_images.py
重新生成图后（gen_XX.py + validate + rsvg-convert）再跑一次即可刷新。
"""
import base64
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
DOC = os.path.join(ROOT, 'ARCHITECTURE-DETAILED-DESIGN.md')

with open(DOC, 'r', encoding='utf-8') as f:
    text = f.read()

pat = re.compile(r'!\[([^\]]*)\]\((doc/diagrams/[^)]+\.png)\)')


def repl(m):
    alt, rel = m.group(1), m.group(2)
    with open(os.path.join(ROOT, rel), 'rb') as f:
        b64 = base64.b64encode(f.read()).decode('ascii')
    return '![%s](data:image/png;base64,%s)' % (alt, b64)


new_text, n = pat.subn(repl, text)

with open(DOC, 'w', encoding='utf-8') as f:
    f.write(new_text)

print('inlined %d images -> %s (%.1f KB)' % (n, DOC, os.path.getsize(DOC) / 1024.0))
