"""Exact V4 identity checks and separate fresh historical-control orchestration.
Identity validation never certifies execution of historical controls.
"""
from pathlib import Path
import contextlib, copy, io, json, os, stat, subprocess, sys, tempfile, types, zipfile
from tools.build import distribution_b_v2_contract as c

AUTHORITY='ATD_PURPUR_GAMEPLAY_SOURCE_V4'
PATH='tools/certification/authority/gameplay-successor-v4.json'
MODULE='tools/build/gameplay_successor_v4.py'
TESTS='tools/build/tests/test_gameplay_successor_v4.py'
SELECTOR='ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY'
FILE='ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE'
PARENT='tools/certification/authority/gameplay-successor-v3.json'
PARENT_SHA='179a8fb1fce386cfd4f78aa4d96e5f6dfab94701fac35a2f27ccbb17b78ed44c'
REVIEW='tools/certification/authority/gameplay-independent-code-review-v9.json'
REVIEW_SHA='03417de31539158d52be879f13ae5aa065915ae688459f0296ab5c4b5fb58ed9'
TARGET='port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkActions.java'
JUNIT='src/test/java/com/diskree/achievetodo/certification/GameplaySuccessorAuthorityTest.java'
FIXTURE='tools/certification/fixtures/gameplay-source-v3-predecessor.zip'
ARCHIVE_FILES={'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkActions.java': {'sha256': '8c401975413d9350f10618425cf7fd05b31e656a8422a6ff806e2a7865c4c4ca', 'bytes': 25715}, 'build.gradle': {'sha256': '20ce36b27e18c90ce7def86933c1889a986d85649c73b17d30ed964e85a7f444', 'bytes': 43126}, 'tools/build/distribution_b_v2_contract.py': {'sha256': '42d3aebe32e77d6d4a42a4191927e051cb581e319d5677c39428ad13076df97f', 'bytes': 30489}, 'src/test/java/com/diskree/achievetodo/certification/UiUxMaintenanceSourceContract.java': {'sha256': '109038d557d4724b3df4e16a3fea899045dd4784f72fd7dac2b8d8bc08c2f16f', 'bytes': 24409}, 'src/test/java/com/diskree/achievetodo/certification/GameplaySuccessorAuthorityTest.java': {'sha256': '1408d2e617c2cea9892d9d96de55f012dbab31727db6a9b83fd7837a81c9ba33', 'bytes': 1486}}
MAX_ARCHIVE=190000
MAX_UNCOMPRESSED=160000
CURRENT_TEST_IDS=['test_exact_current_identity (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_exact_current_identity)', 'test_matching_timestamp_pyc_cannot_replace_authenticated_v3_source (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_matching_timestamp_pyc_cannot_replace_authenticated_v3_source)', 'test_module_entry_ignores_matching_timestamp_source_and_prefix_pyc (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_module_entry_ignores_matching_timestamp_source_and_prefix_pyc)', 'test_selector_orphan_parent_and_wrong_path_rejected (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_selector_orphan_parent_and_wrong_path_rejected)', 'test_wrong_literal_digest_rejected (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_wrong_literal_digest_rejected)', 'test_mixed_public_authority_rejected (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_mixed_public_authority_rejected)', 'test_archive_exact_originals_and_hostile_members (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_archive_exact_originals_and_hostile_members)', 'test_all_code_source_review_and_archive_tampering (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_all_code_source_review_and_archive_tampering)', 'test_repinned_semantic_mutations_rejected (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_repinned_semantic_mutations_rejected)', 'test_midvalidation_mutation_and_hardlink_rejected (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_midvalidation_mutation_and_hardlink_rejected)', 'test_parent_exact_ids_count_and_terminal_are_required (tools.build.tests.test_gameplay_successor_v4.V4Controls.test_parent_exact_ids_count_and_terminal_are_required)']
PARENT_TEST_IDS=['test_exact_five_original_bytes (tools.build.tests.test_gameplay_successor_v3.FiveOriginalArchiveTests.test_exact_five_original_bytes)', 'test_missing_extra_duplicate_case_alias_and_unsafe_paths (tools.build.tests.test_gameplay_successor_v3.FiveOriginalArchiveTests.test_missing_extra_duplicate_case_alias_and_unsafe_paths)', 'test_tampered_nonregular_compressed_encrypted_and_oversized (tools.build.tests.test_gameplay_successor_v3.FiveOriginalArchiveTests.test_tampered_nonregular_compressed_encrypted_and_oversized)', 'test_redeclared_original_inventory_rejected (tools.build.tests.test_gameplay_successor_v3.FiveOriginalArchiveTests.test_redeclared_original_inventory_rejected)', 'test_unknown_orphan_wrong_path_and_missing_parent_selectors (tools.build.tests.test_gameplay_successor_v3.SelectionAndLegacyTests.test_unknown_orphan_wrong_path_and_missing_parent_selectors)', 'test_public_mixed_authority_rejected (tools.build.tests.test_gameplay_successor_v3.SelectionAndLegacyTests.test_public_mixed_authority_rejected)', 'test_eleven_unchanged_v2_controls_are_mandatory (tools.build.tests.test_gameplay_successor_v3.SelectionAndLegacyTests.test_eleven_unchanged_v2_controls_are_mandatory)', 'test_exact_current_source_and_all_unchanged_parent_controls (tools.build.tests.test_gameplay_successor_v3.CurrentCompositionTests.test_exact_current_source_and_all_unchanged_parent_controls)', 'test_wrong_manifest_digest_rejected (tools.build.tests.test_gameplay_successor_v3.CurrentCompositionTests.test_wrong_manifest_digest_rejected)', 'test_immutable_v2_still_rejects_current_changed_source (tools.build.tests.test_gameplay_successor_v3.CurrentCompositionTests.test_immutable_v2_still_rejects_current_changed_source)', 'test_manifest_helper_review_original_extra_missing_and_source_tampering (tools.build.tests.test_gameplay_successor_v3.CurrentCompositionTests.test_manifest_helper_review_original_extra_missing_and_source_tampering)', 'test_repinned_semantically_invalid_manifests (tools.build.tests.test_gameplay_successor_v3.CurrentCompositionTests.test_repinned_semantically_invalid_manifests)']
SCHEMA={'schema','authority','state','oldV3','ancestorPins','productionDelta','currentProductionClosure','allGameplayChanges','auxiliaryDelta','codeBindings','independentReview','fixture','archiveEntries','predecessorContext'}

