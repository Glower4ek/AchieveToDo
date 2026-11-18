"""V5 exact current source identity plus authenticated disposable V4 controls."""
from pathlib import Path, PurePosixPath
import argparse, hashlib, json, os, shutil, stat, subprocess, sys, tempfile, types, zipfile, io
from tools.build import distribution_b_v2_contract as c

AUTHORITY='ATD_PURPUR_GAMEPLAY_SOURCE_V5'
PATH='tools/certification/authority/gameplay-successor-v5.json'
MODULE='tools/build/gameplay_successor_v5.py'
TESTS='tools/build/tests/test_gameplay_successor_v5.py'
SELECTOR='ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY'
FILE='ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE'
EXPECTED_DIGEST='ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_SHA256'
PARENT='tools/certification/authority/gameplay-successor-v4.json'
PARENT_SHA='e069f6109cbbf2dd679859b10e814d987e77ba8c9c9338899e13be9b4abcce53'
V4_MODULE='tools/build/gameplay_successor_v4.py'
V4_PATH='tools/certification/authority/gameplay-successor-v4.json'
FIXTURE='tools/certification/fixtures/gameplay-source-v5-predecessor.zip'
REVIEW='tools/certification/authority/gameplay-independent-code-review-v11.json'
UIUX='src/test/java/com/diskree/achievetodo/certification/UiUxMaintenanceSourceContract.java'
JUNIT='src/test/java/com/diskree/achievetodo/certification/GameplaySuccessorAuthorityTest.java'
JUNIT_V1='tools/certification/authority/gameplay-junit-fixture-successor-v1.json'
JUNIT_V2='tools/certification/authority/gameplay-junit-fixture-successor-v2.json'
JUNIT_V1_SHA='74650d947ccc695af15fe6985cac53638dc8d0b102ca742b5be8c131dcaad3d4'
REPAIR_SELECTOR='ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR'
REPAIR_FILE='ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR_FILE'
FIXTURE_SELECTOR='ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY'
FIXTURE_FILE='ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY_FILE'
FIXTURE_DIGEST='ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY_SHA256'
GATE_SELECTOR='ACHIEVETODO_GATE_B_AUTHORITY'
GATE_FILE='ACHIEVETODO_GATE_B_APPROVAL_FILE'
UIUX_SELECTOR='ACHIEVETODO_UI_UX_CERTIFICATION_AUTHORITY'
UIUX_FILE='ACHIEVETODO_UI_UX_CERTIFICATION_FILE'
UI07_SELECTOR='ACHIEVETODO_UI07_AUTHORITY'
UI07_FILE='ACHIEVETODO_UI07_AUTHORITY_FILE'
TARGETS={
 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/landmark/CanonicalLandmarkMembership.java',
 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/landmark/bukkit/BukkitExactLandmarkRuntime.java',
}
MAX_ARCHIVE=100000
MAX_UNCOMPRESSED=80000
UIUX_BEFORE='972d84da4d56ae72a09d7d1032ef6d799b97bd3edea59218b81ca4aceee14b4d'
REVIEWED_CODE_PATHS={UIUX,JUNIT,MODULE,TESTS,FIXTURE,JUNIT_V2}
ROOT_BUILD_EXCLUDES={'.git','build','.gradle'}
SCHEMA={'schema','authority','state','oldV4','ancestorPins','productionDelta','currentProductionClosure','allGameplayChanges','auxiliaryDelta','sourceContractDelta','codeBindings','independentReview','fixture','archiveEntries','predecessorContext','ownerBApproval','PRODUCT_GAMEPLAY_READY'}
CURRENT_TEST_IDS=[
 'test_exact_current_identity (tools.build.tests.test_gameplay_successor_v5.V5Controls.test_exact_current_identity)',
 'test_selector_orphan_parent_and_wrong_path_rejected (tools.build.tests.test_gameplay_successor_v5.V5Controls.test_selector_orphan_parent_and_wrong_path_rejected)',
 'test_wrong_manifest_digest_rejected (tools.build.tests.test_gameplay_successor_v5.V5Controls.test_wrong_manifest_digest_rejected)',
 'test_two_production_deltas_and_full_closure_are_exact (tools.build.tests.test_gameplay_successor_v5.V5Controls.test_two_production_deltas_and_full_closure_are_exact)',
 'test_fixture_exact_originals_and_hostile_archives (tools.build.tests.test_gameplay_successor_v5.V5Controls.test_fixture_exact_originals_and_hostile_archives)',
 'test_source_contract_fixture_successor_is_bound (tools.build.tests.test_gameplay_successor_v5.V5Controls.test_source_contract_fixture_successor_is_bound)',
]

