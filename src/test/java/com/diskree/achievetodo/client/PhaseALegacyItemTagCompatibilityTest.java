package com.diskree.achievetodo.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseALegacyItemTagCompatibilityTest {

    private static final Path ITEM_TAG_DIRECTORY = Path.of("src", "main", "resources", "data", "minecraft", "tags", "item");
    private static final Path NETHER_ROOTS = ITEM_TAG_DIRECTORY.resolve("nether_roots.json");
    private static final Path NETHER_FUNGUS = ITEM_TAG_DIRECTORY.resolve("nether_fungus.json");

    @Test
    void bundledLegacyNetherRootsTagMatchesPinnedHistoricalMembers() throws IOException {
        assertLegacyItemTag(
            NETHER_ROOTS,
            Set.of("minecraft:crimson_roots", "minecraft:warped_roots")
        );
    }

    @Test
    void bundledLegacyNetherFungusTagMatchesPinnedHistoricalMembers() throws IOException {
        assertLegacyItemTag(
            NETHER_FUNGUS,
            Set.of("minecraft:crimson_fungus", "minecraft:warped_fungus")
        );
    }

    private static void assertLegacyItemTag(Path path, Set<String> expectedValues) throws IOException {
        assertTrue(
            path.startsWith(ITEM_TAG_DIRECTORY),
            "Legacy compatibility item tags must live under data/minecraft/tags/item: " + path
        );
        assertEquals(
            "item",
            path.getParent().getFileName().toString(),
            "Legacy compatibility item tags must not use an obsolete plural resource path"
        );
        assertTrue(Files.exists(path), "Missing bundled legacy compatibility item tag: " + path);

        JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        assertTrue(json.has("replace"), "Bundled legacy compatibility item tag must pin replace=false: " + path);
        assertFalse(json.get("replace").getAsBoolean(), "Bundled legacy compatibility item tag must preserve merge semantics: " + path);

        JsonArray values = json.getAsJsonArray("values");
        Set<String> actualValues = new LinkedHashSet<>();
        values.forEach(value -> actualValues.add(value.getAsString()));
        assertEquals(expectedValues, actualValues, "Bundled legacy compatibility item tag members must stay exact: " + path);
        assertEquals(expectedValues.size(), values.size(), "Bundled legacy compatibility item tag must not contain duplicates or extras: " + path);
    }
}
