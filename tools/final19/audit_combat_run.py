"""Independent actual post-fix combat TEMP/run audit; zero promotion here."""
import hashlib,json,pathlib,shutil,sys,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[2]
BASE=ROOT/'reference/phase_a_planning/final19';TMP=ROOT/'build/tmp/final19_implementation'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def main():
 prefix,mode,gate=sys.argv[1:];exact=mode=='exact';a=read(TMP/(prefix+'_temp.json'));r=read(TMP/(prefix+'_run.json'))
 paths=['reference/phase_a_preservation/files/final/bacap.zip','src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java','src/main/resources/achievetodo.mixins.json','tools/final19/java/com/diskree/achievetodo/certification/Final19CombatStaticContext.java','tools/final19/java/com/diskree/achievetodo/certification/Final19CombatEvidence.java','tools/final19/gametest/com/diskree/achievetodo/certification/Final19NativeActors.java','tools/final19/gametest/com/diskree/achievetodo/certification/Final19'+('Shield' if prefix=='shield' else 'Raider')+'GameTest.java','src/test/resources/phase_a_certification/runtime_test_matrix.json']
 if prefix=='shield':paths += ['tools/final19/gametest/com/diskree/achievetodo/certification/Final19ShieldProbe.java','tools/final19/gametest/com/diskree/achievetodo/certification/probe/Final19ShieldDamageMixin.java','tools/final19/gametest/com/diskree/achievetodo/certification/probe/Final19NativeAiShotMixin.java']
 fingerprint=hashlib.sha256(''.join(p+':'+sha(ROOT/p)+'\n' for p in paths).encode()).hexdigest()
 assert a['prefix']==r['prefix']==prefix and a['exact']==r['exact']==exact and a['runId']==r['runId'] and a['fingerprint']==r['fingerprint']==fingerprint
 assert r['stage']==('TEMP_PROMOTABLE' if exact else 'TEMP_DIAGNOSTIC')
 pack=ROOT/'build/run/gameTest/world/datapacks/bacap.zip';assert a['runtimeCopySha256']==r['runtimeCopySha256']==sha(pack)
 assert a['productionConverterSha256']==sha(ROOT/paths[1])=='1bea3ba9caf22963d1ef26de69b19534a4b16d1eab85a39c5e1fc17750227a31'
 if prefix=='raider_integrity':expected={f'feeling_ill/{t}/{m}' for t in ['vindicator','pillager','ravager','witch','evoker'] for m in ['raid','nonraid']}|{f'dungeon_crawler/{t}/{m}' for t in ['pillager','vindicator','evoker'] for m in ['raid','nonraid']}
 elif prefix=='raider':expected={'ordinary','banner_only','raid_captain','outsider','captain'} if exact else {'ordinary','captain'}
 else:expected={'inactive_shield','melee_skeleton','other_owner','locked_shield','blocked_skeleton'}
 entries=a['entries'];assert len(entries)==len(expected)==r['expectedEntries'] and {e['case'] for e in entries}==expected
 assert len({e['playerUuid'] for e in entries})==len(entries) and len({e['targetUuid'] for e in entries})==len(entries)
 for e in entries:
  key=e['case'];positive=(key.endswith('/raid') if key.startswith('feeling_ill') else key.endswith('/nonraid') if key.startswith('dungeon_crawler') else key in ['captain','blocked_skeleton'])
  assert e['runId']==a['runId'] and e['gameMode']=='SURVIVAL' and not e['criterionBefore'] and e['criterionAfter']==positive
  assert all(e[k] for k in ['joined','connectionRegistered','clientLoaded','finiteMaterials','noDirectTrigger','noManualAward','nativeAction'])
  assert all(e['cleanup'][k] for k in ['playerRemoved','connectionRemoved','channelSettled','fixtureEntitiesRemoved','blocksRestored'])
  if prefix!='shield':
   w=e['raiderWitness'];raid=key.endswith('/raid') or key=='raid_captain';captain=key in ['captain','raid_captain']
   assert w['hasRaid']==raid and w['isCaptain']==w['patrolLeader']==captain and w['nativeKillCredit'] and w['damageType']=='minecraft:player_attack'
   assert e['decodedPredicate']=={'hasRaid':key.startswith('feeling_ill'),'isCaptain':prefix=='raider'}
   if raid:assert w['activeRaid'] and w['registeredLiveRaid'] and w['hasRaidAfterKill'] and e['cleanup']['raidStoppedAndUnregistered']
   if captain:assert w['exactOminousBanner'] and w['raiderTagMember']
   if key=='banner_only':assert w['exactOminousBanner'] and not w['patrolLeader']
   if key=='outsider':assert not w['raiderTagMember']
  else:
   w=e['damageWitness'];assert w['nativeHurtObserved'] and w['isProjectile']==(key!='melee_skeleton') and w['skeletonSource']==(key!='other_owner') and w['blocked']==(key in ['blocked_skeleton','other_owner'])
   assert e['shieldGate']['lockedAfter']==(key=='locked_shield')
   if w['isProjectile']:assert w['liveProjectileOwnerObserved'] and w['nativeAiShotObserved'] and w['sourceUuid']==e['targetUuid']
   if w['blocked']:assert e['blockingTicks']>=5 and e['shieldDamageAfter']>e['shieldDamageBefore']
 log=(TMP/(gate+'.gradle.log')).read_text(encoding='utf-8');assert (TMP/(gate+'.exit')).read_text().strip()=='0' and 'BUILD SUCCESSFUL' in log and ' (1 tests)' in log and 'entries='+str(len(entries)) in log
 start=log.index('FINAL19_NATIVE_RUN_START family='+prefix);assert all(t not in log[start:] for t in ['/WARN]','/ERROR]','Failed to handle packet','Leak:'])
 if exact and prefix!='raider_integrity':
  canary=read(TMP/(prefix+'_canary_evidence.json'));assert a['runId']!=canary['runId'] and {e['playerUuid'] for e in entries}.isdisjoint({e['playerUuid'] for e in canary['entries']}) and {e['targetUuid'] for e in entries}.isdisjoint({e['targetUuid'] for e in canary['entries']})
 with zipfile.ZipFile(pack) as z:marker=z.read('achievetodo_compatibility/compat_26_2.properties').decode();assert 'raiderPredicateKeys=snake_case' in marker and 'version=compat_26_2_r15' in marker
 archive=BASE/'combat_runtime_copies';archive.mkdir(exist_ok=True);destination=archive/(a['runtimeCopySha256']+'.zip')
 if not destination.exists():shutil.copyfile(pack,destination)
 audit={'runId':a['runId'],'prefix':prefix,'mode':mode,'actualEntries':len(entries),'actualTempSha256':sha(TMP/(prefix+'_temp.json')),'actualRunStateSha256':sha(TMP/(prefix+'_run.json')),'runtimeCopySha256':sha(pack),'runtimeCopyArchive':destination.relative_to(ROOT).as_posix(),'fingerprint':fingerprint,'nativeWindowWarnings':0,'verdict':'ACTUAL_TEMP_RUN_STATE_GREEN'}
 write(TMP/(prefix+'_'+mode+'_actual_audit.json'),audit)
 if not exact:shutil.copyfile(TMP/(prefix+'_temp.json'),TMP/(prefix+'_canary_evidence.json'));shutil.copyfile(TMP/(prefix+'_run.json'),TMP/(prefix+'_canary_run.json'))
 print(json.dumps(audit))
if __name__=='__main__':main()
