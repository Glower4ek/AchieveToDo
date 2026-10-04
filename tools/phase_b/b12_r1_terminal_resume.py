"""Read-only B12-R1 evidence verifier; never stages, packages, or repairs state.

Self-tests are pure in-memory tests. Normal verification reads the original B12
byte authority and a separately recorded start-of-R1 snapshot. No file is written
by this tool; stdout may be captured only in build/tmp/phase_b_b12_r1/.
"""
import argparse
import ast
import collections
import hashlib
import json
import pathlib
import re
import subprocess
import unittest

ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p / '.git').exists())
TEMP = ROOT / 'build/tmp/phase_b_b12_r1'
HEAD = 'b261b02cd03b4aeae2835c63f982c9aa53c46eed'
BRANCH = 'phase-b-bacap-26.2'
SUCCESS = 'PHASE_B_B12_TERMINALLY_RECONCILED'
REASON = 'NON_TEST_ASSEMBLY_BLOCKED_BY_EXISTING_PUBLISHED_VERSION_JAR_GUARD'
ALLOW = (
    'tools/phase_b/b12_r1_terminal_resume.py',
    'reference/phase_b/b12_terminal_reconciliation_resumed.json',
    'reference/phase_b/B12_TERMINAL_RECONCILIATION_RESUMED.md',
    'reference/phase_b/b12_milestone_manifest_resumed.json',
)
ORIGINAL = {
    'tools/phase_b/b12_terminal_reconciliation.py': 'cc3ebdb951a1382539c0e1350084bf19ec4d36143bec60946d6e8f1ef9811fd4',
    'reference/phase_b/b12_terminal_reconciliation.json': 'c0476c21beaa3941af0dfab1b506fb78ec8cb539f673e95c533985c1e9aac94b',
    'reference/phase_b/B12_TERMINAL_RECONCILIATION.md': '9dd41b5349466064987dea93a995c72b6ea02dd9c4ae59bb94b725b2c75bf924',
    'reference/phase_b/b12_product_ledger.json': 'c75b3cff8e80fd25f4bf94942145e1705b0eaeb13c7d04f9e277ee9668915038',
    'reference/phase_b/b12_milestone_manifest.json': '25d0320b11df842533434c88d40b25bdb54a6e195ca9710aa3bbccc268cca215',
}
PROTECTED = 'build/tmp/phase_b_b12/preflight/protected_files.json'
PROTECTED_SHA = '2286ce26676abffc23ab26ab6b7af7185161b2362a6c25b81146eb4223d597e6'
START_SHA = '8f02c53e0cd6907e4012d020f08b1781ec67d00fdcf48e114956ddb7a4b70296'
CATEGORIES_OLD = {'RUNTIME_PRODUCT': 255, 'LOCALIZATION_OR_LICENSE': 1, 'CURRENT_TEST': 8,
                  'GAMETEST_RUNTIME_CERTIFICATION': 9, 'PHASE_B_TOOL': 7, 'PHASE_B_EVIDENCE': 65}
CATEGORIES_NEW = {**CATEGORIES_OLD, 'PHASE_B_TOOL': 8, 'PHASE_B_EVIDENCE': 68}
FOLDERS = ('reference', 'licenses', 'tools/phase_b', 'src/main', 'src/test', 'src/gametest')


def require(condition, reason):
    if not condition:
        raise AssertionError(reason)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def file_sha(path):
    return sha((ROOT / path).read_bytes())


def load(path):
    return json.loads((ROOT / path).read_text(encoding='utf8'))


def git(*args):
    # Only callers below supply fixed read-only Git commands.
    return subprocess.check_output(['git', *args], cwd=ROOT, stderr=subprocess.PIPE)


