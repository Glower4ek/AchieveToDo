"""Explicit test-only Stage1 successor over frozen R5. No product/runtime approval."""
from pathlib import Path
import argparse,contextlib,hashlib,importlib,io,json,os,shutil,stat,subprocess,sys,tempfile,zipfile
HERE=Path(__file__).resolve().parents[2]
CONTRACT=HERE/'tools/build/distribution_b_v2_contract.py'
if hashlib.sha256(CONTRACT.read_bytes()).hexdigest()!='f2650bcca64212a0bbeb3e38990f301f8becf74ffe472139e3d6992949621a76':raise ValueError('STAGE1_REJECTED: frozen contract before import')
from tools.build import distribution_b_v2_contract as c
ID='ATD_STAGE1_R5_TEST_SOURCE_V1'
MAN='tools/certification/authority/stage1-test-source-v1.json'
MOD='tools/build/stage1_test_source_v1.py'
TEST='tools/build/tests/test_stage1_test_source_v1.py'
ZIP='tools/certification/fixtures/stage1-r5-predecessor-v1.zip'
REVIEW='tools/certification/authority/stage1-test-source-review-v1.json'
PARENT='tools/certification/authority/gameplay-successor-v5.json'
PARENT_SHA='225c0d70f862db2260648a432526465b969fa958d10d92c60e696ddb7f496294'
PARENT_INVENTORY_SHA='a0d575bda884f4d1639af14b1d041689ed534ae8afb2af26396e8f0a5aa9aada'
UIUX='src/test/java/com/diskree/achievetodo/certification/UiUxMaintenanceSourceContract.java'
G='src/test/java/com/diskree/achievetodo/certification/GateBInputAuthorityContractTest.java'
J='src/test/java/com/diskree/achievetodo/certification/GameplaySuccessorAuthorityTest.java'
F='src/test/java/com/diskree/achievetodo/client/PhaseBPackTestFixtures.java'
OLD={'build.gradle',UIUX,G,J,F}
NEW={MAN,MOD,TEST,ZIP,REVIEW}
CODE={UIUX,G,J,F,MOD,TEST,ZIP}
SELECT='ATD_STAGE1_AUTHORITY';FILE='ATD_STAGE1_AUTHORITY_FILE';DIGEST='ATD_STAGE1_AUTHORITY_SHA256'
PARENT_FILES={
 'ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE':PARENT,
 'ACHIEVETODO_GATE_B_APPROVAL_FILE':'tools/certification/authority/distribution-b-v2-approval.json',
 'ACHIEVETODO_UI_UX_CERTIFICATION_FILE':'tools/certification/authority/ui-ux-maintenance-v1.json',
 'ACHIEVETODO_UI07_AUTHORITY_FILE':'tools/certification/authority/ui07-successor-v1.json',
 'ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR_FILE':'tools/certification/authority/gameplay-junit-overlay-v1.json',
 'ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY_FILE':'tools/certification/authority/gameplay-junit-fixture-successor-v2.json',
}
EXCLUDES=('build/','.gradle/','.git/','logs/','port/AchieveToDo-Purpur-26.2/build/','port/AchieveToDo-Purpur-26.2/.gradle/','port/AchieveToDo-Purpur-26.2/logs/')
def require(ok,reason):
 if not ok:raise ValueError('STAGE1_REJECTED: '+reason)
def digest(data):return hashlib.sha256(data).hexdigest()
def filehash(root,p):return digest(c.safe_file(root,p).read_bytes())
def originals(root,m):
 raw=c.safe_file(root,ZIP).read_bytes();require(len(raw)<512000,'predecessor archive size')
 with zipfile.ZipFile(io.BytesIO(raw)) as z:
  entries=z.infolist();require(len(entries)==len(OLD) and {i.filename for i in entries}==OLD,'exact predecessor archive inventory')
  out={}
  for i in entries:
   require(i.filename==i.orig_filename and not i.is_dir() and not i.flag_bits&1 and i.compress_type==zipfile.ZIP_STORED and stat.S_IFMT(i.external_attr>>16) in (0,stat.S_IFREG),'regular predecessor member')
   data=z.read(i);require(len(data)<200000 and digest(data)==m['parentSourceInventory'][i.filename],'predecessor bytes: '+i.filename);out[i.filename]=data
 return out
