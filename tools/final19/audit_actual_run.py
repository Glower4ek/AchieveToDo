"""Independent actual TEMP/run-state audit; no invented receipt/count or promotion."""
import hashlib,json,pathlib,sys,shutil
ROOT=pathlib.Path(__file__).resolve().parents[2]
BASE=ROOT/'reference/phase_a_planning/final19'
SCRATCH=ROOT/'build/tmp/final19_implementation'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,v):p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def main():
    family,mode=sys.argv[1:];assert mode in ('canary','exact');exact=mode=='exact'
    prefix,executor={'DUAL_ITEM_BLOCK_TAG_CONTEXT':('dual_tags','Final19DualTagsGameTest'),'INVENTORY_ENCHANTMENT_ITEM_TAG_CONTEXT':('inventory_masters','Final19InventoryMastersGameTest'),'MIXED_WORLDGEN_PREDICATE_CONTEXT':('mixed_worldgen','Final19MixedWorldgenGameTest')}[family]
    catalog_path='src/test/resources/final19_certification/'+family.lower()+'_catalog.json';catalog=read(ROOT/catalog_path)
    static_paths=['reference/phase_a_preservation/files/final/bacap.zip','src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java','tools/final19/java/com/diskree/achievetodo/certification/Final19StaticContext.java',catalog_path]
    # Exactly reproduce independently the current named-input fingerprints.
    static=hashlib.sha256(''.join(p+':'+sha(ROOT/p)+'\n' for p in static_paths).encode()).hexdigest()
    other=['tools/final19/java/com/diskree/achievetodo/certification/Final19FamilyEvidence.java','tools/final19/gametest/com/diskree/achievetodo/certification/Final19NativeActors.java','tools/final19/gametest/com/diskree/achievetodo/certification/'+executor+'.java','src/test/resources/phase_a_certification/runtime_test_matrix.json']
    fingerprint=hashlib.sha256((static+''.join(p+':'+sha(ROOT/p) for p in other)).encode()).hexdigest()
    temp=read(SCRATCH/(prefix+'_temp.json'));run=read(SCRATCH/(prefix+'_run.json'));gate=read(SCRATCH/(prefix+'_static.json'))
    assert gate['inputFingerprint']==static and temp['fingerprint']==run['fingerprint']==fingerprint
    assert temp['family']==run['family']==family and temp['exact']==run['exact']==exact
    assert temp['runId']==run['runId'] and run['stage']==('TEMP_PROMOTABLE' if exact else 'TEMP_DIAGNOSTIC')
    assert run['runtimeCopySha256']==sha(ROOT/'build/run/gameTest/world/datapacks/bacap.zip')
    forbidden={'c01abdba-e443-46d3-93d0-e021716c72f1','0937d889-b634-4397-b591-96e25f4307ee','5c40ea07-d9ad-45c6-b1ad-c267fe43740d','62c46b0a-8e9f-4f26-be99-6299336d8ac3','30c8e9d9-4710-476a-852f-30e3f4baa96f'}
    assert temp['runId'] not in forbidden
    rows=catalog['cases']
    if not exact:
        if prefix=='dual_tags':rows=[r for r in rows if r['advancementId'].endswith(('/happy_birthday','/wax_off'))]
        if prefix=='inventory_masters':rows=[r for r in rows if (r['advancementId'].endswith('/master_armorer') and r['requirementGroup']==0) or r['advancementId'].endswith('/master_axeman')]
    expected={(r['advancementId'],r['requirementGroup'],r['criterion']) for r in rows};entries=temp['entries'];seen={(r['advancementId'],r['requirementGroup'],r['criterion']) for r in entries}
    assert seen==expected and len(entries)==len(expected)==run['expectedEntries'];assert len({r['playerUuid'] for r in entries})==len(entries)
    for r in entries:
        assert r['runId']==temp['runId'] and not r['criterionBefore'] and r['criterionAfter'] and r['gameMode']=='SURVIVAL'
        assert all(r[k] for k in ('joined','connectionRegistered','clientLoaded','finiteMaterials','groupSatisfied','noDirectTrigger','noManualAward'))
        assert all(r['cleanup'][k] for k in ('playerRemoved','connectionRemoved','channelSettled','fixtureEntitiesRemoved','blocksRestored'))
        if prefix=='dual_tags':assert all(r[k] for k in ('itemTagMember','blockTagMember','nativeMutation','wrongItemNegative','noOpNegative','lockedGateNegative','productionGateUnlocked'));assert r['nativeBoundary']=='ServerboundUseItemOnPacket.handle'
        if prefix=='inventory_masters':
            assert all(r[k] for k in ('itemTagMember','supportedEnchantments','compatibleEnchantments','nativePickup','missingEnchantmentNegative','underlevelNegative')) and r['inventoryCount']==1
            assert len(r['missingObservedEnchantments'])==len(r['observedEnchantments'])-1 and r['underlevelObservedEnchantments']!=r['observedEnchantments']
        if prefix=='mixed_worldgen':
            assert r['wrongContextNegative'] and r['nativeEvent'] and r['cleanup']['structureRestored']
            if r['criterion']=='boatception':assert r['boatTagMember'] and r['vehicleStructurePresent'] and r['nativeMount'] and r['observedStructure']=='minecraft:shipwreck' and r['observedVehicle']=='minecraft:spruce_boat'
            else:
                assert all(r[k] for k in ('legalFrostWalkerBoots','nativeWaterToFrostedIce','wrongBootsNegative','wrongIceNegative','spectatorNegative'))
                assert r['observedBiome']=='minecraft:deep_lukewarm_ocean' and r['observedSteppingBlock']=='minecraft:frosted_ice' and r['observedFrostWalkerLevel']==2 and r['waterBefore']=='Block{minecraft:water}[level=0]' and r['iceAfter'].startswith('Block{minecraft:frosted_ice}') and r['cleanup']['biomeRestored']
                controls=r['negativeObservations'];assert set(controls)=={'wrongBiome','wrongBoots','wrongIce','spectator'} and all(not v['criterion'] and v['observedTicks']==25 for v in controls.values())
                assert controls['wrongBiome']['biome']=='minecraft:the_void' and controls['wrongBoots']['bootsEmpty'] and controls['wrongIce']['steppingBlock']=='minecraft:ice' and controls['spectator']['gameMode']=='SPECTATOR'
    name=prefix+'_'+mode;log=(SCRATCH/(name+'.gradle.log')).read_text(encoding='utf-8');assert (SCRATCH/(name+'.exit')).read_text().strip()=='0';assert 'BUILD SUCCESSFUL' in log and ' (1 tests)' in log and 'entries='+str(len(entries)) in log
    start=log.index('FINAL19_NATIVE_RUN_START family='+family);assert all(t not in log[start:] for t in ('/WARN]','/ERROR]','Failed to handle packet','Leak:'))
    auth=read(BASE/'cauldron_gate_authorized_application.json');assert sha(ROOT/auth['patchPath'])==auth['patchSha256'];assert all(sha(ROOT/p)==v for p,v in auth['postconditions'].items())
    if exact:assert temp['runId']!=read(SCRATCH/(prefix+'_canary_evidence.json'))['runId']
    audit={'family':family,'mode':mode,'runId':temp['runId'],'actualEntries':len(entries),'actualTempSha256':sha(SCRATCH/(prefix+'_temp.json')),'actualRunStateSha256':sha(SCRATCH/(prefix+'_run.json')),'fingerprint':fingerprint,'runtimeCopySha256':run['runtimeCopySha256'],'nativeWindowWarnings':0,'productionPostconditions':auth['postconditions'],'verdict':'ACTUAL_TEMP_RUN_STATE_GREEN'}
    write(SCRATCH/(name+'_actual_audit.json'),audit)
    if not exact:
        shutil.copyfile(SCRATCH/(prefix+'_temp.json'),SCRATCH/(prefix+'_canary_evidence.json'));shutil.copyfile(SCRATCH/(prefix+'_run.json'),SCRATCH/(prefix+'_canary_run.json'))
    print(json.dumps(audit))
if __name__=='__main__':main()