def _load_v3(source):
    raw=c.safe_file(source,PARENT).read_bytes()
    c.require(c.sha(raw)==PARENT_SHA,'V4 immutable V3 manifest')
    parent=c.strict_json(raw)
    name='tools/build/gameplay_successor_v3.py'
    path=c.safe_file(source,name)
    source_bytes=path.read_bytes()
    c.require(c.sha(source_bytes)==parent['codeBindings'][name], 'V4 parent helper before import')
    # Compile the exact bytes just authenticated; SourceFileLoader may otherwise accept a matching .pyc.
    v3=types.ModuleType('atd_v4_immutable_v3')
    v3.__file__=str(path)
    v3.__package__='tools.build'
    exec(compile(source_bytes,str(path),'exec',dont_inherit=True),v3.__dict__)
    return parent,v3

def archive_originals(raw,entries):
    c.require(entries==ARCHIVE_FILES and len(raw)<=MAX_ARCHIVE and sum(x['bytes'] for x in entries.values())<=MAX_UNCOMPRESSED,'V4 bounded exact archive')
    result={}
    try:
        with zipfile.ZipFile(io.BytesIO(raw)) as z:
            c.require(len(z.infolist())==5,'V4 archive cardinality')
            for info in z.infolist():
                name=info.filename
                c.require(name==info.orig_filename and name in entries and name not in result,'V4 archive extra/duplicate/unsafe member')
                c.require(not info.is_dir() and not info.flag_bits&1 and stat.S_IFMT(info.external_attr>>16) in (0,stat.S_IFREG) and info.compress_type==zipfile.ZIP_STORED,'V4 archive regular uncompressed member')
                data=z.read(info);row=entries[name]
                c.require(info.file_size==row['bytes'] and len(data)==row['bytes'] and c.sha(data)==row['sha256'],'V4 original bytes')
                result[name]=data
    except (zipfile.BadZipFile,RuntimeError,OSError) as error:
        raise ValueError('V4 invalid predecessor archive') from error
    c.require(set(result)==set(entries),'V4 missing originals')
    return result

def _old_read(source,name,originals):
    if name in originals:
        c.safe_file(source,name)  # Current overlay path must also be regular, canonical, single-link.
        return originals[name]
    return c.safe_file(source,name).read_bytes()

