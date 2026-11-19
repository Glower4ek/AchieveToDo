"""Provision owner-supplied sealed S5-R5 inputs. No network or authority fallback."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import os
import sys
import zipfile

AUTHORITY = 'S5_R3_ALT_METADATA_V1'
R5_SHA256 = '0f4f75aa35f0aa3d47ad5b20a0c1477c55c5d24069e7c8be7b2c1e718b0ad804'
R5_BYTES = 144474242


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def external(source, destination):
    source, destination = Path(source).resolve(), Path(destination).absolute()
    require(not destination.resolve().is_relative_to(source), 'EXTERNAL_DESTINATION_REQUIRED')
    require(not source.is_relative_to(destination.resolve()), 'SOURCE_ANCESTOR_REJECTED')
    for p in [destination, *destination.parents]:
        require(not p.is_symlink() and not p.is_junction(), 'LINK_REJECTED')
    return destination


def provision(source, archive, destination, authority):
    require(authority == AUTHORITY, 'EXPLICIT_AUTHORITY_REQUIRED')
    destination = external(source, destination)
    archive = Path(archive)
    require(archive.stat().st_size == R5_BYTES, 'R5_ARCHIVE_SIZE')
    with archive.open('rb') as f:
        require(hashlib.file_digest(f, 'sha256').hexdigest() == R5_SHA256, 'R5_ARCHIVE_HASH')
    with zipfile.ZipFile(archive) as z:
        names = z.namelist()
        require(len(names) == len(set(names)) == 12196, 'R5_ENTRY_INVENTORY')
        require(z.testzip() is None, 'R5_CRC')
        manifest = json.loads(z.read('r5_integrity_manifest.json'))
        rows = {r['path']: r for r in manifest['files']}
        require(len(rows) == 12195 and set(names) == set(rows) | {'r5_integrity_manifest.json'}, 'R5_MANIFEST_COVERAGE')
        contract = json.loads(z.read('r5_provisioning_manifest.json'))
        selected = {r['destination'] for r in contract['files']}
        selected |= {'r5_provisioning_manifest.json', 'r3_alternative_metadata_manifest.json',
                     'r3_fabric_source_oracle.json', 'scripts/verify_alternative.py'}
        payload = []
        for name in sorted(selected):
            rel = Path(name)
            require(not rel.is_absolute() and '..' not in rel.parts, 'PATH_ESCAPE')
            data = z.read(name)
            r = rows[name]
            require(len(data) == r['bytes'] and digest(data) == r['sha256'], 'SEALED_INPUT_IDENTITY:' + name)
            target = destination / rel
            for p in [target, *target.parents]:
                require(not p.is_symlink() and not p.is_junction(), 'LINK_REJECTED')
            require(not target.exists() or target.read_bytes() == data, 'DESTINATION_COLLISION:' + name)
            payload.append((target, data))
        for target, data in payload:
            target.parent.mkdir(parents=True, exist_ok=True)
            if not target.exists():
                with target.open('xb') as f:
                    f.write(data)
    return verify(source, destination, authority)


def verify(source, assets, authority):
    require(authority == AUTHORITY, 'EXPLICIT_AUTHORITY_REQUIRED')
    assets = external(source, assets)
    p = assets / 'scripts/verify_alternative.py'
    require(digest(p.read_bytes()) == '9c69cf115602fb2f7bd04c737d8aa27bd0f31ba6b040126e2c1a4e8698d3f7e3', 'VERIFIER_IDENTITY')
    spec = importlib.util.spec_from_file_location('r6_sealed_alternative_verifier', p)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    result = module.verify(Path(source), assets / 'inputs/alternative',
                           assets / 'r3_alternative_metadata_manifest.json', authority)
    contract_path = assets / 'r5_provisioning_manifest.json'
    require(digest(contract_path.read_bytes()) == '3fa9d1fb613e2c6bb2563bc4ca5ceee0e0fa63c5d65980149bf79e4d217f1270', 'PROVISIONING_CONTRACT_IDENTITY')
    contract = json.loads(contract_path.read_bytes())
    for row in contract['files']:
        p = assets / row['destination']
        for ancestor in [p, *p.parents]:
            require(not ancestor.is_symlink() and not ancestor.is_junction(), 'LINK_REJECTED')
        require(p.is_file() and not p.is_symlink() and not p.is_junction(), 'MISSING_OR_LINKED_INPUT')
        data = p.read_bytes()
        require(len(data) == row['bytes'] and digest(data) == row['sha256'], 'INPUT_IDENTITY:' + row['destination'])
    return {**result, 'provisionedFiles': len(contract['files']), 'sealedR5': R5_SHA256,
            'redistribution': 'Owner-supplied internal verification assets; no public redistribution authorized.'}


def b10(source, assets, authority, mode, attempt):
    verify(source, assets, authority)
    source, assets = Path(source).resolve(), Path(assets).resolve()
    require(attempt and all(c.isalnum() or c in '-_' for c in attempt), 'ATTEMPT_NAME')
    require(mode in ('MAIN', 'TERRALITH'), 'SUPPORTED_B10_MODE')
    payload = source / 'tools/phase_b/r3_b10_current_source_v1.py'
    require(digest(payload.read_bytes()) == '1eb776e43f0ec660e980333975fca31fe811a1529ef4cee933d7ab3a8c938df1', 'B10_SCRIPT_REVISION')
    converted = assets / 'inputs/b10-current/BACAP-converted.zip'
    core = source / 'build/tmp/s5_r3_b10_core_v1'
    for f in (assets / 'inputs/b10-current/core').iterdir():
        if f.is_file():
            target = core / f.name
            require(not target.exists() or target.read_bytes() == f.read_bytes(), 'B10_CORE_COLLISION')
            target.parent.mkdir(parents=True, exist_ok=True)
            if not target.exists():
                with target.open('xb') as out:
                    out.write(f.read_bytes())
    os.environ.update({'ACHIEVETODO_B10_AUTHORITY': 'S5_R3_B10_CURRENT_SOURCE_V1',
                       'ACHIEVETODO_SOURCE_ORACLE_FILE': str(assets / 'r3_fabric_source_oracle.json'),
                       'ACHIEVETODO_ALTERNATIVE_MANIFEST': str(assets / 'r3_alternative_metadata_manifest.json'),
                       'ACHIEVETODO_TEST_INPUTS_DIR': str(assets / 'inputs/alternative')})
    literal = "pathlib.Path('E:/Vibecode/AchieveToDo_DualRuntime_S5_R3_Evidence/inputs/b10-current/BACAP-converted.zip')"
    code = payload.read_text(encoding='utf8')
    require(code.count(literal) == 1, 'B10_RELOCATION_SITE')
    code = code.replace(literal, 'pathlib.Path(' + repr(str(converted)) + ')')
    sys.path.insert(0, str(source / 'tools/phase_b'))
    namespace = {'__name__': 'r6_b10_portable', '__file__': str(payload)}
    exec(compile(code, str(payload), 'exec'), namespace)
    namespace['prepare'](mode, attempt, deep=mode == 'MAIN', companion=mode == 'TERRALITH')
    return {'status': 'PASS', 'mode': mode, 'attempt': attempt, 'sourceSemanticChange': False,
            'plan': str(source / 'build/tmp/s5_r3_b10_v1' / attempt / 'plan.json')}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['provision', 'verify', 'b10'])
    parser.add_argument('--source', required=True)
    parser.add_argument('--assets', required=True)
    parser.add_argument('--authority', required=True)
    parser.add_argument('--archive')
    parser.add_argument('--mode', choices=['MAIN', 'TERRALITH'])
    parser.add_argument('--attempt')
    args = parser.parse_args()
    if args.action == 'provision':
        require(args.archive, 'ARCHIVE_REQUIRED')
        result = provision(args.source, args.archive, args.assets, args.authority)
    elif args.action == 'b10':
        result = b10(args.source, args.assets, args.authority, args.mode, args.attempt)
    else:
        result = verify(args.source, args.assets, args.authority)
    print(json.dumps(result, indent=2))