def inventory(root):
 return {p.relative_to(root).as_posix() for p in root.rglob('*') if p.is_file() and not p.relative_to(root).as_posix().startswith(EXCLUDES) and '__pycache__' not in p.parts and p.suffix!='.pyc'}
def validate(root,env=None):
 root=Path(root).absolute();env=dict(os.environ) if env is None else env;c._no_links(root)
 require(root.resolve()==root,'canonical source root')
 require(env.get(SELECT)==ID and env.get(FILE)==str(root/MAN),'unknown/orphan/noncanonical Stage1 selector')
 expected=env.get(DIGEST);require(isinstance(expected,str) and len(expected)==64 and all(x in '0123456789abcdef' for x in expected),'Stage1 digest selector')
 require(env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY')=='ATD_PURPUR_GAMEPLAY_SOURCE_V5' and env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE')==str(root/PARENT) and env.get('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_SHA256')==PARENT_SHA,'exact V5 parent selection')
 for key,value in [('ACHIEVETODO_GATE_B_AUTHORITY',c.AUTHORITY),('ACHIEVETODO_GATE_B_APPROVAL_FILE',str(root/c.CAPSULE_PATH)),('ACHIEVETODO_UI07_AUTHORITY','ATD_UI07_SCOPED_SUCCESSOR_V1'),('ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR','ATD_GAMEPLAY_JUNIT_REPAIR_V1'),('ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY','ATD_GAMEPLAY_JUNIT_FIXTURE_SOURCE_V2')]:require(env.get(key)==value,'parent selector '+key)
 require(env.get('ACHIEVETODO_UI_UX_CERTIFICATION_AUTHORITY')=='ATD_UI_UX_MAINTENANCE_V1','Gate A selector')
 require(env.get('ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY_SHA256')=='d5e4361d87b4023927ecf32c15ac1e2d6fe8ad0aa367001dfc0d13f501b6112f','fixture V2 digest selector')
 require({k for k in env if k.startswith('ATD_STAGE1_')}=={SELECT,FILE,DIGEST},'Stage1 selector namespace')
 for key,rel in PARENT_FILES.items():require(env.get(key)==str(root/rel),'noncanonical parent file '+key)
 require(not any(k in env for k in ['ACHIEVETODO_INPUT_AUTHORITY','ACHIEVETODO_ALTERNATIVE_MANIFEST','ACHIEVETODO_SOURCE_ORACLE','ACHIEVETODO_SOURCE_ORACLE_FILE']),'mixed historical/alternative selectors')
 raw=c.safe_file(root,MAN).read_bytes();require(digest(raw)==expected,'manifest digest');m=c.strict_json(raw)
 require(set(m)=={'schema','authority','state','parentV5','parentInventorySHA256','parentSourceInventory','modifiedPaths','addedPaths','codeBindings','normalizedBuildSHA256','independentReview','scope','PRODUCT_GAMEPLAY_READY'},'manifest schema')
 require(m['schema']==1 and m['authority']==ID and m['state']=='BOUNDED_TEST_SOURCE_SUCCESSOR_ONLY' and m['PRODUCT_GAMEPLAY_READY'] is False,'identity/scope/readiness')
 require(m['parentV5']=={'path':PARENT,'sha256':PARENT_SHA} and filehash(root,PARENT)==PARENT_SHA,'immutable V5 parent')
 require(m['parentInventorySHA256']==PARENT_INVENTORY_SHA and digest(json.dumps(m['parentSourceInventory'],sort_keys=True,separators=(',',':')).encode())==PARENT_INVENTORY_SHA,'exact3647parent source inventory')
 require(len(m['parentSourceInventory'])==3647 and set(m['modifiedPaths'])==OLD and set(m['addedPaths'])==NEW,'exact source delta paths')
 require(set(m['codeBindings'])==CODE,'exact current code bindings')
 require(inventory(root)==set(m['parentSourceInventory'])|NEW,'current source inventory additions/omissions')
 for p,h in m['parentSourceInventory'].items():
  if p not in OLD:require(filehash(root,p)==h,'file bytes '+p)
 for p,h in m['codeBindings'].items():require(filehash(root,p)==h,'file bytes '+p)
 for p in OLD-{'build.gradle'}:require(m['modifiedPaths'][p]=={'beforeSHA256':m['parentSourceInventory'][p],'afterSHA256':m['codeBindings'][p]},'exact current delta '+p)
 require(m['modifiedPaths']['build.gradle']=={'beforeSHA256':m['parentSourceInventory']['build.gradle'],'normalizedAfterSHA256':m['normalizedBuildSHA256']},'build binding')
 build=c.safe_file(root,'build.gradle').read_bytes();slot=b"STAGE1_MANIFEST_SHA256='"+expected.encode()+b"'";require(build.count(slot)==1,'single exact manifest digest slot')
 require(digest(build.replace(slot,b"STAGE1_MANIFEST_SHA256='"+b'0'*64+b"'"))==m['normalizedBuildSHA256'],'normalized exact build bytes')
 originals(root,m)
 review=m['independentReview'];require(review['path']==REVIEW and filehash(root,REVIEW)==review['sha256'],'independent code review binding')
 r=c.strict_json(c.safe_file(root,REVIEW).read_bytes());require(r.get('reviewer')=='/root/stage1_independent' and r.get('verdict')=='ACCEPTED_BOUNDED_STAGE1_TEST_SOURCE_FOR_EXECUTION_ONLY' and r.get('accepted') is True and r.get('runtimeAccepted') is False and r.get('PRODUCT_GAMEPLAY_READY') is False and r.get('reviewedCodeBindings')==m['codeBindings'] and r.get('findings')==[],'independent exact code review')
 parent=c.strict_json(c.safe_file(root,PARENT).read_bytes())
 actual={p.relative_to(root).as_posix():digest(p.read_bytes()) for name in ['src/main','port/AchieveToDo-Purpur-26.2/src/main'] for p in (root/name).rglob('*') if p.is_file()}
 require(actual==parent['currentProductionClosure'] and len(actual)==2504,'unchanged2504production closure')
 return m
