package com.diskree.achievetodo.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.diskree.achievetodo.ability.AbilityResolvers;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.tests.TestSupport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;
import static com.diskree.achievetodo.tests.TestSupport.assertSame;
import static com.diskree.achievetodo.tests.TestSupport.assertTrue;

public final class EmbeddedDatapackResourcesTest {

    private EmbeddedDatapackResourcesTest() {
    }

    public static void run() {
        Map<String, String> expectedResources = EmbeddedDatapack.renderResources();
        Path datapackRoot = Path.of("src", "main", "resources", "datapack");

        assertTrue(Files.isDirectory(datapackRoot), () -> "Datapack directory is missing: " + datapackRoot.toAbsolutePath());
        assertEquals(AbilityType.values().length * 2 + 4, expectedResources.size());
        assertTrue(expectedResources.containsKey("data/achievetodo/advancement/abilities/root.json"));
        assertTrue(expectedResources.containsKey("pack.mcmeta"));
        assertPackMetadata(expectedResources.get("pack.mcmeta"));

        Set<String> seenAdvancementIds = new LinkedHashSet<>();
        for (AbilityType abilityType : AbilityType.values()) {
            assertTrue(seenAdvancementIds.add(abilityType.getAdvancementId()), () -> "Duplicate advancement id: " + abilityType.getAdvancementId());
            assertSame(
                abilityType,
                AbilityType.findByAdvancementId(AbilityResolvers.findAdvancementId(abilityType)).orElseThrow()
            );
            String advancementPath = EmbeddedDatapack.relativeAdvancementPath(abilityType);
            String functionPath = EmbeddedDatapack.relativeFunctionPath(abilityType);
            String advancement = expectedResources.get(advancementPath);
            String function = expectedResources.get(functionPath);
            assertTrue(advancement != null, () -> "Missing generated advancement for " + abilityType);
            assertTrue(function != null, () -> "Missing generated function for " + abilityType);
            assertTrue(advancement.contains("\"parent\": \"" + EmbeddedDatapack.ROOT_ADVANCEMENT_ID + "\""));
            assertTrue(advancement.contains("\"" + EmbeddedDatapack.DEMYSTIFIED_CRITERION + "\""));
            assertTrue(advancement.contains("\"" + EmbeddedDatapack.UNLOCKED_CRITERION + "\""));
            assertTrue(advancement.contains("\"function\": \"" + abilityType.getAdvancementId() + "\""));
            assertTrue(function.contains("/advancementssearch highlight " + abilityType.getAdvancementId() + " obtained_status"));
        }

        try {
            Set<String> actualFiles = new LinkedHashSet<>();
            try (var paths = Files.walk(datapackRoot)) {
                paths.filter(Files::isRegularFile).forEach(path -> actualFiles.add(toResourceKey(datapackRoot, path)));
            }
            assertEquals(expectedResources.keySet(), actualFiles);

            for (Map.Entry<String, String> entry : expectedResources.entrySet()) {
                Path resourcePath = datapackRoot.resolve(entry.getKey().replace('/', java.io.File.separatorChar));
                String actual = Files.readString(resourcePath, StandardCharsets.UTF_8);
                assertEquals(entry.getValue(), actual);
            }
        } catch (IOException exception) {
            throw new AssertionError("Failed to validate embedded datapack resources", exception);
        }
    }

    private static String toResourceKey(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    private static void assertPackMetadata(String packMcmeta) {
        JsonObject root = JsonParser.parseString(packMcmeta).getAsJsonObject();
        JsonObject pack = root.getAsJsonObject("pack");
        assertTrue(pack != null, () -> "pack.mcmeta is missing the pack object");
        assertEquals(EmbeddedDatapack.DATAPACK_DESCRIPTION, pack.get("description").getAsString());
        assertEquals(EmbeddedDatapack.MAX_FORMAT, pack.get("max_format").getAsInt());
        JsonArray minFormat = pack.getAsJsonArray("min_format");
        assertTrue(minFormat != null, () -> "pack.mcmeta is missing min_format");
        assertEquals(2, minFormat.size());
        assertEquals(EmbeddedDatapack.MIN_FORMAT_MAJOR, minFormat.get(0).getAsInt());
        assertEquals(EmbeddedDatapack.MIN_FORMAT_MINOR, minFormat.get(1).getAsInt());
        assertTrue(!pack.has("pack_format"), () -> "pack.mcmeta must not use the legacy pack_format field for 26.2");
    }
}
