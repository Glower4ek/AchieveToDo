"""Exact G test-source successor. Independent external review of bootstrap is required.
No OwnerB approval is issued here. No historical helper/source is modified.
"""
from pathlib import Path
import io, os, stat, sys, zipfile, subprocess, json
from tools.build import distribution_b_v2_contract as c
AUTHORITY="ATD_GAMEPLAY_JUNIT_REPAIR_V1"
SELECTOR="ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR"
FILE="ACHIEVETODO_GAMEPLAY_JUNIT_REPAIR_FILE"
PATH="tools/certification/authority/gameplay-junit-overlay-v1.json"
MODULE="tools/build/gameplay_junit_overlay_v1.py"
TESTS="tools/build/tests/test_gameplay_junit_overlay_v1.py"
G="src/test/java/com/diskree/achievetodo/certification/GateBInputAuthorityContractTest.java"
ZIP="tools/certification/fixtures/gameplay-junit-overlay-predecessor-v1.zip"
OLD="dd0ea394fbe31839781bfd4a2b4c04b84f78c70ede6f858395c9f20e0e50cbcd"
V4="e069f6109cbbf2dd679859b10e814d987e77ba8c9c9338899e13be9b4abcce53"
CAP="39116d736a8c352aca6a05453474f0fae965cc300a075fdabdbf266523a3f9f9"
CODE={G,MODULE,TESTS,ZIP}
TEST_IDS=["test_exact_composition","test_legacy_current_new_test_rejected","test_new_selection_missing_orphan_unknown_alias","test_manifest_and_code_tamper","test_original_archive_exact_and_hostile","test_other_seven_owner_bindings","test_parent_capsule_and_v4_tamper","test_same_path_change_and_hardlink","test_candidate_report_rejects_repair_selectors"]

def canonical_selection(source,env):
    source=Path(source).absolute();c._no_links(source)
    c.require(env.get(SELECTOR)==AUTHORITY and env.get(FILE)==str(source/PATH),"JUnit repair unknown/orphan/path selector")
    c.require(env.get(c.SELECTOR)==c.AUTHORITY and env.get(c.APPROVAL_FILE)==str(source/c.CAPSULE_PATH),"JUnit repair requires GateB")
    c.require(env.get("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY")=="ATD_PURPUR_GAMEPLAY_SOURCE_V4" and env.get("ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE")==str(source/c.GAMEPLAY_V4_MANIFEST),"JUnit repair requires canonical V4")
    c.require(env.get("ACHIEVETODO_UI07_AUTHORITY")=="ATD_UI07_SCOPED_SUCCESSOR_V1" and env.get("ACHIEVETODO_UI07_AUTHORITY_FILE")==str(source/c.UI07_MANIFEST),"JUnit repair requires canonical UI07")
    return source

def original(raw):
    c.require(len(raw)<30000,"JUnit original archive bound")
    try:
        with zipfile.ZipFile(io.BytesIO(raw)) as z:
            infos=z.infolist();c.require(len(infos)==1,"JUnit original archive cardinality");i=infos[0]
            c.require(i.filename==i.orig_filename==G and not i.is_dir() and not i.flag_bits&1 and i.compress_type==zipfile.ZIP_STORED and stat.S_IFMT(i.external_attr>>16) in (0,stat.S_IFREG),"JUnit original archive regular exact member")
            data=z.read(i);c.require(len(data)==i.file_size and c.sha(data)==OLD,"JUnit original bytes");return data
    except (zipfile.BadZipFile,RuntimeError,OSError) as e:raise ValueError("JUnit original invalid archive") from e