def safe_path(path):
    require(isinstance(path, str) and path and '\\' not in path and ':' not in path, 'Unsafe path')
    parts = pathlib.PurePosixPath(path).parts
    require(not pathlib.PurePosixPath(path).is_absolute() and '..' not in parts, 'Unsafe path')
    forbidden = {'build', '.gradle', '.gradle-user-home', '.cache', '__pycache__', '.git',
                 '.idea', '.vscode', 'node_modules', 'attachments'}
    require(not set(parts) & forbidden, 'Generated/cache path: ' + path)
    require(not path.endswith(('.zip', '.pyc', '.class', '.tmp', '.log')), 'Non-durable path')


def parse_status(data):
    result = {}
    for token in filter(None, data.decode('utf8').split('\0')):
        code, path = token[:2], token[3:]
        require(code in ('??', ' M', ' D'), 'Unsupported or staged Git status: ' + token)
        safe_path(path)
        require(path not in result, 'Duplicate Git path')
        result[path] = {'??': 'ADD', ' M': 'MODIFY', ' D': 'DELETE'}[code]
    return result


def preflight():
    require(pathlib.Path(git('rev-parse', '--show-toplevel').decode().strip()).resolve() == ROOT, 'Repository root')
    require(git('branch', '--show-current').decode().strip() == BRANCH, 'Branch')
    require(git('rev-parse', 'HEAD').decode().strip() == HEAD, 'HEAD')
    require(not git('diff', '--cached', '--name-only').strip(), 'Staged paths')
    require(not git('ls-files', '-u').strip(), 'Conflicted paths')
    gd = pathlib.Path(git('rev-parse', '--git-dir').decode().strip())
    if not gd.is_absolute():
        gd = ROOT / gd
    operations = [n for n in ('MERGE_HEAD', 'CHERRY_PICK_HEAD', 'REVERT_HEAD',
                              'rebase-merge', 'rebase-apply', 'sequencer') if (gd / n).exists()]
    require(not operations, 'Active Git operation: ' + repr(operations))
    return {'branch': BRANCH, 'HEAD': HEAD, 'stagedPaths': [], 'activeOperations': [],
            'unmergedPaths': [], 'result': 'PASS'}


def partition(manifest):
    rows = [(p, key.upper()) for key in ('add', 'modify', 'delete') for p in manifest[key]]
    require(len(rows) == len({p for p, a in rows}), 'Duplicate/overlapping manifest paths')
    for p, a in rows:
        safe_path(p)
    return dict(rows)


def validate_manifest(manifest, actual, counts, categories):
    rows = partition(manifest)
    require(rows == actual, 'Exact path/status mismatch')
    require(dict(collections.Counter(rows.values())) == counts == manifest['counts'], 'Action counts')
    if isinstance(manifest['paths'], int):
        require(manifest['paths'] == len(rows), 'Manifest paths')
    else:
        require(set(manifest['paths']) == set(rows) and len(manifest['paths']) == len(rows), 'Manifest paths')
    entries = manifest['classifications']
    if isinstance(entries, list):
        require(len(entries) == len({r['path'] for r in entries}), 'Duplicate classification')
        require({r['path']: r['action'] for r in entries} == rows, 'Classification action coverage')
        classes = {r['path']: r['category'] for r in entries}
    else:
        classes = entries
    require(set(classes) == set(rows), 'Classification path coverage')
    require(dict(collections.Counter(classes.values())) == categories
            == manifest['categoryCounts'], 'Classification counts')
    require(manifest['generatedArtifactsIncluded'] is False, 'Generated artifacts included')


def staging_eligible(manifest):
    require(manifest['status'] == SUCCESS and manifest['readyForB13'] is True
            and manifest['stagingAuthorized'] is True and not manifest.get('blockers'), 'Blocked staging authority')
    require(manifest['packagingStatus'] == 'DEFERRED_UNTIL_EXPLICIT_VERSION_BUMP'
            and manifest['postGuardPackagingCertified'] is False
            and manifest['publicationReady'] is False and manifest['releaseAuthorized'] is False,
            'Uncertified packaging/release boundary')


