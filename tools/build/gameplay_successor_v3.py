"""One reviewed workbench source successor; no runtime/readiness authority.

The caller authenticates this module against its literal manifest pin before import.
Unchanged V2 controls execute against exact authenticated predecessor bytes.
"""
from pathlib import Path, PurePosixPath
import io
import os
import stat
import subprocess
import sys
import tempfile
import zipfile

from tools.build import distribution_b_v2_contract as c

AUTHORITY = 'ATD_PURPUR_GAMEPLAY_SOURCE_V3'
PATH = 'tools/certification/authority/gameplay-successor-v3.json'
SELECTOR = 'ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY'
FILE = 'ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE'
PARENT = 'tools/certification/authority/gameplay-successor-v2.json'
PARENT_SHA = '522fecccba5e2c3f657afe2db0bc2b2db54c93d674c95206a2061bf695c48bab'
TARGET = 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkActions.java'
JUNIT = 'src/test/java/com/diskree/achievetodo/certification/GameplaySuccessorAuthorityTest.java'
MODULE = 'tools/build/gameplay_successor_v3.py'
TESTS = 'tools/build/tests/test_gameplay_successor_v3.py'
REVIEW = 'tools/certification/authority/gameplay-independent-code-review-v8.json'
REVIEW_SHA = 'aa81f6793dd2f205f9d1e90a2f758e7c5590e68628d372131a3115af440b426c'
FIXTURE = 'tools/certification/fixtures/gameplay-source-v2-predecessor.zip'
MAX_ARCHIVE = 180_000
MAX_UNCOMPRESSED = 150_000
ARCHIVE_FILES = {
    TARGET: {'sha256': 'b1a65b3447e3c90a789ca14ead6a4c382755de7691ce8a1d25455393697a7dc0', 'bytes': 25273},
    'build.gradle': {'sha256': '2312f103474c7113b9d2514af3bf5a5d62396290ad53ef69e05ea62e03a9963e', 'bytes': 42879},
    'tools/build/distribution_b_v2_contract.py': {'sha256': '848ec3a0bc651ddb1567d0d576baa6d9c73b276b2098268590b132c3a1b594b6', 'bytes': 29418},
    'src/test/java/com/diskree/achievetodo/certification/UiUxMaintenanceSourceContract.java': {'sha256': 'd05d19e55e156005e9061a707f2ea64e8c785dde0b02d12fbd1369a63de199f3', 'bytes': 22745},
    JUNIT: {'sha256': '1c2b0f510e2b28cac34de5212999f85b269ae82ddc972211c2b254fa4a5afa5e', 'bytes': 1330},
}
OLD_CODE_PATHS = frozenset({
    'tools/build/gameplay_successor_v2.py',
    'tools/build/tests/test_gameplay_successor_v2.py',
    'tools/certification/authority/gameplay-independent-code-review-v7.json',
    JUNIT, 'tools/certification/fixtures/gameplay-source-v1-predecessor.zip',
    'tools/certification/authority/gameplay-successor-v1.json',
    'tools/build/gameplay_successor.py', 'tools/build/tests/test_gameplay_successor.py',
    'tools/certification/authority/gameplay-owner-scope-v1.json',
    'tools/certification/authority/gameplay-independent-code-review-v4.json',
})
CODE_PATHS = OLD_CODE_PATHS | {PARENT, MODULE, TESTS, REVIEW, FIXTURE}
PRODUCTION_ROOTS = ('src/main', 'port/AchieveToDo-Purpur-26.2/src/main')


def relative(name):
    c.require(isinstance(name, str) and name and '\x00' not in name,
              'V3 relative path type')
    p = PurePosixPath(name)
    c.require(p.as_posix() == name and not p.is_absolute()
              and '\\' not in name and ':' not in name
              and all(x not in ('', '.', '..') for x in name.split('/')),
              'V3 unsafe relative path')
    return name


def production(source):
    observed = {}
    for directory in PRODUCTION_ROOTS:
        for p in (source / directory).rglob('*'):
            c._no_links(p)
            if p.is_file():
                name = p.relative_to(source).as_posix()
                observed[name] = c.sha(c.safe_file(source, name).read_bytes())
    return observed