@contextlib.contextmanager
def parent_context(root,env=None):
 root=Path(root).absolute();env=dict(os.environ) if env is None else dict(env);m=validate(root,env);old=originals(root,m)
 with tempfile.TemporaryDirectory(prefix='atd-stage1-r5-parent-') as tmp:
  parent=Path(tmp)/'source';parent.mkdir()
  for p,h in m['parentSourceInventory'].items():
   target=parent/p;target.parent.mkdir(parents=True,exist_ok=True)
   if p in old:target.write_bytes(old[p])
   else:shutil.copyfile(c.safe_file(root,p),target)
   require(filehash(parent,p)==h,'authentic R5 projection '+p)
  clean={k:v for k,v in env.items() if not k.startswith('ATD_STAGE1_') and k!='PYTHONPATH'}
  for key,rel in PARENT_FILES.items():clean[key]=str(parent/rel)
  clean.update(PYTHONDONTWRITEBYTECODE='1',ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY='ATD_PURPUR_GAMEPLAY_SOURCE_V5',ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE=str(parent/PARENT),ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_SHA256=PARENT_SHA)
  yield parent,clean
 validate(root,env)
def parent_run(root,mode,external=None):
 with parent_context(root) as (parent,env):
  cmd=[sys.executable,'-B','-X','utf8','-m','tools.build.gameplay_successor_v5',mode,'--source-root',str(parent),'--expected-digest',PARENT_SHA]
  if external is not None:cmd+=['--external-root',str(external)]
  r=subprocess.run(cmd,cwd=parent,env=env,capture_output=True,text=True,timeout=5400)
  require(r.returncode==0,'authentic R5 '+mode+' failed: '+r.stdout[-3500:]+'\n'+r.stderr[-3500:]);return r