def state(path):
    p = ROOT / path
    if not p.exists():
        return {'state': 'ABSENT'}
    require(p.is_file(), 'Non-file protected path: ' + path)
    return {'state': 'PRESENT', 'sha256': file_sha(path), 'bytes': p.stat().st_size}


def compare_records(expected, actual):
    require(expected == actual, 'Protected bytes/state differ: ' + repr(sorted(
        p for p in set(expected) | set(actual) if expected.get(p) != actual.get(p))))


def current_protected_paths():
    result = set()
    for args in (('ls-files', '-z'), ('ls-files', '--others', '--exclude-standard', '-z')):
        result.update(p.decode('utf8') for p in git(*args).split(b'\0') if p)
    for folder in FOLDERS:
        result.update(p.relative_to(ROOT).as_posix() for p in (ROOT / folder).rglob('*')
                      if p.is_file() and '__pycache__' not in p.parts)
    return result


def guard_contract(current, baseline, version, jar_exists, receipt):
    require(current.replace('\r\n', '\n') == baseline.replace('\r\n', '\n'), 'Build source drift')
    guard = re.search(r'jar\s*\{\s*doFirst\s*\{\s*if\s*\(modVersion ==~ /\\d\+\\\.\\d\+\\\.\\d\+\\\.\\d\+/ && archiveFile\.get\(\)\.asFile\.exists\(\)\)\s*\{\s*throw new GradleException\("Smoke candidate \$\{modVersion\} already exists; increment the final modVersion component before producing a changed JAR\."\)', current)
    require(guard is not None, 'Publication guard not found in baseline/current source')
    require(version == '0.1.5.4' and re.fullmatch(r'\d+\.\d+\.\d+\.\d+', version) and jar_exists, 'Guard preconditions')
    require(receipt['failedTask'] == ':jar' and receipt['exitCode'] == 1
            and receipt['message'] == 'Smoke candidate 0.1.5.4 already exists; increment the final modVersion component before producing a changed JAR.', 'Observed guard receipt')
    return {'assembleGate': 'NOT_APPLICABLE_TO_UNVERSIONED_MILESTONE_COMMIT',
            'observedAssembleResult': 'EXPECTED_PUBLICATION_GUARD_BLOCK',
            'productionRegressionDetectedByCertifiedNonPackagingGates': False,
            'postGuardPackagingCertified': False, 'packagingDeferred': True,
            'releaseVersionDecisionDeferred': True, 'modVersion': version,
            'guardProvenance': 'Actual current build.gradle equals baseline HEAD:build.gradle under Git newline transport; exact jar.doFirst guard matched',
            'guardSourceSha256': sha(guard.group().encode('utf8')),
            'guardChangedDuringPhaseB': False, 'guardBypassed': False,
            'existingCandidateDeletedOrMovedByB12R1': False}


