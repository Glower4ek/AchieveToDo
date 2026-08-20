package com.diskree.achievetodo.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final Pattern JSON_TRANSLATE_PATTERN = Pattern.compile("\"translate\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern COMMAND_TRANSLATE_PATTERN = Pattern.compile("translate\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern JSON_KEY_PATTERN = Pattern.compile("^\\s+\"((?:\\\\.|[^\"\\\\])+)\":", Pattern.MULTILINE);

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
        JsonObject overlay = loadJsonObject(BETTER_RU_OVERLAY);
        List<Map<String, String>> rows = loadTsvRows(FINAL_MANUAL_REMAINING);

        int translated = 0;
        int intentionallyEnglish = 0;
        for (Map<String, String> row : rows) {
            String key = row.get("key");
            String expectedRussian = row.get("ru_manual");
            if (expectedRussian.isBlank()) {
                intentionallyEnglish++;
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
        Set<String> runtimeTranslateKeys = collectCurrentRuntimeTranslateKeys();
        JsonObject overlay = loadJsonObject(BETTER_RU_OVERLAY);
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
    void achievetodoRussianLocaleStillCoversAllModOwnedKeys() throws IOException {
        JsonObject english = loadJsonObject(ACHIEVETODO_EN);
        JsonObject russian = loadJsonObject(ACHIEVETODO_RU);

        assertEquals(532, english.size(), "This regression test pins the current AchieveToDo-owned keyset");
        assertEquals(
            english.keySet(),
            russian.keySet(),
            "Existing AchieveToDo-owned Russian keys should remain intact while Better RU is integrated separately"
        );
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
    void pinnedHistoricalBacapSourceRemainsUnchanged() {
        assertTrue(Files.exists(PHASE_A_BACAP), "Pinned historical BACAP source should exist in the preservation bundle");
        assertTrue(
            ExternalPackCompatibility.isPinnedHistoricalSource(PHASE_A_BACAP, ExternalPack.BACAP),
            "The Phase A overlay step must not alter the preserved historical BACAP source zip"
        );
    }

    private static JsonObject loadJsonObject(Path path) throws IOException {
        assertTrue(Files.exists(path), "Missing JSON resource: " + path);
        String raw = Files.readString(path, StandardCharsets.UTF_8);
        JsonObject parsed = JsonParser.parseString(raw).getAsJsonObject();
        assertNotNull(parsed, "Expected a JSON object in " + path);
        return parsed;
    }

    private static Set<String> collectCurrentRuntimeTranslateKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        addTranslateKeysFromDirectory(Path.of("src", "main", "resources", "resourcepacks", "bacap_override"), keys);
        addTranslateKeysFromDirectory(Path.of("src", "main", "resources", "resourcepacks", "bacap_hardcore_override"), keys);
        addTranslateKeysFromDirectory(Path.of("src", "main", "resources", "resourcepacks", "bacap_terralith_override"), keys);
        addTranslateKeysFromDirectory(Path.of("src", "main", "resources", "resourcepacks", "bacap_amplified_nether_override"), keys);
        addTranslateKeysFromDirectory(Path.of("src", "main", "resources", "resourcepacks", "bacap_nullscape_override"), keys);
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
        Set<String> keys = new HashSet<>();
        for (Map<String, String> row : loadTsvRows(FINAL_MANUAL_REMAINING)) {
            if (row.get("ru_manual").isBlank()) {
                keys.add(row.get("key"));
            }
        }
        return keys;
    }
}