@contextlib.contextmanager
def legacy_v4_context(root):
 with parent_context(root) as (parent,env):
  from tools.build import gameplay_successor_v5 as g
  g.validate(parent,env,PARENT_SHA)
  m=c.strict_json((parent/PARENT).read_bytes());old=g._archive_originals((parent/g.FIXTURE).read_bytes(),m['archiveEntries'])
  with tempfile.TemporaryDirectory(prefix='atd-stage1-v4-consumer-') as tmp:
   projected=Path(tmp)/'source';g._copy_v4_projection(parent,projected,old);legacy=g._legacy_env(projected,env)
   r=subprocess.run([sys.executable,'-B','-X','utf8',str(projected/'tools/build/verify_inputs_v2.py'),'--source-root',str(projected),'--approval-only'],cwd=projected,env=legacy,capture_output=True,text=True,timeout=1800);require(r.returncode==0,'authenticated V4 consumer context: '+r.stderr[-2000:])
   saved=dict(os.environ);os.environ.clear();os.environ.update(legacy)
   try:yield projected
   finally:os.environ.clear();os.environ.update(saved)
def run_current_controls(root):
 r=subprocess.run([sys.executable,'-B','-X','utf8','-m','unittest','tools.build.tests.test_stage1_test_source_v1','-v'],cwd=root,capture_output=True,text=True,timeout=1800)
 require(r.returncode==0 and '\nRan 6 tests' in r.stderr and '\nOK\n' in r.stderr and 'skipped=' not in r.stderr,'current Stage1 controls: '+r.stdout[-3000:]+r.stderr[-3000:]);return r
def main():
 p=argparse.ArgumentParser();m=p.add_mutually_exclusive_group(required=True)
 for flag in ['approval-only','verify-inputs','controls','junit-fixture-only','gateb-controls','validate-only']:m.add_argument('--'+flag,action='store_true')
 p.add_argument('--source-root',type=Path,default=Path.cwd());p.add_argument('--expected-digest',required=True);p.add_argument('--external-root',type=Path);a=p.parse_args();require(a.expected_digest==PARENT_SHA,'V5 parent digest argument');root=a.source_root.absolute();validate(root)
 if a.validate_only:print('STAGE1_CURRENT_SOURCE_IDENTITY_PASS production=2504 historical_controls=NOT_RUN');return
 if a.gateb_controls:
  with legacy_v4_context(root) as projected:r=subprocess.run([sys.executable,'-B','-X','utf8','-m','unittest','tools.build.tests.test_distribution_b_v2_contract','-v'],cwd=projected,capture_output=True,text=True,timeout=1800)
  require(r.returncode==0 and '\nOK' in r.stderr,'frozen Gate B controls failed');sys.stdout.write(r.stdout);sys.stderr.write(r.stderr);return
 mode='--verify-inputs' if a.verify_inputs else '--controls' if a.controls else '--junit-fixture-only' if a.junit_fixture_only else '--approval-only'
 if a.verify_inputs:require(a.external_root is not None and a.external_root.is_absolute(),'explicit external input root')
 r=parent_run(root,mode,a.external_root);sys.stdout.write(r.stdout);sys.stderr.write(r.stderr)
 if a.controls:
  current=run_current_controls(root);sys.stdout.write(current.stdout);sys.stderr.write(current.stderr);print('STAGE1_FRESH_PARENT_R5_AND_CURRENT_CONTROLS_PASS current=6 production=2504')
 validate(root)
if __name__=='__main__':main()
