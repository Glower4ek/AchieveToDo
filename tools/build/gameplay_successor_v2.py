"""Exact source-only successor; historical controls run against authenticated originals.

This module never accepts runtime evidence or grants product/distribution readiness.
The calling literal authority anchor must authenticate this module before import.
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

AUTHORITY = 'ATD_PURPUR_GAMEPLAY_SOURCE_V2'
PATH = 'tools/certification/authority/gameplay-successor-v2.json'
SELECTOR = 'ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY'
FILE = 'ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE'
FIXTURE = 'tools/certification/fixtures/gameplay-source-v1-predecessor.zip'
V1_PATH = 'tools/certification/authority/gameplay-successor-v1.json'
V1_SHA = '64b9f7cefac0b862366e5b6988e0e95847986fcf7868e4734327790b7e838693'
MAX_ARCHIVE = 400_000
MAX_UNCOMPRESSED = 350_000
PRODUCTION_ROOTS = ('src/main', 'port/AchieveToDo-Purpur-26.2/src/main')

# Exact bounded gameplay envelope; authority anchors are excluded to avoid self-pinning.
ALLOWED = frozenset(['port/AchieveToDo-Purpur-26.2/build.gradle.kts', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/bridge/BridgeWire.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/bridge/BukkitBridgeRuntime.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/core/AchieveToDoPlugin.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/core/ProductGameplayAdmission.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/datapack/BacapPacks.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/BukkitExpandedGameplayListener.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/BukkitFoodGameplayListener.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/BukkitGameplayRuntime.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/GameplayDenialEffects.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/NativeActionBindings.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/NativeComplexBindings.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/NativeMovementBindings.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/NativePortalBindings.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/NativeSourceReloadBindings.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/NativeToolBindings.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/NativeTransactionBindings.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeActionAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeComplexAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeComplexSpecs.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkActions.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkSupplementAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkSupplementSpecs.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeMovementAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativePortalAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeSourceReloadAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeToolAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeToolCombatInvocation.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeToolSpecs.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeTransactionAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeTransactionPremain.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/landmark/CanonicalLandmarkMembership.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/landmark/bukkit/BukkitExactLandmarkRuntime.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitBacapRegistry.java', 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/bridge/BridgeDenialIntentTest.java', 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/core/ProductGameplayAdmissionTest.java', 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/GameplayDenialEffectsTest.java', 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/nativeguard/NativeCombatScopeContractTest.java', 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/nativeguard/NativeCombatTargetContractTest.java', 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/nativeguard/NativeDenialIntentContractTest.java', 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/nativeguard/SourceReloadContractTest.java', 'src/bridgeTest/java/com/diskree/achievetodo/bridge/BridgeDenialIntentTest.java', 'src/main/java/com/diskree/achievetodo/bridge/BridgeWire.java', 'src/main/java/com/diskree/achievetodo/client/AchieveToDoClient.java', 'src/main/java/com/diskree/achievetodo/client/bridge/BridgeClient.java', 'src/main/java/com/diskree/achievetodo/injection/mixin/client/ClientPlayerEntityMixin.java'])
AUX_BEFORE = {'port/AchieveToDo-Purpur-26.2/build.gradle.kts': '3c846c5fcafe6ca27ccf0db470be5b5e2d214fd54dbc90dd1cf159ab79df58e4', 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/bridge/BridgeDenialIntentTest.java': None, 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/core/ProductGameplayAdmissionTest.java': None, 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/GameplayDenialEffectsTest.java': None, 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/nativeguard/NativeCombatScopeContractTest.java': None, 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/nativeguard/NativeCombatTargetContractTest.java': None, 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/nativeguard/NativeDenialIntentContractTest.java': None, 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/gameplay/nativeguard/SourceReloadContractTest.java': None, 'src/bridgeTest/java/com/diskree/achievetodo/bridge/BridgeDenialIntentTest.java': None}
CODE_PATHS = frozenset({
    "tools/build/gameplay_successor_v2.py",
    "tools/build/tests/test_gameplay_successor_v2.py",
    "tools/certification/authority/gameplay-independent-code-review-v7.json",
    "src/test/java/com/diskree/achievetodo/certification/GameplaySuccessorAuthorityTest.java",
    FIXTURE, V1_PATH,
    "tools/build/gameplay_successor.py",
    "tools/build/tests/test_gameplay_successor.py",
    "tools/certification/authority/gameplay-owner-scope-v1.json",
    "tools/certification/authority/gameplay-independent-code-review-v4.json",
})


def relative(name):
    c.require(isinstance(name, str), 'V2 relative path type')
    p = PurePosixPath(name)
    c.require(isinstance(name, str) and name and p.as_posix() == name
              and not p.is_absolute() and '\\' not in name and ':' not in name
              and all(x not in ('', '.', '..') for x in name.split('/')),
              'V2 unsafe relative path')
    return name


def archive_originals(raw, entries):
    """Validate every member before writing anything; never extractall()."""
    c.require(len(raw) <= MAX_ARCHIVE, 'V2 oversized archive')
    c.require(len(entries) == 32 and sum(x['bytes'] for x in entries.values()) <= MAX_UNCOMPRESSED,
              'V2 predecessor archive bounds')
    for name, row in entries.items():
        relative(name)
        c.require(set(row) == {'sha256', 'bytes'} and 0 <= row['bytes'] <= MAX_UNCOMPRESSED,
                  'V2 archive entry schema')
    c.require(len({x.casefold() for x in entries}) == len(entries), 'V2 archive case aliases')
    result = {}
    try:
        with zipfile.ZipFile(io.BytesIO(raw)) as z:
            infos = z.infolist()
            c.require(len(infos) == len(entries), 'V2 archive cardinality')
            for info in infos:
                c.require(info.orig_filename == info.filename, 'V2 normalized archive filename')
                name = relative(info.filename)
                c.require(name in entries and name not in result, 'V2 extra/duplicate archive member')
                row = entries[name]
                mode = info.external_attr >> 16
                c.require(not info.is_dir() and stat.S_IFMT(mode) in (0, stat.S_IFREG)
                          and not info.flag_bits & 1 and info.file_size == row['bytes']
                          and info.compress_type == zipfile.ZIP_STORED,
                          'V2 nonregular/encrypted/compressed archive member')
                data = z.read(info)
                c.require(len(data) == row['bytes'] and c.sha(data) == row['sha256'],
                          'V2 predecessor original bytes')
                result[name] = data
    except (zipfile.BadZipFile, RuntimeError, OSError) as error:
        raise ValueError('V2 invalid predecessor ZIP') from error
    c.require(set(result) == set(entries), 'V2 missing archive members')
    return result


def production(source):
    observed = {}
    for directory in PRODUCTION_ROOTS:
        for p in (source / directory).rglob('*'):
            c._no_links(p)
            if p.is_file():
                name = p.relative_to(source).as_posix()
                observed[name] = c.sha(c.safe_file(source, name).read_bytes())
    return observed


def predecessor_closure(source, ui07):
    raw = c.safe_file(source, V1_PATH).read_bytes()
    c.require(c.sha(raw) == V1_SHA, 'V2 immutable V1 manifest')
    old = c.strict_json(raw)
    c.require(old['oldGateA'] == c.policy(source)['gateAManifestSHA256']
              and old['oldOwnerB'] == c.OWNER_B_CAPSULE_SHA256
              and old['oldUI07'] == c.UI07_SUCCESSOR_SHA256, 'V2 V1 ancestor anchors')
    result = dict(ui07)
    for name, row in old['changes'].items():
        if name.startswith(('src/main/', 'port/AchieveToDo-Purpur-26.2/src/main/')):
            c.require(result.get(name) == row['beforeSHA256'], 'V2 V1 predecessor delta')
            result[name] = row['afterSHA256']
    c.require(len(result) == 2500, 'V2 V1 closure cardinality')
    return old, result


def require_legacy_result(run):
    c.require(run.returncode == 0 and 'Ran 4 tests' in run.stderr
              and '\nOK\n' in run.stderr and 'skipped=' not in run.stderr,
              'V2 unchanged four V1 controls failed: ' + run.stdout + run.stderr)


def legacy_controls(source, env, manifest, old, expected, originals):
    """Reconstruct exact V1 context and execute its unchanged four controls."""
    context = manifest['predecessorContext']
    required = set(expected) | set(old['changes']) | set(old['codeBindings']) | {
        V1_PATH, c.POLICY_PATH, c.GATE_A_MANIFEST, c.UI07_MANIFEST,
        'tools/build/distribution_b_v2_contract.py',
    }
    c.require(set(context) == required, 'V2 exact predecessor control context')
    c.require(all(context[p] == digest for p, digest in expected.items()), 'V2 predecessor production context')
    c.require(all(context[p] == digest for p, digest in old['codeBindings'].items()), 'V2 predecessor control identities')
    for p, row in old['changes'].items():
        c.require(context[p] == row['afterSHA256'], 'V2 predecessor auxiliary identities')
    c.require(context[V1_PATH] == V1_SHA, 'V2 predecessor manifest context')
    c.require(set(originals) <= required, 'V2 unused archive originals')
    with tempfile.TemporaryDirectory(prefix='atd-authentic-v1-') as tmp:
        root = Path(tmp)
        for name, digest in context.items():
            relative(name)
            data = originals[name] if name in originals else c.safe_file(source, name).read_bytes()
            c.require(c.sha(data) == digest, 'V2 exact predecessor reconstruction: ' + name)
            dest = root / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(data)
        c.require(production(root) == expected, 'V2 reconstructed full V1 closure')
        clean = dict(env)
        clean[SELECTOR] = 'ATD_PURPUR_GAMEPLAY_SOURCE_V1'
        clean[FILE] = str(root / V1_PATH)
        clean['PYTHONDONTWRITEBYTECODE'] = '1'
        clean.pop('PYTHONPATH', None)
        run = subprocess.run([sys.executable, '-B', '-X', 'utf8', '-m', 'unittest',
                              'tools.build.tests.test_gameplay_successor', '-v'],
                             cwd=root, env=clean, capture_output=True, text=True, timeout=300)
        require_legacy_result(run)


def validate(source, env, expected_digest, ui07):
    source = Path(source).absolute()
    c.require(env.get(SELECTOR) == AUTHORITY and env.get(FILE) == str(source / PATH),
              'V2 unknown/orphan/path selector')
    c.require(env.get('ACHIEVETODO_UI07_AUTHORITY') == 'ATD_UI07_SCOPED_SUCCESSOR_V1'
              and env.get(c.SELECTOR) == c.AUTHORITY, 'V2 predecessor selectors')
    raw = c.safe_file(source, PATH).read_bytes()
    c.require(c.sha(raw) == expected_digest, 'V2 manifest digest')
    m = c.strict_json(raw)
    c.require(set(m) == {'schema', 'authority', 'state', 'oldGateA', 'oldOwnerB', 'oldUI07',
                        'oldV1', 'allGameplayChanges', 'incrementalProductionChanges',
                        'codeBindings', 'independentReview', 'fixture', 'archiveEntries',
                        'predecessorContext'}, 'V2 exact manifest schema')
    c.require(m['schema'] == 2 and m['authority'] == AUTHORITY
              and m['state'] == 'AUTHORIZED_BOUNDED_SOURCE_FOR_CERTIFICATION_ONLY', 'V2 state/scope')
    c.require(m['oldGateA'] == c.policy(source)['gateAManifestSHA256']
              and m['oldOwnerB'] == c.OWNER_B_CAPSULE_SHA256 and m['oldUI07'] == c.UI07_SUCCESSOR_SHA256
              and m['oldV1'] == V1_SHA, 'V2 predecessor anchors')
    c.require(set(m['allGameplayChanges']) == ALLOWED and set(m['codeBindings']) == CODE_PATHS,
              'V2 exact gameplay and authority auxiliary allowlists')
    for name, digest in m['codeBindings'].items():
        c.require(c.sha(c.safe_file(source, relative(name)).read_bytes()) == digest, 'V2 code identity: ' + name)
    review_ref = m['independentReview']
    c.require(review_ref['path'] == 'tools/certification/authority/gameplay-independent-code-review-v7.json'
              and m['codeBindings'].get(review_ref['path']) == review_ref['sha256'], 'V2 review binding')
    review = c.strict_json(c.safe_file(source, review_ref['path']).read_bytes())
    c.require(review.get('schema') == 1
              and review.get('reviewer') == 'independent agent /root/independent_v7_review'
              and review.get('verdict') == 'CODE_REVIEW_PASS_WITH_RUNTIME_CERTIFICATION_REQUIRED'
              and review.get('unresolvedSourceFindingCount') == 0
              and review.get('candidateMutationsByReviewer') is False
              and review.get('PRODUCT_GAMEPLAY_READY') is False
              and review.get('runtimeAccepted') is False
              and review.get('primaryIntegrationAccepted') is False
              and review.get('finalSourceAuthorityImplementationAccepted') is False,
              'V2 actual independent source acceptance required')
    reviewed = {r['path']: r['observedSHA256'] for r in review['sourceBindings']}
    c.require(len(reviewed) == len(review['sourceBindings'])
              and reviewed == {p: r['afterSHA256'] for p, r in m['allGameplayChanges'].items()},
              'V2 exact reviewed gameplay paths')
    c.require(all(r['matches'] is True and r['expectedSHA256'] == r['observedSHA256']
                  and r['beforeSHA256'] == m['allGameplayChanges'][r['path']]['beforeSHA256']
                  for r in review['sourceBindings']), 'V2 reviewed before and after identities')
    old, previous = predecessor_closure(source, ui07)
    for name, digest in old['codeBindings'].items():
        if name != 'src/test/java/com/diskree/achievetodo/certification/GameplaySuccessorAuthorityTest.java':
            c.require(m['codeBindings'].get(name) == digest, 'V2 immutable V1 code identity')
    result = dict(previous)
    for name, row in m['incrementalProductionChanges'].items():
        c.require(set(row) == {'beforeSHA256', 'afterSHA256'}
                  and row['beforeSHA256'] != row['afterSHA256']
                  and previous.get(name) == row['beforeSHA256'], 'V2 exact incremental delta')
        c.require(name.startswith(('src/main/', 'port/AchieveToDo-Purpur-26.2/src/main/')),
                  'V2 production-only incremental delta')
        result[name] = row['afterSHA256']
    c.require(production(source) == result, 'V2 full current production closure')
    c.require(review['productionClosure'] == result and review['productionFileCount'] == len(result),
              'V2 independently reviewed full production closure')
    derived_changes = {p: {'beforeSHA256': ui07.get(p), 'afterSHA256': digest}
                       for p, digest in result.items() if ui07.get(p) != digest}
    declared_changes = {p: row for p, row in m['allGameplayChanges'].items()
                        if p.startswith(('src/main/', 'port/AchieveToDo-Purpur-26.2/src/main/'))}
    c.require(declared_changes == derived_changes, 'V2 complete UI07 production deltas')
    for name, row in m['allGameplayChanges'].items():
        c.require(set(row) == {'beforeSHA256', 'afterSHA256'}
                  and row['beforeSHA256'] != row['afterSHA256'], 'V2 gameplay delta schema')
        c.require(c.sha(c.safe_file(source, name).read_bytes()) == row['afterSHA256'], 'V2 gameplay after bytes')
        if name.startswith(('src/main/', 'port/AchieveToDo-Purpur-26.2/src/main/')):
            c.require(ui07.get(name) == row['beforeSHA256'], 'V2 original UI07 production identity')
        else:
            c.require(row['beforeSHA256'] == AUX_BEFORE[name], 'V2 original auxiliary identity')
    c.require(m['fixture']['path'] == FIXTURE and m['codeBindings'].get(FIXTURE) == m['fixture']['sha256'],
              'V2 fixture binding')
    originals = archive_originals(c.safe_file(source, FIXTURE).read_bytes(), m['archiveEntries'])
    legacy_controls(source, env, m, old, previous, originals)
    return result, sorted(set(m['allGameplayChanges']) | set(m['codeBindings']) | {PATH})
