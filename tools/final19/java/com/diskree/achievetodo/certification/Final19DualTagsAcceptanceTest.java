package com.diskree.achievetodo.certification;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class Final19DualTagsAcceptanceTest {
    static final String FAMILY="DUAL_ITEM_BLOCK_TAG_CONTEXT";static final Path ROOT=Path.of("").toAbsolutePath();
    static Path path(){return Files.exists(ROOT.resolve(Final19FamilyEvidence.persistent(FAMILY)))?Final19FamilyEvidence.persistent(FAMILY):Final19FamilyEvidence.temp(FAMILY);}
    static void observe(JsonObject a){for(var value:a.getAsJsonArray("entries")){var r=value.getAsJsonObject();assertTrue(r.get("lockedGateNegative").getAsBoolean());assertEquals("ServerboundUseItemOnPacket.handle",r.get("nativeBoundary").getAsString());}}
    @Test void allSixDualTagGroupsPassWithNativeMutationAndGateNegatives()throws Exception{var a=Final19FamilyEvidence.validate(ROOT,FAMILY,path(),true);assertEquals(6,a.getAsJsonArray("entries").size());observe(a);}
    @Test void missingTagsDuplicateStaleFalseGateAndNoMutationClaimsAreRejected()throws Exception{var original=Final19WorldgenEvidence.read(ROOT.resolve(path()));Path scratch=Path.of("build/tmp/final19_implementation/dual_tags_mutated.json");for(int i=0;i<7;i++){var a=original.deepCopy();var entries=a.getAsJsonArray("entries");var r=entries.get(0).getAsJsonObject();switch(i){case 0->entries.remove(0);case 1->entries.add(r.deepCopy());case 2->a.addProperty("fingerprint","stale");case 3->r.addProperty("blockTagMember",false);case 4->r.addProperty("itemTagMember",false);case 5->{r.add("blockAfter",r.get("blockBefore"));r.add("itemAfter",r.get("itemBefore"));}case 6->r.addProperty("lockedGateNegative",false);}Final19WorldgenEvidence.write(ROOT.resolve(scratch),a);assertThrows(Throwable.class,()->{observe(a);Final19FamilyEvidence.validate(ROOT,FAMILY,scratch,true);});}}
}
