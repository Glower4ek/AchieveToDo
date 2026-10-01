"""Keep the historical test immutable; adapt only its release-stage marker fixture."""
import pathlib, hashlib, json, shutil
OUT = pathlib.Path(__file__).resolve().parent
ROOT = OUT.parents[2]
source = ROOT/'src/test/java/com/diskree/achievetodo/client/ExternalPackCompatibilityTest.java'
raw = source.read_bytes()
text = raw.decode('utf-8')
old_class = 'class ExternalPackCompatibilityTest {'
new_class = 'class Pre26ReleaseExternalPackCompatibilityTest {'
needle = 'properties.setProperty("llamaCarpetNbtMapping", "equipment.body");'
assert text.count(old_class) == text.count(needle) == 1
text = text.replace(old_class,new_class).replace(needle,needle+'\n            properties.setProperty("raiderPredicateKeys", "snake_case");')
target = OUT/'java/com/diskree/achievetodo/client/Pre26ReleaseExternalPackCompatibilityTest.java'
target.parent.mkdir(parents=True,exist_ok=True)
target.write_bytes(text.encode('utf-8'))
failed = OUT/'diagnostics/initial_gate_failure'
failed.mkdir(parents=True,exist_ok=True)
for name in ['pre_release_gates.gradle.log','pre_release_gates.exit','gate_arguments.json']:
    shutil.copyfile(OUT/name,failed/name)
shutil.copytree(OUT/'junit',failed/'junit',dirs_exist_ok=True)
record = {'classification':'STALE_TEST_FIXTURE_NO_PRODUCTION_BUG','failedTest':'ExternalPackCompatibilityTest.admitsOnlyRawOrCurrentCompatiblePackCopies','failedLine':867,'cause':'The historical current marker fixture lacks the authorized FINAL19 mandatory raiderPredicateKeys=snake_case property. Production correctly rejects it.','sourcePath':source.relative_to(ROOT).as_posix(),'sourceSha256':hashlib.sha256(raw).hexdigest(),'releaseTestPath':target.relative_to(ROOT).as_posix(),'releaseTestSha256':hashlib.sha256(target.read_bytes()).hexdigest(),'adaptation':'Class renamed to Pre26ReleaseExternalPackCompatibilityTest; one fixture property added; every original assertion retained.','productionChanges':False,'historicalTestChanged':False,'acceptedEvidenceRegenerated':False,'originalGateCounts':{'tests':72,'failures':1,'errors':0,'skipped':0}}
(OUT/'stale_fixture_diagnosis.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
print(json.dumps(record,indent=2))