def carried_gates(b12):
    tests = b12['testResults']
    for name, count in [('b7_localization_certification', 8), ('b8_bacap_static_certification', 14),
                        ('b9_regression_certification', 8), ('b11_manual_advancement_review', 13)]:
        v = tests[name]
        require((v['tests'], v['passed'], v['failures'], v['errors'], v['skipped']) == (count, count, 0, 0, 0), name)
        require(file_sha('tools/phase_b/' + name + '.py') == v['sourceSha256'], name + ' source identity')
    for name, count in [('b8_junit', 15), ('b9_junit', 23)]:
        v = tests[name]
        require((v['tests'], v['failures'], v['errors'], v['skipped']) == (count, 0, 0, 0), name)
    full = tests['fullSuite']
    require((full['tests'], full['failures'], full['errors'], full['skipped']) == (429, 11, 0, 0), 'Full suite')
    require(len(full['failureNames']) == len(set(full['failureNames'])) == 11, 'Full failure identity')
    classes = collections.Counter(r['classification'] for r in full['failureRecords'])
    require(classes == {'PRE_EXISTING_ACCEPTED_BASELINE_FAILURE': 4,
                        'EXPECTED_PHASE_B_HISTORICAL_ORACLE_STALENESS': 7}, 'Full failure partition')
    expected = b12['regressionCertification']['finalFullSuite']
    require(full['failureNames'] == expected['failureNames'], 'B9 failure names')
    require({r['name']: r['messageSha256'] for r in full['failureRecords']}
            == {r['name']: r['messageSha256'] for r in expected['failureRecords']}, 'B9 failure messages')
    require(tests['compileResourceGametestSurface']['result'] == 'GREEN'
            and tests['compileResourceGametestSurface']['exitCode'] == 0, 'Non-packaging build')
    require((tests['b12Tool']['tests'], tests['b12Tool']['passed'], tests['b12Tool']['failed'],
             tests['b12Tool']['errors']) == (17, 17, 0, 0), 'Original B12 tests')
    return {'mode': 'CARRIED_FORWARD_NOT_RERUN', 'authorityPath': 'reference/phase_b/b12_terminal_reconciliation.json',
            'authoritySha256': ORIGINAL['reference/phase_b/b12_terminal_reconciliation.json'],
            'protectedManifestSha256': PROTECTED_SHA, 'relevantInputsByteIdentical': True,
            'results': {k: v for k, v in tests.items() if k != 'assemble'},
            'fullFailurePartition': dict(classes), 'newFailureNames': [], 'currentRegressions': 0}


