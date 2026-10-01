package com.diskree.achievetodo.certification;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
class PhaseALocationHolderSetWorldgenCertificationTest {
    @Test void everyFrozenRequirementHasOneDistinctSelectedKill() throws Exception {
        var cases = PhaseALocationHolderSetWorldgenCertification.cases(Path.of("").toAbsolutePath()); assertEquals(93, cases.size()); var keys = new HashSet<String>(); var groups = new HashSet<String>();
        for (var row : cases) { assertTrue(keys.add(PhaseALocationHolderSetWorldgenCertification.key(row))); assertTrue(groups.add(row.get("advancementId").getAsString() + "#" + row.get("requirementGroup").getAsInt())); assertTrue(row.getAsJsonArray("alternatives").asList().contains(row.get("criterion"))); }
    }
}
