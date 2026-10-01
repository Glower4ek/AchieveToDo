package com.diskree.achievetodo.certification;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class Final19PreservedVariantsAcceptanceTest {
    final Path root=Path.of("").toAbsolutePath();static final String FAMILY="ENTITY_VARIANT_COMPONENT_CONTEXT";
    @Test void all23NativeVariantGroupsAreIndependentlyValid()throws Exception {
        Path path=Files.exists(root.resolve(Final19FamilyEvidence.persistent(FAMILY)))?Final19FamilyEvidence.persistent(FAMILY):Final19FamilyEvidence.temp(FAMILY);
        assertEquals(23,Final19PreservedFamilyEvidence.validate(root,FAMILY,path,true).getAsJsonArray("entries").size());
        try(var context=new Final19StaticContext(root,FAMILY)){assertEquals(23,context.validate(FAMILY).get("criteriaCount").getAsInt());}
    }
    @Test void missingDuplicateStalePreTamedWrongVariantAndUnexecutedNegativesAreRejected()throws Exception {
        Path accepted=Files.exists(root.resolve(Final19FamilyEvidence.persistent(FAMILY)))?Final19FamilyEvidence.persistent(FAMILY):Final19FamilyEvidence.temp(FAMILY);var original=Final19WorldgenEvidence.read(root.resolve(accepted));Path path=Path.of("build/tmp/final19_implementation/variants_mutated.json");
        for(int mutation=0;mutation<8;mutation++){var bad=original.deepCopy();var entries=bad.getAsJsonArray("entries");var first=entries.get(0).getAsJsonObject();if(mutation==0)entries.remove(0);if(mutation==1)entries.add(first.deepCopy());if(mutation==2)bad.addProperty("fingerprint","stale");if(mutation==3)first.addProperty("tamedBefore",true);if(mutation==4)first.addProperty("observedVariant","minecraft:no_such_variant");if(mutation==5)first.addProperty("negativeResourceConsumed",0);if(mutation==6)first.addProperty("resourceConsumed",0);if(mutation==7)first.getAsJsonObject("cleanup").addProperty("fixtureEntitiesRemoved",false);Final19WorldgenEvidence.write(root.resolve(path),bad);assertThrows(Exception.class,()->Final19PreservedFamilyEvidence.validate(root,FAMILY,path,true));}
    }
}
