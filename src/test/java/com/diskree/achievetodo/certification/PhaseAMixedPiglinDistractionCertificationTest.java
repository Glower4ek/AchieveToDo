package com.diskree.achievetodo.certification;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class PhaseAMixedPiglinDistractionCertificationTest {
    @Test void oneFrozenOrGroupSelectsNativeAdultInteraction()throws Exception{
        var cases=PhaseAMixedPiglinDistractionCertification.cases(Path.of("").toAbsolutePath());assertEquals(1,cases.size());var row=cases.getFirst();assertEquals(2,row.getAsJsonArray("alternatives").size());assertTrue(row.getAsJsonArray("alternatives").asList().contains(row.get("criterion")));assertEquals("distract_piglin_directly",row.get("criterion").getAsString());
    }
}
