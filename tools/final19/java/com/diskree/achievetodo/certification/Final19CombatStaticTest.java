package com.diskree.achievetodo.certification;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class Final19CombatStaticTest {
 @Test void exactFrozenRaiderAndSkeletonDefinitionsParseWithActualTagsAndComponents()throws Exception{
  Path root=Path.of("").toAbsolutePath();for(String family:new String[]{"RAIDER_PREDICATE_KEY_MIGRATION","SKELETON_PROJECTILE_BLOCK_RUNTIME_PROOF"})try(var c=new Final19CombatStaticContext(root,family)){var result=c.validate(family);assertEquals(1,result.get("criteriaCount").getAsInt());assertEquals(1,result.get("requirementGroupCount").getAsInt());result.addProperty("inputFingerprint",Final19CombatStaticContext.inputFingerprint(root,family));Final19WorldgenEvidence.write(root.resolve("build/tmp/final19_implementation/"+(family.startsWith("RAIDER")?"raider":"shield")+"_static.json"),result);}
 }
}
