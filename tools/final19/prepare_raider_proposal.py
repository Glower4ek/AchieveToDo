"""Review-only proposed converter bytes. Never write the production source."""
import pathlib,hashlib,json,difflib
ROOT=pathlib.Path(__file__).resolve().parents[2]
BASE=ROOT/'reference/phase_a_planning/final19'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    state=json.loads((BASE/'execution_state.json').read_text(encoding='utf-8'))
    assert state['productCertified']==1150 and len(state['completedFamilies'])==5 and state['activeFamily']=='RAIDER_PREDICATE_KEY_MIGRATION'
    source='src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java';path=ROOT/source
    assert sha(path)=='dffeb7c2cf6685c5f689cf2a205b435b81da5b524971103804de3e3a6ae1dc11'
    original=path.read_bytes();text=original.decode();newline='\r\n' if '\r\n' in text else '\n';text=text.replace('\r\n','\n')
    anchors=[('    private static final String LLAMA_CARPET_NBT_PROPERTY = "llamaCarpetNbtMapping";','    private static final String LLAMA_CARPET_NBT_PROPERTY = "llamaCarpetNbtMapping";\n    private static final String RAIDER_PREDICATE_KEYS_PROPERTY = "raiderPredicateKeys";'),
    ('                    || "equipment.body".equals(properties.getProperty(LLAMA_CARPET_NBT_PROPERTY)));','                    || ("equipment.body".equals(properties.getProperty(LLAMA_CARPET_NBT_PROPERTY))\n                        && "snake_case".equals(properties.getProperty(RAIDER_PREDICATE_KEYS_PROPERTY))));'),
    ('            if (LEGACY_HAS_RAID.equals(key)) {\n                targetKey = "hasRaid";\n                changed = true;\n            } else if (LEGACY_IS_CAPTAIN.equals(key)) {\n                targetKey = "isCaptain";\n                changed = true;\n            }','            // RaiderPredicate.CODEC reads snake_case; preserve explicit and omitted booleans.'),
    ('            properties.setProperty(LLAMA_CARPET_NBT_PROPERTY, "equipment.body");','            properties.setProperty(LLAMA_CARPET_NBT_PROPERTY, "equipment.body");\n            properties.setProperty(RAIDER_PREDICATE_KEYS_PROPERTY, "snake_case");')]
    proposed=text
    for old,new in anchors:assert proposed.count(old)==1;proposed=proposed.replace(old,new)
    destination=BASE/'raider_production_fix_proposal_sources'/source;destination.parent.mkdir(parents=True,exist_ok=True);destination.write_bytes(proposed.replace('\n',newline).encode())
    patch=BASE/'raider_production_fix_proposal.patch';patch.write_bytes(''.join(difflib.unified_diff(text.splitlines(True),proposed.splitlines(True),fromfile='a/'+source,tofile='b/'+source)).encode())
    record={'classification':'KNOWN_MAPPED_PRODUCTION_BUG','boundary':'FINAL19_RAIDER_PRODUCTION_FIX_PROPOSAL','date':'2026-10-01','patchPath':patch.relative_to(ROOT).as_posix(),'patchSha256':sha(patch),'productionPreconditions':{source:sha(path)},'proposedProductionPostconditions':{source:sha(destination)},'changedProductionFiles':[source],'patchApplied':False,'productCertified':1150,'authorization':'REQUIRED_SEPARATELY','markerVersionUnchanged':'compat_26_2_r15','proposedBacapFreshnessProperty':{'raiderPredicateKeys':'snake_case'},'requiredAffectedAdvancements':['minecraft:adventure/voluntary_exile','blazeandcave:adventure/feeling_ill','blazeandcave:monsters/dungeon_crawler'],'futureNativeCertificationRequired':True}
    (BASE/'raider_production_fix_proposal.json').write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8');assert path.read_bytes()==original;print(json.dumps(record,indent=2))
if __name__=='__main__':main()
