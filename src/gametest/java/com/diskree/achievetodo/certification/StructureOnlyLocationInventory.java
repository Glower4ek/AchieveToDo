package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class StructureOnlyLocationInventory {
    private static final String MATRIX_RELATIVE_PATH = "src/test/resources/phase_a_certification/location_movement_matrix.json";
    private static final List<Identifier> VILLAGE_TAG_MEMBERS = List.of(
        Identifier.parse("minecraft:village_plains"),
        Identifier.parse("minecraft:village_desert"),
        Identifier.parse("minecraft:village_savanna"),
        Identifier.parse("minecraft:village_snowy"),
        Identifier.parse("minecraft:village_taiga")
    );

    private StructureOnlyLocationInventory() {
    }

    public static List<StructureOnlyCase> allCases() {
        return loadCases();
    }

    public static List<StructureOnlyCase> directCases() {
        return allCases().stream().filter(caseEntry -> caseEntry.predicateShape() == PredicateShape.DIRECT_STRUCTURE_ID).toList();
    }

    public static List<StructureOnlyCase> deferredTagCases() {
        return allCases().stream().filter(caseEntry -> caseEntry.predicateShape() == PredicateShape.STRUCTURE_TAG_OR_HOLDERSET).toList();
    }

    private static List<StructureOnlyCase> loadCases() {
        Path matrixPath = resolveMatrixPath();
        try (Reader reader = Files.newBufferedReader(matrixPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray cases = root.getAsJsonArray("cases");
            var builder = new java.util.ArrayList<StructureOnlyCase>();
            for (int i = 0; i < cases.size(); i++) {
                JsonObject caseJson = cases.get(i).getAsJsonObject();
                if (!"STRUCTURE_LOCATION".equals(caseJson.get("category").getAsString())) {
                    continue;
                }
                if (!"STRUCTURE_ONLY".equals(caseJson.get("structureLocationType").getAsString())) {
                    continue;
                }
                builder.add(parseCase(caseJson));
            }
            return List.copyOf(builder);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load structure certification matrix", e);
        }
    }

    private static StructureOnlyCase parseCase(JsonObject caseJson) {
        Identifier advancementId = Identifier.parse(caseJson.get("advancementId").getAsString());
        String criterion = caseJson.get("criterion").getAsString();
        JsonArray structures = caseJson.getAsJsonArray("structures");
        if (structures == null || structures.isEmpty()) {
            throw new IllegalStateException("Expected at least one structure requirement for " + advancementId + "#" + criterion);
        }
        String frozenPredicate = structures.get(0).getAsString();
        PredicateShape predicateShape = classifyPredicate(frozenPredicate);
        List<Identifier> requiredTagMembers = predicateShape == PredicateShape.STRUCTURE_TAG_OR_HOLDERSET ? VILLAGE_TAG_MEMBERS : List.of();
        return new StructureOnlyCase(
            advancementId,
            criterion,
            frozenPredicate,
            frozenPredicate,
            predicateShape,
            true,
            requiredTagMembers
        );
    }

    private static PredicateShape classifyPredicate(String frozenPredicate) {
        if (frozenPredicate.startsWith("#")) {
            return PredicateShape.STRUCTURE_TAG_OR_HOLDERSET;
        }
        return PredicateShape.DIRECT_STRUCTURE_ID;
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

    public enum PredicateShape {
        DIRECT_STRUCTURE_ID,
        STRUCTURE_TAG_OR_HOLDERSET
    }

    public record StructureOnlyCase(
        Identifier advancementId,
        String criterion,
        String frozenStructurePredicate,
        String runtimeCompatibleStructurePredicate,
        PredicateShape predicateShape,
        boolean semanticEquivalent,
        List<Identifier> requiredTagMembers
    ) {
        public boolean isDirect() {
            return predicateShape == PredicateShape.DIRECT_STRUCTURE_ID;
        }

        public boolean isTagPredicate() {
            return predicateShape == PredicateShape.STRUCTURE_TAG_OR_HOLDERSET;
        }

        public Identifier tagId() {
            if (!isTagPredicate()) {
                throw new IllegalStateException("Not a tag predicate: " + frozenStructurePredicate);
            }
            return Identifier.parse(frozenStructurePredicate.substring(1));
        }
    }
}
