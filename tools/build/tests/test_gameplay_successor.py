"""Exact gameplay source composition controls; no fabricated runtime/certification receipts."""
import json,os,shutil,tempfile,unittest
from pathlib import Path
from tools.build import distribution_b_v2_contract as c
from tools.build import gameplay_successor as g
ROOT=Path(__file__).resolve().parents[3]
def predecessor():
 a=c.strict_json((ROOT/c.GATE_A_MANIFEST).read_bytes())['productionClosure'];u=c.strict_json((ROOT/c.UI07_MANIFEST).read_bytes());a=dict(a);d=u['productionDelta'];a[d['path']]=d['afterSHA256'];return a
class GameplaySourceTests(unittest.TestCase):
 def check(self,root=ROOT,env=None,base=None):
  e=dict(os.environ);e[g.SELECTOR]=g.AUTHORITY;e[g.FILE]=str(Path(root)/g.PATH)
  if env is not None:e.update(env)
  return g.validate(root,e,c.GAMEPLAY_SUCCESSOR_SHA256,predecessor() if base is None else base)
 def test_exact_current_source(self):
  result,paths=self.check();self.assertEqual(2500,len(result));self.assertTrue(g.ALLOWED<=set(paths))
 def test_unknown_orphan_and_wrong_path(self):
  for env in [{g.SELECTOR:'UNKNOWN'},{g.SELECTOR:None},{g.FILE:None},{g.FILE:str(ROOT/'unknown.json')},{'ACHIEVETODO_UI07_AUTHORITY':None}]:
   with self.subTest(env=env),self.assertRaises(ValueError):self.check(env=env)
 def test_wrong_predecessor(self):
  before=predecessor();before[next(iter(before))]='0'*64
  with self.assertRaises(ValueError):self.check(base=before)
 def test_mutated_code_manifest_product_and_extra(self):
  m=c.strict_json((ROOT/g.PATH).read_bytes());paths=set(predecessor())|g.ALLOWED|g.CODE_PATHS|{g.PATH,c.POLICY_PATH}
  # policy() reads its exact pinned policy only; production closure and successor bytes are copied unchanged.
  for target in ['tools/build/gameplay_successor.py',g.PATH,next(p for p in g.ALLOWED if '/src/main/' in p),'src/main/extra-negative-control.txt']:
   with self.subTest(target=target),tempfile.TemporaryDirectory(prefix='atd-gameplay-source-') as t:
    d=Path(t)
    for rel in paths:
     src=ROOT/rel
     if src.is_file():dst=d/rel;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst)
    p=d/target;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(p.read_bytes()+b'\nTAMPER' if p.exists() else b'UNAPPROVED')
    with self.assertRaises(ValueError):self.check(root=d)
if __name__=='__main__':unittest.main()
