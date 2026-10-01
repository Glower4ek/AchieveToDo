package com.diskree.achievetodo.certification;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class Final19LedgerTest {
    @Test void separateProductLedgerPreserves1152UniqueHistoricalEntriesAndExactAcceptedDelta()throws Exception {
        Path root=Path.of("").toAbsolutePath();var historical=Final19WorldgenEvidence.read(root.resolve("src/test/resources/phase_a_certification/phase_a_advancement_rollup.json"));
        assertEquals("439f2aa1c577ffbee275525765ac9229bf5633f0f5e1da498c0adc5702bd2810",Final19StaticContext.sha(Files.readAllBytes(root.resolve("src/test/resources/phase_a_certification/phase_a_advancement_rollup.json"))));
        var ledger=Final19WorldgenEvidence.read(root.resolve("reference/phase_a_planning/final19/product_ledger.json"));var baseline=new HashMap<String,JsonObject>();for(var e:historical.getAsJsonArray("entries"))baseline.put(e.getAsJsonObject().get("id").getAsString(),e.getAsJsonObject());
        Set<String> seen=new HashSet<>();int gained=0;for(var e:ledger.getAsJsonArray("entries")){var entry=e.getAsJsonObject().deepCopy();String id=entry.get("id").getAsString();assertTrue(seen.add(id));if(entry.has("productStatus")){assertEquals("FINAL19_CERTIFIED",entry.remove("productStatus").getAsString());assertTrue(entry.remove("productRequirementsSatisfied").getAsBoolean());entry.remove("final19Family");gained++;}assertEquals(baseline.get(id),entry,id);}
        assertEquals(1152,seen.size());assertEquals(1133+gained,ledger.get("productCertified").getAsInt());assertEquals(19-gained,ledger.get("productUncertified").getAsInt());assertEquals(1133,historical.getAsJsonObject("summary").get("totalCertified").getAsInt());
    }
}
