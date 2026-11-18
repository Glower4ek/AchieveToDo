"""Bounded gameplay source successor. This never grants product readiness or distribution rights."""
from pathlib import Path
import hashlib
AUTHORITY='ATD_PURPUR_GAMEPLAY_SOURCE_V1'
PATH='tools/certification/authority/gameplay-successor-v1.json'
SELECTOR='ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY'
FILE='ACHIEVETODO_GAMEPLAY_SOURCE_AUTHORITY_FILE'
ALLOWED=frozenset(['port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/core/AchieveToDoPlugin.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/bridge/BukkitBridgeRuntime.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/BukkitGameplayRuntime.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/core/ProductGameplayAdmission.java', 'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/core/ProductGameplayAdmissionTest.java', 'port/AchieveToDo-Purpur-26.2/build.gradle.kts', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeTransactionAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeActionAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeToolAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeComplexAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativePortalAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeMovementAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkAgent.java', 'port/AchieveToDo-Purpur-26.2/src/main/java/com/diskree/achievetodo/gameplay/nativeguard/NativeLandmarkSupplementAgent.java'])
AUX_BEFORE={'port/AchieveToDo-Purpur-26.2/src/test/java/com/diskree/achievetodo/core/ProductGameplayAdmissionTest.java': None, 'port/AchieveToDo-Purpur-26.2/build.gradle.kts': '3c846c5fcafe6ca27ccf0db470be5b5e2d214fd54dbc90dd1cf159ab79df58e4'}
CODE_PATHS=frozenset({'tools/build/gameplay_successor.py','tools/build/tests/test_gameplay_successor.py','src/test/java/com/diskree/achievetodo/certification/GameplaySuccessorAuthorityTest.java','tools/certification/authority/gameplay-owner-scope-v1.json','tools/certification/authority/gameplay-independent-code-review-v4.json'})
def validate(source,env,expected,predecessor):
 from tools.build import distribution_b_v2_contract as c
 source=Path(source).absolute()
 c.require(env.get(SELECTOR)==AUTHORITY and env.get(FILE)==str(source/PATH),'gameplay unknown/orphan/path selector')
 c.require(env.get('ACHIEVETODO_UI07_AUTHORITY')=='ATD_UI07_SCOPED_SUCCESSOR_V1' and env.get(c.SELECTOR)==c.AUTHORITY,'gameplay predecessor selector required')
 raw=c.safe_file(source,PATH).read_bytes();c.require(c.sha(raw)==expected,'gameplay manifest digest');m=c.strict_json(raw)
 c.require(set(m)=={'schema','authority','state','oldGateA','oldOwnerB','oldUI07','changes','codeBindings','ownerScope','independentReview'},'gameplay exact manifest schema')
 c.require(m['schema']==1 and m['authority']==AUTHORITY and m['state']=='AUTHORIZED_BOUNDED_SOURCE_FOR_CERTIFICATION_ONLY','gameplay scope/state')
 c.require(m['oldGateA']==c.policy(source)['gateAManifestSHA256'] and m['oldOwnerB']==c.OWNER_B_CAPSULE_SHA256 and m['oldUI07']==c.UI07_SUCCESSOR_SHA256,'gameplay predecessor anchors')
 c.require(set(m['changes'])==ALLOWED and set(m['codeBindings'])==CODE_PATHS,'gameplay exact path allowlist')
 result=dict(predecessor)
 for path,row in m['changes'].items():
  c.require(set(row)=={'beforeSHA256','afterSHA256'},'gameplay exact delta schema')
  c.require(row['beforeSHA256']!=row['afterSHA256'],'gameplay non-delta')
  is_product=path.startswith(('src/main/','port/AchieveToDo-Purpur-26.2/src/main/'))
  if is_product:
   c.require(result.get(path)==row['beforeSHA256'],'gameplay exact predecessor bytes: '+path)
   result[path]=row['afterSHA256']
  else:
   c.require(row['beforeSHA256']==AUX_BEFORE[path],'gameplay exact auxiliary predecessor')
  c.require(c.sha(c.safe_file(source,path).read_bytes())==row['afterSHA256'],'gameplay changed bytes: '+path)
 for path,digest in m['codeBindings'].items():c.require(c.sha(c.safe_file(source,path).read_bytes())==digest,'gameplay code identity: '+path)
 for key,name in [('ownerScope','tools/certification/authority/gameplay-owner-scope-v1.json'),('independentReview','tools/certification/authority/gameplay-independent-code-review-v4.json')]:
  c.require(m[key]=={'path':name,'sha256':m['codeBindings'][name]},'gameplay authority reference')
 owner=c.strict_json(c.safe_file(source,m['ownerScope']['path']).read_bytes())
 c.require(owner['origin']=='human-owner:current-task-attachment' and owner['authorization']=='EXISTING_APPROVED_FABRIC_PURPUR_PARITY_ENGINEERING' and owner['newContentDesignAuthorized'] is False,'gameplay owner scope')
 review=c.strict_json(c.safe_file(source,m['independentReview']['path']).read_bytes())
 c.require(review['verdict']=='CODE_REVIEW_PASS_WITH_RUNTIME_CERTIFICATION_REQUIRED' and review['reviewer']=='independent agent /root/independent_preflight','gameplay actual independent code review')
 c.require({x['path']:x['observedSHA256'] for x in review['sourceBindings']}=={p:v['afterSHA256'] for p,v in m['changes'].items()} and all(x['matches'] for x in review['sourceBindings']),'gameplay exact reviewed source bytes')
 observed={}
 for directory in ['src/main','port/AchieveToDo-Purpur-26.2/src/main']:
  for p in (source/directory).rglob('*'):
   c._no_links(p)
   if p.is_file():observed[p.relative_to(source).as_posix()]=c.sha(p.read_bytes())
 c.require(observed==result,'gameplay complete source closure/additions/omissions')
 return result,sorted(CODE_PATHS|ALLOWED|{PATH})