def require(ok,message):
    c.require(ok,'V5 '+message)

def production(source):
    result={}
    source=Path(source).absolute()
    for directory in ('src/main','port/AchieveToDo-Purpur-26.2/src/main'):
        base=source/directory
        require(base.is_dir(),'missing production root: '+directory)
        for path in base.rglob('*'):
            require(not path.is_symlink() and not (hasattr(path,'is_junction') and path.is_junction()),'production link: '+str(path))
            if path.is_file():
                name=path.relative_to(source).as_posix()
                result[name]=c.sha(c.safe_file(source,name).read_bytes())
    return result

def _load_json(source,path,digest,label):
    raw=c.safe_file(source,path).read_bytes()
    require(c.sha(raw)==digest,label+' digest')
    return c.strict_json(raw)

def _archive_originals(raw,entries):
    require(isinstance(entries,dict) and set(entries)==TARGETS|{JUNIT,UIUX},'exact four-entry V4 predecessor inventory')
    require(len(raw)<=MAX_ARCHIVE and sum(row['bytes'] for row in entries.values())<=MAX_UNCOMPRESSED,'bounded V4 predecessor archive')
    result={}
    try:
        with zipfile.ZipFile(io.BytesIO(raw)) as z:
            require(len(z.infolist())==4,'predecessor archive cardinality')
            for info in z.infolist():
                name=info.filename;rel=PurePosixPath(name)
                require(name==info.orig_filename and name in entries and name not in result,'unsafe/extra/duplicate predecessor entry')
                require(not rel.is_absolute() and '\\' not in name and ':' not in name and '..' not in rel.parts and '.' not in rel.parts,'unsafe predecessor path')
                require(not info.is_dir() and not (info.flag_bits&1) and stat.S_IFMT(info.external_attr>>16) in (0,stat.S_IFREG) and info.compress_type==zipfile.ZIP_STORED,'predecessor entry type/compression')
                data=z.read(info);row=entries[name]
                require(type(row['bytes']) is int and len(data)==row['bytes'] and info.file_size==row['bytes'] and c.sha(data)==row['sha256'],'predecessor original bytes: '+name)
                result[name]=data
    except (zipfile.BadZipFile,RuntimeError,OSError,KeyError) as error:
        raise ValueError('V5 invalid predecessor fixture') from error
    require(set(result)==set(entries),'missing predecessor entries')
    return result

