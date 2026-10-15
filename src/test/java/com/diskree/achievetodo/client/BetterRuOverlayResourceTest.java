package com.diskree.achievetodo.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterRuOverlayResourceTest {

    private static final Path BETTER_RU_OVERLAY = Path.of("src", "main", "resources", "assets", "minecraft", "lang", "ru_ru.json");
    private static final Path ACHIEVETODO_RU = Path.of("src", "main", "resources", "assets", "achievetodo", "lang", "ru_ru.json");
    private static final Path ACHIEVETODO_EN = Path.of("src", "main", "resources", "assets", "achievetodo", "lang", "en_us.json");
    private static final Path PHASE_A_BACAP = Path.of("reference", "phase_a_preservation", "files", "final", ExternalPack.BACAP.getFileName());
    private static final Path FINAL_MANUAL_REMAINING = Path.of("reference", "localization", "phase_a_manual_ru_final_v2_remaining.tsv");
    private static final Path FINAL_MANUAL_AUDIT = Path.of("reference", "localization", "phase_a_manual_ru_final_v2_audit.tsv");
    private static final Set<String> TECHNICAL_RUNTIME_TOKENS = Set.of("", "  ", "\t   ", "tab", ">:)");
    private static final Set<String> EXPECTED_INTENTIONAL_ENGLISH_KEYS = Set.of("'s Advancements Pack!", "and");
    private static final Pattern JSON_TRANSLATE_PATTERN = Pattern.compile("\"translate\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern COMMAND_TRANSLATE_PATTERN = Pattern.compile("translate\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern JSON_KEY_PATTERN = Pattern.compile("^\\s+\"((?:\\\\.|[^\"\\\\])+)\":", Pattern.MULTILINE);

    private static final Set<String> COPPER_KEYS = Set.of(
        "achievetodo.ability.use_copper_tools.name",
        "achievetodo.ability.use_copper_tools.description",
        "achievetodo.ability.use_copper_tools.locked_message",
        "achievetodo.ability.equip_copper_armor.name",
        "achievetodo.ability.equip_copper_armor.description",
        "achievetodo.ability.equip_copper_armor.locked_message"
    );
    private static final Path PHASE_B_ORACLE = Path.of("reference/phase_b/b7_localization_certification.json");
    private static final Path PHASE_B_SUCCESSOR = Path.of("reference/phase_b/b7_m2_current_consumer_set_refresh.json");
    private static final String PHASE_B_ORACLE_SHA256 = "e0866de8bf91d079de75935e6b21b2396b0cc84c8605096eb81e28d68e54ca7c";
    private static final String PHASE_B_SUCCESSOR_SHA256 = "943bfc15b65923461b8004559a3a5ac6e225c5b0ad74d8f4a5d009d8ea4fbd4b";
    // D-S01 ZIP 7b9fb984de6bb7bec5588bb1acc1671da5ddfc6ab6e617e85c953b2c4219c378,
    // before/src/main/resources/assets/achievetodo/lang/{en_us,ru_ru}.json (533 entries each).
    // Raw SHA256: EN 2fe18b7a28dc1797bc4c7c470d0e59e7a87c7475cbe10966b8db45feb48d01c1,
    // RU 74918b4e5fca10c5a54b295efae3321a07bb8b0f21c272ab2af39d6c2081e628.
    // Fingerprints frame sorted UTF8 key/value bytes with their signed int32 big-endian lengths.
    private static final String HISTORICAL_EN_VALUES = "91c03bdfe9a34f02a7a7a99e997fe3b9c8e940cba65a7c5d7b7844eafd4b4a3a";
    private static final String HISTORICAL_RU_VALUES = "40757eaf940ae781b9d15b272d0136261e948dfa14603cf53d12bb2a630ab8f8";

    @Test
    void integratedBetterRuOverlayParsesWithoutDuplicateKeys() throws IOException {
        assertTrue(Files.exists(BETTER_RU_OVERLAY), "Better RU overlay should be bundled as a client language resource");

        String raw = Files.readString(BETTER_RU_OVERLAY, StandardCharsets.UTF_8);
        JsonObject parsed = JsonParser.parseString(raw).getAsJsonObject();
        assertFalse(parsed.isEmpty(), "Better RU overlay should not be empty");
        assertEquals(List.of(), findDuplicateKeys(raw), "Bundled Better RU overlay should not contain duplicate JSON keys");
    }

    @Test
    void productionRuOverlayMatchesApprovedFinalManualDecisions() throws IOException {
        JsonObject overlay = loadJsonObject(PhaseBPackTestFixtures.FROZEN_RU);
        List<Map<String, String>> rows = loadTsvRows(FINAL_MANUAL_REMAINING);
        Set<String> intentionalEnglishKeys = loadIntentionalEnglishKeys(rows);

        int translated = 0;
        int intentionallyEnglish = 0;
        for (Map<String, String> row : rows) {
            String key = row.get("key");
            String expectedRussian = row.get("ru_manual");
            if (intentionalEnglishKeys.contains(key)) {
                intentionallyEnglish++;
                assertEquals(key, expectedRussian, "Intentional English fallback rows should stay explicitly pinned to their runtime key");
                assertFalse(overlay.has(key), "Intentional English fragment should stay absent from RU overlay: " + key);
                continue;
            }
            translated++;
            assertTrue(overlay.has(key), "Approved manual RU is missing from the production overlay: " + key);
            assertEquals(expectedRussian, overlay.get(key).getAsString(), "Manual RU should match exactly for: " + key);
        }

        assertEquals(30, rows.size(), "Pinned final Phase A workload should still contain 30 reviewed rows");
        assertEquals(28, translated, "Pinned final Phase A workload should contain 28 Russian manual corrections");
        assertEquals(2, intentionallyEnglish, "Pinned final Phase A workload should contain 2 intentional English fragments");

        assertEquals("Найдите карту путешественника", overlay.get("Find a Traveller's Map").getAsString());
        assertEquals(
            "Третий закон Ньютона — единственный известный человечеству способ куда-то попасть:",
            overlay.get("Newton's third law - the only way humans have ever").getAsString()
        );
        assertEquals(
            "нужно оставить что-то позади",
            overlay.get("figured out of getting somewhere is to leave something behind").getAsString()
        );
        assertEquals(
            "Где-то в глубине джунглей сегодня подозрительно тихо...",
            overlay.get("In the jungle, the mighty jungle,\\n\\nThe lion sleeps tonight\\n\\nIn the jungle, the quiet jungle,\\n\\nThe lion sleeps tonight").getAsString()
        );
        assertEquals(
            "Вручается отличившимся игрокам, выполнившим испытание Этео.",
            overlay.get("Awarded to distinguished individuals who complete Etho's challenge").getAsString()
        );
        assertEquals("Бирюзовая команда", overlay.get("Aqua Team").getAsString());
        assertEquals("Жёлтая команда", overlay.get("Yellow Team").getAsString());
        assertEquals(
            "В командном совместном режиме достижение, полученное одним игроком, будет засчитано всем игрокам его команды.",
            overlay.get("Team Cooperative Mode makes it so that whenever a player gets an advancement, that advancement will be shared across all players on a team.").getAsString()
        );
        assertEquals(
            "При включении этой настройки все уже полученные достижения игроков будут сразу распространены на всех участников их команды.",
            overlay.get("Everyone's currently-obtained advancements will be shared between all players on their team immediately when turning this setting on.").getAsString()
        );
        assertEquals("и всё же настолько неуловим…", overlay.get("yet so elusive...").getAsString());
    }

    @Test
    void productionRuOverlayKeepsPinnedPhaseARuntimeResolvable() throws IOException {
        Set<String> runtimeTranslateKeys = collectHistoricalRuntimeTranslateKeys();
        JsonObject overlay = loadJsonObject(PhaseBPackTestFixtures.FROZEN_RU);
        JsonObject achievetodoRu = loadJsonObject(ACHIEVETODO_RU);
        Set<String> allowedFallbackKeys = loadAllowedFallbackKeys();
        Set<String> intentionalEnglishKeys = loadIntentionalEnglishKeys();
        List<String> unresolved = new ArrayList<>();

        for (String key : runtimeTranslateKeys) {
            if (TECHNICAL_RUNTIME_TOKENS.contains(key) || key.isBlank()) {
                continue;
            }
            if (overlay.has(key) || achievetodoRu.has(key) || allowedFallbackKeys.contains(key) || intentionalEnglishKeys.contains(key)) {
                continue;
            }
            unresolved.add(key);
        }

        assertEquals(
            3507,
            runtimeTranslateKeys.size(),
            "Pinned Phase A historical BACAP content should not change during the production RU overlay step"
        );
        assertEquals(List.of(), unresolved, "Every current runtime BACAP key should resolve via production RU, vanilla RU, or intentional English fallback");
    }

    @Test
    void achievetodoRussianLocaleStillCoversAllModOwnedKeys() throws Exception {
        JsonObject english = loadStrictLocale(ACHIEVETODO_EN);
        JsonObject russian = loadStrictLocale(ACHIEVETODO_RU);
        Set<String> historicalKeys = new HashSet<>();
        for (var row : historicalRequirements().entrySet()) {
            if (row.getValue().get("provider").getAsString().equals("ATD_RU_DICTIONARY")) historicalKeys.add(row.getKey());
        }
        assertEquals(533, historicalKeys.size(), "Independent frozen Phase B ATD keyset");
        assertTrue(java.util.Collections.disjoint(historicalKeys, COPPER_KEYS));
        Set<String> expectedKeys = new HashSet<>(historicalKeys);
        expectedKeys.addAll(COPPER_KEYS);
        assertEquals(expectedKeys, english.keySet(), "Only the six approved copper keys may extend the historical set");
        assertEquals(
            english.keySet(),
            russian.keySet(),
            "Existing AchieveToDo-owned Russian keys should remain intact while Better RU is integrated separately"
        );
        assertEquals(539, english.size());
        assertEquals(539, russian.size());
        assertEquals(HISTORICAL_EN_VALUES, localeFingerprint(english, historicalKeys), "All 533 historical EN values must remain exact");
        assertEquals(HISTORICAL_RU_VALUES, localeFingerprint(russian, historicalKeys), "All 533 historical RU values must remain exact");
        for (String key : COPPER_KEYS) {
            assertFalse(english.get(key).getAsString().isBlank(), key + " EN");
            assertFalse(russian.get(key).getAsString().isBlank(), key + " RU");
        }
    }

    @Test
    void betterRuOverlayDoesNotConflictWithAchieveToDoNamespaceAndEnglishStaysAvailable() throws IOException {
        JsonObject overlay = loadJsonObject(BETTER_RU_OVERLAY);
        JsonObject achievetodoRussian = loadJsonObject(ACHIEVETODO_RU);
        JsonObject achievetodoEnglish = loadJsonObject(ACHIEVETODO_EN);

        assertFalse(overlay.get("Kilometre Walk").getAsString().isBlank());
        assertFalse(achievetodoRussian.get("achievetodo.downloader.download").getAsString().isBlank());
        assertEquals("Download", achievetodoEnglish.get("achievetodo.downloader.download").getAsString());
        assertFalse(
            overlay.has("achievetodo.downloader.download"),
            "Better RU should stay in the minecraft language layer instead of replacing AchieveToDo-owned translations"
        );
    }

    @Test
    void pinnedHistoricalBacapSourceRemainsUnchanged() throws Exception {
        assertEquals(PHASE_A_BACAP, PhaseBPackTestFixtures.historical());
        assertFalse(ExternalPackCompatibility.isPinnedHistoricalSource(PHASE_A_BACAP, ExternalPack.BACAP));
        assertTrue(ExternalPackCompatibility.isPinnedHistoricalSource(PhaseBPackTestFixtures.current(), ExternalPack.BACAP));
    }

    @Test
    void productionRuOverlayResolvesCurrentPhaseBEffectiveConsumers() throws Exception {
        var actual = PhaseBPackTestFixtures.currentLocalization();
        var receipt = loadFrozenReceipt(PHASE_B_SUCCESSOR, PHASE_B_SUCCESSOR_SHA256);
        var expected = receipt.getAsJsonObject("currentConsumerOracle");
        Map<String, JsonObject> current = indexRequirements(actual.getAsJsonArray("requirements"));
        Map<String, JsonObject> historical = historicalRequirements();
        Set<String> extendedKeys = new HashSet<>(historical.keySet());
        extendedKeys.addAll(COPPER_KEYS);
        assertEquals(extendedKeys, current.keySet(), "No historical requirement may disappear or unrelated requirement appear");

        Map<String, JsonObject> projection = new LinkedHashMap<>(current);
        JsonObject declaration = new JsonObject();
        declaration.addProperty("owner", "ATD_DICTIONARY_DECLARATION");
        declaration.addProperty("path", ACHIEVETODO_EN.toString().replace('\\', '/'));
        JsonArray copperConsumers = new JsonArray(); copperConsumers.add(declaration);
        for (String key : COPPER_KEYS) {
            JsonObject row = projection.remove(key);
            assertNotNull(row, key);
            assertEquals("ATD_RU_DICTIONARY", row.get("provider").getAsString(), key);
            assertEquals(copperConsumers, row.getAsJsonArray("consumers"), "Only the approved copper dictionary declaration is new: " + key);
        }
        for (var row : historical.entrySet()) {
            JsonObject projected = projection.get(row.getKey());
            assertNotNull(projected, row.getKey());
            assertEquals(row.getValue().get("provider"), projected.get("provider"), row.getKey());
            assertEquals(consumerMultiset(row.getValue()), consumerMultiset(projected), "Historical consumers: " + row.getKey());
        }
        assertEquals(4301, projection.size());
        assertEquals(expected.get("requirements").getAsInt(), projection.size());
        assertEquals(expected.get("providerPartition"), providerPartition(projection));
        assertEquals(receipt.get("effectiveCoverage"), effectiveCoverage(projection, receipt.getAsJsonObject("effectiveCoverage").keySet()));
        assertEquals(receipt.get("effectiveCoverage"), actual.get("effectiveCoverage"));
        assertEquals(4290, placeholderRequirements(projection));
        assertEquals(expected.get("placeholderContracts").getAsInt(), placeholderRequirements(projection));

        assertEquals(4307, current.size());
        assertEquals(current.size(), actual.get("totalRequirements").getAsInt());
        JsonObject extendedProviders = expected.getAsJsonObject("providerPartition").deepCopy();
        extendedProviders.addProperty("ATD_RU_DICTIONARY", extendedProviders.get("ATD_RU_DICTIONARY").getAsInt() + COPPER_KEYS.size());
        assertEquals(extendedProviders, providerPartition(current));
        assertEquals(extendedProviders, actual.get("providerCounts"));
        assertEquals(4296, placeholderRequirements(current));
        assertEquals(placeholderRequirements(current), actual.get("placeholderChecked").getAsInt());
        assertTrue(actual.getAsJsonArray("placeholderMismatches").isEmpty());
        assertEquals(3482, actual.get("dictionaryCount").getAsInt());
        assertEquals(receipt.getAsJsonObject("dictionary").get("sha256").getAsString(),
            PhaseBPackTestFixtures.hash(Files.readAllBytes(BETTER_RU_OVERLAY), "SHA-256"));
        var provenance = actual.getAsJsonObject("provenance");
        assertEquals(receipt.getAsJsonObject("provenance").get("partition"), provenance.get("partition"));
        for (String field : List.of("missing", "duplicates", "hashMismatch")) assertEquals(0, provenance.get(field).getAsInt());
        for (var row : actual.getAsJsonArray("requirements")) assertFalse(row.getAsJsonObject().get("provider").getAsString().equals("MISSING"));
        var selected = actual.getAsJsonObject("selectedCompanionCoverage"); assertEquals(53, selected.size());
        for (var entry : selected.entrySet()) assertFalse(entry.getValue().getAsString().equals("MISSING"));
    }

    private static JsonObject loadFrozenReceipt(Path path, String sha256) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        assertEquals(sha256, PhaseBPackTestFixtures.hash(bytes, "SHA-256"), "Immutable historical authority: " + path);
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    /** B7-M2 is reconstructed from frozen B7 rows and its three explicitly recorded message replacements. */
    private static Map<String, JsonObject> historicalRequirements() throws Exception {
        JsonObject successor = loadFrozenReceipt(PHASE_B_SUCCESSOR, PHASE_B_SUCCESSOR_SHA256);
        Map<String, JsonObject> historical = frozenRequirementRows();
        assertEquals(4304, historical.size());
        Set<String> retired = new HashSet<>();
        for (var replacement : successor.getAsJsonArray("b8R2ReplacementMapping")) {
            JsonObject mapping = replacement.getAsJsonObject();
            String oldKey = mapping.get("old").getAsString(), newKey = mapping.get("new").getAsString();
            assertTrue(retired.add(oldKey), "Duplicate historical replacement");
            JsonObject old = historical.remove(oldKey), retained = historical.get(newKey);
            assertNotNull(old, oldKey); assertNotNull(retained, newKey);
            assertEquals(mapping.get("newProvider"), retained.get("provider"));
            assertEquals(0, mapping.get("oldCurrentConsumers").getAsInt());
            // Copies only: the original receipt and its row objects remain historical evidence.
            JsonObject merged = retained.deepCopy();
            merged.getAsJsonArray("consumers").addAll(old.getAsJsonArray("consumers").deepCopy());
            assertEquals(mapping.get("newCurrentConsumers").getAsInt(), merged.getAsJsonArray("consumers").size());
            historical.put(newKey, merged);
        }
        Set<String> recordedRetired = new HashSet<>();
        for (var key : successor.getAsJsonObject("exactRequirementSetDelta").getAsJsonArray("historicalOnly")) recordedRetired.add(key.getAsString());
        assertEquals(recordedRetired, retired);
        assertTrue(successor.getAsJsonObject("exactRequirementSetDelta").getAsJsonArray("currentOnly").isEmpty());
        assertEquals(4301, historical.size());
        return historical;
    }

    /** The large B7 receipt also contains unrelated audit data; retain only the independently pinned rows. */
    private static Map<String, JsonObject> frozenRequirementRows() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var stream = Files.newInputStream(PHASE_B_ORACLE)) {
            byte[] buffer = new byte[64 * 1024];
            for (int count; (count = stream.read(buffer)) >= 0;) digest.update(buffer, 0, count);
        }
        assertEquals(PHASE_B_ORACLE_SHA256, java.util.HexFormat.of().formatHex(digest.digest()));
        JsonArray requirements = null;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(PHASE_B_ORACLE, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("independentExtraction")) { reader.skipValue(); continue; }
                reader.beginObject();
                while (reader.hasNext()) {
                    if (reader.nextName().equals("requirements")) requirements = JsonParser.parseReader(reader).getAsJsonArray();
                    else reader.skipValue();
                }
                reader.endObject();
            }
            reader.endObject(); assertEquals(JsonToken.END_DOCUMENT, reader.peek());
        }
        assertNotNull(requirements, "Frozen B7 requirements");
        return indexRequirements(requirements);
    }

    private static Map<String, JsonObject> indexRequirements(JsonArray requirements) {
        Map<String, JsonObject> rows = new LinkedHashMap<>();
        for (var element : requirements) {
            JsonObject row = element.getAsJsonObject(); String key = row.get("key").getAsString();
            assertFalse(rows.containsKey(key), "Duplicate requirement: " + key); rows.put(key, row);
        }
        return rows;
    }

    private static Map<JsonElement, Integer> consumerMultiset(JsonObject row) {
        Map<JsonElement, Integer> counts = new LinkedHashMap<>();
        for (var consumer : row.getAsJsonArray("consumers")) counts.merge(consumer, 1, Integer::sum);
        return counts;
    }

    private static JsonObject providerPartition(Map<String, JsonObject> rows) {
        JsonObject counts = new JsonObject();
        for (var row : rows.values()) {
            String provider = row.get("provider").getAsString();
            counts.addProperty(provider, (counts.has(provider) ? counts.get(provider).getAsInt() : 0) + 1);
        }
        return counts;
    }

    private static int placeholderRequirements(Map<String, JsonObject> rows) {
        return (int) rows.values().stream().filter(r -> !r.get("provider").getAsString().equals("INTENTIONALLY_LANGUAGE_NEUTRAL")
            && !r.get("provider").getAsString().equals("MISSING")).count();
    }

    private static JsonObject effectiveCoverage(Map<String, JsonObject> rows, Set<String> views) {
        JsonObject coverage = new JsonObject();
        for (String view : views) {
            int required = 0, resolved = 0; Set<String> missing = new TreeSet<>();
            for (var row : rows.entrySet()) {
                boolean used = row.getValue().getAsJsonArray("consumers").asList().stream().map(JsonElement::getAsJsonObject)
                    .anyMatch(c -> c.has("combination") && c.get("combination").getAsString().equals(view));
                if (used) {
                    required++;
                    if (row.getValue().get("provider").getAsString().equals("MISSING")) missing.add(row.getKey()); else resolved++;
                }
            }
            JsonObject result = new JsonObject(); result.addProperty("required", required); result.addProperty("resolved", resolved);
            JsonArray absent = new JsonArray(); missing.forEach(absent::add); result.add("missing", absent); coverage.add(view, result);
        }
        return coverage;
    }

    /** Streaming names are checked before parsing can collapse duplicate locale keys, regardless of layout. */
    private static JsonObject loadStrictLocale(Path path) throws IOException {
        JsonObject result = new JsonObject();
        try (JsonReader reader = new JsonReader(new StringReader(Files.readString(path, StandardCharsets.UTF_8)))) {
            reader.setLenient(false); reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName(); assertFalse(result.has(key), "Duplicate locale key in " + path + ": " + key);
                assertEquals(JsonToken.STRING, reader.peek(), "Locale value must be a string: " + key);
                result.addProperty(key, reader.nextString());
            }
            reader.endObject(); assertEquals(JsonToken.END_DOCUMENT, reader.peek());
        }
        return result;
    }

    private static String localeFingerprint(JsonObject locale, Set<String> historicalKeys) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String key : new TreeSet<>(historicalKeys)) {
            for (String value : List.of(key, locale.get(key).getAsString())) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array()); digest.update(bytes);
            }
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    @Test
    void productionRuOverlayMatchesCurrentB6ManualDecisions() throws Exception {
        var overlay = loadJsonObject(BETTER_RU_OVERLAY); var frozen = loadJsonObject(PhaseBPackTestFixtures.FROZEN_RU);
        assertFalse(frozen.has("and")); assertEquals("и", overlay.get("and").getAsString());
        var b6 = PhaseBPackTestFixtures.json(Path.of("reference/phase_b/b6_ru_translation_manifest.json"));
        var ownership = b6.getAsJsonArray("perChangedKeyProvenance").asList().stream()
            .map(com.google.gson.JsonElement::getAsJsonObject).filter(r -> r.get("key").getAsString().equals("and")).findFirst().orElseThrow();
        assertEquals("MANUAL_B6", ownership.get("sourceType").getAsString());
        assertEquals(ownership.get("finalValueSha256").getAsString(), PhaseBPackTestFixtures.hash("и".getBytes(StandardCharsets.UTF_8), "SHA-256"));
        var current = PhaseBPackTestFixtures.currentLocalization();
        var conjunction = current.getAsJsonArray("requirements").asList().stream().map(com.google.gson.JsonElement::getAsJsonObject)
            .filter(r -> r.get("key").getAsString().equals("and")).findFirst().orElseThrow();
        assertEquals("MINECRAFT_RU_OVERLAY", conjunction.get("provider").getAsString());
        assertFalse(conjunction.getAsJsonArray("consumers").isEmpty());
        int changed = 0;
        for (var row : loadTsvRows(FINAL_MANUAL_REMAINING)) {
            String key = row.get("key");
            if (key.equals("and")) { changed++; continue; }
            if (key.startsWith("In the jungle, the mighty jungle,")) {
                assertTrue(frozen.has(key)); assertFalse(overlay.has(key));
                assertTrue(current.getAsJsonArray("requirements").asList().stream()
                    .noneMatch(r -> r.getAsJsonObject().get("key").getAsString().equals(key)));
                var removal = b6.getAsJsonArray("actualRemovals").asList().stream().map(com.google.gson.JsonElement::getAsJsonObject)
                    .filter(r -> r.get("key").getAsString().equals(key)).findFirst().orElseThrow();
                assertEquals(0, removal.get("consumerCount").getAsInt()); changed++;
            } else assertEquals(frozen.get(key), overlay.get(key), key);
        }
        assertEquals(2, changed);
    }

    private static JsonObject loadJsonObject(Path path) throws IOException {
        assertTrue(Files.exists(path), "Missing JSON resource: " + path);
        String raw = Files.readString(path, StandardCharsets.UTF_8);
        JsonObject parsed = JsonParser.parseString(raw).getAsJsonObject();
        assertNotNull(parsed, "Expected a JSON object in " + path);
        return parsed;
    }

    private static Set<String> collectHistoricalRuntimeTranslateKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        addTranslateKeysFromZip(PhaseBPackTestFixtures.FROZEN_INTERNAL, keys);
        addTranslateKeysFromZip(PHASE_A_BACAP, keys);
        return keys;
    }

    private static void addTranslateKeysFromDirectory(Path directory, Set<String> keys) throws IOException {
        if (Files.notExists(directory)) {
            return;
        }
        try (var stream = Files.walk(directory)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                String fileName = path.getFileName().toString();
                if (fileName.endsWith(".json") || fileName.endsWith(".mcfunction")) {
                    addTranslateKeys(Files.readString(path, StandardCharsets.UTF_8), keys);
                }
            }
        }
    }

    private static void addTranslateKeysFromZip(Path zipPath, Set<String> keys) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith(".json") || name.endsWith(".mcfunction")) {
                    String text = new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                    addTranslateKeys(text, keys);
                }
            }
        }
    }

    private static void addTranslateKeys(String text, Set<String> keys) {
        addPatternMatches(text, JSON_TRANSLATE_PATTERN, keys);
        addPatternMatches(text, COMMAND_TRANSLATE_PATTERN, keys);
    }

    private static void addPatternMatches(String text, Pattern pattern, Set<String> keys) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            keys.add(unescapeJson(matcher.group(1)));
        }
    }

    private static List<String> findDuplicateKeys(String raw) {
        List<String> duplicates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Matcher matcher = JSON_KEY_PATTERN.matcher(raw);
        while (matcher.find()) {
            String key = unescapeJson(matcher.group(1));
            if (!seen.add(key) && !duplicates.contains(key)) {
                duplicates.add(key);
            }
        }
        return duplicates;
    }

    private static String unescapeJson(String value) {
        return JsonParser.parseString('"' + value + '"').getAsString();
    }

    private static List<Map<String, String>> loadTsvRows(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertFalse(lines.isEmpty(), "Expected TSV rows in " + path);

        String[] headers = lines.get(0).split("\t", -1);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split("\t", -1);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++) {
                row.put(headers[j], j < values.length ? values[j] : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private static Set<String> loadAllowedFallbackKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        for (Map<String, String> row : loadTsvRows(FINAL_MANUAL_AUDIT)) {
            String classification = row.get("classification");
            if ("VANILLA_RU".equals(classification)
                || "PROPER_NAME_NO_TRANSLATION".equals(classification)
                || "COMMAND_NO_TRANSLATION".equals(classification)
                || "SYMBOL_NO_TRANSLATION".equals(classification)) {
                keys.add(row.get("key"));
            }
        }
        return keys;
    }

    private static Set<String> loadIntentionalEnglishKeys() throws IOException {
        return loadIntentionalEnglishKeys(loadTsvRows(FINAL_MANUAL_REMAINING));
    }

    private static Set<String> loadIntentionalEnglishKeys(List<Map<String, String>> rows) {
        Set<String> keys = new HashSet<>();
        for (Map<String, String> row : rows) {
            if (row.get("key").equals(row.get("ru_manual"))) {
                keys.add(row.get("key"));
            }
        }
        assertEquals(
            EXPECTED_INTENTIONAL_ENGLISH_KEYS,
            keys,
            "Only the pinned intentional-English Phase A fragments may resolve via runtime English fallback"
        );
        return keys;
    }
}
