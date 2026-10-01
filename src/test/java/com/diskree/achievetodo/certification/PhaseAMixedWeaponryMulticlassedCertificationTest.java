package com.diskree.achievetodo.certification;
import java.nio.file.Path;import java.util.*;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class PhaseAMixedWeaponryMulticlassedCertificationTest{
    @Test void everyFrozenGroupHasOneSelectedNativeBoundary()throws Exception{var cases=PhaseAMixedWeaponryMulticlassedCertification.cases(Path.of("").toAbsolutePath());assertEquals(17,cases.size());Set<String> keys=new HashSet<>();Set<Integer> groups=new HashSet<>();for(var r:cases){assertTrue(keys.add(PhaseAMixedWeaponryMulticlassedCertification.key(r)));assertTrue(groups.add(r.get("requirementGroup").getAsInt()));assertTrue(r.getAsJsonArray("alternatives").asList().contains(r.get("criterion")));}assertEquals(7,PhaseAMixedWeaponryMulticlassedEvidenceValidation.canaryKeys(cases).size());}
}