def archive_originals(raw, entries):
    c.require(entries == ARCHIVE_FILES and len(raw) <= MAX_ARCHIVE
              and sum(x['bytes'] for x in entries.values()) <= MAX_UNCOMPRESSED,
              'V3 exact bounded five originals')
    result = {}
    try:
        with zipfile.ZipFile(io.BytesIO(raw)) as z:
            infos = z.infolist()
            c.require(len(infos) == 5, 'V3 archive cardinality')
            for info in infos:
                c.require(info.orig_filename == info.filename,
                          'V3 normalized archive filename')
                name = relative(info.filename)
                c.require(name in entries and name not in result,
                          'V3 extra/duplicate archive member')
                row = entries[name]
                c.require(not info.is_dir() and not info.flag_bits & 1
                          and stat.S_IFMT(info.external_attr >> 16) in (0, stat.S_IFREG)
                          and info.compress_type == zipfile.ZIP_STORED
                          and info.file_size == row['bytes'],
                          'V3 nonregular/encrypted/compressed archive member')
                data = z.read(info)
                c.require(len(data) == row['bytes'] and c.sha(data) == row['sha256'],
                          'V3 predecessor original bytes')
                result[name] = data
    except (zipfile.BadZipFile, RuntimeError, OSError) as error:
        raise ValueError('V3 invalid predecessor ZIP') from error
    c.require(set(result) == set(ARCHIVE_FILES), 'V3 missing originals')
    return result


def parent_data(source):
    raw = c.safe_file(source, PARENT).read_bytes()
    c.require(c.sha(raw) == PARENT_SHA, 'V3 immutable V2 manifest')
    parent = c.strict_json(raw)
    c.require(set(parent['codeBindings']) == OLD_CODE_PATHS,
              'V3 exact immutable V2 code paths')
    ref = parent['independentReview']
    review_raw = c.safe_file(source, relative(ref['path'])).read_bytes()
    c.require(c.sha(review_raw) == ref['sha256']
              == parent['codeBindings'][ref['path']], 'V3 parent review identity')
    old = c.strict_json(review_raw)['productionClosure']
    c.require(len(old) == 2504 and old.get(TARGET) == ARCHIVE_FILES[TARGET]['sha256'],
              'V3 parent production closure')
    return parent, old


def predecessor_context(parent, old):
    # The older predecessorContext alone is V1, not current V7: overlay V2 first.
    result = dict(parent['predecessorContext'])
    result.update(old)
    result.update({p: row['afterSHA256'] for p, row in parent['allGameplayChanges'].items()})
    result.update(parent['codeBindings'])
    result.update({p: row['sha256'] for p, row in ARCHIVE_FILES.items()})
    result[PARENT] = PARENT_SHA
    for name in result:
        relative(name)
    c.require(len({p.casefold() for p in result}) == len(result),
              'V3 predecessor case aliases')
    return result


def require_legacy_result(run):
    c.require(run.returncode == 0 and 'Ran 11 tests' in run.stderr
              and '\nOK\n' in run.stderr and 'skipped=' not in run.stderr
              and 'expected failures=' not in run.stderr
              and 'test_exact_current_source_and_unchanged_four_legacy_controls' in run.stderr,
              'V3 unchanged eleven V2 controls failed: ' + run.stdout + run.stderr)


def legacy_controls(source, env, context, old, originals):
    with tempfile.TemporaryDirectory(prefix='atd-authentic-v2-') as tmp:
        root = Path(tmp)
        for name, digest in context.items():
            data = originals[name] if name in originals else c.safe_file(source, name).read_bytes()
            c.require(c.sha(data) == digest, 'V3 exact predecessor reconstruction: ' + name)
            dest = root / relative(name)
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(data)
        c.require(production(root) == old, 'V3 reconstructed full V2 closure')
        clean = dict(env)
        clean[SELECTOR] = 'ATD_PURPUR_GAMEPLAY_SOURCE_V2'
        clean[FILE] = str(root / PARENT)
        clean['PYTHONDONTWRITEBYTECODE'] = '1'
        clean.pop('PYTHONPATH', None)
        run = subprocess.run([sys.executable, '-B', '-X', 'utf8', '-m', 'unittest',
                              'tools.build.tests.test_gameplay_successor_v2', '-v'],
                             cwd=root, env=clean, capture_output=True, text=True, timeout=600)
        require_legacy_result(run)


