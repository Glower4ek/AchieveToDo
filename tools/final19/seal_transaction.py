"""Preserve accepted gate outputs in FINAL19 namespace before advancing."""
import json, pathlib, shutil
from checkpoint import ROOT, BASE, sha, write, main as checkpoint

def main():
    state=json.loads((BASE/'execution_state.json').read_text(encoding='utf-8-sig'))
    scratch=ROOT/'build/tmp/final19_implementation'
    prefixes={'WORLDGEN_HOLDERSET_CONTEXT':'worldgen','ENTITY_VARIANT_COMPONENT_CONTEXT':'variants',
              'DUAL_ITEM_BLOCK_TAG_CONTEXT':'dual_tags','INVENTORY_ENCHANTMENT_ITEM_TAG_CONTEXT':'inventory_masters',
              'MIXED_WORLDGEN_PREDICATE_CONTEXT':'mixed_worldgen',
              'RAIDER_PREDICATE_KEY_MIGRATION':'raider','SKELETON_PROJECTILE_BLOCK_RUNTIME_PROOF':'shield'}
    for family in state['completedFamilies']:
        output=BASE/'accepted_transactions'/family.lower()
        if output.exists():
            if family in ('RAIDER_PREDICATE_KEY_MIGRATION','SKELETON_PROJECTILE_BLOCK_RUNTIME_PROOF'):
                prefix=prefixes[family]
                assert (scratch/(prefix+'_persistent_regression.exit')).read_text().strip()=='0'
                inventory=json.loads((output/'gate_inventory.json').read_text(encoding='utf-8'))
                for suffix in ('_persistent_regression.exit','_persistent_regression.gradle.log'):
                    source=scratch/(prefix+suffix)
                    shutil.copyfile(source,output/source.name)
                    inventory['files'][(output/source.name).relative_to(ROOT).as_posix()]=sha(output/source.name)
                inventory['transactionSeal']='GREEN'
                write(output/'gate_inventory.json',inventory)
                gate=next(g for g in state['gates'] if g['family']==family)
                gate['persistentRegression']=gate['transactionSeal']='GREEN'
            continue
        prefix=prefixes[family]
        accepted=next(e for e in state['persistentEvidenceInventory'] if e['family']==family)
        assert sha(ROOT/pathlib.Path(accepted['path']))==accepted['sha256']
        assert accepted['runId'] in state['acceptedRunIds']
        output.mkdir(parents=True)
        files={}
        for suffix in ['_canary_evidence.json','_canary_run.json','_run.json','_static.json','_canary_actual_audit.json','_exact_actual_audit.json',
                       '_compile_static.gradle.log','_compile_static.exit','_canary-processes.json','_exact-processes.json',
                       '_canary.gradle.log','_canary.exit','_exact.gradle.log','_exact.exit',
                       '_validation.gradle.log','_validation.exit','_independent_validation.gradle.log',
                       '_independent_validation.exit','_promotion.gradle.log','_promotion.exit',
                       '_persistent_regression.gradle.log','_persistent_regression.exit']:
            source=scratch/(prefix+suffix)
            if source.exists():
                shutil.copyfile(source,output/source.name)
                files[(output/source.name).relative_to(ROOT).as_posix()]=sha(output/source.name)
        for source in (scratch/'junit').glob('TEST-*.xml'):
            if 'Final19' in source.name:
                shutil.copyfile(source,output/source.name)
                files[(output/source.name).relative_to(ROOT).as_posix()]=sha(output/source.name)
        write(output/'gate_inventory.json',{'family':family,'acceptedPersistentEvidence':accepted,'files':files})
    state['transactionStage']='TERMINAL_RECONCILIATION' if len(state['completedFamilies'])==7 else 'DERIVE_EXACT_SEMANTICS'
    state['firstUnfinishedTransaction']=state['activeFamily']+': first unfinished atomic transaction'
    write(BASE/'execution_state.json',state)
    checkpoint()
if __name__=='__main__':main()
