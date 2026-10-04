"""Read-only terminal Phase B reconciliation. Outputs only ignored B12 candidates.

Publishing the four durable receipts is a separate audited operation. This tool
never stages files, modifies product state, or overwrites historical evidence.
"""
import argparse
import collections
import hashlib
import json
import pathlib
import re
import subprocess
import sys
import unittest

ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p / '.git').exists())
TEMP = ROOT / 'build/tmp/phase_b_b12'
PHASE = ROOT / 'reference/phase_b'
HEAD = 'b261b02cd03b4aeae2835c63f982c9aa53c46eed'
ALLOW = [
    'tools/phase_b/b12_terminal_reconciliation.py',
    'reference/phase_b/b12_terminal_reconciliation.json',
    'reference/phase_b/B12_TERMINAL_RECONCILIATION.md',
    'reference/phase_b/b12_product_ledger.json',
    'reference/phase_b/b12_milestone_manifest.json',
]
sys.path.insert(0, str(ROOT / 'tools/phase_b'))
import b3_bacap_semantic_diff as b3
import b7_localization_certification as b7
import b9_regression_certification as b9


def require(condition, reason):
    if not condition:
        raise AssertionError(reason)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def file_sha(path):
    return sha((ROOT / path).read_bytes())


def load(path):
    return json.loads(pathlib.Path(path).read_text(encoding='utf8'))


def write_temp(name, value):
    TEMP.mkdir(parents=True, exist_ok=True)
    (TEMP / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, stderr=subprocess.PIPE)


def safe_path(path):
    parts = pathlib.PurePosixPath(path).parts
    forbidden = {'build', '.gradle', '.gradle-user-home', '.cache', '__pycache__', '.idea', '.vscode', 'node_modules', '.git', 'attachments'}
    require(not set(parts) & forbidden, 'Generated/cache path: ' + path)
    require(not path.endswith(('.pyc', '.log', '.tmp', '.zip', '.class')), 'Ephemeral/archive path: ' + path)
    require(not pathlib.PurePosixPath(path).is_absolute() and '..' not in parts, 'Unsafe path: ' + path)


def path_partition(rows):
    names = [r['path'] for r in rows]
    require(len(names) == len(set(names)), 'Duplicate path')
    require(all(r['action'] in ['ADD', 'MODIFY', 'DELETE'] for r in rows), 'Unknown action')
    return dict(collections.Counter(r['action'] for r in rows))


def validate_manifest(manifest, actual):
    rows = [{'path': p, 'action': a.upper()} for a in ['add', 'modify', 'delete'] for p in manifest[a]]
    path_partition(rows)
    for r in rows:
        safe_path(r['path'])
    require({r['path']: r['action'] for r in rows} == actual, 'Manifest/status mismatch')


def require_staging_ready(manifest):
    """A complete inventory is not staging authorization after a B12 STOP."""
    require(manifest.get('status') == 'PHASE_B_B12_TERMINALLY_RECONCILED'
            and manifest.get('readyForB13') is True
            and manifest.get('stagingAuthorized') is True
            and not manifest.get('blockers'), 'B12 manifest is blocked; staging forbidden')


def validate_chain(chain):
    for left, right in zip(chain, chain[1:]):
        require(left['afterSha256'] == right['beforeSha256'], 'Broken supersession chain')


def git_inventory():
    require(git('branch', '--show-current').decode().strip() == 'phase-b-bacap-26.2', 'Branch')
    require(git('rev-parse', 'HEAD').decode().strip() == HEAD, 'HEAD')
    require(not git('diff', '--cached', '--name-only').strip(), 'Staged changes')
    tokens = [s.decode('utf8') for s in git('status', '--porcelain=v1', '-uall', '-z').split(b'\0') if s]
    result = {}
    for token in tokens:
        code, path = token[:2], token[3:]
        require(code in ['??', ' M', ' D'], 'Unsupported/staged/renamed status: ' + token)
        safe_path(path)
        require(path not in result, 'Duplicate status path')
        result[path] = {'??': 'ADD', ' M': 'MODIFY', ' D': 'DELETE'}[code]
    return result