def _old_production(source,originals,observed_production):
    result=dict(observed_production)
    result[TARGET]=c.sha(originals[TARGET])
    return result

def production(source):
    result={}
    for directory in ('src/main','port/AchieveToDo-Purpur-26.2/src/main'):
        for p in (source/directory).rglob('*'):
            c._no_links(p)
            if p.is_file():
                name=p.relative_to(source).as_posix();result[name]=c.sha(c.safe_file(source,name).read_bytes())
    return result

def _validate_v3_structure(source, env, expected_digest, ui07, originals, v3, observed_production):
    source = Path(source).absolute()
    c._no_links(source)
    c.require(env.get(v3.SELECTOR) == v3.AUTHORITY and env.get(v3.FILE) == str(source / v3.PATH) and (env.get('ACHIEVETODO_UI07_AUTHORITY') == 'ATD_UI07_SCOPED_SUCCESSOR_V1') and (env.get(c.SELECTOR) == c.AUTHORITY), 'V3 unknown/orphan/path/parent selector')
    raw = _old_read(source, v3.PATH, originals)
    c.require(c.sha(raw) == expected_digest, 'V3 manifest digest')
    m = c.strict_json(raw)
    c.require(set(m) == {'schema', 'authority', 'state', 'oldV2', 'ancestorPins', 'productionDelta', 'currentProductionClosure', 'allGameplayChanges', 'auxiliaryDelta', 'codeBindings', 'independentReview', 'fixture', 'archiveEntries', 'predecessorContext'}, 'V3 exact manifest schema')
    c.require(m['schema'] == 3 and m['authority'] == v3.AUTHORITY and (m['state'] == 'AUTHORIZED_BOUNDED_SOURCE_FOR_CERTIFICATION_ONLY'), 'V3 state/scope')
    c.require(m['oldV2'] == {'path': v3.PARENT, 'sha256': v3.PARENT_SHA}, 'V3 parent pin')
    parent, old = v3.parent_data(source)
    ancestors = {k: parent[k] for k in ('oldGateA', 'oldOwnerB', 'oldUI07', 'oldV1')}
    c.require(m['ancestorPins'] == ancestors and ancestors['oldGateA'] == c.policy(source)['gateAManifestSHA256'] and (ancestors['oldOwnerB'] == c.OWNER_B_CAPSULE_SHA256) and (ancestors['oldUI07'] == c.UI07_SUCCESSOR_SHA256), 'V3 ancestor identities')
    c.require(set(m['productionDelta']) == {v3.TARGET}, 'V3 exact one production delta')
    delta = m['productionDelta'][v3.TARGET]
    c.require(set(delta) == {'beforeSHA256', 'afterSHA256'} and delta['beforeSHA256'] == old[v3.TARGET] and (delta['beforeSHA256'] != delta['afterSHA256']), 'V3 exact production delta identities')
    result = dict(old)
    result[v3.TARGET] = delta['afterSHA256']
    c.require(m['currentProductionClosure'] == result and _old_production(source, originals, observed_production) == result, 'V3 full current production closure')
    flattened = {p: dict(row) for p, row in parent['allGameplayChanges'].items()}
    flattened[v3.TARGET]['afterSHA256'] = delta['afterSHA256']
    c.require(m['allGameplayChanges'] == flattened and all((ui07.get(p) == row['beforeSHA256'] for p, row in flattened.items() if p.startswith(('src/main/', 'port/AchieveToDo-Purpur-26.2/src/main/')))), 'V3 exact flattened original UI07 changes')
    c.require(set(m['auxiliaryDelta']) == {v3.JUNIT}, 'V3 exact JUnit auxiliary delta')
    auxiliary = m['auxiliaryDelta'][v3.JUNIT]
    c.require(set(auxiliary) == {'beforeSHA256', 'afterSHA256'} and auxiliary['beforeSHA256'] == parent['codeBindings'][v3.JUNIT] == v3.ARCHIVE_FILES[v3.JUNIT]['sha256'] and (auxiliary['afterSHA256'] != auxiliary['beforeSHA256']), 'V3 JUnit identities')
    c.require(set(m['codeBindings']) == v3.CODE_PATHS, 'V3 exact authority code paths')
    for name, digest in m['codeBindings'].items():
        c.require(c.sha(_old_read(source, v3.relative(name), originals)) == digest, 'V3 code identity: ' + name)
    for name, digest in parent['codeBindings'].items():
        c.require(m['codeBindings'][name] == (auxiliary['afterSHA256'] if name == v3.JUNIT else digest), 'V3 immutable parent code or bounded JUnit overlay')
    c.require(m['codeBindings'][v3.PARENT] == v3.PARENT_SHA and m['independentReview'] == {'path': v3.REVIEW, 'sha256': v3.REVIEW_SHA} and (m['codeBindings'][v3.REVIEW] == v3.REVIEW_SHA), 'V3 actual independent review pin')
    review = c.strict_json(_old_read(source, v3.REVIEW, originals))
    c.require(review.get('schema') == 1 and review.get('reviewer') == 'independent agent /root/independent_v7_review' and (review.get('verdict') == 'NO_CODE_LEVEL_BLOCKER_IDENTIFIED_PENDING_AUTHORITY_BUILD_AND_RUNTIME_REGRESSION') and (review.get('acceptedForNextEngineeringChecks') is True) and all((review.get(k) is False for k in ('sourceAuthorityAccepted', 'compiledAccepted', 'runtimeAccepted', 'productReady'))) and (review.get('findings') == []) and (review.get('sourceChanges') == [{'path': v3.TARGET, **delta}]) and (review.get('productionClosure') == {'paths': 2504, 'otherPathsExactV7': 2503, 'all2504OriginalBytesIndependentlyHashVerified': True}), 'V3 exact independent one-file code-only acceptance')
    c.require(m['fixture'] == {'path': v3.FIXTURE, 'sha256': m['codeBindings'][v3.FIXTURE]}, 'V3 fixture binding')
    originals = v3.archive_originals(_old_read(source, v3.FIXTURE, originals), m['archiveEntries'])
    context = v3.predecessor_context(parent, old)
    c.require(m['predecessorContext'] == context, 'V3 exact predecessor context')
    return (result, sorted(set(flattened) | v3.CODE_PATHS | {v3.PATH}))

