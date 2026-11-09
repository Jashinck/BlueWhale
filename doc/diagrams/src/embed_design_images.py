# -*- coding: utf-8 -*-
"""把三份核心设计文档中的 doc/diagrams/*.png 引用内联为 base64 data URI。"""
import base64
import os
import re

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
DOCS = [
    'CONTEXT-FRAMEWORK-DESIGN.md',
    'ARCHITECTURE-DETAILED-DESIGN.md',
    'KNOWLEDGE-CLOSED-LOOP-DESIGN.md',
]

pat = re.compile(r'!\[([^\]]*)\]\((doc/diagrams/[^)]+\.png)\)')


def repl(m):
    alt, rel = m.group(1), m.group(2)
    with open(os.path.join(ROOT, rel), 'rb') as f:
        b64 = base64.b64encode(f.read()).decode('ascii')
    return '![%s](data:image/png;base64,%s)' % (alt, b64)


total = 0
for name in DOCS:
    path = os.path.join(ROOT, name)
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    new, n = pat.subn(repl, text)
    if n:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new)
        total += n
        print('%-40s %2d images' % (name, n))

print('total inlined:', total)
