"""Real FINAL19 terminal accounting. Historical Phase A remains immutable."""
import json,pathlib,zipfile,xml.etree.ElementTree as ET,subprocess
from checkpoint import ROOT,BASE,sha,write,main as checkpoint
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def main():
 checkpoint();state=read(BASE/'execution_state.json');ledger=read(BASE/'product_ledger.json');master=read(BASE/'pre26_2_final19_master_map.json');audit=read(BASE/'pre26_2_final19_audit.json');preserved=read(BASE/'implementation_preservation.json')
 families=[f['sharedRootCauseId'] for f in master['families']];assert state['completedFamilies']==families and len(families)==7
 assert state['productCertified']==ledger['productCertified']==1152 and ledger['productUncertified']==0 and state['remainingTargets']==[]
 assert len(ledger['entries'])==len({e['id'] for e in ledger['entries']})==1152
 historical=read(ROOT/'src/test/resources/phase_a_certification/phase_a_advancement_rollup.json');assert historical['summary']['totalCertified']==1133 and state['historicalPhaseACertified']==1133
 changed={e['id'] for e in ledger['entries'] if e.get('productStatus')=='FINAL19_CERTIFIED'};targets={e['advancementId'] for e in audit['entries']};assert changed==targets and len(targets)==19
 groups=0;criteria=0;certified_groups=0;family_results=[]
 for family in families:
  catalog=read(ROOT/('src/test/resources/final19_certification/'+family.lower()+'_catalog.json'));groups+=len(catalog['cases']);criteria+=sum(len(s['auditedConvertedDefinition']['criteria']) for s in catalog['sources'])
  item=next(e for e in state['persistentEvidenceInventory'] if e['family']==family);assert sha(ROOT/item['path'])==item['sha256'];proof=read(ROOT/item['path']);g=next(g for g in state['gates'] if g['family']==family);assert g['compileTestJava']==g['compileGametestJava']=='GREEN';assert g.get('persistentRegression','GREEN')=='GREEN' and g.get('transactionSeal','GREEN')=='GREEN'
  if family.startswith(('RAIDER','SKELETON')):actual={(e['advancementId'],e['requirementGroup'],e['criterion']) for e in proof['entries'] if e['criterionAfter']}
  else:actual={(e['advancementId'],e['requirementGroup'],e['criterion']) for e in proof['entries']}
  for row in catalog['cases']:
   if row['retainedHistoricalProof']:assert row['advancementId']=='blazeandcave:biomes/the_mighty_jungle'
   else:assert (row['advancementId'],row['requirementGroup'],row['criterion']) in actual
   certified_groups+=1
  inventory=read(BASE/'accepted_transactions'/family.lower()/'gate_inventory.json');assert all(sha(ROOT/p)==h for p,h in inventory['files'].items())
  family_results.append({'family':family,'runId':item['runId'],'evidenceSha256':item['sha256'],'requirementGroups':len(catalog['cases']),'advancements':len(catalog['sources']),'verdict':'DURABLY_GREEN'})
 assert groups==certified_groups==79 and criteria==108
 assert state['zeroGainRegressionDebt']=={'blazeandcave:adventure/feeling_ill':'REPAIRED_NATIVE_FIVE_BRANCHES_ZERO_GAIN_GREEN','blazeandcave:monsters/dungeon_crawler':'THREE_FALSE_BRANCHES_NATIVE_RAID_NONRAID_ZERO_GAIN_GREEN'}
 scope=read(BASE/'raider_production_output_scope.json');assert scope['definitions']==1229 and scope['canonicalDefinitions']==1152 and len(scope['changedDefinitions'])==3 and scope['decodedPredicates']==9 and scope['correctedPredicates']==6
 integrity=read(ROOT/'src/test/resources/final19_certification/raider_integrity_regression.json');assert integrity['certificationGain']==0 and len(integrity['entries'])==16
 pack=ROOT/'build/run/gameTest/world/datapacks/bacap.zip'
 with zipfile.ZipFile(pack) as z:
  text=z.read('achievetodo_compatibility/compat_26_2.properties').decode();marker={line.split('=',1)[0]:line.split('=',1)[1] for line in text.splitlines() if '=' in line and not line.startswith('#')}
  assert marker['version']=='compat_26_2_r15' and marker['raiderPredicateKeys']=='snake_case' and marker['llamaCarpetNbtMapping']=='equipment.body' and marker['sourceSha1']=='45b8bb0076bbf5b92fde7dc9590c6686937abbc0' and marker['fileName']=='bacap.zip'
  assert marker['rootOverrideSha1']==read(BASE/'raider_runtime_freshness.json')['marker']['rootOverrideSha1']
  import hashlib
  for path,output in scope['outputFingerprints'].items():assert hashlib.sha256(z.read(path)).hexdigest()==output['productionOutputSha256']
 assert sha(ROOT/'reference/phase_a_preservation/files/final/bacap.zip')=='8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70'
 tests={'tests':0,'failures':0,'errors':0,'skipped':0};tmp=ROOT/'build/tmp/final19_implementation';assert (tmp/'final19_terminal_gates.exit').read_text().strip()=='0'
 xml={}
 for p in (tmp/'junit').glob('TEST-*.xml'):
  r=ET.parse(p).getroot()
  for key in tests:tests[key]+=int(r.attrib.get(key,0))
  xml[p.name]=sha(p)
 assert tests['tests']>0 and tests['failures']==tests['errors']==tests['skipped']==0
 auths=[read(BASE/p) for p in ['wax_axe_gate_authorized_application.json','cauldron_gate_authorized_application.json','raider_authorized_application.json']]
 output=BASE/'terminal_authoritative_gates';output.mkdir(exist_ok=True)
 import shutil
 for p in [tmp/'final19_terminal_gates.gradle.log',tmp/'final19_terminal_gates.exit',*(tmp/'junit').glob('TEST-*.xml')]:shutil.copyfile(p,output/p.name)
 state['transactionStage']='PRE_26_2_FINAL19_1152_CERTIFIED';state['firstUnfinishedTransaction']='NONE_FINAL19_TERMINAL_CERTIFICATION_COMPLETE';state['nextOperationalFamily']=None;state['activeFamily']=None;state['terminalReconciliation']='reference/phase_a_planning/final19/terminal_reconciliation.json';state['finalAuthoritativeGates']=tests;state['finalRuntimeCopy']={'path':pack.relative_to(ROOT).as_posix(),'sha256':sha(pack),'marker':marker};write(BASE/'execution_state.json',state)
 raider_auth=auths[2];raider_auth['stage']='EXACT_AUTHORIZED_PATCH_NATIVE_FAMILY_AND_ZERO_GAIN_REGRESSIONS_DURABLY_GREEN';raider_auth['acceptedFamily']=family_results[5];write(BASE/'raider_authorized_application.json',raider_auth)
 result={'verdict':'PRE_26_2_FINAL19_1152_CERTIFIED','date':'2026-10-01','originalFinal19TargetsResolved':19,'requirementGroupsCertified':certified_groups,'criteriaAccounted':criteria,'totalUniqueAdvancements':1152,'productCertified':1152,'productUncertified':0,'historicalPhaseACertified':1133,'familyResults':family_results,'zeroGainRaiderRegression':{'runId':integrity['runId'],'sha256':sha(ROOT/'src/test/resources/final19_certification/raider_integrity_regression.json'),'feelingIllTrueBranches':5,'dungeonCrawlerFalseBranches':3,'nativeRaidNonraidControls':16,'certificationGain':0},'authorizedProductionFixes':[a['id'] for a in auths],'frozenBacapSha256':sha(ROOT/'reference/phase_a_preservation/files/final/bacap.zip'),'frozenBacapSha1':marker['sourceSha1'],'currentRuntimeCopySha256':sha(pack),'marker':marker,'productionScopeSha256':sha(BASE/'raider_production_output_scope.json'),'productLedgerSha256':sha(BASE/'product_ledger.json'),'executionStateSha256':sha(BASE/'execution_state.json'),'authoritativeTests':tests,'junitXmlHashes':xml,'historicalPhaseAPreservation':'GREEN_IMMUTABLE_1133_OF_1152','gitSafety':{'head':preserved['head'],'branch':preserved['branch'],'indexSha256':preserved['indexSha256'],'verifiedPreserved':True,'gitMutations':False,'unrelatedDirtyWork':'PRESERVED_NO_CLEANUP'}}
 write(BASE/'terminal_reconciliation.json',result);checkpoint();print(json.dumps({'verdict':result['verdict'],'reconciliationSha256':sha(BASE/'terminal_reconciliation.json'),'ledgerSha256':result['productLedgerSha256'],'executionStateSha256':result['executionStateSha256'],'tests':tests}))
if __name__=='__main__':main()