def validate(source,env,expected_digest,ui07):
    source=Path(source).absolute();c._no_links(source)
    c.require(env.get(SELECTOR)==AUTHORITY and env.get(FILE)==str(source/PATH) and env.get(c.SELECTOR)==c.AUTHORITY and env.get('ACHIEVETODO_UI07_AUTHORITY')=='ATD_UI07_SCOPED_SUCCESSOR_V1','V4 selector/path/parent')
    raw=c.safe_file(source,PATH).read_bytes();c.require(c.sha(raw)==expected_digest,'V4 manifest digest');m=c.strict_json(raw)
    c.require(set(m)==SCHEMA and m['schema']==4 and m['authority']==AUTHORITY and m['state']=='AUTHORIZED_BOUNDED_SOURCE_FOR_CERTIFICATION_ONLY','V4 exact schema/state')
    parent,v3=_load_v3(source)
    c.require(m['oldV3']=={'path':PARENT,'sha256':PARENT_SHA},'V4 parent pin')
    c.require(m['ancestorPins']==parent['ancestorPins'],'V4 ancestors')
    originals=archive_originals(c.safe_file(source,FIXTURE).read_bytes(),m['archiveEntries'])
    oldenv=dict(env);oldenv[SELECTOR]=v3.AUTHORITY;oldenv[FILE]=str(source/v3.PATH)
    # v3 is a local argument to the copied structural function, not a mutable old module hook.
    observed_production=production(source)  # Fresh for THIS call only; never persisted or shared.
    identity_names=set(m['codeBindings'])|set(ARCHIVE_FILES)|{PATH}
    observed_identity={name:c.sha(c.safe_file(source,name).read_bytes()) for name in identity_names}
    _validate_v3_structure(source,oldenv,PARENT_SHA,ui07,originals,v3,observed_production)
    old=parent['currentProductionClosure'];delta=m['productionDelta']
    c.require(set(delta)=={TARGET} and set(delta[TARGET])=={'beforeSHA256','afterSHA256'} and delta[TARGET]['beforeSHA256']==old[TARGET] and delta[TARGET]['afterSHA256']!=old[TARGET],'V4 one production delta')
    current=dict(old);current[TARGET]=delta[TARGET]['afterSHA256']
    c.require(m['currentProductionClosure']==current and observed_production==current and len(current)==2504,'V4 full current production closure')
    flat=copy.deepcopy(parent['allGameplayChanges']);flat[TARGET]['afterSHA256']=delta[TARGET]['afterSHA256']
    c.require(m['allGameplayChanges']==flat,'V4 flattened original UI source identities')
    aux=m['auxiliaryDelta'];c.require(set(aux)=={JUNIT} and set(aux[JUNIT])=={'beforeSHA256','afterSHA256'} and aux[JUNIT]['beforeSHA256']==parent['codeBindings'][JUNIT]==ARCHIVE_FILES[JUNIT]['sha256'] and aux[JUNIT]['afterSHA256']!=aux[JUNIT]['beforeSHA256'],'V4 JUnit overlay')
    code=set(parent['codeBindings'])|{PARENT,MODULE,TESTS,REVIEW,FIXTURE}
    c.require(set(m['codeBindings'])==code,'V4 exact code paths')
    for name,digest in m['codeBindings'].items():
        v3.relative(name);c.require(c.sha(c.safe_file(source,name).read_bytes())==digest,'V4 code identity: '+name)
    for name,digest in parent['codeBindings'].items():
        c.require(m['codeBindings'][name]==(aux[JUNIT]['afterSHA256'] if name==JUNIT else digest),'V4 old immutable code')
    c.require(m['codeBindings'][PARENT]==PARENT_SHA and m['independentReview']=={'path':REVIEW,'sha256':REVIEW_SHA} and m['codeBindings'][REVIEW]==REVIEW_SHA,'V4 review pin')
    review=c.strict_json(c.safe_file(source,REVIEW).read_bytes())
    c.require(review.get('accepted') is True and review.get('sourceOnly') is True and review['sourceBindings'][0]['path']==TARGET and review['sourceBindings'][0]['beforeSHA256']==delta[TARGET]['beforeSHA256'] and review['sourceBindings'][0]['afterSHA256']==delta[TARGET]['afterSHA256'],'V4 independent source-only review')
    c.require(m['fixture']=={'path':FIXTURE,'sha256':m['codeBindings'][FIXTURE]},'V4 fixture identity')
    context=dict(parent['predecessorContext']);context.update(old);context.update({p:r['afterSHA256'] for p,r in parent['allGameplayChanges'].items()});context.update(parent['codeBindings']);context.update({p:r['sha256'] for p,r in ARCHIVE_FILES.items()});context[PARENT]=PARENT_SHA
    c.require(m['predecessorContext']==context and len(context)==2535,'V4 authentic V8 context')
    c.require(len({v3.relative(p).casefold() for p in context})==len(context),'V4 context case aliases')
    for name,digest in context.items():
        observed=(c.sha(_old_read(source,name,originals)) if name in originals or name not in observed_production else observed_production[name])
        c.require(observed==digest,'V4 predecessor identity: '+name)
    # Independent final refresh catches changes after the initial snapshot and path/link changes.
    c.require(production(source)==observed_production,'V4 source changed during identity validation')
    c.require(all(c.sha(c.safe_file(source,name).read_bytes())==digest for name,digest in observed_identity.items()),'V4 authority changed during identity validation')
    print('SOURCE_IDENTITY_ONLY historical_controls=NOT_RUN (fresh normal Gradle controls node required)',file=sys.stderr)
    return current,sorted(set(flat)|code|{PATH})

