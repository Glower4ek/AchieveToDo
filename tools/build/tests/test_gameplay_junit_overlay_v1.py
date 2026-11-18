"""Narrow test-source successor controls; original controls remain unchanged."""
from pathlib import Path
import io,os,tempfile,unittest,zipfile,warnings,subprocess,sys,json
from tools.build import distribution_b_v2_contract as c
from tools.build import gameplay_junit_overlay_v1 as v
ROOT=Path(__file__).resolve().parents[3]
class RepairControls(unittest.TestCase):
    def env(self,root):
        return {**os.environ,c.SELECTOR:c.AUTHORITY,c.APPROVAL_FILE:str(root/c.CAPSULE_PATH),v.SELECTOR:v.AUTHORITY,v.FILE:str(root/v.PATH),"ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY":"ATD_PURPUR_GAMEPLAY_SOURCE_V4","ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE":str(root/c.GAMEPLAY_V4_MANIFEST),"ACHIEVETODO_UI07_AUTHORITY":"ATD_UI07_SCOPED_SUCCESSOR_V1","ACHIEVETODO_UI07_AUTHORITY_FILE":str(root/c.UI07_MANIFEST),"ACHIEVETODO_UI_UX_CERTIFICATION_AUTHORITY":"ATD_UI_UX_MAINTENANCE_V1","ACHIEVETODO_UI_UX_CERTIFICATION_FILE":str(root/c.GATE_A_MANIFEST)}
    def clone(self,root):
        cap=c.strict_json((ROOT/c.CAPSULE_PATH).read_bytes());names=v.CODE|set(cap["codeBindings"])|{v.PATH,c.CAPSULE_PATH,c.GAMEPLAY_V4_MANIFEST,c.GATE_A_MANIFEST,c.POLICY_PATH}
        for p in names:
            q=root/p;q.parent.mkdir(parents=True,exist_ok=True);q.write_bytes((ROOT/p).read_bytes())
    def check(self,root):return c.junit_repair(root,self.env(root))
    def test_exact_composition(self):self.assertEqual(v.OLD,self.check(ROOT)["historicalOriginalG"])
    def test_legacy_current_new_test_rejected(self):
        env=self.env(ROOT);env.pop(v.SELECTOR);env.pop(v.FILE)
        with self.assertRaisesRegex(ValueError,"code binding: "+v.G):c.validate_selection(ROOT,env)
    def test_new_selection_missing_orphan_unknown_alias(self):
        for update in [{v.SELECTOR:None},{v.FILE:None},{v.SELECTOR:"UNKNOWN"},{v.FILE:str(ROOT)+"/./"+v.PATH},{v.FILE:str(ROOT/v.PATH).upper()}]:
            with self.subTest(update=update),self.assertRaises(ValueError):c.junit_repair(ROOT,{**self.env(ROOT),**update})
    def test_manifest_and_code_tamper(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);self.clone(root)
            for p in [v.PATH,*v.CODE]:
                q=root/p;old=q.read_bytes();q.write_bytes(old+b"\n")
                try:
                    with self.subTest(path=p),self.assertRaises(ValueError):self.check(root)
                finally:q.write_bytes(old)
    def test_original_archive_exact_and_hostile(self):
        data=v.original((ROOT/v.ZIP).read_bytes());self.assertEqual(v.OLD,c.sha(data))
        for names in [[],[v.G,v.G],["../escape"],[v.G.upper()]]:
            out=io.BytesIO()
            with warnings.catch_warnings(record=True) as seen:
                warnings.simplefilter("always")
                with zipfile.ZipFile(out,"w") as z:
                    for n in names:z.writestr(n,data)
            self.assertEqual(1 if len(names)>1 else 0,len(seen))
            if seen:self.assertIn("Duplicate name:",str(seen[0].message))
            with self.subTest(names=names),self.assertRaises(ValueError):v.original(out.getvalue())
    def test_other_seven_owner_bindings(self):
        cap=c.strict_json((ROOT/c.CAPSULE_PATH).read_bytes())
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);self.clone(root)
            for p in set(cap["codeBindings"])-{v.G}:
                q=root/p;old=q.read_bytes();q.write_bytes(old+b"\n")
                try:
                    with self.subTest(path=p),self.assertRaises(ValueError):self.check(root)
                finally:q.write_bytes(old)
    def test_parent_capsule_and_v4_tamper(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);self.clone(root)
            for p in [c.CAPSULE_PATH,c.GAMEPLAY_V4_MANIFEST]:
                q=root/p;old=q.read_bytes();q.write_bytes(old+b"\n")
                try:
                    with self.subTest(path=p),self.assertRaises(ValueError):self.check(root)
                finally:q.write_bytes(old)
    def test_candidate_report_rejects_repair_selectors(self):
        # Exercise the unchanged OwnerB-bound CLI, not a synthetic local branch.
        clean=dict(os.environ)
        for key in list(clean):
            if key.startswith("ACHIEVETODO_GATE_B") or key in {v.SELECTOR,v.FILE,"ACHIEVETODO_INPUT_AUTHORITY","ACHIEVETODO_ALTERNATIVE_MANIFEST","ACHIEVETODO_SOURCE_ORACLE","ACHIEVETODO_SOURCE_ORACLE_FILE"}:clean.pop(key,None)
        cases=[{v.SELECTOR:v.AUTHORITY},{v.FILE:str(ROOT/v.PATH)},{v.SELECTOR:v.AUTHORITY,v.FILE:str(ROOT/v.PATH)}]
        for selected in cases:
            command=[sys.executable,"-B","-X","utf8",str(ROOT/"tools/build/verify_inputs_v2.py"),"--source-root",str(ROOT),"--candidate-report"]
            run=subprocess.run(command,cwd=ROOT,env={**clean,**selected},capture_output=True,text=True,timeout=30)
            with self.subTest(keys=sorted(selected)):
                self.assertNotEqual(0,run.returncode,run.stdout+run.stderr)
                self.assertEqual("",run.stdout.strip())
                self.assertIn("candidate diagnostic cannot ignore JUnit repair selectors",run.stderr)
                self.assertNotIn("EXTERNAL_INPUTS_V2_PASS",run.stdout+run.stderr)
                self.assertNotIn("payloadIdentities",run.stdout+run.stderr)
            print("JUNIT_REPAIR_CANDIDATE_REPORT_REJECTED "+json.dumps({"argv":command,"selectors":selected,"exit":run.returncode,"stdout":run.stdout,"stderr":run.stderr,"unchangedCLI_SHA256":c.sha((ROOT/"tools/build/verify_inputs_v2.py").read_bytes())},sort_keys=True))
    def test_same_path_change_and_hardlink(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);self.clone(root);self.check(root);q=root/v.G;old=q.read_bytes();q.write_bytes(old+b"\n")
            with self.assertRaises(ValueError):self.check(root)
            q.write_bytes(old);os.link(q,root/"extra-link")
            with self.assertRaises(ValueError):self.check(root)
if __name__=="__main__":unittest.main()