def verify_core():
    pre = preflight()
    for p, h in ORIGINAL.items():
        require(file_sha(p) == h, 'Original B12 hash: ' + p)
    b12 = load('reference/phase_b/b12_terminal_reconciliation.json')
    require(b12['status'] == 'PHASE_B_B12_STOP' and b12['readyForB13'] is False
            and b12['stopReason'] == REASON, 'Original STOP')
    old = load('reference/phase_b/b12_milestone_manifest.json')
    require(old['status'] == 'PHASE_B_B12_STOP' and old['readyForB13'] is False
            and old['stagingAuthorized'] is False, 'Old manifest must remain blocked')
    require(file_sha(PROTECTED) == PROTECTED_SHA, 'Historical protected manifest hash')
    locks = load(PROTECTED)
    require(len(locks) == 3595, 'Historical protected record count')
    for section in ('historicalPreservation', 'postB12Preservation'):
        require(b12[section]['protectedManifestSha256'] == PROTECTED_SHA, 'Historical manifest binding')
    compare_records(locks, {p: state(p) for p in locks})
    require(file_sha(TEMP / 'execution_start.json') == START_SHA, 'Start-of-R1 snapshot hash')
    start = load(TEMP / 'execution_start.json')
    require(start['noWritePreflightPassedBeforeSnapshot'] is True, 'No-write ordering proof')
    require(len(start['records']) == 3600, 'Execution start record count')
    compare_records(start['records'], {p: state(p) for p in start['records']})
    require(git('for-each-ref', '--format=%(refname) %(objectname)').decode() == start['gitRefs'], 'Git refs changed')
    for name, args in [('refs', ('for-each-ref', '--format=%(refname) %(objectname)')),
                       ('tag', ('rev-parse', 'v0.1.5.4')), ('publishedBranch', ('rev-parse', '26.2-port'))]:
        require(git(*args) == (ROOT / 'build/tmp/phase_b_b12/preflight' / (name + '.txt')).read_bytes(), 'B12 published Git identity: ' + name)
    actual = parse_status(git('status', '--porcelain=v1', '-uall', '-z'))
    original = partition(old)
    require(start['initialGitInventory'] == original, 'Start inventory must equal original B12 inventory')
    validate_manifest(old, original, {'ADD': 265, 'MODIFY': 71, 'DELETE': 9}, CATEGORIES_OLD)
    require({p: a for p, a in actual.items() if p not in ALLOW} == original, 'Pre-existing Git path/status drift')
    new = current_protected_paths() - set(start['records'])
    require(new <= set(ALLOW), 'Non-allowlisted persistent additions: ' + repr(sorted(new - set(ALLOW))))
    require(set(start['records']) - current_protected_paths() == set(), 'Pre-existing protected path disappeared')
    ledger = load('reference/phase_b/b12_product_ledger.json')
    product = {p: a for p, a in actual.items() if p.startswith('src/main/')}
    rows = ledger['records']
    require(len(rows) == len({r['path'] for r in rows}) == 255, 'Product duplicates/count')
    require(product == {r['path']: r['action'] for r in rows}, 'Product path/status identity')
    require(dict(collections.Counter(product.values())) == ledger['counts']
            == {'ADD': 177, 'MODIFY': 69, 'DELETE': 9}, 'Product actions')
    for row in rows:
        s = state(row['path'])
        require(s.get('sha256') == row['currentSha256']
                and s['state'] == ('PRESENT' if row['currentState'] == 'PRESENT' else 'ABSENT'), 'Product byte/state authority: ' + row['path'])
    require(ledger['b5HashModel'] == {'original': 251, 'exact': 249, 'superseded': 2, 'additionalPostB5': 4}, 'B5 model')
    authority = b12['criticalRuntimeHashes']
    for p, h in authority['hashes'].items():
        require(file_sha(p) == h, 'Critical current product authority: ' + p)
    converter = (ROOT / 'src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java').read_text(encoding='utf8')
    require('compat_26_2_r19' in converter and 'properties.setProperty("sourceSha1", actualSourceSha1)' in converter, 'Converter semantics')
    categories = re.findall(r'"([a-z_]+)"', converter.split('CATEGORY_ROOTS = {', 1)[1].split('};', 1)[0])
    root = hashlib.sha1()
    for category in categories:
        p = '/resourcepacks/bacap_override/data/bacap_rewards/function/' + category + '/root.mcfunction'
        root.update(p.encode()); root.update(b'\0'); root.update((ROOT / ('src/main/resources' + p)).read_bytes()); root.update(b'\n')
    require(root.hexdigest() == authority['rootOverrideSha1'] == 'cbc432be35d5525001872c430877541cfa1fcad6', 'Root digest')
    jar = b12['publicationPreservation']['publishedJar']
    require(jar['path'] == 'build/libs/achievetodo-mc26.2+0.1.5.4.jar', 'Recorded artifact path')
    require(state(jar['path']) == {'state': 'PRESENT', 'bytes': 1546595,
                                 'sha256': '0fc29fd0fbe1d874ca037ba2bd932f4e667593d95a1f2d3bb42936765710978b'}, 'Accepted JAR identity')
    require(jar['sha256'] == file_sha(jar['path']) and jar['size'] == 1546595, 'B12 JAR binding')
    receipt = b12['testResults']['assemble']
    log = ROOT / 'build/tmp/phase_b_b12/tests/assemble.log'
    require(sha(log.read_bytes()) == receipt['logSha256'] and receipt['message'] in log.read_text(encoding='utf8'), 'Original assemble log')
    version = re.search(r'^modVersion=(.+)$', (ROOT / 'gradle.properties').read_text(encoding='utf8'), re.M).group(1)
    assembly = guard_contract((ROOT / 'build.gradle').read_text(encoding='utf8'),
                              git('show', 'HEAD:build.gradle').decode('utf8'), version,
                              (ROOT / jar['path']).exists(), receipt)
    carried = carried_gates(b12)
    return {'preflight': pre, 'originalB12Hashes': ORIGINAL, 'originalStopReason': REASON,
            'preservationAuthority': {'path': PROTECTED, 'recordCount': 3595, 'sha256': PROTECTED_SHA,
                                      'boundByB12HistoricalPreservation': True, 'boundByB12PostPreservation': True,
                                      'checked': 3595, 'changed': []},
            'executionPreservation': {'snapshotPath': 'build/tmp/phase_b_b12_r1/execution_start.json',
                                     'snapshotSha256': START_SHA, 'recordsChecked': 3600,
                                     'changedPreExistingPaths': [], 'newPersistentPaths': sorted(new),
                                     'gitRefsUnchanged': True},
            'assemblyContract': assembly, 'publishedJar': {**jar, 'byteIdentical': True},
            'currentProductAuthority': authority, 'carriedForwardGates': carried,
            'productLedger': {'path': 'reference/phase_b/b12_product_ledger.json',
                              'sha256': ORIGINAL['reference/phase_b/b12_product_ledger.json'],
                              'counts': ledger['counts'], 'paths': 255, 'b5HashModel': ledger['b5HashModel']},
            'currentGitInventory': actual}