def _validate_fixture_successor(source,m,v4):
    row=m['codeBindings'][JUNIT_V2]
    raw=c.safe_file(source,JUNIT_V2).read_bytes();require(c.sha(raw)==row,'JUnit fixture V2 code binding')
    f=c.strict_json(raw)
    require(set(f)=={'schema','authority','state','parents','sourceBeforeSHA256','sourcePostimage','v4ArchiveEntries','v5PredecessorArchiveEntries','scope','ownerBApproval','PRODUCT_GAMEPLAY_READY'},'JUnit fixture V2 exact schema')
    require(f['schema']==2 and f['authority']=='ATD_GAMEPLAY_JUNIT_FIXTURE_SOURCE_V2' and f['state']=='AUTHORIZED_BOUNDED_TEST_FIXTURE_SUCCESSOR_ONLY','JUnit fixture V2 identity/state')
    require(f['parents']=={'junitOverlayV1':{'path':'tools/certification/authority/gameplay-junit-overlay-v1.json','sha256':'5bec165c457465e7c9fd61c21689f8cffa7238289826c6f658a12310ef284a5f'},'junitFixtureSuccessorV1':{'path':JUNIT_V1,'sha256':JUNIT_V1_SHA},'gameplaySourceV4':{'path':V4_PATH,'sha256':PARENT_SHA}},'JUnit fixture V2 parents')
    old=_load_json(source,JUNIT_V1,JUNIT_V1_SHA,'JUnit fixture V1')
    require(f['sourceBeforeSHA256']==old['sourcePostimage']['sha256']==UIUX_BEFORE,'JUnit fixture V2 predecessor source')
    post=f['sourcePostimage'];require(set(post)=={'path','sha256'} and post['path']==UIUX and post['sha256']==m['codeBindings'][UIUX],'JUnit fixture V2 source postimage')
    require(sorted(f['v4ArchiveEntries'])==sorted(v4['archiveEntries']),'JUnit fixture V2 V4 archive inventory')
    require(sorted(f['v5PredecessorArchiveEntries'])==sorted(m['archiveEntries']),'JUnit fixture V2 V5 archive inventory')
    require(f['scope']=='V5 JUnit fixture closure only; use the exact V5 predecessor inventory for a disposable V4 source projection.' and f['ownerBApproval']=='NOT_GRANTED' and f['PRODUCT_GAMEPLAY_READY'] is False,'JUnit fixture V2 scope/readiness')

def _load_v4(source,v4manifest):
    raw=c.safe_file(source,V4_MODULE).read_bytes()
    require(c.sha(raw)==v4manifest['codeBindings'][V4_MODULE],'V4 helper immutable identity')
    module=types.ModuleType('atd_v5_authenticated_v4')
    path=c.safe_file(source,V4_MODULE);module.__file__=str(path);module.__package__='tools.build'
    exec(compile(raw,str(path),'exec',dont_inherit=True),module.__dict__)
    return module

