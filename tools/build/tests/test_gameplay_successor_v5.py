"""V5 current-source authority controls; V4 historical controls run separately."""
import io, json, os, stat, tempfile, unittest, zipfile
from pathlib import Path
from tools.build import gameplay_successor_v5 as g
ROOT=Path(__file__).resolve().parents[3]

class V5Controls(unittest.TestCase):
    def env(self,root=ROOT):
        m=g.c.strict_json((root/g.PATH).read_bytes())
        digest=g.c.sha((root/g.PATH).read_bytes())
        env=dict(os.environ)
        env.update({
            g.SELECTOR:g.AUTHORITY,g.FILE:str(root/g.PATH),g.EXPECTED_DIGEST:digest,
            g.c.SELECTOR:g.c.AUTHORITY,g.GATE_SELECTOR:g.c.AUTHORITY,
            'ACHIEVETODO_UI07_AUTHORITY':'ATD_UI07_SCOPED_SUCCESSOR_V1',
            g.REPAIR_SELECTOR:'ATD_GAMEPLAY_JUNIT_REPAIR_V1',
            g.FIXTURE_SELECTOR:'ATD_GAMEPLAY_JUNIT_FIXTURE_SOURCE_V2',
            g.FIXTURE_FILE:str(root/g.JUNIT_V2),
            g.FIXTURE_DIGEST:m['codeBindings'][g.JUNIT_V2],
        })
        return env,digest,m

    def test_exact_current_identity(self):
        env,digest,m=self.env();production,paths=g.validate(ROOT,env,digest)
        self.assertEqual(2504,len(production))
        self.assertEqual(m['currentProductionClosure'],production)
        self.assertTrue(g.TARGETS.issubset(production))
        self.assertIn(g.TESTS,paths)
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp)
            (root/"source.txt").write_bytes(b"source bytes")
            cache=root/".gradle";cache.mkdir();(cache/"locked-cache.bin").write_bytes(b"generated cache")
            self.assertEqual({"source.txt":(g.c.sha(b"source bytes"),len(b"source bytes"))},g._tree_identity(root))

    def test_selector_orphan_parent_and_wrong_path_rejected(self):
        env,digest,_=self.env()
        for changed in [
            {g.SELECTOR:None},{g.SELECTOR:'UNKNOWN'},
            {g.SELECTOR:'ATD_PURPUR_GAMEPLAY_SOURCE_V4'},
            {g.FILE:None},{g.FILE:str(ROOT/'wrong')},
            {g.c.SELECTOR:None},{g.GATE_SELECTOR:None},
            {g.FIXTURE_SELECTOR:None},{g.FIXTURE_FILE:str(ROOT/'wrong')},
        ]:
            with self.subTest(changed=changed),self.assertRaises(ValueError):
                g.validate(ROOT,{**env,**changed},digest)

    def test_wrong_manifest_digest_rejected(self):
        env,_,_=self.env()
        with self.assertRaises(ValueError):g.validate(ROOT,env,'0'*64)

    def test_two_production_deltas_and_full_closure_are_exact(self):
        env,digest,m=self.env();v4=g._load_json(ROOT,g.V4_PATH,g.PARENT_SHA,'V4 parent')
        self.assertEqual(g.TARGETS,set(m['productionDelta']))
        for path,row in m['productionDelta'].items():
            self.assertEqual(v4['currentProductionClosure'][path],row['beforeSHA256'])
            self.assertEqual(row['afterSHA256'],g.c.sha(g.c.safe_file(ROOT,path).read_bytes()))
        self.assertEqual(2504,len(m['currentProductionClosure']))
        self.assertEqual(m['currentProductionClosure'],g.production(ROOT))
        g.validate(ROOT,env,digest)

    def test_fixture_exact_originals_and_hostile_archives(self):
        env,digest,m=self.env();raw=(ROOT/g.FIXTURE).read_bytes()
        original=g._archive_originals(raw,m['archiveEntries'])
        self.assertEqual(g.TARGETS|{g.JUNIT,g.UIUX},set(original))
        variants=[{},dict(list(original.items())[:-1]),{**original,'../escape':b'x'},{**original,'EXTRA':b'x'}]
        for values in variants:
            b=io.BytesIO()
            with zipfile.ZipFile(b,'w',compression=zipfile.ZIP_STORED) as z:
                for name,data in values.items():z.writestr(name,data)
            with self.subTest(entries=sorted(values)),self.assertRaises(ValueError):g._archive_originals(b.getvalue(),m['archiveEntries'])
        corrupt=bytearray(raw);corrupt[len(corrupt)//2]^=1
        with self.assertRaises(ValueError):g._archive_originals(bytes(corrupt),m['archiveEntries'])
        compressed=io.BytesIO()
        with zipfile.ZipFile(compressed,'w',compression=zipfile.ZIP_DEFLATED) as z:
            for name,data in original.items():
                info=zipfile.ZipInfo(name);info.compress_type=zipfile.ZIP_DEFLATED;info.create_system=3;info.external_attr=(stat.S_IFREG|0o644)<<16;z.writestr(info,data)
        with self.assertRaises(ValueError):g._archive_originals(compressed.getvalue(),m['archiveEntries'])
        g.validate(ROOT,env,digest)

    def test_source_contract_fixture_successor_is_bound(self):
        env,digest,m=self.env();v4=g._load_json(ROOT,g.V4_PATH,g.PARENT_SHA,'V4 parent')
        g._validate_fixture_successor(ROOT,m,v4)
        bad=dict(env);bad[g.FIXTURE_FILE]=str(ROOT/g.JUNIT_V1)
        with self.assertRaises(ValueError):g.validate(ROOT,bad,digest)

if __name__=='__main__':unittest.main()