def validate_new_text(path):
    raw = (ROOT / path).read_bytes()
    text = raw.decode('utf8')
    require(not raw.startswith(b'\xef\xbb\xbf') and raw.endswith(b'\n'), 'UTF-8/final newline: ' + path)
    require(not re.search(r'^(<<<<<<< |=======\s*$|>>>>>>> )', text, re.M), 'Conflict markers: ' + path)
    if path.endswith('.json'):
        json.loads(text)
    if path.endswith('.py'):
        ast.parse(text)
    return {'path': path, 'sha256': sha(raw), 'validUtf8': True, 'finalNewline': True,
            'conflictMarkers': False, 'jsonParsed': path.endswith('.json')}


def verify_all():
    facts = verify_core()
    actual = facts['currentGitInventory']
    old = load('reference/phase_b/b12_milestone_manifest.json')
    expected = {**partition(old), **{p: 'ADD' for p in ALLOW}}
    require(actual == expected, 'Resumed status must equal original plus exactly four ADDs')
    require(facts['executionPreservation']['newPersistentPaths'] == sorted(ALLOW), 'Exactly four new persistent paths')
    new = load(ALLOW[3])
    validate_manifest(new, actual, {'ADD': 269, 'MODIFY': 71, 'DELETE': 9}, CATEGORIES_NEW)
    require(new['stage'] == 'B12-R1' and new['baselineHead'] == HEAD, 'Resumed manifest identity')
    staging_eligible(new)
    require(new['originalBlocker'] == REASON and new['resolutionClassification']
            == 'EXPECTED_PUBLICATION_GUARD_AT_UNCHANGED_RELEASE_VERSION', 'Narrow guard resolution')
    require(new['originalManifestSha256'] == ORIGINAL['reference/phase_b/b12_milestone_manifest.json'], 'Original manifest link')
    require([r for r in new['classifications'] if r['path'] not in ALLOW] == old['classifications'],
            'Original path classifications must remain exact')
    omitted = {ALLOW[1], ALLOW[3]}
    require(set(new['currentPathSha256']) == set(actual) - omitted, 'Exact hash coverage excluding explicit self-reference boundary')
    for p, h in new['currentPathSha256'].items():
        require(state(p).get('sha256') == h, 'Manifest byte drift: ' + p)
    resumed = load(ALLOW[1])
    require(resumed['status'] == SUCCESS and resumed['readyForB13'] is True, 'Resumed terminal status')
    require(resumed['assemblyContract'] == facts['assemblyContract'], 'Resumed packaging boundary')
    require(resumed['preservationAuthority'] == facts['preservationAuthority'], 'Resumed historical authority')
    require(resumed['currentProductAuthority'] == facts['currentProductAuthority'], 'Resumed product authority')
    require(resumed['historicalB12BytePreservationVerified'] is True
            and resumed['b12R1ExecutionPreservationVerified'] is True, 'Separate preservation proofs')
    require(resumed['runtimeProductDeltaDuringB12R1'] == {'ADD': 0, 'MODIFY': 0, 'DELETE': 0}, 'Runtime delta')
    require(resumed['finalCumulativeRuntimeDiff'] == {'ADD': 177, 'MODIFY': 69, 'DELETE': 9}
            and resumed['uniqueRuntimePaths'] == 255, 'Runtime totals')
    require(resumed['milestoneManifest']['sha256'] == file_sha(ALLOW[3]), 'Resumed manifest hash')
    require(resumed['publicationReady'] is False and resumed['releaseAuthorized'] is False
            and resumed['remoteMutationPerformedByB12R1'] is False
            and resumed['remoteStateIndependentlyReverified'] is False, 'Publication claims')
    require(resumed['executionPreservation'] == facts['executionPreservation'], 'Execution snapshot comparison')
    require(resumed['carriedForwardGates'] == facts['carriedForwardGates'], 'Carried gate authority')
    report = load(TEMP / 'self_tests.json')
    require(report['toolSha256'] == file_sha(ALLOW[0]) and report['tests'] == report['passed']
            and report['failed'] == report['errors'] == report['skipped'] == 0, 'Fresh tool tests')
    require(resumed['b12R1SelfTests'] == report, 'Tool test receipt')
    require(report['tests'] == len(unittest.defaultTestLoader.loadTestsFromTestCase(ContractTests)._tests),
            'Exact self-test method count')
    for section in ('advancementReconciliation', 'localizationReconciliation', 'staticCertification',
                    'regressionCertification', 'runtimeCertification', 'manualReviewCertification', 'upstreamAnomalies'):
        require(resumed[section] == load('reference/phase_b/b12_terminal_reconciliation.json')[section],
                'Carried-forward section changed: ' + section)
    check = subprocess.run(['git', 'diff', '--check'], cwd=ROOT, capture_output=True)
    require(check.returncode == 0, 'git diff --check')
    facts['newTextIntegrity'] = [validate_new_text(p) for p in ALLOW]
    facts['status'] = SUCCESS
    facts['readyForB13'] = True
    facts['stagingAuthorized'] = True
    facts['diffCheck'] = {'result': 'PASS', 'untrackedFilesCovered': False,
                          'separateFourFileTextIntegrity': 'PASS'}
    return facts


