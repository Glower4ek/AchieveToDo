package com.diskree.achievetodo.certification;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Final19WorldgenAcceptanceTest {
    final Path root=Path.of("").toAbsolutePath();
    @Test void actualExactReceiptsAndRetainedJunglePassIndependentValidation() throws Exception {
        var receipts=Final19WorldgenEvidence.validate(root,Files.exists(root.resolve(Final19WorldgenEvidence.PERSISTENT))?Final19WorldgenEvidence.PERSISTENT:Final19WorldgenEvidence.TEMP,true);
        assertEquals(36,receipts.getAsJsonArray("entries").size());
        var jungle=PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(root).get("blazeandcave:biomes/the_mighty_jungle");
        assertNotNull(jungle);assertEquals(Set.of("jungle"),jungle.greenCriteria());
        try(var context=new Final19StaticContext(root,Final19WorldgenEvidence.FAMILY)){assertEquals(37,context.validate(Final19WorldgenEvidence.FAMILY).get("requirementGroupCount").getAsInt());}
    }
    @Test void fabricatedStaleDuplicateIncompleteAndWrongContextReceiptsAreRejected() throws Exception {
        var original=Final19WorldgenEvidence.read(root.resolve(Files.exists(root.resolve(Final19WorldgenEvidence.PERSISTENT))?Final19WorldgenEvidence.PERSISTENT:Final19WorldgenEvidence.TEMP));
        var path=Path.of("build/tmp/final19_implementation/worldgen_mutation.json");
        for(int mutation=0;mutation<8;mutation++){
            var bad=original.deepCopy();var entries=bad.getAsJsonArray("entries");var first=entries.get(0).getAsJsonObject();
            if(mutation==0)entries.remove(0);
            if(mutation==1)entries.add(first.deepCopy());
            if(mutation==2)bad.addProperty("fingerprint","stale");
            if(mutation==3)first.addProperty("observedBiome","minecraft:no_such_biome");
            if(mutation==4)first.addProperty("criterionBefore",true);
            if(mutation==5)first.addProperty("noteAfter",first.get("noteBefore").getAsInt());
            if(mutation==6)first.getAsJsonObject("cleanup").addProperty("biomeRestored",false);
            if(mutation==7)first.addProperty("wrongBlockNegative",false);
            Final19WorldgenEvidence.write(root.resolve(path),bad);assertThrows(Exception.class,()->Final19WorldgenEvidence.validate(root,path,true));
        }
    }
}
