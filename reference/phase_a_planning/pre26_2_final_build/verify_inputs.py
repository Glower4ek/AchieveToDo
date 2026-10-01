"""Read-only release input verification. Writes only the new build namespace."""
import hashlib, json, pathlib, subprocess, re
ROOT = pathlib.Path(__file__).resolve().parents[3]
OUT = pathlib.Path(__file__).resolve().parent
BASE = ROOT / 'reference/phase_a_planning/final19'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def git(*args): return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()
def main():
    state, ledger, terminal, durable, preservation = [read(BASE / n) for n in ['execution_state.json','product_ledger.json','terminal_reconciliation.json','durable_hashes.json','implementation_preservation.json']]
    assert sha(BASE/'terminal_reconciliation.json') == '1ab689c783c6a7e85d2e84dd9c526b2d4333934171b81b4a22d5ae1e4454e962'
    assert terminal['verdict'] == state['transactionStage'] == 'PRE_26_2_FINAL19_1152_CERTIFIED'
    assert (terminal['totalUniqueAdvancements'], terminal['productCertified'], terminal['productUncertified']) == (1152,1152,0)
    assert (terminal['originalFinal19TargetsResolved'],terminal['requirementGroupsCertified'],terminal['criteriaAccounted']) == (19,79,108)
    assert (ledger['productCertified'], ledger['productUncertified'], state['productCertified']) == (1152,0,1152)
    assert len(ledger['entries']) == len({e['id'] for e in ledger['entries']}) == 1152
    assert len(state['completedFamilies']) == len(terminal['familyResults']) == 7
    assert all(f['verdict'] == 'DURABLY_GREEN' for f in terminal['familyResults'])
    assert state['remainingTargets'] == []
    historical = read(ROOT/'reference/phase_a_planning/phase_a_terminal_reconciliation_20260930.json')
    rollup = read(ROOT/'src/test/resources/phase_a_certification/phase_a_advancement_rollup.json')
    assert rollup['summary']['totalCertified'] == state['historicalPhaseACertified'] == terminal['historicalPhaseACertified'] == 1133
    assert sha(BASE/'product_ledger.json') == terminal['productLedgerSha256']
    assert sha(BASE/'execution_state.json') == terminal['executionStateSha256']
    for p,h in durable.items(): assert sha(ROOT/p) == h, 'Durable drift: '+p
    for p,h in state['historicalHashes'].items(): assert sha(ROOT/p) == h, 'Historical drift: '+p
    frozen = ROOT/'reference/phase_a_preservation/files/final/bacap.zip'
    assert sha(frozen) == terminal['frozenBacapSha256'] == '8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70'
    assert hashlib.sha1(frozen.read_bytes()).hexdigest() == terminal['frozenBacapSha1'] == '45b8bb0076bbf5b92fde7dc9590c6686937abbc0'
    auths = [read(BASE/n) for n in ['wax_axe_gate_authorized_application.json','cauldron_gate_authorized_application.json','raider_authorized_application.json']]
    assert [a['id'] for a in auths] == terminal['authorizedProductionFixes']
    expected = dict(preservation['files'])
    for a in auths:
        assert sha(ROOT/a['patchPath']) == a['patchSha256']
        post = a.get('postconditions', {a.get('productionFile'):a.get('postSha256')})
        expected.update(post)
        for p,h in post.items(): assert sha(ROOT/p) == h, 'Production drift: '+p
    for p,h in expected.items(): assert sha(ROOT/p) == h, 'Preserved drift: '+p
    current = {p.relative_to(ROOT).as_posix():sha(p) for parent in ['src/main/java','src/main/resources'] for p in (ROOT/parent).rglob('*') if p.is_file()}
    assert set(current) == {p for p in expected if p.startswith(('src/main/java/','src/main/resources/'))}, 'Production path drift'
    assert sha(ROOT/'src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java') == '1bea3ba9caf22963d1ef26de69b19534a4b16d1eab85a39c5e1fc17750227a31'
    for e in state['persistentEvidenceInventory'] + state['auxiliaryRegressionEvidence']:
        assert sha(ROOT/e['path']) == e['sha256']
    assert all(v.endswith('GREEN') for v in state['zeroGainRegressionDebt'].values())
    assert [len(read(ROOT/e['path'])['entries']) for e in state['auxiliaryRegressionEvidence']] == [14,24,16]
    assert git('rev-parse','HEAD') == preservation['head'] and git('branch','--show-current') == preservation['branch']
    assert sha(ROOT/'.git/index') == preservation['indexSha256']
    config = read(ROOT/'src/main/resources/achievetodo.mixins.json')
    names = config.get('mixins',[]) + config.get('client',[]) + config.get('server',[])
    for n in names:
        p = ROOT/'src/main/java'/((config['package']+'.'+n).split('$')[0].replace('.','/')+'.java')
        assert p.exists(), 'Missing mixin: '+n
    resource_files = [p for parent in ['src/main/resources','src/main/generated'] for p in (ROOT/parent).rglob('*') if p.is_file()]
    windows_paths = []
    for p in resource_files:
        if p.suffix in ['.json','.mcmeta','.properties','.mcfunction','.java','.txt']:
            if re.search(r'[A-Za-z]:[\\/]',p.read_text(encoding='utf-8-sig')): windows_paths.append(str(p.relative_to(ROOT)))
    assert not windows_paths, windows_paths
    result = {'verdict':'INPUTS_GREEN','terminalSha256':sha(BASE/'terminal_reconciliation.json'),'productLedgerSha256':sha(BASE/'product_ledger.json'),'durableFilesVerified':len(durable),'preservedFilesVerified':len(expected),'productionFiles':current,'authorizations':auths,'auxiliaryRegressionCases':[14,24,16],'historicalPhaseA':1133,'productCertified':1152,'frozenBacapSha256':sha(frozen),'frozenBacapSha1':hashlib.sha1(frozen.read_bytes()).hexdigest(),'head':preservation['head'],'branch':preservation['branch'],'indexSha256':preservation['indexSha256'],'resourceFileCount':len(resource_files),'mixinReferenceCount':len(names),'absoluteWindowsPaths':windows_paths,'protectedFiles':{p.relative_to(ROOT).as_posix():sha(p) for parent in ['src','tools/final19','reference/phase_a_planning/final19'] for p in (ROOT/parent).rglob('*') if p.is_file() and '__pycache__' not in p.parts},'initialGitStatus':git('status','--short')}
    (OUT/'input_verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k not in ['productionFiles','protectedFiles','initialGitStatus','authorizations']},indent=2))
if __name__ == '__main__': main()
