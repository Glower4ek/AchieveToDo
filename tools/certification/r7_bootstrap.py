"""R7 two-stage immutable offline bootstrap. Never invokes or exempts Gradle gates.

The 372 selected bytes carry CACHE-BASELINE-LIMITED authority, not publisher
authentication. A reviewed full cache bundle can supply additional prerequisites
using a separately pinned external manifest. Locally generated inputs stay labeled.
"""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, os, re, zipfile

AUTHORITY = 'S5_R7_OFFLINE_BOOTSTRAP_V1'
BASE_MANIFEST = '1dbe9d7cd2fe653454f8692b7cdf5e54787c3df74eb12461c01164b99190ac77'

def require(ok, message):
    if not ok: raise ValueError(message)

def digest(data): return hashlib.sha256(data).hexdigest()

def unique(pairs):
    out = {}
    for k, v in pairs:
        require(k not in out, 'DUPLICATE_JSON_KEY')
        out[k] = v
    return out

def relative(name):
    require(isinstance(name, str) and name and '\\' not in name and ':' not in name, 'UNSAFE_PATH')
    p = PurePosixPath(name)
    require(not p.is_absolute() and all(x not in ('', '.', '..') for x in name.split('/')), 'UNSAFE_PATH')
    require(all(not x.endswith(('.', ' ')) and not re.fullmatch(r'(?i)(con|prn|aux|nul|com[0-9]|lpt[0-9])(?:\..*)?', x) for x in p.parts), 'UNSAFE_WINDOWS_PATH')
    return p

def target(root, name):
    p = Path(root).absolute().joinpath(*relative(name).parts)
    for ancestor in [p, *p.parents]:
        require(not ancestor.is_symlink() and not ancestor.is_junction(), 'LINK_REJECTED')
    require(p.resolve().is_relative_to(Path(root).resolve()), 'PATH_ESCAPE')
    return p

def selected(source):
    data = (Path(source)/'src/test/resources/build_inputs/manifest.json').read_bytes()
    require(digest(data) == BASE_MANIFEST, 'BASE_MANIFEST_TAMPER')
    rows = json.loads(data, object_pairs_hook=unique)['dependencyInputs']
    require(len(rows) == len({r['gradleUserHomeRelativePath'] for r in rows}) == 372, 'INCOMPLETE_372_MANIFEST')
    for r in rows: relative(r['gradleUserHomeRelativePath'])
    return rows

def load_manifest(source, path, expected):
    data = Path(path).read_bytes()
    require(re.fullmatch('[0-9a-f]{64}', expected or '') and digest(data) == expected, 'BOOTSTRAP_MANIFEST_TAMPER')
    m = json.loads(data, object_pairs_hook=unique)
    require(m['authority'] == AUTHORITY and m['sourceAuthority'] == 'OWNER_INTERNAL_R6_CACHE_SNAPSHOT', 'SOURCE_AUTHORITY')
    rows = m['files']; names = [r['path'] for r in rows]
    require(len(names) == len(set(names)) and len({n.casefold() for n in names}) == len(names), 'DUPLICATE_PATH')
    for r in rows:
        relative(r['path'])
        require(r['kind'] in ('SELECTED_CACHE_INPUT','SUPPLEMENTAL_DOWNLOADED_CACHE','LOCALLY_GENERATED_CACHE'), 'UNKNOWN_INPUT_CLASS')
        require(type(r['bytes']) is int and r['bytes'] >= 0 and re.fullmatch('[0-9a-f]{64}', r['sha256']), 'MALFORMED_IDENTITY')
    byname = {r['path']:r for r in rows}
    for r in selected(source):
        n = r['gradleUserHomeRelativePath']; got = byname.get(n)
        require(got and got['bytes'] == r['size'] and got['sha256'] == r['SHA256'] and got['kind'] == 'SELECTED_CACHE_INPUT', 'SELECTED_INPUT_MISMATCH:'+n)
    require(sum(r['kind'] == 'SELECTED_CACHE_INPUT' for r in rows) == 372, 'INCOMPLETE_372_MANIFEST')
    return m

def provision(source, manifest, expected, bundle, home, authority, stop_after=None):
    require(authority == AUTHORITY, 'EXPLICIT_AUTHORITY')
    m = load_manifest(source, manifest, expected)
    require(not Path(home).resolve().is_relative_to(Path(source).resolve()), 'EXTERNAL_HOME_REQUIRED')
    with zipfile.ZipFile(bundle) as z:
        names = z.namelist()
        require(len(names) == len(set(names)) and set(names) == {r['path'] for r in m['files']}, 'UNEXPECTED_OR_MISSING_CONTENT')
        require(z.comment == b'', 'ARCHIVE_COMMENT')
        # Complete validation before the first write; a truncated or malicious bundle
        # cannot partially provision unchecked bytes. Resume re-verifies all bytes.
        for r in m['files']:
            p = target(home, r['path']); b = z.read(r['path'])
            require(len(b) == r['bytes'] and digest(b) == r['sha256'], 'INPUT_IDENTITY:'+r['path'])
            require(not p.exists() or p.read_bytes() == b, 'DESTINATION_COLLISION:'+r['path'])
        written = 0
        for r in m['files']:
            p = target(home, r['path'])
            if not p.exists():
                p.parent.mkdir(parents=True, exist_ok=True)
                temp = p.with_name(p.name+'.r7-part')
                require(not temp.exists(), 'UNEXPECTED_PARTIAL_FILE')
                with temp.open('xb') as f:
                    f.write(z.read(r['path'])); f.flush(); os.fsync(f.fileno())
                os.replace(temp, p)
                written += 1
                if stop_after and written == stop_after: raise InterruptedError('CONTROLLED_INTERRUPTION_AFTER_VERIFIED_ATOMIC_INPUT')
    return {'status':'PASS','authority':AUTHORITY,'files':len(m['files']),'selected':372,'newlyWritten':written,'networkAcquisition':False,'publisherAuthentication':'NOT_CLAIMED','strictBuildGate':'UNCHANGED'}

if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['source','manifest','manifest-sha256','bundle','home','authority']: p.add_argument('--'+name,required=True)
    a=p.parse_args()
    print(json.dumps(provision(a.source,a.manifest,a.manifest_sha256,a.bundle,a.home,a.authority),indent=2))
