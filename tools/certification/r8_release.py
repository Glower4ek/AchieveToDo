"""Reproducible private notice-only candidates from exact accepted artifacts.

No gameplay payload, version, publication permission, or upstream grant changes.
The Purpur source mirror must supply narrowly scoped notices separately.
"""
from pathlib import Path
import argparse, hashlib, json, zipfile

ACCEPTED = {'fabric': '92faa6506d1462da790787a9b7a9c5d4987ed4e1867589de347e0ea71bae5f38',
            'purpur': '974a6584b0cb5363170f27ce4054817626f5e916af57719b78b391b62bd8a53a'}
NOTICES = ['META-INF/NOTICE-R8.txt', 'META-INF/licenses/UPSTREAM_UI_MIT_LICENSE.txt']

def identity(p):
    with Path(p).open('rb') as f:
        return {'bytes': Path(p).stat().st_size, 'sha256': hashlib.file_digest(f,'sha256').hexdigest()}

def require(ok, message):
    if not ok: raise ValueError(message)

def package(kind, original, out, source, purpur_notices=None):
    require(identity(original)['sha256'] == ACCEPTED[kind], 'WRONG_ACCEPTED_'+kind.upper())
    if kind == 'fabric':
        replacements = {'LICENSE_bacap-better-ru': (source/'tools/certification/notices/bacap-better-ru.LICENSE.txt').read_bytes()}
        require(b'No BACAP Language Pack ARR content is included' not in next(iter(replacements.values())), 'FALSE_ARR_ABSENCE_NOTICE')
        additions = {}
    else:
        replacements = {}; additions = {n:(purpur_notices/n).read_bytes() for n in NOTICES}
        require(b'Public redistribution rights' in additions['META-INF/NOTICE-R8.txt'], 'NOTICE_SCOPE_MISSING')
        require(b'MIT License' in additions[NOTICES[1]], 'UPSTREAM_LICENSE_MISSING')
    out.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(original) as src, zipfile.ZipFile(out,'w') as dst:
        names=src.namelist();require(len(names)==len(set(names)) and src.testzip() is None, 'INVALID_INPUT_ZIP')
        for i in src.infolist(): dst.writestr(i,replacements.get(i.filename,src.read(i.filename)))
        for n,b in sorted(additions.items()):
            require(n not in names,'NOTICE_COLLISION')
            i=zipfile.ZipInfo(n,(1980,1,1,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16;dst.writestr(i,b)
    with zipfile.ZipFile(original) as a,zipfile.ZipFile(out) as b:
        require(b.testzip() is None and len(b.namelist())==len(set(b.namelist())), 'INVALID_OUTPUT_ZIP')
        require(set(b.namelist())==set(a.namelist())|set(additions),'ENTRY_SET_CHANGED')
        changed=[n for n in a.namelist() if a.read(n)!=b.read(n)]
        require(set(changed)==set(replacements),'UNAUTHORIZED_PAYLOAD_CHANGE')
        classes=[n for n in a.namelist() if n.endswith('.class')]
        require(all(a.read(n)==b.read(n) for n in classes),'PRODUCTION_CLASS_CHANGED')
    return {'kind':kind,'original':identity(original),'candidate':identity(out),'changed':changed,
            'added':sorted(additions),'unchangedClasses':len(classes),'publicRights':'NOT_PROVEN',
            'publicationAuthorized':False,'PRODUCT_GAMEPLAY_READY':False}

def main():
    p=argparse.ArgumentParser()
    for n in ['source','fabric','purpur','purpur-notices','output']:p.add_argument('--'+n,required=True,type=Path)
    a=p.parse_args();out=a.output.absolute();out.mkdir(parents=True,exist_ok=True)
    rows=[package('fabric',a.fabric,out/'AchieveToDo-Fabric-26.2-R8-NOTICE-INTERNAL.jar',a.source),
          package('purpur',a.purpur,out/'AchieveToDo-Purpur-26.2-R8-NOTICE-INTERNAL.jar',a.source,a.purpur_notices)]
    (out/'packaging-receipt.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf8')
    print('R8_PRIVATE_NOTICE_CANDIDATES_PACKAGED')

if __name__=='__main__':main()
