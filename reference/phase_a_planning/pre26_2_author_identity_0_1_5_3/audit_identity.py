"""Identity-only audit; never modifies existing evidence or release artifacts."""
import collections, hashlib, io, json, re, subprocess, sys, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
REL = OUT.relative_to(ROOT).as_posix()
PAT = re.compile(rb'diskree', re.I)

def sha(data):
    return hashlib.sha256(data).hexdigest()

def save(name, value):
    (OUT / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def occurrences(data, path, archive=False):
    result = []
    for match in PAT.finditer(data):
        start, end = match.span()
        prefix = data[max(0, start-5):start].lower()
        context = data[max(0,start-100):min(len(data),end+140)].decode('utf-8', 'replace')
        technical = prefix.endswith((b'com.', b'com/', b'com\\', b'com$')) and not re.search(rb'[A-Za-z0-9_-]\.com[/\\.]$', data[max(0,start-40):start])
        if path == 'gradle.properties' and data[max(0,start-7):start].endswith(b'author='):
            category, decision, reason = 'A', 'CHANGE', 'Canonical current author property.'
        elif path == 'LICENSE':
            category, decision, reason = 'D', 'CHANGE', 'Current project holder notice; full transfer stated by user.'
        elif technical:
            category, decision, reason = 'E', 'KEEP', 'Required existing Java namespace/class or path reference.'
        elif 'bacap-better-ru' in path:
            category, decision, reason = 'H', 'KEEP', 'Third-party BACAP Better RU 2.3 original author provenance; not current AchieveToDo ownership.'
        elif path.startswith(('reference/phase_a_planning/', 'reference/phase_a_preservation/', 'evidence/')):
            category, decision, reason = 'F', 'KEEP', 'Retained prior audit, evidence, snapshot or release record; never current ownership authority.'
        elif path.startswith(('tmp_', '_tmp_', 'audittmp/', 'dist/', 'logs/', 'com/', 'META-INF/', 'assets/', 'data/', 'net/')) or path.endswith(('.log', '.xml', '.class', '.jar')):
            category, decision, reason = 'G', 'KEEP', 'Pre-existing diagnostic/extracted artifact or upstream snapshot; historical provenance.'
        elif path.startswith('licenses/'):
            category, decision, reason = 'H', 'KEEP', 'Third-party license attribution.'
        elif path.startswith('src/test/'):
            category, decision, reason = 'I', 'KEEP', 'Existing fixture; review required for non-namespace matches.'
        else:
            category, decision, reason = 'J', 'REVIEW', 'Manual context review required.'
        result.append(dict(path=path, locationType='archive-byte-offset' if archive else 'file-byte-offset', byteOffset=start,
                           line=None if archive else data.count(b'\n',0,start)+1, category=category, decision=decision, reason=reason, context=context))
    return result

def baseline():
    command = ['rg','--files','--hidden','--no-ignore','-g','!.git/**','-g','!.gradle/**',
               '-g','!.gradle-user-home/**','-g','!.idea/**','-g','!build/**','-g',f'!{REL}/**']
    files = subprocess.check_output(command, cwd=ROOT).decode('utf-8').splitlines()
    rows, names, archives, errors = [], [], [], []
    for file in files:
        p = ROOT / file
        path = p.relative_to(ROOT).as_posix()
        data = p.read_bytes()
        rows.extend(occurrences(data,path))
        for m in re.finditer('diskree',path,re.I):
            names.append(dict(path=path, offset=m.start(), category='E' if re.search(r'com[/\\]diskree',path,re.I) else 'G', decision='KEEP', reason='Existing technical path or historical diagnostic filename.'))
        if zipfile.is_zipfile(io.BytesIO(data)):
            try:
                with zipfile.ZipFile(io.BytesIO(data)) as z:
                    for info in z.infolist():
                        if info.is_dir(): continue
                        label = path+'!'+info.filename
                        archives.extend(occurrences(z.read(info), label, True))
                        for m in re.finditer('diskree',info.filename,re.I):
                            names.append(dict(path=label,offset=m.start(),category='E' if re.search(r'com[/\\]diskree',info.filename,re.I) else 'G',decision='KEEP',reason='Technical archive-entry path or historical filename.'))
            except Exception as e:
                errors.append(dict(path=path,error=str(e)))
    save('repository_occurrences_before.json', rows)
    save('archive_occurrences_before.json',archives)
    save('path_occurrences_before.json',names)
    inventory = {}
    for file in files:
        path = (ROOT/file).relative_to(ROOT).as_posix()
        if path.startswith(('src/','reference/','tools/','.agents/','gradle/','licenses/')) or path in ['build.gradle','gradle.properties','LICENSE','README.md']:
            inventory[path] = sha((ROOT/path).read_bytes())
    for p in (ROOT/'build/libs').glob('*.jar'):
        inventory[p.relative_to(ROOT).as_posix()] = sha(p.read_bytes())
    for path in ['.git/HEAD','.git/index','.git/config']:
        if (ROOT/path).exists(): inventory[path]=sha((ROOT/path).read_bytes())
    save('protected_input_hashes.json',inventory)
    print(json.dumps(dict(files=len(files), contentMatches=len(rows),archiveMatches=len(archives),pathMatches=len(names), categories=collections.Counter(r['category'] for r in rows),archiveCategories=collections.Counter(r['category'] for r in archives), errors=errors)))
    save('manual_review_queue.json',[r for r in rows+archives if r['category'] in ['J','I','H']])

if __name__ == '__main__':
    baseline()
