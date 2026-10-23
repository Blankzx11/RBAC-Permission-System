"""Package final sources and user-facing assets; exclude transient QA previews."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
ROOT=Path(__file__).resolve().parents[1]
target=ROOT/'RBAC0-需求与设计交付包.zip'
files=[]
for p in ROOT.rglob('*'):
    if not p.is_file():continue
    rel=p.relative_to(ROOT)
    if any(part in ['.git','__pycache__','.codex','.agents','.history','.work','node_modules'] for part in rel.parts):continue
    if p.suffix in ['.zip','.pyc'] or p.name.endswith('-preview.png') or p.name in ['design-reader.png','browser-failure.png','vue-browser-failure.png']:continue
    files.append(p)
with ZipFile(target,'w',ZIP_DEFLATED) as archive:
    for p in sorted(files):archive.write(p,'RBAC0-需求与设计交付包/'+p.relative_to(ROOT).as_posix())
import shutil
shutil.copyfile(target,ROOT/'RBAC-需求与设计交付包.zip')
print(f'{target.name}: {len(files)} files, {target.stat().st_size} bytes')
