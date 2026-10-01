"""Bounded atomic promotion of actual accepted FINAL19 combat evidence, with local ledger delta."""
import json,pathlib,shutil,sys,xml.etree.ElementTree as ET
from checkpoint import ROOT,BASE,sha,write,main as checkpoint
TMP=ROOT/'build/tmp/final19_implementation';RES=ROOT/'src/test/resources/final19_certification'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def accepted_gate(name):
 assert (TMP/(name+'.exit')).read_text().strip()=='0'
 assert 'BUILD SUCCESSFUL' in (TMP/(name+'.gradle.log')).read_text(encoding='utf-8')
def main():
 prefix=sys.argv[1];state=read(BASE/'execution_state.json');ledger=read(BASE/'product_ledger.json');a=read(TMP/(prefix+'_temp.json'));audit=read(TMP/(prefix+'_exact_actual_audit.json'))
 assert a['runId']==audit['runId'] and audit['actualTempSha256']==sha(TMP/(prefix+'_temp.json')) and audit['actualRunStateSha256']==sha(TMP/(prefix+'_run.json')) and audit['verdict']=='ACTUAL_TEMP_RUN_STATE_GREEN' and audit['nativeWindowWarnings']==0
 accepted_gate(prefix+'_validation')
 for p in (TMP/'junit').glob('TEST-*.xml'):
  r=ET.parse(p).getroot();assert all(int(r.attrib.get(k,0))==0 for k in ['failures','errors','skipped'])
 if prefix=='raider_integrity':
  assert a['certificationGain']==0 and len(a['entries'])==16 and state['productCertified']==1150
  target=RES/'raider_integrity_regression.json';assert not target.exists();shutil.copyfile(TMP/(prefix+'_temp.json'),target)
  item={'id':'AUTHORIZED_RAIDER_ZERO_GAIN_INTEGRITY','runId':a['runId'],'path':target.relative_to(ROOT).as_posix(),'sha256':sha(target),'certificationGain':0,'feelingIllBranches':5,'dungeonCrawlerFalseBranches':3,'nativeRaidNonraidControls':16}
  state['auxiliaryRegressionEvidence'].append(item);state['zeroGainRegressionDebt']={'blazeandcave:adventure/feeling_ill':'REPAIRED_NATIVE_FIVE_BRANCHES_ZERO_GAIN_GREEN','blazeandcave:monsters/dungeon_crawler':'THREE_FALSE_BRANCHES_NATIVE_RAID_NONRAID_ZERO_GAIN_GREEN'}
  output=BASE/'authorized_production_fixes/raider_integrity';output.mkdir(parents=True,exist_ok=True)
 else:
  family='RAIDER_PREDICATE_KEY_MIGRATION' if prefix=='raider' else 'SKELETON_PROJECTILE_BLOCK_RUNTIME_PROOF';targetid='minecraft:adventure/voluntary_exile' if prefix=='raider' else 'minecraft:story/deflect_arrow'
  assert state['activeFamily']==family and family not in state['completedFamilies']
  assert a['certificationGain']==1 and len(a['entries'])==5
  acceptance=read(TMP/(prefix+'_acceptance.json'));assert acceptance['runId']==a['runId'] and acceptance['evidenceSha256']==sha(TMP/(prefix+'_temp.json')) and acceptance['independentAcceptance']=='GREEN'
  canary=read(TMP/(prefix+'_canary_evidence.json'));ca=read(TMP/(prefix+'_canary_actual_audit.json'));assert canary['runId']==ca['runId']!=a['runId'] and ca['actualTempSha256']==sha(TMP/(prefix+'_canary_evidence.json'))
  for name in [prefix+'_compile_static',prefix+'_canary',prefix+'_exact']:accepted_gate(name)
  if prefix=='raider':assert all(v.endswith('GREEN') for v in state['zeroGainRegressionDebt'].values()) and len(state['completedFamilies'])==5
  else:assert len(state['completedFamilies'])==6
  target=RES/(family.lower()+'_evidence.json');assert not target.exists();shutil.copyfile(TMP/(prefix+'_temp.json'),target)
  entries=ledger['entries'];assert len(entries)==len({e['id'] for e in entries})==1152;entry=next(e for e in entries if e['id']==targetid);assert 'productStatus' not in entry
  before=ledger['productCertified'];entry.update(productStatus='FINAL19_CERTIFIED',productRequirementsSatisfied=True,final19Family=family)
  count=1133+sum(e.get('productStatus')=='FINAL19_CERTIFIED' for e in entries);assert count==before+1;ledger['productCertified']=count;ledger['productUncertified']=1152-count
  state['productCertified']=count;state['remainingTargets'].remove(targetid);state['completedFamilies'].append(family);state['acceptedRunIds'].append(a['runId'])
  item={'family':family,'path':target.relative_to(ROOT).as_posix(),'sha256':sha(target),'runId':a['runId'],'catalogSha256':sha(RES/(family.lower()+'_catalog.json'))}
  state['persistentEvidenceInventory'].append(item);state['gates'].append({'family':family,'compileTestJava':'GREEN','compileGametestJava':'GREEN','exactRunId':a['runId'],'canaryRunId':canary['runId'],'exactReceipts':5,'actualTempRunAudit':'GREEN','nativeWindowWarnings':0,'independentValidation':'GREEN','persistentRegression':'PENDING','transactionSeal':'PENDING'})
  state['activeFamily']='SKELETON_PROJECTILE_BLOCK_RUNTIME_PROOF' if prefix=='raider' else 'TERMINAL_FINAL19_RECONCILIATION';state['nextOperationalFamily']=state['activeFamily'];state['transactionStage']='PERSISTENT_REGRESSION';state['firstUnfinishedTransaction']=family+': persistent regression, seal, checkpoint'
  write(BASE/'product_ledger.json',ledger);output=BASE/'accepted_transactions'/family.lower();output.mkdir(parents=True,exist_ok=True)
 files={}
 for p in TMP.iterdir():
  if p.is_file() and p.name.startswith(prefix+'_') and (p.suffix in ['.json','.exit'] or p.name.endswith('.gradle.log')) and '.previous.' not in p.name:
   shutil.copyfile(p,output/p.name);files[(output/p.name).relative_to(ROOT).as_posix()]=sha(output/p.name)
 for p in (TMP/'junit').glob('TEST-*Final19*.xml'):
  shutil.copyfile(p,output/p.name);files[(output/p.name).relative_to(ROOT).as_posix()]=sha(output/p.name)
 write(output/'gate_inventory.json',{'acceptedPersistentEvidence':item,'files':files});write(BASE/'execution_state.json',state);checkpoint();print('Accepted '+prefix+'; derived productCertified='+str(state['productCertified']))
if __name__=='__main__':main()
