package com.diskree.achievetodo.tracking;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhaseBBacapTrackerContractTest {
    @BeforeAll static void initializeVanillaRegistries() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static JsonObject json(String name) throws Exception {
        try (var in = PhaseBBacapTrackerContractTest.class.getResourceAsStream("/phase_b_certification/" + name + ".json")) {
            assertNotNull(in);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private static JsonObject receipt() throws Exception { return json("b9_regression_receipt"); }
    private static JsonObject probe() throws Exception { return receipt().getAsJsonObject("compiledProductionProbe"); }
    private static int n(JsonObject o, String key) { return o.get(key).getAsInt(); }
    private static boolean b(JsonObject o, String key) { return o.get(key).getAsBoolean(); }
    private static String hash(Path p) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));
    }
    private static void exactHashes(JsonObject hashes) throws Exception {
        for (var e : hashes.entrySet()) assertEquals(e.getValue().getAsString(), hash(ROOT.resolve(e.getKey())), e.getKey());
    }

    @Test void all86BindingsRemainBoundToCurrentProductionDefinitions() throws Exception {
        exactHashes(receipt().getAsJsonObject("runtimeHashes"));
        var tracker=json("bacap_1_21_static_receipt").getAsJsonObject("trackers");
        assertEquals(86,n(tracker,"total"));assertEquals(80,n(tracker,"keep"));assertEquals(6,n(tracker,"updated"));assertEquals(0,n(tracker,"unresolved"));
        assertEquals(33,TrackedScoreType.values().length);assertEquals(50,TrackedStatisticsDataType.values().length);
        Set<String> names=new HashSet<>();for(var value:tracker.getAsJsonArray("bindings"))assertTrue(names.add(value.getAsJsonObject().get("name").getAsString()));assertEquals(86,names.size());
    }
    @Test void updatedScoreAndBreedingContractsAreCurrent() throws Exception {
        assertEquals(100,TrackedScoreType.WHERES_THE_HONEY_LEBOWSKI.getFinalValue());assertEquals(1000,TrackedScoreType.PUPIL_POPPERS.getFinalValue());assertEquals(2500,TrackedStatisticsDataType.TWO_BY_TWO.getFinalValue());
        var trackers=json("bacap_1_21_static_receipt").getAsJsonObject("trackers").getAsJsonArray("bindings");
        for(var v:trackers){var row=v.getAsJsonObject();var binding=row.getAsJsonObject("binding");
            switch(row.get("name").getAsString()){
                case "TWO_BY_TWO" -> assertEquals("blazeandcave:statistics/overpopulation",binding.get("advancementId").getAsString());
                case "WHERES_THE_HONEY_LEBOWSKI" -> assertEquals("bac_consume_honey_bottle",binding.getAsJsonArray("scoreboardObjectives").get(0).getAsString());
                case "PUPIL_POPPERS" -> assertEquals("bac_consume_spider_eye",binding.getAsJsonArray("scoreboardObjectives").get(0).getAsString());
            }
        }
    }
    @Test void nearbySetsAndRadiiRetainExactTargetContracts() throws Exception {
        var m=JsonParser.parseString(Files.readString(ROOT.resolve("reference/phase_b/b4_product_change_manifest.json"))).getAsJsonObject();
        var rows=json("bacap_1_21_static_receipt").getAsJsonObject("trackers").getAsJsonArray("bindings");
        for(var v:rows){var row=v.getAsJsonObject();var binding=row.getAsJsonObject("binding");if(!binding.get("type").getAsString().equals("NEARBY_ENTITIES"))continue;
            var expected=new ArrayList<JsonObject>();for(var a:m.getAsJsonArray("trackers"))if(a.getAsJsonObject().getAsJsonObject("binding").get("name").equals(row.get("name")))expected.add(a.getAsJsonObject().getAsJsonObject("targetBinding"));
            assertEquals(1,expected.size());for(String key:List.of("nearbyEntities","literalParameter","babySeparated","advancementId"))assertEquals(expected.get(0).get(key),binding.get(key));
        }
    }
    @Test void reviewedRailAndHalfHeartSpecialCasesRemainExact() throws Exception {
        assertEquals(1000,TrackedScoreType.ON_A_RAIL.getFinalValue());assertEquals(60,TrackedScoreType.HALF_HEART_LIFE.getFinalValue());
        var list=json("bacap_1_21_static_receipt").getAsJsonObject("trackers").getAsJsonArray("unchangedSpecialCases");assertEquals(Set.of("ON_A_RAIL","HALF_HEART_LIFE"),Set.of(list.get(0).getAsString(),list.get(1).getAsString()));
    }
}