def validate(source,env,expected_digest):
    source=Path(source).absolute();c._no_links(source)
    require(source.resolve()==source,'noncanonical source root')
    require(env.get(SELECTOR)==AUTHORITY and env.get(FILE)==str(source/PATH) and env.get(EXPECTED_DIGEST)==expected_digest and env.get(c.SELECTOR)==c.AUTHORITY
        and env.get('ACHIEVETODO_UI07_AUTHORITY')=='ATD_UI07_SCOPED_SUCCESSOR_V1' and env.get(GATE_SELECTOR)==c.AUTHORITY,'selector/path/parents')
    require(env.get(REPAIR_SELECTOR)=='ATD_GAMEPLAY_JUNIT_REPAIR_V1' and env.get(FIXTURE_SELECTOR)=='ATD_GAMEPLAY_JUNIT_FIXTURE_SOURCE_V2'
        and env.get(FIXTURE_FILE)==str(source/JUNIT_V2),'JUnit fixture selector/path')
    require(isinstance(expected_digest,str) and len(expected_digest)==64 and all(ch in '0123456789abcdef' for ch in expected_digest),'expected manifest digest')
    raw=c.safe_file(source,PATH).read_bytes();require(c.sha(raw)==expected_digest,'manifest digest');m=c.strict_json(raw)
    require(set(m)==SCHEMA and m['schema']==5 and m['authority']==AUTHORITY and m['state']=='AUTHORIZED_BOUNDED_SOURCE_FOR_CERTIFICATION_ONLY','exact manifest schema/state')
    v4raw=c.safe_file(source,V4_PATH).read_bytes();require(c.sha(v4raw)==PARENT_SHA,'immutable V4 manifest')
    v4=c.strict_json(v4raw);v4module=_load_v4(source,v4)
    require(m['oldV4']=={'path':V4_PATH,'sha256':PARENT_SHA} and m['ancestorPins']==v4['ancestorPins'],'V4 parent/ancestors')
    require(m['ownerBApproval']=='NOT_GRANTED' and m['PRODUCT_GAMEPLAY_READY'] is False,'readiness boundary')
    require(m['predecessorContext']==v4['predecessorContext'] and len(m['predecessorContext'])==2535,'V4 predecessor context')
    original_expected={path:{'sha256':v4['currentProductionClosure'][path],'bytes':None} for path in TARGETS}
    original_expected[JUNIT]={'sha256':v4['codeBindings'][JUNIT],'bytes':None}
    original_expected[UIUX]={'sha256':UIUX_BEFORE,'bytes':None}
    entries=m['archiveEntries']
    require(set(entries)==set(original_expected),'exact V5 archive paths')
    for name,row in original_expected.items():
        require(set(entries[name])=={'sha256','bytes'} and entries[name]['sha256']==row['sha256'] and type(entries[name]['bytes']) is int and entries[name]['bytes']>0,'V4 archive binding: '+name)
    fixture=m['fixture'];require(fixture=={'path':FIXTURE,'sha256':m['codeBindings'][FIXTURE]},'fixture binding')
    originals=_archive_originals(c.safe_file(source,FIXTURE).read_bytes(),entries)
    for name,data in originals.items():require(len(data)==entries[name]['bytes'],'fixture byte length: '+name)
    production_before=dict(v4['currentProductionClosure']);delta=m['productionDelta']
    require(set(delta)==TARGETS,'exact two production deltas')
    expected_current=dict(production_before)
    for name in TARGETS:
        row=delta[name]
        require(set(row)=={'beforeSHA256','afterSHA256'} and row['beforeSHA256']==production_before[name] and row['afterSHA256']!=row['beforeSHA256'],'production delta identity: '+name)
        expected_current[name]=row['afterSHA256']
    observed_production=production(source)
    require(len(observed_production)==2504 and m['currentProductionClosure']==expected_current and observed_production==expected_current,'complete V5 production closure')
    flattened=json.loads(json.dumps(v4['allGameplayChanges']))
    for name in TARGETS:
        require(name in flattened and flattened[name]['afterSHA256']==delta[name]['beforeSHA256'],'flattened predecessor: '+name)
        flattened[name]['afterSHA256']=delta[name]['afterSHA256']
    require(m['allGameplayChanges']==flattened,'flattened gameplay changes')
    auxiliary=m['auxiliaryDelta']
    require(set(auxiliary)=={JUNIT} and set(auxiliary[JUNIT])=={'beforeSHA256','afterSHA256'}
        and auxiliary[JUNIT]['beforeSHA256']==v4['codeBindings'][JUNIT] and auxiliary[JUNIT]['afterSHA256']!=auxiliary[JUNIT]['beforeSHA256'],'V5 JUnit authority test delta')
    source_delta=m['sourceContractDelta']
    require(source_delta=={'path':UIUX,'beforeSHA256':UIUX_BEFORE,'afterSHA256':c.sha(c.safe_file(source,UIUX).read_bytes())},'V5 source-contract fixture delta')
    _validate_fixture_successor(source,m,v4)
    code=set(v4['codeBindings'])|{V4_PATH,MODULE,TESTS,REVIEW,FIXTURE,UIUX,JUNIT_V2}
    require(set(m['codeBindings'])==code,'exact V5 code path set')
    for name,digest in m['codeBindings'].items():
        require(c.sha(c.safe_file(source,name).read_bytes())==digest,'V5 code identity: '+name)
    for name,digest in v4['codeBindings'].items():
        expected=auxiliary[JUNIT]['afterSHA256'] if name==JUNIT else digest
        require(m['codeBindings'][name]==expected,'immutable V4 code binding: '+name)
    require(m['codeBindings'][V4_PATH]==PARENT_SHA,'V4 manifest code binding')
    review_pin=m['independentReview'];require(set(review_pin)=={'path','sha256'} and review_pin['path']==REVIEW and review_pin['sha256']==m['codeBindings'][REVIEW],'independent review binding')
    review=c.strict_json(c.safe_file(source,REVIEW).read_bytes())
    required_bindings=[{'path':name,'beforeSHA256':delta[name]['beforeSHA256'],'afterSHA256':delta[name]['afterSHA256']} for name in sorted(TARGETS)]
    reviewed=[{'path':name,'sha256':m['codeBindings'][name]} for name in sorted(REVIEWED_CODE_PATHS)]
    require(review.get('reviewer')=='independent agent /root/gameplay_gap_audit' and review.get('verdict')=='ACCEPTED_FOR_SOURCE_AUTHORITY_CONSTRUCTION_ONLY'
        and review.get('accepted') is True and review.get('sourceOnly') is True and review.get('sourceAuthorityAccepted') is False
        and review.get('compiledAccepted') is False and review.get('runtimeAccepted') is False and review.get('productReady') is False
        and review.get('sourceBindings')==required_bindings and review.get('reviewedCodeBindings')==reviewed and review.get('findings')==[],'independent static source review')
    identity_names=set(m['codeBindings'])|set(entries)|{PATH,V4_PATH}
    before={name:c.sha(c.safe_file(source,name).read_bytes()) for name in identity_names}
    require(production(source)==observed_production,'V5 production changed during identity validation')
    require(all(c.sha(c.safe_file(source,name).read_bytes())==digest for name,digest in before.items()),'V5 authority changed during identity validation')
    return expected_current,sorted(set(flattened)|code|{PATH})