class ContractTests(unittest.TestCase):
    def manifest(self):
        return {'status': SUCCESS, 'readyForB13': True, 'stagingAuthorized': True,
                'blockers': [], 'packagingStatus': 'DEFERRED_UNTIL_EXPLICIT_VERSION_BUMP',
                'postGuardPackagingCertified': False, 'publicationReady': False, 'releaseAuthorized': False}

    def test_original_inventory_arithmetic(self):
        self.assertEqual(345, sum(CATEGORIES_OLD.values())); self.assertEqual(345, 265 + 71 + 9)

    def test_resumed_inventory_arithmetic(self):
        self.assertEqual(349, sum(CATEGORIES_NEW.values())); self.assertEqual(349, 269 + 71 + 9)

    def test_exact_four_new_paths(self):
        self.assertEqual(4, len(set(ALLOW))); self.assertEqual(349, 345 + len(ALLOW))

    def test_manifest_disjoint(self):
        self.assertEqual({'a': 'ADD', 'b': 'MODIFY', 'c': 'DELETE'}, partition({'add': ['a'], 'modify': ['b'], 'delete': ['c']}))

    def test_overlap_rejected(self):
        with self.assertRaises(AssertionError): partition({'add': ['a'], 'modify': ['a'], 'delete': []})

    def test_duplicate_rejected(self):
        with self.assertRaises(AssertionError): partition({'add': ['a', 'a'], 'modify': [], 'delete': []})

    def test_generated_paths_rejected(self):
        for path in ('build/tmp/a', 'src/main/generated/.cache/x', 'reference/a.zip', '.git/a', '../a', 'C:/a'):
            with self.subTest(path=path), self.assertRaises(AssertionError): safe_path(path)

    def test_old_blocked_manifest_ineligible(self):
        x = self.manifest(); x.update(status='PHASE_B_B12_STOP', readyForB13=False, stagingAuthorized=False)
        with self.assertRaises(AssertionError): staging_eligible(x)

    def test_new_manifest_future_authority(self):
        staging_eligible(self.manifest())

    def test_packaging_is_deferred(self):
        x = self.manifest(); x['packagingStatus'] = 'PACKAGED'
        with self.assertRaises(AssertionError): staging_eligible(x)

    def test_post_guard_cannot_be_certified(self):
        x = self.manifest(); x['postGuardPackagingCertified'] = True
        with self.assertRaises(AssertionError): staging_eligible(x)

    def test_release_unauthorized(self):
        x = self.manifest(); x['releaseAuthorized'] = True
        with self.assertRaises(AssertionError): staging_eligible(x)

    def test_publication_not_ready(self):
        x = self.manifest(); x['publicationReady'] = True
        with self.assertRaises(AssertionError): staging_eligible(x)

    def test_unexpected_statuses_rejected(self):
        for code in ('R ', ' C', ' T', 'UU', 'M ', 'AM'):
            with self.subTest(code=code), self.assertRaises(AssertionError): parse_status((code + ' a\0').encode())

    def test_exact_status_identity(self):
        self.assertEqual({'a': 'ADD', 'b': 'MODIFY', 'c': 'DELETE'}, parse_status(b'?? a\0 M b\0 D c\0'))

    def test_counts_equal_paths_different_rejected(self):
        x = {'add': ['a'], 'modify': [], 'delete': [], 'paths': ['a'], 'counts': {'ADD': 1},
             'classifications': {'a': 'PHASE_B_TOOL'}, 'categoryCounts': {'PHASE_B_TOOL': 1}, 'generatedArtifactsIncluded': False}
        with self.assertRaises(AssertionError): validate_manifest(x, {'b': 'ADD'}, {'ADD': 1}, {'PHASE_B_TOOL': 1})

    def test_path_status_mismatch_rejected(self):
        x = {'add': ['a'], 'modify': [], 'delete': []}
        with self.assertRaises(AssertionError): validate_manifest(x, {'a': 'MODIFY'}, {}, {})

    def test_counts_equal_bytes_different_rejected(self):
        with self.assertRaises(AssertionError): compare_records({'a': {'state': 'PRESENT', 'sha256': 'x', 'bytes': 3}}, {'a': {'state': 'PRESENT', 'sha256': 'y', 'bytes': 3}})

    def test_missing_byte_authority_rejected(self):
        with self.assertRaises(AssertionError): compare_records({'a': {'state': 'PRESENT'}}, {})

    def test_publication_guard_classification(self):
        source = 'jar { doFirst { if (modVersion ==~ /\\d+\\.\\d+\\.\\d+\\.\\d+/ && archiveFile.get().asFile.exists()) { throw new GradleException("Smoke candidate ${modVersion} already exists; increment the final modVersion component before producing a changed JAR.") } } }'
        receipt = {'failedTask': ':jar', 'exitCode': 1, 'message': 'Smoke candidate 0.1.5.4 already exists; increment the final modVersion component before producing a changed JAR.'}
        result = guard_contract(source, source, '0.1.5.4', True, receipt)
        self.assertEqual('EXPECTED_PUBLICATION_GUARD_BLOCK', result['observedAssembleResult'])
        self.assertFalse(result['postGuardPackagingCertified']); self.assertTrue(result['packagingDeferred'])
        with self.assertRaises(AssertionError): guard_contract(source, source, '0.1.5.4', False, receipt)
        with self.assertRaises(AssertionError): guard_contract(source, source + ' altered', '0.1.5.4', True, receipt)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        result = unittest.TextTestRunner(verbosity=2).run(unittest.defaultTestLoader.loadTestsFromTestCase(ContractTests))
        raise SystemExit(0 if result.wasSuccessful() else 1)
    facts = verify_all()
    print(json.dumps({'status': facts['status'], 'readyForB13': facts['readyForB13'],
                      'protectedB12': 3595, 'executionProtected': 3600,
                      'currentInventory': len(facts['currentGitInventory']),
                      'runtime': facts['productLedger'], 'packaging': facts['assemblyContract'],
                      'newTextIntegrity': facts['newTextIntegrity']}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
