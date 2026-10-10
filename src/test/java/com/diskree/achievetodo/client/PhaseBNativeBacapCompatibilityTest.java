package com.diskree.achievetodo.client;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhaseBNativeBacapCompatibilityTest {

    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static JsonObject json(String name) throws Exception {
        try (var in = PhaseBNativeBacapCompatibilityTest.class.getResourceAsStream("/phase_b_certification/" + name + ".json")) {
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
        for (var e : hashes.entrySet()) assertEquals(e.getValue().getAsString(), (e.getKey().equals("tools/phase_b/b7_localization_certification.py")
            ? PhaseBPackTestFixtures.hash(PhaseBPackTestFixtures.historicalToolBytes(), "SHA-256") : hash(ROOT.resolve(e.getKey()))), e.getKey());
    }

    private static String transform(String name, String input) throws Exception {
        var m = ExternalPackCompatibility.class.getDeclaredMethod(name, String.class);
        m.setAccessible(true);
        var result = m.invoke(null, input);
        var text = result.getClass().getDeclaredMethod("text");
        text.setAccessible(true);
        return (String) text.invoke(result);
    }
    private static void idempotent(String key) throws Exception {
        var row = probe().getAsJsonObject("secondPassCorpus").getAsJsonObject(key);
        assertTrue(n(row,"entriesChecked") > 0);
        assertEquals(0,n(row,"secondPassByteChanges"));
        assertEquals(0,n(row,"secondPassSemanticChanges"));
        assertTrue(b(row,"passed"));
    }
    @Test void currentR19AuthorityIsExact() throws Exception {
        var r=receipt(); exactHashes(r.getAsJsonObject("authorityHashes"));
        assertEquals("compat_26_2_r19",r.get("marker").getAsString());
        assertEquals("6ab0674902b4783c39c08dfd802cd0067d1436d973345834f092a001a4db9fec",hash(ROOT.resolve("src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java")));
        var m=ExternalPackCompatibility.class.getDeclaredMethod("currentRootOverrideSha1");m.setAccessible(true);
        assertEquals(r.get("rootOverrideSha1").getAsString(),m.invoke(null));
    }
    @Test void nativeMainOnlySuppressesAnnouncements() throws Exception {
        var w=probe().getAsJsonObject("nativeMainWitness");
        assertEquals(1244,n(w,"intentionalAnnouncementSuppressions"));
        assertEquals(1244,n(w,"changedFiles"));assertEquals(0,n(w,"historicalRuleFunctionChanges"));
        assertEquals(0,n(w,"legacyMigrationSemanticChanges"));assertEquals(0,n(w,"parseFailures"));
    }
    @Test void nativeHardcoreOnlySuppressesAnnouncements() throws Exception {
        var w=probe().getAsJsonObject("nativeHardcoreWitness");
        assertEquals(17,n(w,"intentionalAnnouncementSuppressions"));assertEquals(17,n(w,"changedFiles"));
        assertEquals(0,n(w,"historicalRuleFunctionChanges"));assertEquals(0,n(w,"legacyMigrationSemanticChanges"));
    }
    @Test void nativeSelectedPolicySecondPassIsByteIdempotent() throws Exception {
        idempotent("nativeMain");idempotent("nativeHardcore");
    }
    @Test void allHistoricalSecondPassesAreByteIdempotent() throws Exception {
        for(String key:List.of("historicalMain","BACAP_TERRALITH","BACAP_AMPLIFIED_NETHER","BACAP_NULLSCAPE"))idempotent(key);
    }
    @Test void bothTerralithHelpersUseRealAdvancementCodec() throws Exception {
        var c=receipt().getAsJsonObject("directHelperCodec");assertEquals("Advancement.CODEC",c.get("codec").getAsString());
        assertEquals(2,n(c,"checked"));assertEquals(2,n(c,"passed"));assertEquals(0,n(c,"failed"));assertTrue(b(c,"negativeControlRejected"));
        assertEquals(2,n(probe().getAsJsonObject("idempotence"),"checked"));
    }
    @Test void actualSourceIdentityIsDistinctFromCurrentPin() throws Exception {
        var m=probe().getAsJsonObject("identityMatrix");var h=m.getAsJsonObject("historical");var c=m.getAsJsonObject("current");
        assertFalse(b(h,"rawCurrent"));assertFalse(b(h,"derivedCurrent"));assertFalse(b(h,"derivedCompatible"));
        assertTrue(b(h,"markerRecordsActualSource"));assertTrue(b(c,"rawCurrent"));assertTrue(b(c,"derivedCurrent"));
        assertTrue(b(m,"oldR18Rejected"));assertTrue(b(m,"malformedR17Rejected"));
        assertFalse(ExternalPackCompatibility.isCurrentWorldPack(ROOT.resolve("reference/phase_a_preservation/files/final/bacap.zip"),ExternalPack.BACAP));
        assertTrue(ExternalPackCompatibility.isCurrentWorldPack(PhaseBPackTestFixtures.current(),ExternalPack.BACAP));
        for(var value:probe().getAsJsonArray("companions")){
            var row=value.getAsJsonObject();assertTrue(b(row,"rawCurrent"));assertTrue(b(row,"derivedCurrent"));assertTrue(b(row,"markerRecordsActualSource"));assertTrue(b(row,"oldR18Rejected"));
        }
    }
    @Test void rootDigestAndEveryMarkerFieldFailClosed() throws Exception {
        assertTrue(b(probe(),"preR2DigestCopyRejected"));assertTrue(b(probe(),"postR2DigestCopyAccepted"));
        assertEquals("cbc432be35d5525001872c430877541cfa1fcad6",probe().get("rootOverrideSha1").getAsString());
        for(var value:probe().getAsJsonObject("markerIntegrity").entrySet())assertTrue(value.getValue().getAsBoolean(),value.getKey());
    }
    @Test void verifiedCurrentSourceRepairsEveryStaleWorldFixture() throws Exception {
        var sync=probe().getAsJsonObject("mainWorldSync");
        assertEquals("ALREADY_CURRENT",sync.getAsJsonObject("current").get("firstResult").getAsString());
        for(String key:List.of("historicalDerived","malformedR17","oldR18","preR2Root")){
            var row=sync.getAsJsonObject(key);assertEquals("UPDATED",row.get("firstResult").getAsString());assertTrue(b(row,"currentAfterSync"));assertTrue(b(row,"thenAlreadyCurrent"));
        }
    }
    @Test void historicalGlowHoneyPayloadAndBothShaIdentitiesArePreserved() throws Exception {
        var h=probe().getAsJsonObject("identityMatrix").getAsJsonObject("historical");
        assertEquals("45b8bb0076bbf5b92fde7dc9590c6686937abbc0",h.get("markerSourceSha1").getAsString());
        assertEquals("14da3f07b5467e8b59ffc0253fd8212c938cd739",ExternalPack.BACAP.getSha1());
        assertEquals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70",hash(ROOT.resolve("reference/phase_a_preservation/files/final/bacap.zip")));
        var cmp=probe().getAsJsonObject("historicalMainPayloadComparison");assertEquals(6489,n(cmp,"nonMarkerEntriesCompared"));assertEquals(0,n(cmp,"differences"));
    }
    @Test void treasureHunterNativeTagCodecRejectsObsoleteR16Rewrite() throws Exception {
        var t=probe().getAsJsonObject("treasureHunterWitness");assertTrue(b(t,"rawAccepted"));assertTrue(b(t,"r19Accepted"));assertFalse(b(t,"r16Accepted"));assertTrue(b(t,"nativePredicatePreserved"));
    }
    @Test void currentB7OracleIsM2AndHistoricalSnapshotRemainsDistinct() throws Exception {
        var r=receipt();var current=r.getAsJsonObject("B7Current");var historical=r.getAsJsonObject("B7Historical");
        assertEquals(4301,n(current,"requirements"));assertEquals(4290,n(current,"placeholders"));assertEquals(3432,n(current.getAsJsonObject("providers"),"MINECRAFT_RU_OVERLAY"));
        assertEquals(4304,n(historical,"requirements"));assertEquals(4293,n(historical,"placeholders"));
        assertEquals(current.get("toolHash").getAsString(), PhaseBPackTestFixtures.hash(PhaseBPackTestFixtures.historicalToolBytes(), "SHA-256"));
        PhaseBPackTestFixtures.assertCurrentToolAuthority();
    }
    @Test void currentB8ReceiptAndToolAuthoritiesRemainExact() throws Exception {
        var lock=receipt().getAsJsonObject("B8Lock");
        for(var e:lock.getAsJsonObject("receiptHashes").entrySet())assertEquals(e.getValue().getAsString(),hash(ROOT.resolve("src/test/resources/phase_b_certification/"+e.getKey())));
        assertEquals(lock.get("toolHash").getAsString(),hash(ROOT.resolve("tools/phase_b/b8_bacap_static_certification.py")));
        for(var e:lock.getAsJsonObject("effectiveCodecs").entrySet())assertEquals(0,n(e.getValue().getAsJsonObject(),"failed"));
    }
    @Test void legacyPlayerMigrationIsScopedHistoricalOnlyAndIdempotent() throws Exception {
        String raw="{\"conditions\":{\"player\":{\"player\":{\"advancements\":{\"example:done\":true}}}}}";
        String modern=transform("convertJson",raw);
        assertTrue(modern.contains("type_specific/player"));assertEquals(modern,transform("convertJson",modern));
        assertEquals(raw,transform("convertNativeJson",raw));
        String nonPredicate="{\"data\":{\"player\":{\"advancements\":{\"example:done\":true}}}}";
        assertEquals(nonPredicate,transform("convertJson",nonPredicate));
    }
    @Test void historicalFamiliesAndModernCorporaHaveNoUnexpectedDrift() throws Exception {
        for(var e:receipt().getAsJsonObject("modernFalsePositiveScan").entrySet()){
            var row=e.getValue().getAsJsonObject();assertEquals(0,n(row,"unexpectedHistoricalSemanticChanges"));assertEquals(0,n(row,"functionChanges"));assertEquals(0,n(row,"runtimeChatChanges"));
        }
        for(var e:probe().getAsJsonObject("extraFamilyWitnesses").entrySet()){
            var row=e.getValue().getAsJsonObject();assertTrue(b(row,"historicalMigrationOccurs"));assertEquals(0,n(row,"secondPassChanges"));
        }
    }
}