def _excluded(rel):
    parts=PurePosixPath(rel).parts
    return bool(parts and parts[0] in ROOT_BUILD_EXCLUDES)

def _tree_identity(root):
    root=Path(root).absolute();result={}
    for base,dirs,files in os.walk(root,followlinks=False):
        base_path=Path(base);relative_base=base_path.relative_to(root)
        if relative_base==Path('.'):
            dirs[:]=[d for d in dirs if d not in ROOT_BUILD_EXCLUDES]
        for d in list(dirs):
            p=base_path/d
            require(not p.is_symlink() and not (hasattr(p,'is_junction') and p.is_junction()),'source projection directory link: '+str(p))
        for name in files:
            p=base_path/name;rel=p.relative_to(root).as_posix()
            if _excluded(rel):continue
            require(not p.is_symlink() and not (hasattr(p,'is_junction') and p.is_junction()),'source projection file link: '+rel)
            st=p.stat();require(stat.S_ISREG(st.st_mode) and st.st_nlink==1,'source projection nonregular/hardlinked file: '+rel)
            data=c.safe_file(root,rel).read_bytes();result[rel]=(c.sha(data),len(data))
    return result

def _copy_v4_projection(source,destination,originals):
    source=Path(source).absolute();destination=Path(destination).absolute()
    before=_tree_identity(source)
    def ignore_root(directory,names):
        return [name for name in names if Path(directory).absolute()==source and name in ROOT_BUILD_EXCLUDES]
    shutil.copytree(source,destination,copy_function=shutil.copy2,symlinks=False,ignore=ignore_root)
    copied=_tree_identity(destination);require(copied==before,'V4 projection source copy mismatch')
    require(_tree_identity(source)==before,'candidate changed during V4 projection copy')
    for name,data in originals.items():
        target=destination.joinpath(*PurePosixPath(name).parts);target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
    expected=dict(before)
    for name,data in originals.items():expected[name]=(c.sha(data),len(data))
    require(_tree_identity(destination)==expected,'V4 projection differs outside three bound predecessor entries')
    return before

def _legacy_env(source,env):
    env=dict(env)
    env[SELECTOR]='ATD_PURPUR_GAMEPLAY_SOURCE_V4';env[FILE]=str(source/V4_PATH)
    env.pop(EXPECTED_DIGEST,None)
    env[UIUX_SELECTOR]='ATD_UI_UX_MAINTENANCE_V1';env[UIUX_FILE]=str(source/'tools/certification/authority/ui-ux-maintenance-v1.json')
    env[UI07_SELECTOR]='ATD_UI07_SCOPED_SUCCESSOR_V1';env[UI07_FILE]=str(source/'tools/certification/authority/ui07-successor-v1.json')
    env[c.SELECTOR]=c.AUTHORITY;env[c.APPROVAL_FILE]=str(source/c.CAPSULE_PATH)
    env[GATE_SELECTOR]=c.AUTHORITY;env[GATE_FILE]=str(source/c.CAPSULE_PATH)
    env[REPAIR_SELECTOR]='ATD_GAMEPLAY_JUNIT_REPAIR_V1';env[REPAIR_FILE]=str(source/'tools/certification/authority/gameplay-junit-overlay-v1.json')
    env[FIXTURE_SELECTOR]='ATD_GAMEPLAY_JUNIT_FIXTURE_SOURCE_V1';env[FIXTURE_FILE]=str(source/JUNIT_V1);env[FIXTURE_DIGEST]=JUNIT_V1_SHA
    env.pop('PYTHONPATH',None);env['PYTHONDONTWRITEBYTECODE']='1'
    return env

