package com.diskree.achievetodo.certification;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class Final19WorldgenStaticTest {
    final Path root=Path.of("").toAbsolutePath();
    static final String FAMILY="WORLDGEN_HOLDERSET_CONTEXT";
    @Test void exactFrozen37CriteriaAnd37Groups() throws Exception {
        try(var context=new Final19StaticContext(root,FAMILY)) {
            var result=context.validate(FAMILY);
            result.addProperty("inputFingerprint",Final19StaticContext.inputFingerprint(root,FAMILY));
            assertEquals(37,result.get("criteriaCount").getAsInt());assertEquals(37,result.get("requirementGroupCount").getAsInt());
            var out=root.resolve("build/tmp/final19_implementation/worldgen_static.json");Files.createDirectories(out.getParent());Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result));
        }
    }
    @Test void unknownScalarListMixedAndMalformedBiomesRemainRejected() throws Exception {
        try(var context=new Final19StaticContext(root,FAMILY)) {
            var source=context.catalog(FAMILY).getAsJsonArray("sources").get(1).getAsJsonObject();
            for(String selector:new String[]{"\"minecraft:no_such_biome\"","[\"minecraft:no_such_biome\"]","[\"minecraft:plains\",\"minecraft:no_such_biome\"]","12","[12]","\"#minecraft:no_such_tag\""}) {
                var bad=source.getAsJsonObject("auditedConvertedDefinition").deepCopy();
                bad.getAsJsonObject("criteria").getAsJsonObject("plains").getAsJsonObject("conditions").getAsJsonArray("location").get(0).getAsJsonObject().getAsJsonObject("predicate").add("biomes",JsonParser.parseString(selector));
                assertThrows(RuntimeException.class,()->context.parse(bad),selector);
                assertThrows(RuntimeException.class,()->Final19StaticContext.validateExact(source,bad));
            }
        }
    }
    @Test void weakenedPredicateDeletedGroupAndWrongTriggerAreRejectedEvenIfCodecAccepts() throws Exception {
        try(var context=new Final19StaticContext(root,FAMILY)) {
            var source=context.catalog(FAMILY).getAsJsonArray("sources").get(1).getAsJsonObject();
            for(int mutation=0;mutation<3;mutation++){
                var bad=source.getAsJsonObject("auditedConvertedDefinition").deepCopy();
                if(mutation==0)bad.getAsJsonObject("criteria").getAsJsonObject("plains").getAsJsonObject("conditions").getAsJsonArray("location").get(0).getAsJsonObject().getAsJsonObject("predicate").remove("biomes");
                if(mutation==1)bad.getAsJsonObject("criteria").remove("plains");
                if(mutation==2)bad.getAsJsonObject("criteria").getAsJsonObject("plains").addProperty("trigger","minecraft:impossible");
                assertThrows(RuntimeException.class,()->Final19StaticContext.validateExact(source,bad));
            }
        }
    }
    @Test void wrongTypedContextCannotResolveBiomesAndHistoricalStaticFailRemains() throws Exception {
        try(var context=new Final19StaticContext(root,"DUAL_ITEM_BLOCK_TAG_CONTEXT")) {
            var source=context.catalog(FAMILY).getAsJsonArray("sources").get(0).getAsJsonObject();
            assertThrows(RuntimeException.class,()->context.parse(source.getAsJsonObject("auditedConvertedDefinition")));
        }
        assertEquals("STATIC_FAIL",PhaseACertification.analyzeControlCase(root,"blazeandcave:biomes/the_mighty_jungle").validationEntry().status().name());
    }
}
