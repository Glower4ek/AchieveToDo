package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CertifiedLocationMovementMatrix {
    private static final String MATRIX_RELATIVE_PATH = "src/test/resources/phase_a_certification/location_movement_matrix.json";
    private static final Map<Identifier, List<CaseDefinition>> AUTOMATION_SUPPORTED_CASES = loadAutomationSupportedCases();

    private CertifiedLocationMovementMatrix() {
    }

    public static List<CaseDefinition> automatedSimpleCasesFor(Identifier advancementId) {
        List<CaseDefinition> cases = filterCases(advancementId, "BIOME_ONLY", "BIOME_SET");
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("Missing certified simple LOCATION_MOVEMENT cases for " + advancementId);
        }
        return cases;
    }

    public static List<CaseDefinition> automatedDimensionOnlyCasesFor(Identifier advancementId) {
        List<CaseDefinition> cases = new ArrayList<>();
        for (CaseDefinition caseDefinition : automatedCasesFor(advancementId)) {
            if ("DIMENSION_ONLY".equals(caseDefinition.dimensionLocationType())) {
                cases.add(caseDefinition);
            }
        }
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("Missing certified dimension-only LOCATION_MOVEMENT cases for " + advancementId);
        }
        return List.copyOf(cases);
    }

    private static List<CaseDefinition> filterCases(Identifier advancementId, String... allowedCategories) {
        List<CaseDefinition> cases = automatedCasesFor(advancementId);
        List<CaseDefinition> filtered = new ArrayList<>();
        for (CaseDefinition caseDefinition : cases) {
            for (String allowedCategory : allowedCategories) {
                if (allowedCategory.equals(caseDefinition.category())) {
                    filtered.add(caseDefinition);
                    break;
                }
            }
        }
        return List.copyOf(filtered);
    }

    private static List<CaseDefinition> automatedCasesFor(Identifier advancementId) {
        List<CaseDefinition> cases = AUTOMATION_SUPPORTED_CASES.get(advancementId);
        if (cases == null) {
            throw new IllegalArgumentException("Missing certified LOCATION_MOVEMENT cases for " + advancementId);
        }
        return cases;
    }

    public static Set<Identifier> automatedAdvancementIds() {
        return AUTOMATION_SUPPORTED_CASES.keySet();
    }

    public static final class CaseDefinition {
        private final Identifier advancementId;
        private final String criterion;
        private final String category;
        private final String dimensionLocationType;
        private final List<String> biomeIds;
        private final List<String> dimensionIds;

        public CaseDefinition(
            Identifier advancementId,
            String criterion,
            String category,
            String dimensionLocationType,
            List<String> biomeIds,
            List<String> dimensionIds
        ) {
            this.advancementId = advancementId;
            this.criterion = criterion;
            this.category = category;
            this.dimensionLocationType = dimensionLocationType;
            this.biomeIds = List.copyOf(biomeIds);
            this.dimensionIds = List.copyOf(dimensionIds);
        }

        public Identifier advancementId() {
            return advancementId;
        }

        public String criterion() {
            return criterion;
        }

        public String category() {
            return category;
        }

        public String dimensionLocationType() {
            return dimensionLocationType;
        }

        public List<String> biomeIds() {
            return biomeIds;
        }

        public List<String> dimensionIds() {
            return dimensionIds;
        }
    }

    private static Map<Identifier, List<CaseDefinition>> loadAutomationSupportedCases() {
        Path matrixPath = resolveMatrixPath();
        try (Reader reader = Files.newBufferedReader(matrixPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Path runtimeCompatiblePackPath = resolveRelativePath(root.get("runtimeCompatibleSource").getAsString());
            JsonArray cases = root.getAsJsonArray("cases");
            Map<Identifier, List<CaseDefinition>> groupedCases = new LinkedHashMap<>();
            try (FileSystem runtimePackFileSystem = openZipFileSystem(runtimeCompatiblePackPath)) {
                for (int i = 0; i < cases.size(); i++) {
                    JsonObject caseJson = cases.get(i).getAsJsonObject();
                    if (!"SUPPORTED".equals(caseJson.get("automationEligibility").getAsString())) {
                        continue;
                    }
                    String category = caseJson.get("category").getAsString();
                    if (!("BIOME_ONLY".equals(category) || "BIOME_SET".equals(category))) {
                        continue;
                    }
                    Identifier advancementId = Identifier.parse(caseJson.get("advancementId").getAsString());
                    String criterion = caseJson.get("criterion").getAsString();
                    String dimensionLocationType = caseJson.has("dimensionLocationType")
                        ? caseJson.get("dimensionLocationType").getAsString()
                        : "";
                    List<String> biomeIds = expandBiomeArray(caseJson.getAsJsonArray("biomes"), runtimePackFileSystem);
                    List<String> dimensionIds = readStringArray(caseJson.getAsJsonArray("dimensions"));
                    groupedCases.computeIfAbsent(advancementId, ignored -> new ArrayList<>())
                        .add(new CaseDefinition(advancementId, criterion, category, dimensionLocationType, biomeIds, dimensionIds));
                }
            }
            groupedCases.values().forEach(caseDefinitions -> caseDefinitions.sort(Comparator.comparing(CaseDefinition::criterion)));
            return Map.copyOf(groupedCases);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load certified LOCATION_MOVEMENT matrix from " + matrixPath, e);
        }
    }

    private static List<String> expandBiomeArray(JsonArray values, FileSystem runtimePackFileSystem) throws IOException {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (int i = 0; i < values.size(); i++) {
            expandBiomeValue(values.get(i).getAsString(), runtimePackFileSystem, new LinkedHashSet<>(), result);
        }
        return List.copyOf(result);
    }

    private static List<String> readStringArray(JsonArray values) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            result.add(values.get(i).getAsString());
        }
        return List.copyOf(result);
    }

    private static void expandBiomeValue(
        String biomeOrTag,
        FileSystem runtimePackFileSystem,
        Set<String> visitingTags,
        LinkedHashSet<String> resolvedBiomes
    ) throws IOException {
        if (!biomeOrTag.startsWith("#")) {
            resolvedBiomes.add(biomeOrTag);
            return;
        }

        if (!visitingTags.add(biomeOrTag)) {
            throw new IllegalStateException("Detected recursive biome tag reference: " + biomeOrTag);
        }
        try {
            Identifier tagId = Identifier.parse(biomeOrTag.substring(1));
            Path tagPath = runtimePackFileSystem.getPath(
                "data",
                tagId.getNamespace(),
                "tags",
                "worldgen",
                "biome",
                tagId.getPath() + ".json"
            );
            if (!Files.isRegularFile(tagPath)) {
                throw new IllegalStateException("Missing biome tag " + biomeOrTag + " in runtime-compatible pack");
            }
            try (Reader reader = Files.newBufferedReader(tagPath)) {
                JsonObject tagRoot = JsonParser.parseReader(reader).getAsJsonObject();
                JsonArray tagValues = tagRoot.getAsJsonArray("values");
                if (tagValues == null) {
                    throw new IllegalStateException("Biome tag " + biomeOrTag + " does not define values");
                }
                for (int i = 0; i < tagValues.size(); i++) {
                    expandBiomeValue(tagValues.get(i).getAsString(), runtimePackFileSystem, visitingTags, resolvedBiomes);
                }
            }
        } finally {
            visitingTags.remove(biomeOrTag);
        }
    }

    private static Path resolveMatrixPath() {
        return resolveRelativePath(MATRIX_RELATIVE_PATH);
    }

    private static Path resolveRelativePath(String relativePath) {
        Path[] roots = candidateRoots();
        for (Path root : roots) {
            Path current = root;
            while (current != null) {
                Path candidate = current.resolve(relativePath);
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
                current = current.getParent();
            }
        }
        throw new IllegalStateException("Unable to locate required file at " + relativePath);
    }

    private static Path[] candidateRoots() {
        return new Path[]{
            Path.of("").toAbsolutePath().normalize(),
            net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize()
        };
    }

    private static FileSystem openZipFileSystem(Path zipPath) throws IOException {
        return FileSystems.newFileSystem(URI.create("jar:" + zipPath.toUri()), Map.of());
    }
}