def validate(source, env, expected_digest, ui07):
    source = Path(source).absolute()
    c._no_links(source)
    c.require(env.get(SELECTOR) == AUTHORITY and env.get(FILE) == str(source / PATH)
              and env.get('ACHIEVETODO_UI07_AUTHORITY') == 'ATD_UI07_SCOPED_SUCCESSOR_V1'
              and env.get(c.SELECTOR) == c.AUTHORITY, 'V3 unknown/orphan/path/parent selector')
    raw = c.safe_file(source, PATH).read_bytes()
    c.require(c.sha(raw) == expected_digest, 'V3 manifest digest')
    m = c.strict_json(raw)
    c.require(set(m) == {'schema', 'authority', 'state', 'oldV2', 'ancestorPins',
                        'productionDelta', 'currentProductionClosure', 'allGameplayChanges',
                        'auxiliaryDelta', 'codeBindings', 'independentReview', 'fixture',
                        'archiveEntries', 'predecessorContext'}, 'V3 exact manifest schema')
    c.require(m['schema'] == 3 and m['authority'] == AUTHORITY
              and m['state'] == 'AUTHORIZED_BOUNDED_SOURCE_FOR_CERTIFICATION_ONLY', 'V3 state/scope')
    c.require(m['oldV2'] == {'path': PARENT, 'sha256': PARENT_SHA}, 'V3 parent pin')
    parent, old = parent_data(source)
    ancestors = {k: parent[k] for k in ('oldGateA', 'oldOwnerB', 'oldUI07', 'oldV1')}
    c.require(m['ancestorPins'] == ancestors
              and ancestors['oldGateA'] == c.policy(source)['gateAManifestSHA256']
              and ancestors['oldOwnerB'] == c.OWNER_B_CAPSULE_SHA256
              and ancestors['oldUI07'] == c.UI07_SUCCESSOR_SHA256, 'V3 ancestor identities')
    c.require(set(m['productionDelta']) == {TARGET}, 'V3 exact one production delta')
    delta = m['productionDelta'][TARGET]
    c.require(set(delta) == {'beforeSHA256', 'afterSHA256'}
              and delta['beforeSHA256'] == old[TARGET]
              and delta['beforeSHA256'] != delta['afterSHA256'], 'V3 exact production delta identities')
    result = dict(old)
    result[TARGET] = delta['afterSHA256']
    c.require(m['currentProductionClosure'] == result and production(source) == result,
              'V3 full current production closure')
    flattened = {p: dict(row) for p, row in parent['allGameplayChanges'].items()}
    flattened[TARGET]['afterSHA256'] = delta['afterSHA256']
    c.require(m['allGameplayChanges'] == flattened
              and all(ui07.get(p) == row['beforeSHA256'] for p, row in flattened.items()
                      if p.startswith(('src/main/', 'port/AchieveToDo-Purpur-26.2/src/main/'))),
              'V3 exact flattened original UI07 changes')
    c.require(set(m['auxiliaryDelta']) == {JUNIT}, 'V3 exact JUnit auxiliary delta')
    auxiliary = m['auxiliaryDelta'][JUNIT]
    c.require(set(auxiliary) == {'beforeSHA256', 'afterSHA256'}
              and auxiliary['beforeSHA256'] == parent['codeBindings'][JUNIT]
              == ARCHIVE_FILES[JUNIT]['sha256']
              and auxiliary['afterSHA256'] != auxiliary['beforeSHA256'], 'V3 JUnit identities')
    c.require(set(m['codeBindings']) == CODE_PATHS, 'V3 exact authority code paths')
    for name, digest in m['codeBindings'].items():
        c.require(c.sha(c.safe_file(source, relative(name)).read_bytes()) == digest,
                  'V3 code identity: ' + name)
    for name, digest in parent['codeBindings'].items():
        c.require(m['codeBindings'][name] == (auxiliary['afterSHA256'] if name == JUNIT else digest),
                  'V3 immutable parent code or bounded JUnit overlay')
    c.require(m['codeBindings'][PARENT] == PARENT_SHA
              and m['independentReview'] == {'path': REVIEW, 'sha256': REVIEW_SHA}
              and m['codeBindings'][REVIEW] == REVIEW_SHA, 'V3 actual independent review pin')
    review = c.strict_json(c.safe_file(source, REVIEW).read_bytes())
    c.require(review.get('schema') == 1
              and review.get('reviewer') == 'independent agent /root/independent_v7_review'
              and review.get('verdict') == 'NO_CODE_LEVEL_BLOCKER_IDENTIFIED_PENDING_AUTHORITY_BUILD_AND_RUNTIME_REGRESSION'
              and review.get('acceptedForNextEngineeringChecks') is True
              and all(review.get(k) is False for k in ('sourceAuthorityAccepted', 'compiledAccepted', 'runtimeAccepted', 'productReady'))
              and review.get('findings') == []
              and review.get('sourceChanges') == [{'path': TARGET, **delta}]
              and review.get('productionClosure') == {'paths': 2504, 'otherPathsExactV7': 2503,
                                                      'all2504OriginalBytesIndependentlyHashVerified': True},
              'V3 exact independent one-file code-only acceptance')
    c.require(m['fixture'] == {'path': FIXTURE, 'sha256': m['codeBindings'][FIXTURE]},
              'V3 fixture binding')
    originals = archive_originals(c.safe_file(source, FIXTURE).read_bytes(), m['archiveEntries'])
    context = predecessor_context(parent, old)
    c.require(m['predecessorContext'] == context, 'V3 exact predecessor context')
    legacy_controls(source, env, context, old, originals)
    return result, sorted(set(flattened) | CODE_PATHS | {PATH})
