package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class PhaseAPureEnchantmentInventoryChangedCertificationTest {
    @Test void frozenOrGroupsHaveExactlyOneDistinctExecutableWitness() throws Exception {
        var cases = PhaseAPureEnchantmentInventoryChangedCertification.cases(Path.of("").toAbsolutePath());
        assertEquals(211, cases.size()); var groups = new HashSet<String>(); var keys = new HashSet<String>();
        for (var row : cases) {
            assertTrue(keys.add(PhaseAPureEnchantmentInventoryChangedCertification.key(row)));
            assertTrue(groups.add(row.get("advancementId").getAsString() + "#" + row.get("requirementGroup").getAsInt()));
            assertTrue(row.getAsJsonArray("alternatives").asList().contains(row.get("criterion")));
        }
        assertEquals(209, cases.stream().filter(row -> row.get("storage").getAsString().equals("STORED_ENCHANTMENTS")).count());
        assertEquals(1, cases.stream().filter(row -> row.get("action").getAsString().equals("EQUIP_HEAD")).count());
    }
}