def preservation():
    pre = TEMP / 'preflight'
    locks = load(pre / 'protected_files.json')
    changed = []
    for path, record in locks.items():
        present = (ROOT / path).is_file()
        if present != (record['state'] == 'PRESENT') or (present and file_sha(path) != record['sha256']):
            changed.append(path)
    require(not changed, 'Protected pre-existing bytes changed: ' + repr(changed))
    for name, args in [('refs', ['for-each-ref', '--format=%(refname) %(objectname)']), ('tag', ['rev-parse', 'v0.1.5.4']), ('publishedBranch', ['rev-parse', '26.2-port'])]:
        require(git(*args) == (pre / (name + '.txt')).read_bytes(), 'Published Git identity changed: ' + name)
    initial = {s.decode()[3:]: s.decode()[:2] for s in (pre / 'status_z.txt').read_bytes().split(b'\0') if s}
    actual = git_inventory()
    require(set(actual) - set(initial) <= set(ALLOW), 'Non-allowlisted persistent addition')
    require(set(initial) <= set(actual), 'Pre-existing status disappeared')
    state_codes = {'ADD': '??', 'MODIFY': ' M', 'DELETE': ' D'}
    require(all(state_codes[actual[p]] == c for p, c in initial.items()), 'Pre-existing Git status changed')
    # Explicitly cover ignored historical/reference files and generated sources
    # that were present in the initial protected snapshot as well.
    for folder in ['reference', 'licenses', 'tools/phase_b', 'src/main', 'src/test', 'src/gametest']:
        current = {p.relative_to(ROOT).as_posix() for p in (ROOT / folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts}
        old = {p for p, v in locks.items() if p.startswith(folder + '/') and v['state'] == 'PRESENT'}
        require(current - old <= set(ALLOW) and old <= current, 'Protected directory path drift: ' + folder)
    return {'checked': len(locks), 'changed': [], 'protectedManifestSha256': file_sha(pre.relative_to(ROOT) / 'protected_files.json'),
            'gitRefsUnchanged': True, 'v0_1_5_4Unchanged': True, 'published26_2PortUnchanged': True,
            'publicReleaseMutationPerformed': False, 'networkFetchPerformed': False}


def baseline_product_diff(inventory):
    baseline = load(TEMP / 'baseline_runtime_blobs.json')
    audited = load(TEMP / 'baseline_byte_transport_audit.json')
    require(not audited['nonLineEndingDifferences'], 'Unexpected byte transport change')
    require(len(audited['rawOnlyLineEndingPaths']) == 191 and len(audited['ignoredGeneratedPaths']) == 3, 'Checkout/generated scope drift')
    require(git('config', '--get', 'core.autocrlf').decode().strip() == 'true', 'Checkout EOL policy changed')
    for path in audited['rawOnlyLineEndingPaths']:
        original = git('cat-file', 'blob', baseline[path]['gitBlob'])
        require(original.replace(b'\r\n', b'\n') == (ROOT / path).read_bytes().replace(b'\r\n', b'\n'), path + ' transport drift')
        require(git('hash-object', '--path=' + path, path).decode().strip() == baseline[path]['gitBlob'], path + ' Git clean-filter mismatch')
    for path in audited['ignoredGeneratedPaths']:
        require(subprocess.run(['git', 'check-ignore', '-q', path], cwd=ROOT).returncode == 0, 'Generated input not ignored')
    product = {p: a for p, a in inventory.items() if p.startswith('src/main/')}
    direct = {}
    transport = set(audited['rawOnlyLineEndingPaths'])
    ignored = set(audited['ignoredGeneratedPaths'])
    current = {p.relative_to(ROOT).as_posix(): sha(p.read_bytes()) for p in (ROOT / 'src/main').rglob('*') if p.is_file()}
    for path in set(baseline) | set(current):
        if path in ignored or path in transport:
            continue
        if baseline.get(path, {}).get('sha256') != current.get(path):
            direct[path] = 'ADD' if path not in baseline else 'DELETE' if path not in current else 'MODIFY'
    require(direct == product, 'Direct Git/blob/file product diff mismatch')
    require(collections.Counter(product.values()) == {'ADD': 177, 'MODIFY': 69, 'DELETE': 9}, 'Runtime counts')
    return product, baseline, audited


def product_ledger(inventory):
    product, baseline, transport = baseline_product_diff(inventory)
    b5 = load(PHASE / 'b5_7_structural_reconciliation.json')
    records = b5['productOwnership']['records']
    require(len(records) == 251 and path_partition([{'path': r['path'], 'action': r['gitAction']} for r in records]) == {'ADD': 177, 'MODIFY': 65, 'DELETE': 9}, 'B5 checkpoint')
    old = {r['path']: r for r in records}
    require(set(product) == set(old) | set(b7.CURRENT_POST_B5_PATHS), '255-path authority model')
    require(len(b7.CURRENT_B5_SUPERSESSIONS) == 2, 'Explicit supersession set')
    repairs = [load(PHASE / n) for n in ['b7_r1_source_identity_marker_repair.json', 'b8_r1_terralith_player_predicate_repair.json']]
    r2 = load(PHASE / 'b8_r2_message_binding_repair.json')
    rows = []
    exact = 0
    for path, action in sorted(product.items()):
        current_hash = file_sha(path) if action != 'DELETE' else None
        chain = []
        if path in old:
            origin = old[path]['transaction']
            accepted = old[path]['currentSha256']
            latest = 'B5.7'
            if path in b7.CURRENT_B5_SUPERSESSIONS:
                require(current_hash == b7.CURRENT_B5_SUPERSESSIONS[path], 'Superseded authority: ' + path)
                if path == b9.CONVERTER:
                    chain = [{'stage': 'B5.2', 'afterSha256': accepted, 'marker': 'compat_26_2_r17'}]
                    for stage, evidence in zip(['B7-R1', 'B8-R1'], repairs):
                        chain.append({'stage': stage, **evidence['productionFile'], 'evidence': 'reference/phase_b/' + ('b7_r1_source_identity_marker_repair.json' if stage == 'B7-R1' else 'b8_r1_terralith_player_predicate_repair.json')})
                    latest = 'B8-R1'
                else:
                    change = next(r for r in r2['repairs'] if r['path'] == path)
                    chain = [{'stage': 'B5.4', 'afterSha256': accepted}, {'stage': 'B8-R2', 'beforeSha256': change['beforeSha256'], 'afterSha256': change['afterSha256']}]
                    latest = 'B8-R2'
                validate_chain(chain)
            else:
                require(current_hash == accepted, 'Unapproved B5 drift: ' + path)
                exact += 1
        else:
            require(current_hash == b7.CURRENT_POST_B5_PATHS[path], 'Post-B5 authority: ' + path)
            origin = latest = 'B6' if path == b7.RU_PATH else 'B8-R2'
        require((path in baseline) == (action != 'ADD'), 'Baseline action: ' + path)
        rows.append({'path': path, 'baselineState': 'PRESENT' if path in baseline else 'ABSENT',
                     'currentState': 'DELETED' if action == 'DELETE' else 'PRESENT', 'action': action,
                     'baselineSha256': baseline.get(path, {}).get('sha256'), 'currentSha256': current_hash,
                     'baselineGitBlob': baseline.get(path, {}).get('gitBlob'), 'originStage': origin,
                     'latestAuthorityStage': latest, 'supersessionChronology': chain,
                     'B5CheckpointAuthority': old.get(path, {}).get('acceptedHashEvidence')})
    require(exact == 249 and len(rows) == 255, '249 + 2 + 1 + 3')
    return {'phase': 'B', 'stage': 'B12', 'baselineHead': HEAD, 'paths': 255, 'counts': path_partition(rows),
            'historicalCheckpoint': {'path': 'reference/phase_b/b5_7_structural_reconciliation.json', 'sha256': file_sha('reference/phase_b/b5_7_structural_reconciliation.json')},
            'b5HashModel': {'original': 251, 'exact': exact, 'superseded': 2, 'additionalPostB5': 4},
            'baselineByteTransportAudit': transport, 'records': rows}


def status_chronology():
    names = ['b0_baseline', 'b1_official_bacap_pin', 'b2_atd_integration_map', 'b3_advancement_diff', 'b4_product_change_manifest',
             'b5_1_pins_pack_metadata', 'b5_2_converter_r17', 'b5_3_setup_resource_contracts',
             'b5_4_root_reward_messages', 'b5_4_naughtylus_contract_resolution', 'b5_4_benchmarking_contract_resolution', 'b5_4_root_reward_messages_resumed',
             'b5_5_trackers_gui', 'b5_6_companions', 'b5_6_unwanted_passenger_contract_resolution', 'b5_6_companions_resumed',
             'b5_7_structural_reconciliation', 'b6_ru_translation_manifest', 'b7_localization_certification',
             'b7_full_test_gate_resolution', 'b7_r1_source_identity_marker_repair', 'b7_post_r1_test_gate_resolution',
             'b7_localization_certification_resumed', 'b8_static_certification', 'b8_r1_terralith_player_predicate_repair',
             'b8_static_certification_post_r1_stop', 'b8_r2_message_binding_repair', 'b8_static_certification_resumed',
             'b9_regression_certification', 'b7_m1_current_successor_authority_refresh', 'b7_m2_current_consumer_set_refresh',
             'b9_regression_certification_resumed', 'b10_runtime_smoke', 'b10_c1_live_advancement_contract_refresh',
             'b10_d1_main_deep_runtime', 'b10_d2_companion_runtime', 'b10_runtime_smoke_resumed', 'b11_manual_advancement_review']
    rows = []
    for name in names:
        path = 'reference/phase_b/' + name + '.json'
        r = load(ROOT / path)
        require(r.get('phase') == 'B' and r.get('status'), 'Stage chronology: ' + path)
        rows.append({'path': path, 'sha256': file_sha(path), 'stage': r.get('stage'), 'status': r['status'],
                     'executionStatus': r.get('executionStatus'), 'stopReason': r.get('stopReason'), 'immutableHistory': True})
    require(sum('STOP' in r['status'] for r in rows) == 9, 'STOP chronology accounting')
    return rows


def source_and_review_facts():
    old_path = 'reference/phase_a_preservation/files/final/bacap.zip'
    new_path = "build/tmp/phase_b_b1/BlazeandCave's Advancements Pack 1.21.zip"
    old = b3.load_pack(ROOT / old_path, 'old', '8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70')
    target = b3.load_pack(ROOT / new_path, 'target', 'c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2')
    require(hashlib.sha1((ROOT / old_path).read_bytes()).hexdigest() == '45b8bb0076bbf5b92fde7dc9590c6686937abbc0', 'Old source SHA1')
    require(hashlib.sha1((ROOT / new_path).read_bytes()).hexdigest() == '14da3f07b5467e8b59ffc0253fd8212c938cd739', 'Target source SHA1')
    a, b = set(old['adv']), set(target['adv'])
    counts = {'old': len(a), 'target': len(b), 'common': len(a & b), 'oldOnly': len(a - b), 'targetOnly': len(b - a)}
    require(counts == {'old': 1229, 'target': 1332, 'common': 1222, 'oldOnly': 7, 'targetOnly': 110}, 'Advancement source sets')
    canonical = {'old': sum(r['canonical'] for r in old['adv'].values()), 'target': sum(r['canonical'] for r in target['adv'].values())}
    require(canonical == {'old': 1152, 'target': 1242}, 'Canonical source counts')
    diff = load(PHASE / 'b3_advancement_diff.json')
    classes = collections.Counter('BYTE_IDENTICAL' if old['adv'][i]['bytes'] == target['adv'][i]['bytes'] else 'SEMANTICALLY_IDENTICAL_BYTES_DIFFER' if old['adv'][i]['semanticSha256'] == target['adv'][i]['semanticSha256'] else 'SEMANTICALLY_CHANGED' for i in a & b)
    require(classes == {'BYTE_IDENTICAL': 594, 'SEMANTICALLY_IDENTICAL_BYTES_DIFFER': 4, 'SEMANTICALLY_CHANGED': 624}, 'Independent common classifications')
    review = load(PHASE / 'b11_manual_advancement_review.json')
    require(review['status'] == 'PHASE_B_B11_MANUAL_REVIEW_CERTIFIED' and review['readyForB12'], 'B11 handoff')
    records = review['reviewRecords']
    require(len(records) == len({r['resourceId'] for r in records}) == 734, 'B11 record identities')
    require(collections.Counter(r['manualVerdict'] for r in records) == collections.Counter({'PASS': 600, 'PASS_WITH_NOTE': 131, 'FLAG_UPSTREAM_ANOMALY': 3}), 'B11 verdicts')
    expected = (b - a) | {i for i in a & b if old['adv'][i]['semanticSha256'] != target['adv'][i]['semanticSha256']}
    require(expected == {r['resourceId'] for r in records}, 'B11 exact corpus')
    require(all(r['manualReason'] and target['adv'][r['resourceId']]['semanticSha256'] == r['targetSemanticSha256'] for r in records), 'B11 source fingerprints')
    for c in review['coverage'].values():
        if isinstance(c, dict) and 'required' in c:
            require(c['reviewed'] == c['required'], 'Manual coverage')
    mappings = review['removalRenameContext']
    require(len(mappings) == 7, 'Seven context mappings')
    for r in mappings:
        require(r['oldId'] not in b and r['newId'] in b and not r['currentExecutableOldIdReferences'] and not r['progressMigration'], 'Moved ID contract')
    anomalies = []
    for r in review['upstreamAnomalies']:
        require(r['nativeAndMainEffectiveMeaningMatch'] and not r['atdIntegrationIntentAmbiguous'], 'Ambiguous upstream anomaly')
        anomalies.append({**r, 'terminalClassification': 'UPSTREAM_NATIVE_ANOMALY_PRESERVED'})
    return {'sourceIdentity': review['sourceIdentity'], 'advancements': {**counts, **classes},
            'canonical': {**canonical, 'removed': 7, 'added': 97, 'newNonCanonical': 13, 'netGrowth': 90},
            'mappings': mappings, 'manualReview': {k: review[k] for k in ['status', 'reviewCorpus', 'coverage', 'verdictCounts', 'toolSelfTests', 'B9FilteredJUnit']}, 'upstreamAnomalies': anomalies}


def critical_facts():
    b9.precheck()
    converter = (ROOT / b9.CONVERTER).read_text(encoding='utf8')
    require('properties.setProperty("sourceSha1", actualSourceSha1)' in converter, 'Actual-source marker writer')
    categories = re.findall(r'"([a-z_]+)"', converter.split('CATEGORY_ROOTS = {', 1)[1].split('};', 1)[0])
    digest = hashlib.sha1()
    for category in categories:
        path = '/resourcepacks/bacap_override/data/bacap_rewards/function/' + category + '/root.mcfunction'
        digest.update(path.encode()); digest.update(b'\0'); digest.update((ROOT / ('src/main/resources' + path)).read_bytes()); digest.update(b'\n')
    require(digest.hexdigest() == b9.ROOT_SHA, 'Current root digest')
    return {'marker': b7.CURRENT_MARKER, 'rootOverrideSha1': digest.hexdigest(),
            'hashes': {**b7.CURRENT_B5_SUPERSESSIONS, **b7.CURRENT_POST_B5_PATHS}, 'actualSourceMarkerWriter': True}


def verify():
    inventory = git_inventory()
    before = preservation()
    ledger = product_ledger(inventory)
    facts = source_and_review_facts()
    facts['criticalRuntime'] = critical_facts()
    facts['chronology'] = status_chronology()
    facts['preservation'] = before
    facts['runtimeLedger'] = ledger
    facts['currentGitInventory'] = inventory
    write_temp('terminal_facts.json', facts)
    print(json.dumps({'runtime': ledger['counts'], 'runtimePaths': ledger['paths'], 'chronologyRecords': len(facts['chronology']), 'protectedFiles': before['checked']}, indent=2))
    return facts


class TerminalTests(unittest.TestCase):
    def test_255_runtime_arithmetic(self): self.assertEqual(255, 177 + 69 + 9)
    def test_runtime_partition(self): self.assertEqual({'ADD': 1, 'MODIFY': 1, 'DELETE': 1}, path_partition([{'path': str(i), 'action': a} for i, a in enumerate(['ADD', 'MODIFY', 'DELETE'])]))
    def test_current_authority_model(self): self.assertEqual(255, 249 + 2 + 1 + 3)
    def test_bacap_inventory_arithmetic(self): self.assertEqual((1229, 1332, 1222), (1222 + 7, 1222 + 110, 594 + 4 + 624))
    def test_canonical_arithmetic(self): self.assertEqual(1242, 1152 - 7 + 97)
    def test_current_localization_arithmetic(self): self.assertEqual((4301, 4290, 3482), (533 + 3432 + 325 + 11, 4301 - 11, 3186 + 211 + 21 + 64))
    def test_manual_verdict_arithmetic(self): self.assertEqual((734, 734), (110 + 624, 600 + 131 + 3))
    def test_duplicate_path_rejected(self):
        with self.assertRaises(AssertionError): path_partition([{'path': 'x', 'action': 'ADD'}, {'path': 'x', 'action': 'ADD'}])
    def test_manifest_action_overlap_rejected(self):
        with self.assertRaises(AssertionError): validate_manifest({'add': ['a'], 'modify': ['a'], 'delete': []}, {'a': 'ADD'})
    def test_exact_manifest_status(self): validate_manifest({'add': ['a'], 'modify': ['b'], 'delete': ['c']}, {'a': 'ADD', 'b': 'MODIFY', 'c': 'DELETE'})
    def test_missing_manifest_path_rejected(self):
        with self.assertRaises(AssertionError): validate_manifest({'add': ['a'], 'modify': [], 'delete': []}, {'a': 'ADD', 'b': 'ADD'})
    def test_generated_paths_rejected(self):
        for path in ['build/tmp/probe.json', '.gradle/x', '.gradle-user-home/x', 'a/__pycache__/b.pyc', '.idea/workspace.xml', 'a/runtime.log', 'archive.zip', '../x']:
            with self.subTest(path=path), self.assertRaises(AssertionError): safe_path(path)
    def test_valid_durable_path(self): safe_path('reference/phase_b/b12_product_ledger.json')
    def test_supersession_chain(self): validate_chain([{'afterSha256': 'r17'}, {'beforeSha256': 'r17', 'afterSha256': 'r18'}, {'beforeSha256': 'r18', 'afterSha256': 'r19'}])
    def test_broken_chain_rejected(self):
        with self.assertRaises(AssertionError): validate_chain([{'afterSha256': 'r17'}, {'beforeSha256': 'wrong', 'afterSha256': 'r19'}])
    def test_complete_but_blocked_manifest_cannot_stage(self):
        with self.assertRaises(AssertionError): require_staging_ready({'status': 'PHASE_B_B12_STOP', 'readyForB13': False, 'stagingAuthorized': False, 'blockers': ['assembly guard']})
    def test_staging_requires_all_exact_readiness_fields(self):
        require_staging_ready({'status': 'PHASE_B_B12_TERMINALLY_RECONCILED', 'readyForB13': True, 'stagingAuthorized': True, 'blockers': []})


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        unittest.main(argv=['b12_terminal_reconciliation'], verbosity=2)
    else:
        verify()
