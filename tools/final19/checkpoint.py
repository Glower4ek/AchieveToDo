"""FINAL19 durable state; historical artifacts are read-only inputs."""
import hashlib
import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
BASE = ROOT / 'reference/phase_a_planning/final19'
def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()
def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
def main():
    audit = json.loads((BASE / 'pre26_2_final19_audit.json').read_text(encoding='utf-8-sig'))
    master = json.loads((BASE / 'pre26_2_final19_master_map.json').read_text(encoding='utf-8-sig'))
    hashes = {'pre26_2_final19_audit.json':'357f8cfe218ab15cfd23608dfedd15cdda70e297ab4c533fe7ac76b9f04ed408',
              'pre26_2_final19_master_map.json':'221c0041f7668cbdcbe9e647a4a3aec2705b16a304fccfb30c4ff8881fd4b913',
              'pre26_2_final19_report.md':'10425ae261f3747279feb5b56cfb881998859341f35fc4ab357f2c2885f4064d'}
    for name, expected in hashes.items():
        assert sha(BASE / name) == expected, f'Planning drift: {name}'
    for name, expected in audit['baselineSha256'].items():
        assert sha(ROOT / name) == expected, f'Historical drift: {name}'
    preservation = BASE / 'implementation_preservation.json'
    if not preservation.exists():
        paths = set(ROOT.glob('src/test/resources/phase_a_certification/*'))
        paths.update(ROOT.glob('src/main/java/**/*.java'))
        paths.update(ROOT.glob('src/main/resources/**/*'))
        paths.update(ROOT / name for name in audit['baselineSha256'])
        paths.update([ROOT/'build.gradle', ROOT/'src/test/java/com/diskree/achievetodo/client/ExternalPackCompatibilityTest.java'])
        files = {p.relative_to(ROOT).as_posix():sha(p) for p in sorted(paths) if p.is_file()}
        write(preservation, {'files': files, 'head': subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
              'branch':subprocess.check_output(['git','branch','--show-current'],cwd=ROOT,text=True).strip(),
              'indexSha256':sha(ROOT/'.git/index')})
    statepath = BASE / 'execution_state.json'
    if not statepath.exists():
        state = {'task':'PRE_26_2_FINAL19_IMPLEMENTATION_LOOP','schemaVersion':1,'productCertified':1133,'denominator':1152,
          'historicalPhaseACertified':1133,'historicalPhaseAState':'PHASE_A_ACTIONABLE_FRONTIER_EXHAUSTED',
          'remainingTargets':[e['advancementId'] for e in audit['entries']],
          'completedFamilies':[], 'activeFamily':master['families'][0]['sharedRootCauseId'],
          'transactionStage':'DERIVE_EXACT_SEMANTICS','nextOperationalFamily':master['families'][0]['sharedRootCauseId'],
          'familyOrder':[f['sharedRootCauseId'] for f in master['families']], 'reorders':[],
          'persistentEvidenceInventory':[], 'foundationalFixState':{'F_CONTEXT':'PENDING','F_ACCOUNTING':'PENDING','F_NATIVE_LIFECYCLE':'PENDING'},
          'zeroGainRegressionDebt':{'blazeandcave:adventure/feeling_ill':'UNREPAIRED','blazeandcave:monsters/dungeon_crawler':'PENDING_REVALIDATION'},
          'raiderProductionAuthorization':'REQUIRED_AFTER_FAMILIES_1_THROUGH_5_GREEN','productionPatchApplied':False,
          'runtimeCopyFingerprints':[], 'converterSha256':sha(ROOT/'src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java'),
          'planningHashes':hashes,'historicalHashes':audit['baselineSha256'],'acceptedRunIds':[], 'gates':[],
          'firstUnfinishedTransaction':'WORLDGEN_HOLDERSET_CONTEXT: static context, canary, fresh exact bard, jungle validation, independent promotion, ledger'}
        for item in audit['currentRuntimeCopyInventory']:
            path = ROOT / item['path']
            if path.exists():state['runtimeCopyFingerprints'].append({'path':item['path'],'sha256':sha(path)})
        write(statepath,state)
    preserved = json.loads(preservation.read_text(encoding='utf-8'))
    raider_path='src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java'
    raider_auth=json.loads((BASE/'raider_authorized_application.json').read_text(encoding='utf-8'))
    assert raider_auth['id']=='AUTHORIZED_FINAL19_RAIDER_PRODUCTION_FIX'
    assert raider_auth['patchPath']=='reference/phase_a_planning/final19/raider_production_fix_proposal.patch'
    assert sha(ROOT/raider_auth['patchPath'])==raider_auth['patchSha256']=='518ebaae8587b6276610e02473fac985c12cd9431cf3c344bd31c08f48ddbdbe'
    assert raider_auth['authorizedProductionPaths']==[raider_path]
    assert raider_auth['patchApplied'] is True and raider_auth['certificationGain']==0
    assert raider_auth['freshnessProperty']=='raiderPredicateKeys=snake_case'
    assert preserved['files'][raider_path]==raider_auth['preconditions'][raider_path]=='dffeb7c2cf6685c5f689cf2a205b435b81da5b524971103804de3e3a6ae1dc11'
    assert sha(ROOT/raider_path)==raider_auth['postconditions'][raider_path]=='1bea3ba9caf22963d1ef26de69b19534a4b16d1eab85a39c5e1fc17750227a31'
    bridge=raider_auth.get('productionSemanticBridge')
    if bridge:
        assert bridge['path']=='reference/phase_a_planning/final19/raider_production_output_scope.json'
        assert sha(ROOT/bridge['path'])==bridge['sha256']=='e2b0ad1e98b119f4bfb78758f9497f3247dc0916b2fbcbbf559cf225f0c881a4'
        assert bridge['actualProductionConverter'] is True
        assert (bridge['changedDefinitions'],bridge['unchangedDefinitions'],bridge['allDefinitions'],bridge['canonicalDefinitions'])==(3,1226,1229,1152)
    cauldron_paths = {
        'src/main/resources/achievetodo.mixins.json':'56fcb41d75273382681cbeede45882a876de68babcd6580ae63d5c20c0ac211f',
        'src/main/java/com/diskree/achievetodo/injection/mixin/main/AbstractCauldronBlockAccessor.java':'63c46d242eeecabe946be353a8a2353a5ef6555d93982f8e0f8c6846ca4dfd2e',
        'src/main/java/com/diskree/achievetodo/injection/mixin/main/ServerPlayerGameModeCauldronGateMixin.java':'3f21c817198c59fd650c6384e5cc73bcda2b743ef252003b00ec8278d5193e78'}
    cauldron_auth = BASE/'cauldron_gate_authorized_application.json'
    assert cauldron_auth.exists(), 'Exact cauldron production authorization required'
    authorization = json.loads(cauldron_auth.read_text(encoding='utf-8'))
    assert authorization['id']=='AUTHORIZED_FINAL19_CAULDRON_GATE_PRODUCTION_FIX'
    assert authorization['patchPath']=='reference/phase_a_planning/final19/cauldron_gate_production_fix_proposal.patch'
    assert sha(ROOT/authorization['patchPath'])==authorization['patchSha256']=='ec33a350ef1ae9185ac99e1c2425a1cec38ca0d64244819058c3cc610a5a07fe'
    assert set(authorization['authorizedProductionPaths'])==set(cauldron_paths)
    assert authorization['patchApplied'] is True and authorization['certificationGain']==0
    config='src/main/resources/achievetodo.mixins.json'
    assert preserved['files'][config]==authorization['preconditions'][config]=='ed9368d4cf34f18e2d72b47f764ee1c4674418eff82b7d57fe4a1d6825380339'
    for name, expected in cauldron_paths.items():
        assert sha(ROOT/name)==authorization['postconditions'][name]==expected, f'Authorized cauldron drift: {name}'
        if name != config:
            assert name not in preserved['files'] and authorization['preconditions'][name]=='ABSENT'
    for name, expected in {
        'src/main/java/com/diskree/achievetodo/injection/mixin/main/AbstractCauldronBlockMixin.java':'c9473d4918c8d31c7215dcb98a388f3a24660b36940940a598861e9d90985394',
        'src/main/java/com/diskree/achievetodo/injection/mixin/main/AxeItemMixin.java':'4247ef44ce7e331f43820d889ecef541f8cbb0a200a31ede2764c8ecbdd46a13'}.items():
        assert sha(ROOT/name)==authorization['preconditions'][name]==authorization['postconditions'][name]==expected
    # New production paths are also bounded; an unreviewed new source cannot evade preservation.
    actual_production={p.relative_to(ROOT).as_posix() for parent in ('src/main/java','src/main/resources') for p in (ROOT/parent).rglob('*') if p.is_file()}
    preserved_production={p for p in preserved['files'] if p.startswith(('src/main/java/','src/main/resources/'))}
    assert actual_production==preserved_production | set(cauldron_paths), 'Unrelated production path drift'
    for name, expected in preserved['files'].items():
        if name == raider_path:
            assert expected==raider_auth['preconditions'][name]
            assert sha(ROOT/name)==raider_auth['postconditions'][name]
        elif name == config:
            assert sha(ROOT/name)==cauldron_paths[name]
        elif name == 'src/main/java/com/diskree/achievetodo/injection/mixin/main/AxeItemMixin.java' and (BASE/'wax_axe_gate_authorized_application.json').exists():
            # Explicit human authorization, bounded to the exact reviewed patch and original bytes.
            authorization=json.loads((BASE/'wax_axe_gate_authorized_application.json').read_text(encoding='utf-8'))
            assert authorization['id']=='AUTHORIZED_FINAL19_WAX_AXE_GATE_PRODUCTION_FIX'
            assert expected==authorization['preSha256']=='e19c4a65da4df917424a44ff00248220f20dcab57d0668b9ebfbebd01fabbb46'
            assert sha(BASE/'wax_axe_gate_production_fix_proposal.patch')==authorization['patchSha256']=='8aa346920a42de51dc8ce8756d6fd212d5d055709c633e07ce8e223e7a49f1a3'
            assert sha(ROOT/name)==authorization['postSha256']=='4247ef44ce7e331f43820d889ecef541f8cbb0a200a31ede2764c8ecbdd46a13'
        else:
            assert sha(ROOT / name) == expected, f'Preserved file drift: {name}'
    assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip() == preserved['head']
    assert subprocess.check_output(['git','branch','--show-current'],cwd=ROOT,text=True).strip() == preserved['branch']
    assert sha(ROOT/'.git/index') == preserved['indexSha256'], 'Git index drift'
    if not (BASE/'product_ledger.json').exists():
        ledger=json.loads((ROOT/'src/test/resources/phase_a_certification/phase_a_advancement_rollup.json').read_text(encoding='utf-8-sig'))
        ledger['snapshot']='final19_product_certification_ledger'
        ledger['historicalPhaseACertified']=1133
        ledger['productCertified']=1133
        ledger['productUncertified']=19
        write(BASE/'product_ledger.json',ledger)
    current_state=json.loads(statepath.read_text(encoding='utf-8-sig'))
    ledger=json.loads((BASE/'product_ledger.json').read_text(encoding='utf-8-sig'))
    assert ledger['productCertified'] == current_state['productCertified'], 'State/ledger count drift'
    for accepted in current_state['persistentEvidenceInventory']:
        assert sha(ROOT / accepted['path']) == accepted['sha256'], 'Accepted FINAL19 evidence drift'
    for accepted in current_state.get('auxiliaryRegressionEvidence', []):
        assert accepted['certificationGain']==0, 'Auxiliary regression cannot change product counts'
        assert sha(ROOT / accepted['path'])==accepted['sha256'], 'Accepted auxiliary regression evidence drift'
    durable = set(BASE.rglob('*')) | set((ROOT/'tools/final19').rglob('*')) | set((ROOT/'src/test/resources/final19_certification').glob('*'))
    manifest = {p.relative_to(ROOT).as_posix():sha(p) for p in sorted(durable) if p.is_file() and p.name != 'durable_hashes.json' and '__pycache__' not in p.parts}
    write(BASE/'durable_hashes.json', manifest)
    print('FINAL19 checkpoint verified; productCertified=' + str(json.loads(statepath.read_text(encoding='utf-8'))['productCertified']))
if __name__ == '__main__':main()