def ui07_base(source):
    base=dict(c.strict_json(c.safe_file(source,c.GATE_A_MANIFEST).read_bytes())['productionClosure'])
    delta=c.strict_json(c.safe_file(source,c.UI07_MANIFEST).read_bytes())['productionDelta'];base[delta['path']]=delta['afterSHA256'];return base

def require_parent_result(run,expected_ids):
    text=run.stderr
    c.require(run.returncode==0 and '\nRan 12 tests' in text and '\nOK\n' in text and 'skipped=' not in text and 'expected failures=' not in text,'V4 historical terminal outcome')
    observed=[]
    for line in text.splitlines():
        if line.startswith('test_') and line.endswith(' ... ok'):observed.append(line.rsplit(' ... ',1)[0])
    c.require(sorted(observed)==sorted(expected_ids) and len(observed)==12,'V4 exact twelve historical identities')

def run_controls(source,env):
    source=Path(source).absolute();base=ui07_base(source)
    validate(source,env,c.GAMEPLAY_V4_SUCCESSOR_SHA256,base)
    m=c.strict_json(c.safe_file(source,PATH).read_bytes());originals=archive_originals(c.safe_file(source,FIXTURE).read_bytes(),m['archiveEntries'])
    before={p:c.sha(c.safe_file(source,p).read_bytes()) for p in set(m['predecessorContext'])|set(m['codeBindings'])|{PATH}}
    print('V4_CONTROL_CURRENT_BEFORE '+json.dumps(before,sort_keys=True))
    with tempfile.TemporaryDirectory(prefix='atd-authentic-v3-controls-') as tmp, tempfile.TemporaryDirectory(prefix='atd-v4-pycache-v3-') as pycache:
        root=Path(tmp)
        for name,digest in m['predecessorContext'].items():
            data=_old_read(source,name,originals);c.require(c.sha(data)==digest,'V4 controls reconstruction');p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
        clean=dict(env);clean[SELECTOR]='ATD_PURPUR_GAMEPLAY_SOURCE_V3';clean[FILE]=str(root/PARENT);clean['PYTHONDONTWRITEBYTECODE']='1';clean['PYTHONPYCACHEPREFIX']=pycache;clean.pop('PYTHONPATH',None)
        command=[sys.executable,'-B','-X','utf8','-X','pycache_prefix='+pycache,'-m','unittest','tools.build.tests.test_gameplay_successor_v3','-v']
        reconstructed={name:c.sha(c.safe_file(root,name).read_bytes()) for name in m['predecessorContext']}
        c.require(reconstructed==m['predecessorContext'],'V4 actual reconstructed context before controls')
        print('V4_HISTORICAL_COMMAND '+json.dumps({'argv':command,'cwd':str(root),'environment':{k:clean[k] for k in (SELECTOR,FILE,'PYTHONDONTWRITEBYTECODE')},'sourceBindings':reconstructed},sort_keys=True))
        run=subprocess.run(command,cwd=root,env=clean,capture_output=True,text=True,timeout=1800)
        print(run.stdout,end='');print(run.stderr,end='',file=sys.stderr)
        require_parent_result(run,PARENT_TEST_IDS)
        reconstructed_after={name:c.sha(c.safe_file(root,name).read_bytes()) for name in m['predecessorContext']}
        c.require(reconstructed_after==reconstructed,'V4 predecessor changed during controls')
        c.require(not any(Path(pycache).rglob('*')),'V4 historical cache prefix remained empty')
        print('V4_HISTORICAL_AFTER '+json.dumps(reconstructed_after,sort_keys=True))
    with tempfile.TemporaryDirectory(prefix='atd-v4-pycache-current-') as pycache:
        current_env={**env,'PYTHONDONTWRITEBYTECODE':'1','PYTHONPYCACHEPREFIX':pycache}
        current_env.pop('PYTHONPATH',None)
        current_command=[sys.executable,'-B','-X','utf8','-X','pycache_prefix='+pycache,'-m','unittest','tools.build.tests.test_gameplay_successor_v4','-v']
        run=subprocess.run(current_command,cwd=source,env=current_env,capture_output=True,text=True,timeout=1200)
        c.require(not any(Path(pycache).rglob('*')),'V4 current cache prefix remained empty')
    print(run.stdout,end='');print(run.stderr,end='',file=sys.stderr)
    c.require(run.returncode==0 and '\nOK\n' in run.stderr and 'skipped=' not in run.stderr and 'expected failures=' not in run.stderr,'V4 current controls failed')
    c.require('\nRan '+str(len(CURRENT_TEST_IDS))+' tests' in run.stderr and all(any(line.startswith(name+' ... ') for line in run.stderr.splitlines()) for name in CURRENT_TEST_IDS),'V4 exact current control count/identities')
    after={p:c.sha(c.safe_file(source,p).read_bytes()) for p in before}
    c.require(after==before,'V4 current changed during controls')
    print('V4_CONTROL_CURRENT_AFTER '+json.dumps(after,sort_keys=True))
    validate(source,env,c.GAMEPLAY_V4_SUCCESSOR_SHA256,base)
    print('V4_FRESH_HISTORICAL_AND_CURRENT_CONTROLS_PASS parent=12 nestedV2=11 nestedV1=4 noCacheAuthority=true')

def main():
    c.require(sys.argv[1:]==['--controls'],'V4 only explicit fresh controls CLI supported')
    prefix=os.environ.get('PYTHONPYCACHEPREFIX')
    c.require(prefix is not None and Path(prefix).is_dir() and not any(Path(prefix).rglob('*')),
              'V4 invocation requires fresh empty Python cache prefix')
    run_controls(Path.cwd(),dict(os.environ))

if __name__=='__main__':main()
