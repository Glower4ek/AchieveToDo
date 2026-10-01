package com.diskree.achievetodo.certification;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhaseAStackAllItemsCertificationTest {
    @Test void exactFrozenComplementPreservesImmutableAcceptedSlice() throws Exception {
        Path root=Path.of("").toAbsolutePath();var rows=PhaseAStackAllItemsCertification.cases(root);assertEquals(319,rows.size());var keys=new HashSet<String>();var groups=new HashSet<Integer>();
        for(var row:rows){assertTrue(keys.add(PhaseAStackAllItemsCertification.key(row)));assertTrue(groups.add(row.get("requirementGroup").getAsInt()));assertNotEquals("smithing_template",row.get("criterion").getAsString());assertEquals(1,row.getAsJsonArray("alternatives").size());assertEquals("minecraft:inventory_changed",row.get("trigger").getAsString());}
        assertEquals(3,PhaseAStackAllItemsEvidenceValidation.canaryKeys(rows).size());assertEquals(Set.of(1,16,64),rows.stream().filter(r->PhaseAStackAllItemsEvidenceValidation.canaryKeys(rows).contains(PhaseAStackAllItemsCertification.key(r))).map(r->r.get("requiredCount").getAsInt()).collect(java.util.stream.Collectors.toSet()));
        var canonical=JsonParser.parseString(Files.readString(root.resolve("src/test/resources/phase_a_certification/phase_a_advancement_rollup.json"))).getAsJsonObject();var target=canonical.getAsJsonArray("entries").asList().stream().map(e->e.getAsJsonObject()).filter(e->e.get("id").getAsString().equals(PhaseAStackAllItemsCertification.ADVANCEMENT)).findFirst().orElseThrow();
        assertEquals(320,target.getAsJsonArray("completionRequirements").size());assertTrue(target.getAsJsonArray("runtimeGreenCriteria").asList().stream().anyMatch(e->e.getAsString().equals("smithing_template")));
        if(target.get("advancementStatus").getAsString().equals("RUNTIME_PARTIAL"))assertEquals(1,target.getAsJsonArray("runtimeGreenCriteria").size());else assertEquals("RUNTIME_CERTIFIED",target.get("advancementStatus").getAsString());
    }
}
