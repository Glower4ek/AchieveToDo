package com.diskree.achievetodo.certification;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class Final19VariantsStaticTest {
    final Path root=Path.of("").toAbsolutePath();static final String FAMILY="ENTITY_VARIANT_COMPONENT_CONTEXT";
    @Test void all23FrozenVariantsResolveThroughTheirRealTypedRegistries()throws Exception {
        try(var context=new Final19StaticContext(root,FAMILY)){var result=context.validate(FAMILY);assertEquals(23,result.get("criteriaCount").getAsInt());assertEquals(23,result.get("requirementGroupCount").getAsInt());result.addProperty("inputFingerprint",Final19StaticContext.inputFingerprint(root,FAMILY));Final19WorldgenEvidence.write(root.resolve("build/tmp/final19_implementation/variants_static.json"),result);}
    }
    @Test void unknownVariantMalformedComponentsDeletedGroupsAndWrongRegistryRemainRejected()throws Exception {
        try(var context=new Final19StaticContext(root,FAMILY)){
            for(var value:context.catalog(FAMILY).getAsJsonArray("sources")){var source=value.getAsJsonObject();for(int mutation=0;mutation<3;mutation++){var bad=source.getAsJsonObject("auditedConvertedDefinition").deepCopy();var criterion=bad.getAsJsonObject("criteria").entrySet().iterator().next();var components=criterion.getValue().getAsJsonObject().getAsJsonObject("conditions").getAsJsonArray("entity").get(0).getAsJsonObject().getAsJsonObject("predicate").getAsJsonObject("components");String key=components.keySet().iterator().next();if(mutation==0)components.addProperty(key,"minecraft:no_such_variant");if(mutation==1)components.addProperty(key,12);if(mutation==2)bad.getAsJsonObject("criteria").remove(criterion.getKey());if(mutation<2)assertThrows(RuntimeException.class,()->context.parse(bad));assertThrows(RuntimeException.class,()->Final19StaticContext.validateExact(source,bad));}}
        }
        try(var wrong=new Final19StaticContext(root,"WORLDGEN_HOLDERSET_CONTEXT")){for(var value:wrong.catalog(FAMILY).getAsJsonArray("sources"))assertThrows(RuntimeException.class,()->wrong.parse(value.getAsJsonObject().getAsJsonObject("auditedConvertedDefinition")));}
    }
}
