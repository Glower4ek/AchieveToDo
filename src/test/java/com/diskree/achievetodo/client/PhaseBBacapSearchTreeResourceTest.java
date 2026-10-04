package com.diskree.achievetodo.client;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhaseBBacapSearchTreeResourceTest {

    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static JsonObject json(String name) throws Exception {
        try (var in = PhaseBBacapSearchTreeResourceTest.class.getResourceAsStream("/phase_b_certification/" + name + ".json")) {
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

    @Test void orderedTreeKeepsThreeMapsAnd27Entries() throws Exception {
        var gui=json("bacap_1_21_static_receipt").getAsJsonObject("gui");assertEquals(28,n(gui,"explicitBefore"));assertEquals(27,n(gui,"explicitAfter"));assertEquals(3,n(gui,"parentMaps"));assertEquals(17,n(gui,"categories"));assertTrue(b(gui,"architectureUnchanged"));
        exactHashes(receipt().getAsJsonObject("runtimeHashes"));
        String java=Files.readString(ROOT.resolve("src/main/java/com/diskree/achievetodo/injection/mixin/main/PlacedAdvancementMixin.java"));
        assertFalse(java.contains("\"blazeandcave:challenges/constellation\""));
        int count=0;for(var e:gui.getAsJsonObject("orderedChildren").entrySet())count+=e.getValue().getAsJsonArray().size();assertEquals(27,count);
    }
    @Test void allCurrentMessagesAndSearchViewsRemainCertified() throws Exception {
        var s=json("bacap_1_21_static_receipt");var m=s.getAsJsonObject("messages");assertEquals(1300,n(m,"currentCorpus"));
        for(String key:List.of("componentFailures","hoverClickStyleFailures","targetBindingFailures","searchDestinationFailures"))assertEquals(0,n(m,key));
        for(var e:s.getAsJsonObject("search").getAsJsonObject("views").entrySet())assertEquals(0,n(e.getValue().getAsJsonObject(),"unresolvedActive"));
        for(var row:m.getAsJsonArray("records")){var obj=row.getAsJsonObject();assertEquals(obj.get("sha256").getAsString(),hash(ROOT.resolve(obj.get("path").getAsString())));}
    }
    @Test void potionRootTargetsItsOwnTabAndAnnouncesOnce() throws Exception {
        String root=Files.readString(ROOT.resolve("src/main/resources/resourcepacks/bacap_override/data/bacap_rewards/function/potion/root.mcfunction"));
        assertTrue(root.contains("/advancementssearch highlight blazeandcave:potion/root"));assertFalse(root.contains("/advancementssearch highlight blazeandcave:mining/root"));
        assertEquals(1,root.lines().filter(l->l.startsWith("tellraw ")).count());assertTrue(root.contains("adv_id:\"blazeandcave:potion/root\""));
    }
    @Test void currentDescriptionKeysResolveWithoutPruningHistoricalRuEntries() throws Exception {
        var ru=JsonParser.parseString(Files.readString(ROOT.resolve("src/main/resources/assets/minecraft/lang/ru_ru.json"))).getAsJsonObject();
        var current=receipt().getAsJsonObject("B7Current");assertEquals(3482,n(current,"dictionaryKeys"));
        var historical=List.of("Kill a raid captain. Maybe consider staying away from villages for the time being...","Collect a stack of scutes","Kill a Skeleton or Stray while both you and it have levitation");
        var modern=List.of("Kill a raid captain. I’d warn against drinking that bottle they dropped…","Collect a stack of Turtle Scutes","Kill a Skeleton while both you and it have levitation");
        for(String key:historical)assertTrue(ru.has(key));for(String key:modern)assertTrue(ru.has(key)&&!ru.get(key).getAsString().isBlank());
    }
}
