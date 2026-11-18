"""Current Stage1 identity/negative controls. Historical suites run separately on real originals."""
from pathlib import Path
import contextlib,copy,hashlib,json,os,shutil,tempfile,unittest,zipfile
from tools.build import stage1_test_source_v2 as g
SOURCE=Path(__file__).resolve().parents[3]
class Stage1Controls(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.tmp=tempfile.TemporaryDirectory(prefix='atd-stage1-controls-');cls.root=Path(cls.tmp.name)/'source';cls.root.mkdir();m=g.validate(SOURCE);cls.env=dict(os.environ)
  for rel in set(m['parentSourceInventory'])|g.NEW:
   p=cls.root/rel;p.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(SOURCE/rel,p)
  for k in list(cls.env):
   if k.startswith('ACHIEVETODO_') and k.endswith('_FILE'):cls.env[k]=str(cls.root/'tools/certification/authority'/Path(cls.env[k]).name)
  cls.env[g.FILE]=str(cls.root/g.MAN)
 @classmethod
 def tearDownClass(cls):cls.tmp.cleanup()
 def reject(self,env=None,reason=None):
  with self.assertRaises(ValueError) as caught:g.validate(self.root,self.env if env is None else env)
  if reason:self.assertIn(reason,str(caught.exception))
  print('STAGE1_NEGATIVE_REJECTED '+str(caught.exception))
 @contextlib.contextmanager
 def changed(self,rel,data):
  p=self.root/rel;before=p.read_bytes();p.write_bytes(data)
  try:yield
  finally:p.write_bytes(before)
 def test_exact_current_identity_and_parent_production(self):
  m=g.validate(self.root,self.env);self.assertEqual(3647,len(m['parentSourceInventory']));self.assertFalse(m['PRODUCT_GAMEPLAY_READY']);self.assertEqual(g.NEW,set(m['addedPaths']));self.assertEqual(g.OLD,set(m['modifiedPaths']))
 def test_disposable_root_marker_and_retained_v1(self):
  for rel in g.FROZEN_V1:
   with self.changed(rel,(self.root/rel).read_bytes()+b'negative'):self.reject(reason='immutable Stage1 V1')
  import importlib.util
  with g.legacy_v4_context(SOURCE) as projected:
   marker=projected/'.git';self.assertTrue(marker.is_dir());self.assertEqual([],list(marker.iterdir()))
   path=projected/'tools/phase_b/b7_localization_certification.py';spec=importlib.util.spec_from_file_location('stage1_v2_marker_b7',path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
   self.assertEqual(projected,module.ROOT);self.assertEqual(path.resolve(),Path(module.__file__).resolve())
 def test_unknown_orphan_path_digest_and_parent_selectors(self):
  for key,val in [(g.SELECT,'UNKNOWN'),(g.FILE,str(self.root/'wrong.json')),(g.DIGEST,'0'*64),('ACHIEVETODO_GATE_B_AUTHORITY','UNKNOWN'),('ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY','UNKNOWN')]:
   env=dict(self.env);env[key]=val;self.reject(env)
  env=dict(self.env);env.pop(g.SELECT);self.reject(env)
  for key,rel in g.PARENT_FILES.items():
   env=dict(self.env);env[key]=str(self.root/'wrong-directory'/Path(rel).name);self.reject(env,reason='parent')
  for key in ['ACHIEVETODO_UI_UX_CERTIFICATION_AUTHORITY','ACHIEVETODO_GAMEPLAY_JUNIT_FIXTURE_AUTHORITY_SHA256']:
   env=dict(self.env);env[key]='UNKNOWN';self.reject(env)
 def test_current_code_and_same_path_bytes_rejected(self):
  for rel in [g.UIUX,g.G,g.J,g.F,g.MOD,g.TEST,g.LAUNCH]:
   with self.changed(rel,(self.root/rel).read_bytes()+b'\nnegative-control'):self.reject(reason='file bytes '+rel)
 def test_production_addition_omission_and_same_path(self):
  path='src/main/java/unapproved/Stage1Extra.java';p=self.root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text('unapproved')
  try:self.reject(reason='inventory')
  finally:p.unlink();p.parent.rmdir()
  rel='src/main/java/com/diskree/achievetodo/server/AchieveToDoServer.java';p=self.root/rel;original=p.read_bytes();p.unlink()
  try:self.reject(reason='inventory')
  finally:p.write_bytes(original)
  g.validate(self.root,self.env)
  with self.changed(rel,original+b'\nnegative'):self.reject(reason='file bytes '+rel)
 def test_parent_review_and_archive_tampering(self):
  for rel in [g.PARENT,g.REVIEW,g.ZIP]:
   with self.changed(rel,(self.root/rel).read_bytes()+b'\nnegative'):self.reject()
  m=g.validate(self.root,self.env)
  import io
  b=io.BytesIO()
  with zipfile.ZipFile(b,'w') as z:z.writestr('../escape','invalid')
  with self.changed(g.ZIP,b.getvalue()):
   with self.assertRaises(ValueError):g.originals(self.root,m)
 def test_readiness_manifest_and_build_normalization_fail_closed(self):
  m=g.validate(self.root,self.env);m['PRODUCT_GAMEPLAY_READY']=True;raw=json.dumps(m).encode();env=dict(self.env);env[g.DIGEST]=hashlib.sha256(raw).hexdigest()
  with self.changed(g.MAN,raw):self.reject(env,reason='readiness')
  with self.changed('build.gradle',(self.root/'build.gradle').read_bytes()+b'\nnegative'):self.reject(reason='build bytes')
if __name__=='__main__':unittest.main()
