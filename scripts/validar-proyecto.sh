#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "[1/5] Revisando secretos accidentales..."
if grep -RInE --exclude-dir=.git --exclude='*.md' --exclude='.env.example' '(sb_secret_[A-Za-z0-9_-]{10,}|service_role[[:space:]]*=[[:space:]]*[^$])' .; then
  echo "ERROR: se encontró una posible clave secreta en archivos versionados."
  exit 1
fi

echo "[2/5] Validando JavaScript compartido..."
node --check src/frontend/js/api.js
node --check src/frontend/js/utils.js
node --check src/frontend/js/quote-export.js

echo "[3/5] Validando JavaScript embebido..."
python3 - <<'PY'
from pathlib import Path
import re, subprocess, tempfile
pages = sorted(Path('src/frontend/pages').glob('*.html'))
for page in pages:
    text=page.read_text(encoding='utf-8')
    scripts=re.findall(r'<script(?:\s[^>]*)?>(.*?)</script>', text, re.S|re.I)
    code='\n'.join(scripts)
    if not code.strip():
        continue
    with tempfile.NamedTemporaryFile('w',suffix='.js',delete=False,encoding='utf-8') as f:
        f.write(code); name=f.name
    subprocess.run(['node','--check',name],check=True)
    Path(name).unlink(missing_ok=True)
    print('  OK',page)
PY


echo "[4/5] Revisando IDs HTML duplicados..."
python3 - <<'PY'
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
class P(HTMLParser):
    def __init__(self): super().__init__(); self.ids=[]
    def handle_starttag(self,tag,attrs):
        for k,v in attrs:
            if k=='id' and v: self.ids.append(v)
for p in Path('src/frontend/pages').glob('*.html'):
    parser=P(); parser.feed(p.read_text(encoding='utf-8'))
    dup=[x for x,n in Counter(parser.ids).items() if n>1]
    if dup: raise SystemExit(f'IDs duplicados en {p}: {dup}')
print('  OK')
PY

echo "[5/5] Revisando archivos temporales..."
if find . -name '.DS_Store' -o -name '*.class' -o -path '*/target/*' | grep -q .; then
  echo "ADVERTENCIA: hay archivos generados o temporales; no los agregue a Git."
fi

echo "VALIDACIÓN FRONTEND/ESTRUCTURA COMPLETADA"
echo "Falta ejecutar: cd src/backend && ./mvnw -DskipTests clean package"