def _run_gateb(source,env,external_root=None):
    script=source/'tools/build/verify_inputs_v2.py'
    args=[sys.executable,'-B','-X','utf8',str(script),'--source-root',str(source)]
    if external_root is None:args.append('--approval-only')
    else:args.extend(['--external-root',str(external_root)])
    run=subprocess.run(args,cwd=source,env=env,capture_output=True,text=True,timeout=1800)
    require(run.returncode==0,'frozen Gate B V4 projection failed: '+run.stdout[-4000:]+'\n'+run.stderr[-4000:])
    if external_root is None:
        result=c.strict_json(run.stdout.encode('utf-8'))
        require(result.get('authority')==c.AUTHORITY and set(result)=={'authority','sourceAuxiliaryBindings','fixturePaths'},'Gate B approval receipt shape')
    else:require('EXTERNAL_INPUTS_V2_PASS' in run.stdout,'full Gate B projection input receipt')
    return run.stdout

def _run_v4_controls(source,env):
    with tempfile.TemporaryDirectory(prefix='atd-v5-v4-control-pycache-') as pycache:
        require(not any(Path(pycache).rglob('*')),'fresh V4 cache prefix')
        child=dict(env);child['PYTHONPYCACHEPREFIX']=pycache;child['PYTHONDONTWRITEBYTECODE']='1';child.pop('PYTHONPATH',None)
        command=[sys.executable,'-B','-X','utf8','-X','pycache_prefix='+pycache,'-m','tools.build.gameplay_successor_v4','--controls']
        run=subprocess.run(command,cwd=source,env=child,capture_output=True,text=True,timeout=3600)
        require(run.returncode==0 and 'V4_FRESH_HISTORICAL_AND_CURRENT_CONTROLS_PASS' in run.stdout,'frozen V4 controls failed: '+run.stdout[-4000:]+'\n'+run.stderr[-4000:])
        require(not any(Path(pycache).rglob('*')),'V4 control cache prefix remained empty')
        overlay=subprocess.run([sys.executable,'-B','-X','utf8','-X','pycache_prefix='+pycache,'-m','tools.build.gameplay_junit_overlay_v1','--controls'],cwd=source,env=child,capture_output=True,text=True,timeout=900)
        require(overlay.returncode==0 and 'JUNIT_REPAIR_FRESH_CONTROLS_PASS tests=9' in overlay.stdout,'frozen JUnit overlay controls failed: '+overlay.stdout[-3000:]+'\n'+overlay.stderr[-3000:])
        require(not any(Path(pycache).rglob('*')),'JUnit overlay cache prefix remained empty')
        print('V5_JUNIT_OVERLAY_V1_CONTROLS_PASS tests=9')
        return run.stdout+'\n'+overlay.stdout+'\n'+overlay.stderr

