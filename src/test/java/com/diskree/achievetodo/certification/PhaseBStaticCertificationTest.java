package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.client.ExternalPack;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Phase B successor receipts; historical Phase A/FINAL19 assertions are untouched. */
class PhaseBStaticCertificationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String TARGET_SHA = "c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2";
    private static final String CONVERTER_SHA = "6ab0674902b4783c39c08dfd802cd0067d1436d973345834f092a001a4db9fec";

    private static JsonObject fixture(String name) throws Exception {
        try (var stream = PhaseBStaticCertificationTest.class.getResourceAsStream(
            "/phase_b_certification/bacap_1_21_" + name + ".json")) {
            assertNotNull(stream);
            return JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static JsonObject receipt() throws Exception { return fixture("static_receipt"); }
    private static int number(JsonObject o, String key) { return o.get(key).getAsInt(); }
    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    private static Set<String> ids(JsonArray rows) {
        Set<String> result = new HashSet<>();
        for (var row : rows) assertTrue(result.add(row.getAsJsonObject().get("id").getAsString()));
        return result;
    }
    private static void greenCodec(JsonObject o, int count) {
        assertEquals(count, number(o, "checked"));
        assertEquals(count, number(o, "passed"));
        assertEquals(0, number(o, "failed"));
        assertTrue(o.get("negativeControlRejected").getAsBoolean());
    }

    @Test void officialTargetIdentityIsSeparateFromFrozenHistoricalSource() throws Exception {
        var target = fixture("target_inventory").getAsJsonObject("targetIdentity");
        assertEquals("Y2zZ5eSs", target.get("versionId").getAsString());
        assertEquals("VoVJ47kN", target.get("projectId").getAsString());
        assertEquals("26.2", target.get("minecraftVersion").getAsString());
        assertEquals(TARGET_SHA, target.get("sha256").getAsString());
        assertEquals("14da3f07b5467e8b59ffc0253fd8212c938cd739", ExternalPack.BACAP.getSha1());
        assertEquals("8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70",
            hash(ROOT.resolve("reference/phase_a_preservation/files/final/bacap.zip")));
    }

    @Test void currentInventoryReconstructs1332And1242Canonical() throws Exception {
        var rows = fixture("target_inventory").getAsJsonArray("records");
        assertEquals(1332, ids(rows).size());
        int canonical = 0, bacap = 0, minecraft = 0, noDisplay = 0, hidden = 0;
        for (var value : rows) {
            var r = value.getAsJsonObject();
            assertFalse(r.has("criteria"));
            assertTrue(r.get("path").getAsString().contains("/advancement/"));
            assertEquals(64, r.get("sha256").getAsString().length());
            assertEquals(64, r.get("requirementsSha256").getAsString().length());
            assertTrue(number(r, "criteriaCount") > 0);
            if (!r.get("displayPresent").getAsBoolean()) noDisplay++;
            if (r.get("hidden").getAsBoolean()) hidden++;
            if (r.get("canonical").getAsBoolean()) {
                canonical++;
                if (r.get("namespace").getAsString().equals("blazeandcave")) bacap++;
                if (r.get("namespace").getAsString().equals("minecraft")) minecraft++;
            }
        }
        assertEquals(72, noDisplay); assertEquals(17, hidden);
        assertEquals(1242, canonical); assertEquals(1116, bacap); assertEquals(126, minecraft);
        assertEquals(1332 - 72 - 17 - 1, canonical);
    }

    @Test void actual26_2CodecReceiptsCoverTargetAndAllEffectiveViews() throws Exception {
        var codec = fixture("codec_receipt");
        assertEquals("Advancement.CODEC", codec.get("codec").getAsString());
        assertEquals("26.2", codec.get("minecraftVersion").getAsString());
        assertEquals(TARGET_SHA, codec.get("officialTargetSha256").getAsString());
        var context = codec.getAsJsonObject("registryContext");
        assertEquals(context.get("minecraftJarSha256").getAsString(), hash(ROOT.resolve(context.get("minecraftJar").getAsString())));
        var views = codec.getAsJsonObject("views");
        greenCodec(views.getAsJsonObject("target"), 1332);
        greenCodec(views.getAsJsonObject("main"), 2894);
        greenCodec(views.getAsJsonObject("hardcore"), 2894);
        greenCodec(views.getAsJsonObject("terralith"), 2922);
        greenCodec(views.getAsJsonObject("amplifiedNether"), 2894);
        greenCodec(views.getAsJsonObject("nullscape"), 2895);
        assertEquals(2, codec.getAsJsonArray("terralithRepairedHelpers").size());
        for (var row : codec.getAsJsonArray("terralithRepairedHelpers")) assertTrue(row.getAsJsonObject().get("accepted").getAsBoolean());
    }

    @Test void oldTargetAndCanonicalSetArithmeticReconciles() throws Exception {
        var r = receipt().getAsJsonObject("oldTargetReconciliation");
        assertEquals(1222, number(r, "common"));
        assertEquals(7, r.getAsJsonArray("oldOnly").size());
        assertEquals(110, r.getAsJsonArray("targetOnly").size());
        var c = r.getAsJsonObject("classification");
        assertEquals(594, number(c, "BYTE_IDENTICAL"));
        assertEquals(4, number(c, "SEMANTICALLY_IDENTICAL_BYTES_DIFFER"));
        assertEquals(624, number(c, "SEMANTICALLY_CHANGED"));
        assertEquals(1229, 1222 + 7); assertEquals(1332, 1222 + 110);
        var canonical = receipt().getAsJsonObject("canonicalReconciliation");
        assertEquals(1152 - 7 + 97, number(canonical, "target"));
        assertEquals(13, canonical.getAsJsonArray("targetOnlyNonCanonical").size());
        for (var value : canonical.getAsJsonArray("targetOnlyNonCanonical")) {
            assertFalse(value.getAsJsonObject().get("canonical").getAsBoolean());
            assertTrue(value.getAsJsonObject().has("nonCanonicalReason"));
        }
    }

    @Test void sevenMappingsHaveCurrentTargetsAndNoProgressAliases() throws Exception {
        var r = receipt(); var rows = r.getAsJsonArray("renameMoveMappings");
        assertEquals(7, rows.size()); var targets = ids(fixture("target_inventory").getAsJsonArray("records"));
        for (var value : rows) {
            var row = value.getAsJsonObject();
            assertTrue(row.get("oldPresent").getAsBoolean()); assertTrue(row.get("oldAbsentTarget").getAsBoolean());
            assertFalse(targets.contains(row.get("oldId").getAsString()));
            assertTrue(targets.contains(row.get("newId").getAsString()));
            assertFalse(row.get("progressMigration").getAsBoolean());
        }
        assertTrue(r.getAsJsonObject("staleIdScan").getAsJsonArray("activeRemovedOrMalformedIds").isEmpty());
        assertFalse(r.getAsJsonObject("staleIdScan").get("progressAliasesAdded").getAsBoolean());
    }

    @Test void wrapperRewardAndThreeShimContractsResolve() throws Exception {
        var r = receipt();
        assertEquals(1256, number(r.getAsJsonObject("wrapperScan"), "checked"));
        assertEquals(0, number(r.getAsJsonObject("wrapperScan"), "malformed"));
        assertEquals(1294, number(r.getAsJsonObject("rewardFunctionScan"), "checked"));
        assertEquals(0, number(r.getAsJsonObject("rewardFunctionScan"), "missing"));
        assertEquals(3, r.getAsJsonArray("postB4Shims").size());
        assertEquals("function blazeandcave:advancement/unwanted_passenger_fail\n",
            Files.readString(ROOT.resolve("src/main/resources/resourcepacks/bacap_override/data/blazeandcave/function/unwanted_passenger_fail.mcfunction")));
    }

    @Test void allResourceGraphsAndPredicatesHaveNoUnexplainedMissingReferences() throws Exception {
        for (var entry : receipt().getAsJsonObject("resourceGraph").entrySet()) {
            var graph = entry.getValue().getAsJsonObject();
            assertEquals(0, number(graph, "unexplainedDirectMissingResources"));
            assertEquals(0, number(graph, "typedRegistryAndPredicateFailures"));
            assertEquals(0, number(graph, "unexpectedDirectDanglingCalls"));
            assertTrue(number(graph, "typedRegistryReferencesChecked") > 6000);
            assertTrue(number(graph, "predicatesCodecChecked") >= 58);
            assertTrue(graph.getAsJsonObject("functionCalls").has("GUARDED_OPTIONAL_RESOURCE"));
            assertTrue(graph.getAsJsonObject("functionCalls").has("EXTERNAL_BACAP_FANPACK_HOOK"));
        }
    }

    @Test void scoreboardAndRawProgressionRemainSeparateFromPoints() throws Exception {
        var scores = receipt().getAsJsonObject("scoreboardContract");
        assertEquals(110, number(scores.getAsJsonObject("summary"), "targetDefinedObjectives"));
        assertEquals(143, number(scores.getAsJsonObject("summary"), "targetReferencedObjectives"));
        assertEquals(6, scores.getAsJsonArray("rawProgressionObjectives").size());
        assertTrue(scores.get("pointsSeparate").getAsBoolean());
        assertTrue(scores.get("targetObjectiveIdentityMatchesB3").getAsBoolean());
    }

    @Test void all86TrackerBindingsAndRegistryIdentifiersAreCertified() throws Exception {
        var r = receipt(); var trackers = r.getAsJsonObject("trackers");
        assertEquals(86, number(trackers, "total")); assertEquals(80, number(trackers, "keep"));
        assertEquals(6, number(trackers, "updated")); assertEquals(0, number(trackers, "unresolved"));
        int keep = 0, updated = 0;
        for (var value : trackers.getAsJsonArray("bindings")) {
            var row = value.getAsJsonObject(); assertTrue(row.get("resolved").getAsBoolean());
            if (row.get("disposition").getAsString().equals("KEEP")) keep++; else updated++;
        }
        assertEquals(80, keep); assertEquals(6, updated);
        assertTrue(trackers.getAsJsonArray("unchangedSpecialCases").toString().contains("ON_A_RAIL"));
        assertTrue(trackers.getAsJsonArray("unchangedSpecialCases").toString().contains("HALF_HEART_LIFE"));
        assertTrue(r.getAsJsonObject("registryValidation").get("passed").getAsBoolean());
        for (var entry : r.getAsJsonObject("productionFileHashes").entrySet()) assertEquals(entry.getValue().getAsString(), hash(ROOT.resolve(entry.getKey())));
    }

    @Test void guiRetains17TabsAnd27CorrectlyParentedOrderedChildren() throws Exception {
        var gui = receipt().getAsJsonObject("gui");
        assertEquals(17, number(gui, "categories")); assertEquals(28, number(gui, "explicitBefore"));
        assertEquals(27, number(gui, "explicitAfter")); assertEquals(3, number(gui, "parentMaps"));
        assertEquals(0, number(gui, "missing")); assertEquals(0, number(gui, "parentFailures"));
        assertTrue(gui.get("architectureUnchanged").getAsBoolean());
        assertFalse(gui.getAsJsonObject("orderedChildren").toString().contains("constellation"));
    }

    @Test void lexicalSearchCountDiffersOnlyByFourInactiveExamples() throws Exception {
        var search = receipt().getAsJsonObject("search"); assertEquals(0, number(search, "unresolvedActive"));
        for (var entry : search.getAsJsonObject("views").entrySet()) {
            var view = entry.getValue().getAsJsonObject();
            assertEquals(4, number(view, "broadCount") - number(view, "activeCount"));
            assertEquals(4, view.getAsJsonArray("commentedExamples").size());
            assertEquals(0, number(view, "unresolvedActive"));
            for (var value : view.getAsJsonArray("commentedExamples")) {
                var example = value.getAsJsonObject();
                String resource = example.get("function").getAsString().replace(":", "/function/");
                var lines = Files.readAllLines(ROOT.resolve("src/main/resources/resourcepacks/bacap_override/data/" + resource + ".mcfunction"));
                assertTrue(lines.get(number(example, "line") - 1).stripLeading().startsWith("#"));
            }
        }
    }

    @Test void current1300MessageSuccessorHasExactLiveCorpusAndZeroFailures() throws Exception {
        var messages = receipt().getAsJsonObject("messages"); assertEquals(1300, number(messages, "currentCorpus"));
        for (String key : List.of("componentFailures", "hoverClickStyleFailures", "targetBindingFailures", "searchDestinationFailures")) assertEquals(0, number(messages, key));
        Set<String> current = new HashSet<>();
        try (var paths = Files.walk(ROOT.resolve("src/main/resources/resourcepacks"))) {
            for (Path p : paths.filter(path -> path.toString().endsWith(".mcfunction")).toList()) {
                if (Files.readAllLines(p).stream().anyMatch(line -> !line.stripLeading().startsWith("#") && line.contains("tellraw ") && line.contains("/advancementssearch highlight "))) current.add(ROOT.relativize(p).toString().replace('\\', '/'));
            }
        }
        Set<String> expected = new HashSet<>();
        for (var value : messages.getAsJsonArray("records")) {
            var row = value.getAsJsonObject(); String path = row.get("path").getAsString();
            assertTrue(expected.add(path)); assertEquals(row.get("sha256").getAsString(), hash(ROOT.resolve(path)));
        }
        assertEquals(1300, current.size()); assertEquals(expected, current);
    }

    @Test void all17RootWrappersStillDelegateToNativeBookkeeping() throws Exception {
        var r = receipt(); assertTrue(r.get("sharedNativeMacroPreserved").getAsBoolean());
        var roots = r.getAsJsonArray("rootWrappers"); assertEquals(17, roots.size());
        for (var value : roots) {
            var row = value.getAsJsonObject(); assertTrue(row.get("passed").getAsBoolean());
            Path path = ROOT.resolve(row.get("path").getAsString());
            assertEquals(row.get("sha256").getAsString(), hash(path));
            assertTrue(Files.readString(path).contains("function " + row.get("delegate").getAsString() + " {"));
        }
        assertEquals(34, number(r.getAsJsonObject("terralithGating"), "gatedIncrementStatements"));
        assertTrue(r.getAsJsonObject("terralithGating").get("nonIncrementLinesUnchanged").getAsBoolean());
    }

    @Test void fourteenCompanionMergesAndCurrentEffectiveGraphsRemainCertified() throws Exception {
        var r = receipt(); var companion = r.getAsJsonObject("companionMerges");
        assertEquals(14, number(companion, "passed")); assertEquals(24, number(companion, "terralithWrappers"));
        assertEquals(6, number(companion, "hardcoreMessages")); assertEquals("nullscape:root", companion.get("nullscapeSearch").getAsString());
        for (var value : companion.getAsJsonArray("merges")) {
            var row = value.getAsJsonObject(); assertTrue(row.get("codecPassed").getAsBoolean());
            assertEquals(row.get("sha256").getAsString(), hash(ROOT.resolve(row.get("path").getAsString())));
        }
        for (var entry : r.getAsJsonObject("effectiveViews").entrySet()) {
            var view = entry.getValue().getAsJsonObject();
            for (String key : List.of("duplicates", "missingParents", "cycles", "selfParents", "missingRewards", "unresolvedSearch", "unexpectedDirectDanglingCalls")) assertEquals(0, number(view, key));
        }
    }

    @Test void r19AndFourR2BindingAuthoritiesAreCurrent() throws Exception {
        var r = receipt(); var authority = r.getAsJsonObject("currentProductAuthority");
        assertEquals(CONVERTER_SHA, authority.get("converterSha256").getAsString());
        Path converter = ROOT.resolve("src/main/java/com/diskree/achievetodo/client/ExternalPackCompatibility.java");
        assertEquals(CONVERTER_SHA, hash(converter)); assertTrue(Files.readString(converter).contains("compat_26_2_r19"));
        assertEquals(authority.get("ruSha256").getAsString(), hash(ROOT.resolve("src/main/resources/assets/minecraft/lang/ru_ru.json")));
        assertEquals("cbc432be35d5525001872c430877541cfa1fcad6", authority.get("rootOverrideSha1").getAsString());
        var messages = r.getAsJsonObject("messages");
        assertEquals(messages.get("R2RepairReceiptHash").getAsString(), hash(ROOT.resolve("reference/phase_b/b8_r2_message_binding_repair.json")));
        assertEquals(4, messages.getAsJsonArray("repairedBindings").size());
        for (var value : messages.getAsJsonArray("repairedBindings")) {
            var row = value.getAsJsonObject(); Path path = ROOT.resolve(row.get("path").getAsString());
            assertEquals(row.get("afterSha256").getAsString(), hash(path));
            assertTrue(Files.readString(path).contains(row.get("new").getAsString()));
            assertFalse(Files.readString(path).contains(row.get("old").getAsString()));
        }
    }
}
