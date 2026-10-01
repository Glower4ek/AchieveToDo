package com.diskree.achievetodo.certification;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class Final19DualTagsStaticTest {
    static final String FAMILY="DUAL_ITEM_BLOCK_TAG_CONTEXT";final Path root=Path.of("").toAbsolutePath();
    @Test void simultaneousItemAndBlockTagsResolveWithoutDroppingEitherDimension()throws Exception {
        try(var context=new Final19StaticContext(root,FAMILY)){var result=context.validate(FAMILY);assertEquals(6,result.get("criteriaCount").getAsInt());assertEquals(6,result.get("requirementGroupCount").getAsInt());result.addProperty("inputFingerprint",Final19StaticContext.inputFingerprint(root,FAMILY));Final19WorldgenEvidence.write(root.resolve("build/tmp/final19_implementation/dual_tags_static.json"),result);}
    }
    @Test void unknownTagsMembersAndDeletedPredicateDimensionsRemainRejected()throws Exception {
        try(var context=new Final19StaticContext(root,FAMILY)){for(var value:context.catalog(FAMILY).getAsJsonArray("sources")){var source=value.getAsJsonObject();for(int mutation=0;mutation<4;mutation++){var bad=source.getAsJsonObject("auditedConvertedDefinition").deepCopy();var criterion=bad.getAsJsonObject("criteria").entrySet().iterator().next().getValue().getAsJsonObject();var location=criterion.getAsJsonObject("conditions").getAsJsonArray("location");if(mutation==0)location.get(0).getAsJsonObject().getAsJsonObject("predicate").getAsJsonObject("block").addProperty("blocks","#minecraft:no_such_tag");if(mutation==1)location.get(1).getAsJsonObject().getAsJsonObject("predicate").addProperty("items","#minecraft:no_such_tag");if(mutation==2)location.get(1).getAsJsonObject().getAsJsonObject("predicate").addProperty("items","minecraft:no_such_item");if(mutation==3)location.remove(1);if(mutation<3)assertThrows(RuntimeException.class,()->context.parse(bad));assertThrows(RuntimeException.class,()->Final19StaticContext.validateExact(source,bad));}}}
    }
}
