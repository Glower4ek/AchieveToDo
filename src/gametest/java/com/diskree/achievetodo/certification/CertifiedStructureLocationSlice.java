package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CertifiedStructureLocationSlice {
    private static final String MATRIX_RELATIVE_PATH = "src/test/resources/phase_a_certification/location_movement_matrix.json";
    private static final StructureCase GRAVE_ROBBER = requiredDirectStructureOnlyCase(
        Identifier.parse("blazeandcave:adventure/grave_robber"),
        "desert_pyramid",
        Identifier.parse("minecraft:desert_pyramid"),
        Level.OVERWORLD,
        new BlockPos(32, 80, 32)
    );
    private static final StructureCase FOLLOW_ENDER_EYE = requiredDirectStructureOnlyCase(
        Identifier.parse("minecraft:story/follow_ender_eye"),
        "in_stronghold",
        Identifier.parse("minecraft:stronghold"),
        Level.OVERWORLD,
        new BlockPos(48, 80, 48)
    );
    private static final StructureCase FIND_FORTRESS = requiredDirectStructureOnlyCase(
        Identifier.parse("minecraft:nether/find_fortress"),
        "fortress",
        Identifier.parse("minecraft:fortress"),
        Level.NETHER,
        new BlockPos(32, 80, 32)
    );
    private static final StructureCase FIND_END_CITY = requiredDirectStructureOnlyCase(
        Identifier.parse("minecraft:end/find_end_city"),
        "in_city",
        Identifier.parse("minecraft:end_city"),
        Level.END,
        new BlockPos(32, 80, 32)
    );

    private CertifiedStructureLocationSlice() {
    }

    public static List<StructureCase> selectedCases() {
        return List.of(GRAVE_ROBBER, FOLLOW_ENDER_EYE, FIND_FORTRESS, FIND_END_CITY);
    }

    public static StructureCase graveRobber() {
        return GRAVE_ROBBER;
    }

    public static StructureCase followEnderEye() {
        return FOLLOW_ENDER_EYE;
    }

    public static StructureCase findFortress() {
        return FIND_FORTRESS;
    }

    public static StructureCase findEndCity() {
        return FIND_END_CITY;
    }

    public record StructureCase(
        Identifier advancementId,
        String criterion,
        Identifier structureId,
        ResourceKey<Level> dimension,
        BlockPos target
    ) {
    }

    private static StructureCase requiredDirectStructureOnlyCase(
        Identifier advancementId,
        String criterion,
        Identifier expectedStructureId,
        ResourceKey<Level> dimension,
        BlockPos target
    ) {
        Path matrixPath = resolveMatrixPath();
        try (Reader reader = Files.newBufferedReader(matrixPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray cases = root.getAsJsonArray("cases");
            for (int i = 0; i < cases.size(); i++) {
                JsonObject caseJson = cases.get(i).getAsJsonObject();
                if (!advancementId.toString().equals(caseJson.get("advancementId").getAsString())) {
                    continue;
                }
                if (!criterion.equals(caseJson.get("criterion").getAsString())) {
                    continue;
                }
                validateDirectStructureOnlyCase(caseJson, expectedStructureId);
                return new StructureCase(advancementId, criterion, expectedStructureId, dimension, target);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load structure certification matrix", e);
        }
        throw new IllegalArgumentException("Missing structure-only certification case for " + advancementId + "#" + criterion);
    }

    private static void validateDirectStructureOnlyCase(JsonObject caseJson, Identifier expectedStructureId) {
        requireString(caseJson, "category", "STRUCTURE_LOCATION");
        requireString(caseJson, "structureLocationType", "STRUCTURE_ONLY");
        requireString(caseJson, "playerPredicate", "SURVIVAL_OR_NON_SPECTATOR");
        requireEmptyArray(caseJson, "biomes");
        requireEmptyArray(caseJson, "dimensions");
        requireEmptyArray(caseJson, "blockOrFluidConditions");
        requireEmptyArray(caseJson, "additionalConditions");
        requireEmptyArray(caseJson, "completionRequirements");

        JsonArray structures = caseJson.getAsJsonArray("structures");
        if (structures == null || structures.size() != 1) {
            throw new IllegalStateException("Expected exactly one direct structure requirement for " + caseJson.get("advancementId").getAsString());
        }
        Identifier actualStructureId = normalizeStructureId(structures.get(0).getAsString());
        if (!expectedStructureId.equals(actualStructureId)) {
            throw new IllegalStateException(
                "Expected structure " + expectedStructureId + " but matrix declared " + actualStructureId +
                    " for " + caseJson.get("advancementId").getAsString() + "#" + caseJson.get("criterion").getAsString()
            );
        }
    }

    private static void requireString(JsonObject caseJson, String key, String expectedValue) {
        String actualValue = caseJson.has(key) ? caseJson.get(key).getAsString() : "";
        if (!expectedValue.equals(actualValue)) {
            throw new IllegalStateException("Expected " + key + "=" + expectedValue + " but got " + actualValue);
        }
    }

    private static void requireEmptyArray(JsonObject caseJson, String key) {
        JsonArray values = caseJson.getAsJsonArray(key);
        if (values != null && !values.isEmpty()) {
            throw new IllegalStateException("Expected empty " + key + " for " + caseJson.get("advancementId").getAsString());
        }
    }

    private static Identifier normalizeStructureId(String rawId) {
        if (rawId.startsWith("#")) {
            throw new IllegalStateException("Tag-based structure requirement is not part of the direct slice: " + rawId);
        }
        return rawId.contains(":") ? Identifier.parse(rawId) : Identifier.parse("minecraft:" + rawId);
    }

    private static Path resolveMatrixPath() {
        Path[] roots = new Path[]{
            Path.of("").toAbsolutePath().normalize(),
            net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize()
        };
        for (Path root : roots) {
            Path current = root;
            while (current != null) {
                Path candidate = current.resolve(MATRIX_RELATIVE_PATH);
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
                current = current.getParent();
            }
        }
        throw new IllegalStateException("Unable to locate required file at " + MATRIX_RELATIVE_PATH);
    }
}