def validate(source,env,m):
    source=canonical_selection(source,env)
    c.require(set(m)=={"schema","authority","state","parentV4","ownerBCapsule","originalG","codeBindings","scope"} and m["schema"]==1 and m["authority"]==AUTHORITY and m["state"]=="EXACT_TEST_SOURCE_SUCCESSOR_ONLY_NOT_OWNER_B_APPROVAL","JUnit exact schema/scope")
    c.require(m["scope"]=="Only GateBInputAuthorityContractTest stream separation; no production or historic authority edits","JUnit exact scope text")
    c.require(m["parentV4"]=={"path":c.GAMEPLAY_V4_MANIFEST,"sha256":V4} and m["ownerBCapsule"]=={"path":c.CAPSULE_PATH,"sha256":CAP} and m["originalG"]=={"path":G,"sha256":OLD},"JUnit immutable parent identities")
    for parent in (m["parentV4"],m["ownerBCapsule"]):c.require(c.sha(c.safe_file(source,parent["path"]).read_bytes())==parent["sha256"],"JUnit parent bytes")
    code=m["codeBindings"];c.require(isinstance(code,dict) and set(code)==CODE,"JUnit exact code closure")
    for p,digest in code.items():c.require(isinstance(digest,str) and len(digest)==64 and c.sha(c.safe_file(source,p).read_bytes())==digest,"JUnit successor bytes: "+p)
    c.require(code[G]!=OLD,"JUnit requires new G postimage");original(c.safe_file(source,ZIP).read_bytes())
    capsule=c.strict_json(c.safe_file(source,c.CAPSULE_PATH).read_bytes())
    c.require(capsule["codeBindings"].get(G)==OLD and len(capsule["codeBindings"])==8,"JUnit original capsule G binding")
    for p,digest in capsule["codeBindings"].items():
        if p!=G:c.require(c.sha(c.safe_file(source,p).read_bytes())==digest,"JUnit other original OwnerB code: "+p)
    return {"historicalOriginalG":OLD,"currentSuccessorG":code[G],"scope":"NOT_OWNER_B_APPROVAL"}

def controls(source,env):
    source=Path(source).absolute();c.validate_selection(source,env)
    names=CODE|{PATH,"build.gradle","tools/build/distribution_b_v2_contract.py","src/test/java/com/diskree/achievetodo/certification/UiUxMaintenanceSourceContract.java"}
    before={p:c.sha(c.safe_file(source,p).read_bytes()) for p in names}
    import tempfile
    with tempfile.TemporaryDirectory(prefix="atd-junit-repair-pycache-") as prefix:
        clean={**env,"PYTHONPYCACHEPREFIX":prefix,"PYTHONDONTWRITEBYTECODE":"1"};clean.pop("PYTHONPATH",None)
        run=subprocess.run([sys.executable,"-B","-X","utf8","-X","pycache_prefix="+prefix,"-m","unittest","tools.build.tests.test_gameplay_junit_overlay_v1","-v"],cwd=source,env=clean,capture_output=True,text=True,timeout=600)
        print(run.stdout,end="");print(run.stderr,end="",file=sys.stderr)
        expected=[x+" (tools.build.tests.test_gameplay_junit_overlay_v1.RepairControls."+x+") ... ok" for x in TEST_IDS]
        found=[line for line in run.stderr.splitlines() if line.startswith("test_") and " ... " in line]
        c.require(run.returncode==0 and sorted(found)==sorted(expected) and "\nRan 9 tests" in run.stderr and "\nOK\n" in run.stderr and "skipped=" not in run.stderr,"JUnit exact fresh control terminal")
        c.require(not any(Path(prefix).rglob("*")),"JUnit fresh cache remained empty")
    c.validate_selection(source,env)
    after={p:c.sha(c.safe_file(source,p).read_bytes()) for p in names};c.require(before==after,"JUnit source changed during controls")
    print("JUNIT_REPAIR_FRESH_CONTROLS_PASS tests=9 historical_controls=SEPARATE_NORMAL_GRADLE_NODE")

if __name__=="__main__":
    if sys.argv[1:]!=["--controls"]:raise ValueError("Only explicit fresh controls mode")
    controls(Path.cwd(),os.environ)
