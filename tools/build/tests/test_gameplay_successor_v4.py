"""V4 pure current identity and hostile controls. Historical suite is a separate fresh node."""
import copy, importlib.util, io, json, marshal, os, shutil, stat, struct, subprocess, sys, tempfile, threading, unittest, zipfile
from pathlib import Path
from types import SimpleNamespace
from tools.build import gameplay_successor_v4 as g
ROOT=Path(__file__).resolve().parents[3]

def matching_timestamp_pyc(source,payload):
    info=source.stat()
    code=compile(payload,str(source),'exec',dont_inherit=True)
    return importlib.util.MAGIC_NUMBER+struct.pack('<III',0,int(info.st_mtime),info.st_size)+marshal.dumps(code)

class V4Controls(unittest.TestCase):
    def env(self,root):
        return {**os.environ,g.SELECTOR:g.AUTHORITY,g.FILE:str(root/g.PATH),g.c.SELECTOR:g.c.AUTHORITY,'ACHIEVETODO_UI07_AUTHORITY':'ATD_UI07_SCOPED_SUCCESSOR_V1'}
    def check(self,root=ROOT,digest=None):
        return g.validate(root,self.env(root),g.c.GAMEPLAY_V4_SUCCESSOR_SHA256 if digest is None else digest,g.ui07_base(root))
    def clone(self,root):
        m=g.c.strict_json((ROOT/g.PATH).read_bytes())
        for name in set(m['predecessorContext'])|set(m['codeBindings'])|{g.PATH}:
            p=root/name;p.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ROOT/name,p)
    def test_exact_current_identity(self):
        production,paths=self.check();self.assertEqual(2504,len(production));self.assertIn(g.TARGET,paths)
    def test_matching_timestamp_pyc_cannot_replace_authenticated_v3_source(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);self.clone(root)
            source=root/'tools/build/gameplay_successor_v3.py'
            cache=source.parent/'__pycache__'/f'{source.stem}.{sys.implementation.cache_tag}.pyc'
            cache.parent.mkdir(parents=True,exist_ok=True)
            cache.write_bytes(matching_timestamp_pyc(source,"AUTHORITY='PY_CACHE_POISON'\n"))
            previous=sys.pycache_prefix
            try:
                # Reproduce the vulnerable default cache lookup. The repaired loader compiles its pinned bytes.
                sys.pycache_prefix=None
                _,loaded=g._load_v3(root)
            finally:
                sys.pycache_prefix=previous
            self.assertEqual('ATD_PURPUR_GAMEPLAY_SOURCE_V3',loaded.AUTHORITY)
    def test_module_entry_ignores_matching_timestamp_source_and_prefix_pyc(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            for source in (ROOT/'tools').rglob('*.py'):
                if '__pycache__' in source.parts: continue
                target=root/source.relative_to(ROOT)
                target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
            module=root/g.MODULE
            poison=matching_timestamp_pyc(module,"AUTHORITY='PY_CACHE_POISON'\n")
            local_cache=module.parent/'__pycache__'/f'{module.stem}.{sys.implementation.cache_tag}.pyc'
            local_cache.parent.mkdir(parents=True,exist_ok=True);local_cache.write_bytes(poison)
            evil_prefix=root/'attacker-prefix'
            previous=sys.pycache_prefix
            try:
                sys.pycache_prefix=str(evil_prefix)
                prefixed_cache=Path(importlib.util.cache_from_source(str(module)))
            finally:
                sys.pycache_prefix=previous
            prefixed_cache.parent.mkdir(parents=True,exist_ok=True);prefixed_cache.write_bytes(poison)
            fresh_prefix=root/'fresh-prefix';fresh_prefix.mkdir()
            env=dict(os.environ);env['PYTHONPYCACHEPREFIX']=str(evil_prefix);env['PYTHONDONTWRITEBYTECODE']='1';env.pop('PYTHONPATH',None)
            command=[sys.executable,'-B','-X','utf8','-X','pycache_prefix='+str(fresh_prefix),'-c',
                     'import tools.build.gameplay_successor_v4 as m; print(m.AUTHORITY)']
            run=subprocess.run(command,cwd=root,env=env,capture_output=True,text=True,timeout=60)
            self.assertEqual(0,run.returncode,run.stdout+'\n'+run.stderr)
            self.assertEqual(g.AUTHORITY,run.stdout.strip().splitlines()[-1])
            self.assertEqual([],list(fresh_prefix.rglob('*')))
    def test_selector_orphan_parent_and_wrong_path_rejected(self):
        for changed in [{g.SELECTOR:None},{g.SELECTOR:'UNKNOWN'},{g.SELECTOR:'ATD_PURPUR_GAMEPLAY_SOURCE_V3'},{g.FILE:None},{g.FILE:str(ROOT/'wrong')},{g.c.SELECTOR:None},{'ACHIEVETODO_UI07_AUTHORITY':None}]:
            with self.subTest(changed=changed),self.assertRaises(ValueError):g.validate(ROOT,{**self.env(ROOT),**changed},g.c.GAMEPLAY_V4_SUCCESSOR_SHA256,g.ui07_base(ROOT))
    def test_wrong_literal_digest_rejected(self):
        with self.assertRaises(ValueError):self.check(digest='0'*64)
    def test_mixed_public_authority_rejected(self):
        for key in ['ACHIEVETODO_INPUT_AUTHORITY','ACHIEVETODO_ALTERNATIVE_MANIFEST','ACHIEVETODO_SOURCE_ORACLE','ACHIEVETODO_SOURCE_ORACLE_FILE']:
            with self.subTest(key=key),self.assertRaises(ValueError):g.c.validate_selection(ROOT,{**self.env(ROOT),g.c.APPROVAL_FILE:str(ROOT/g.c.CAPSULE_PATH),key:'UNAPPROVED'})
    def test_archive_exact_originals_and_hostile_members(self):
        raw=(ROOT/g.FIXTURE).read_bytes();original=g.archive_originals(raw,g.ARCHIVE_FILES);self.assertEqual(5,len(original))
        variants=[{},dict(list(original.items())[:-1]),{**original,'../escape':b'x'},{**original,'EXTRA':b'x'}]
        for values in variants:
            b=io.BytesIO()
            with zipfile.ZipFile(b,'w') as z:
                for name,data in values.items():z.writestr(name,data)
            with self.assertRaises(ValueError):g.archive_originals(b.getvalue(),g.ARCHIVE_FILES)
        names=list(original)
        for changed in [names+[names[0]],names[:-1]+[names[-1].upper()],names[:-1]+['/absolute'],names[:-1]+['C:/escape'],names[:-1]+['unsafe\\member']]:
            b=io.BytesIO()
            with zipfile.ZipFile(b,'w') as z:
                for name in changed:z.writestr(name,original.get(name,b'not original'))
            with self.assertRaises(ValueError):g.archive_originals(b.getvalue(),g.ARCHIVE_FILES)
        encrypted=bytearray(raw)
        for signature,offset in [(b'PK\x03\x04',6),(b'PK\x01\x02',8)]:
            start=0
            while True:
                index=encrypted.find(signature,start)
                if index<0:break
                encrypted[index+offset]|=1;start=index+4
        with self.assertRaises(ValueError):g.archive_originals(bytes(encrypted),g.ARCHIVE_FILES)
        corrupt=bytearray(raw);corrupt[100]^=1
        with self.assertRaises(ValueError):g.archive_originals(bytes(corrupt),g.ARCHIVE_FILES)
        for mode,compression in [(stat.S_IFLNK,zipfile.ZIP_STORED),(stat.S_IFREG,zipfile.ZIP_DEFLATED)]:
            b=io.BytesIO()
            with zipfile.ZipFile(b,'w') as z:
                for name,data in original.items():
                    info=zipfile.ZipInfo(name);info.create_system=3;info.external_attr=(mode|0o644)<<16;info.compress_type=compression;z.writestr(info,data)
            with self.assertRaises(ValueError):g.archive_originals(b.getvalue(),g.ARCHIVE_FILES)
        for raw in [b'bad ZIP',b'x'*(g.MAX_ARCHIVE+1)]:
            with self.assertRaises(ValueError):g.archive_originals(raw,g.ARCHIVE_FILES)
    def test_all_code_source_review_and_archive_tampering(self):
        m=g.c.strict_json((ROOT/g.PATH).read_bytes());peer=next(p for p in m['currentProductionClosure'] if p!=g.TARGET)
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);self.clone(root)
            for name in [g.PATH,*m['codeBindings'],g.TARGET,peer,'src/main/UNAPPROVED-v4.txt']:
                p=root/name;old=p.read_bytes() if p.exists() else None;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((old or b'')+b'TAMPER')
                with self.subTest(name=name),self.assertRaises(ValueError):self.check(root)
                if old is None:p.unlink()
                else:p.write_bytes(old)
            p=root/peer;old=p.read_bytes();p.unlink()
            with self.assertRaises(ValueError):self.check(root)
            p.write_bytes(old)
    def test_repinned_semantic_mutations_rejected(self):
        m=g.c.strict_json((ROOT/g.PATH).read_bytes());variants=[]
        for key in m['ancestorPins']:
            bad=copy.deepcopy(m);bad['ancestorPins'][key]='0'*64;variants.append(bad)
        for section in ['productionDelta','auxiliaryDelta','codeBindings','predecessorContext']:
            bad=copy.deepcopy(m);bad[section]['UNAPPROVED']='0'*64;variants.append(bad)
        for section,path in [('productionDelta',g.TARGET),('auxiliaryDelta',g.JUNIT)]:
            for field in ['beforeSHA256','afterSHA256']:
                bad=copy.deepcopy(m);bad[section][path][field]='0'*64;variants.append(bad)
        for key in ['oldV3','independentReview','fixture']:
            bad=copy.deepcopy(m);bad[key]['sha256']='0'*64;variants.append(bad)
        bad=copy.deepcopy(m);bad['currentProductionClosure'].pop(next(iter(bad['currentProductionClosure'])));variants.append(bad)
        bad=copy.deepcopy(m);bad['allGameplayChanges'][g.TARGET]['beforeSHA256']=bad['productionDelta'][g.TARGET]['beforeSHA256'];variants.append(bad)
        bad=copy.deepcopy(m);bad['productReady']=True;variants.append(bad)
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);self.clone(root)
            for index,bad in enumerate(variants):
                raw=json.dumps(bad).encode();(root/g.PATH).write_bytes(raw)
                with self.subTest(index=index),self.assertRaises(ValueError):self.check(root,g.c.sha(raw))
    def test_midvalidation_mutation_and_hardlink_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);self.clone(root)
            peer=next(p for p in (root/'src/main').rglob('*') if p.is_file())
            original=peer.read_bytes()
            # Real concurrent file mutation, no authority/module monkeypatch or bypass hook.
            timer=threading.Timer(2.0,lambda:peer.write_bytes(original+b'MID_VALIDATION_TAMPER'))
            timer.start()
            try:
                with self.assertRaisesRegex(ValueError,'source changed during identity validation'):self.check(root)
            finally:
                timer.join();peer.write_bytes(original)
            link_source=root/'hardlink-original.bin';link_source.write_bytes(original);peer.unlink();os.link(link_source,peer)
            with self.assertRaisesRegex(ValueError,'hardlinked'):self.check(root)
    def test_parent_exact_ids_count_and_terminal_are_required(self):
        text='\n'.join(x+' ... ok' for x in g.PARENT_TEST_IDS)+'\n\nRan 12 tests in 1.0s\n\nOK\n'
        g.require_parent_result(SimpleNamespace(returncode=0,stderr=text),g.PARENT_TEST_IDS)
        for code,value in [(1,text),(0,text.replace('12 tests','11 tests')),(0,text.replace('OK','OK (skipped=1)')),(0,text.replace('OK','OK (expected failures=1)')),(0,text.replace(g.PARENT_TEST_IDS[0],g.PARENT_TEST_IDS[1])),(0,'\nRan 12 tests\n\nOK\n')]:
            with self.assertRaises(ValueError):g.require_parent_result(SimpleNamespace(returncode=code,stderr=value),g.PARENT_TEST_IDS)

if __name__=='__main__':unittest.main()