def _projection_run(source,env,mode,external_root=None):
    source=Path(source).absolute();v4raw=c.safe_file(source,V4_PATH).read_bytes();require(c.sha(v4raw)==PARENT_SHA,'V4 projection parent digest')
    v4=c.strict_json(v4raw);m=c.strict_json(c.safe_file(source,PATH).read_bytes());originals=_archive_originals(c.safe_file(source,FIXTURE).read_bytes(),m['archiveEntries'])
    with tempfile.TemporaryDirectory(prefix='atd-gameplay-v5-v4-projection-') as tmp:
        projected=Path(tmp)/'source';before=_copy_v4_projection(source,projected,originals)
        v4module=_load_v4(projected,v4);observed=v4module.production(projected)
        require(observed==v4['currentProductionClosure'] and len(observed)==2504,'authentic V4 projected production closure')
        legacy=_legacy_env(projected,env)
        approval=_run_gateb(projected,legacy)
        if mode=='controls':_run_v4_controls(projected,legacy)
        elif mode=='inputs':_run_gateb(projected,legacy,external_root)
        require(_tree_identity(source)==before,'live source changed during V4 projection controls')
        return approval

def _run_current_controls(source,env,expected_digest):
    with tempfile.TemporaryDirectory(prefix='atd-v5-current-controls-pycache-') as pycache:
        require(not any(Path(pycache).rglob('*')),'fresh V5 cache prefix')
        child=dict(env);child['PYTHONPYCACHEPREFIX']=pycache;child['PYTHONDONTWRITEBYTECODE']='1';child.pop('PYTHONPATH',None)
        command=[sys.executable,'-B','-X','utf8','-X','pycache_prefix='+pycache,'-m','unittest','tools.build.tests.test_gameplay_successor_v5','-v']
        run=subprocess.run(command,cwd=source,env=child,capture_output=True,text=True,timeout=1800)
        text=run.stderr
        require(run.returncode==0 and '\nOK\n' in text and 'skipped=' not in text and 'expected failures=' not in text,'V5 current controls failed: '+run.stdout[-4000:]+'\n'+text[-4000:])
        require('\nRan '+str(len(CURRENT_TEST_IDS))+' tests' in text and all(any(line.startswith(name+' ... ') for line in text.splitlines()) for name in CURRENT_TEST_IDS),'V5 exact current test identities/count')
        require(not any(Path(pycache).rglob('*')),'V5 current cache prefix remained empty')
        return text

def run_controls(source,env,expected_digest):
    source=Path(source).absolute();validate(source,env,expected_digest)
    before_prod=production(source);before_identity={p:c.sha(c.safe_file(source,p).read_bytes()) for p in set(c.strict_json(c.safe_file(source,PATH).read_bytes())['codeBindings'])|{PATH,V4_PATH}}
    approval=_projection_run(source,env,'controls')
    _run_current_controls(source,env,expected_digest)
    require(production(source)==before_prod,'live production changed during controls')
    require({p:c.sha(c.safe_file(source,p).read_bytes()) for p in before_identity}==before_identity,'live authority changed during controls')
    validate(source,env,expected_digest)
    print('V5_FRESH_V4_PROJECTION_AND_CURRENT_CONTROLS_PASS v4=12+11 junitOverlayV1=9 current='+str(len(CURRENT_TEST_IDS))+' liveV5Production=2504')
    return approval

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    modes=parser.add_mutually_exclusive_group(required=True)
    modes.add_argument('--approval-only',action='store_true')
    modes.add_argument('--verify-inputs',action='store_true')
    modes.add_argument('--controls',action='store_true')
    modes.add_argument('--junit-fixture-only',action='store_true')
    parser.add_argument('--source-root',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--expected-digest',required=True)
    parser.add_argument('--external-root',type=Path)
    args=parser.parse_args();source=args.source_root.absolute();env=dict(os.environ)
    validate(source,env,args.expected_digest)
    if args.approval_only:
        output=_projection_run(source,env,'approval')
        sys.stdout.write(output)
    elif args.verify_inputs:
        require(args.external_root is not None and args.external_root.is_absolute(),'explicit external inputs root')
        output=_projection_run(source,env,'inputs',args.external_root)
        sys.stdout.write(output)
    elif args.controls:
        run_controls(source,env,args.expected_digest)
    else:
        manifest=c.strict_json(c.safe_file(source,PATH).read_bytes())
        print('V5_JUNIT_FIXTURE_SOURCE_PASS fixture='+manifest['codeBindings'][JUNIT_V2]+' sourceContract='+manifest['codeBindings'][UIUX])

if __name__=='__main__':main()