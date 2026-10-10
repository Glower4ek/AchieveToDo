package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.diskree.achievetodo.client.ExternalPackCompatibility;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class PhaseACertification {

    public static final String CANONICAL_ROOT_EXCLUSION = "blazeandcave:bacap/root";
    public static final int EXPECTED_CANONICAL_ADVANCEMENTS = 1152;
    public static final Path INVENTORY_SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "advancement_inventory.json");
    public static final Path MATRIX_SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "runtime_test_matrix.json");
    public static final Path RECONCILIATION_JSON = Path.of("build", "reports", "phase-a-certification", "reconciliation_report.json");
    public static final Path RECONCILIATION_MD = Path.of("build", "reports", "phase-a-certification", "reconciliation_report.md");

    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();
    private static final Pattern MISSING_REGISTRY_RESOURCE_KEY_PATTERN =
        Pattern.compile("(?:Registry does not exist:|Can't access registry) ResourceKey\\[minecraft:root / ([^\\]]+)\\]");
    private static final Pattern MISSING_TAG_DIAGNOSTIC_PATTERN =
        Pattern.compile("Missing tag: '([^']+)' in '([^']+)'");
    private static final Set<String> ALLOWED_FULL_REGISTRY_FALLBACK_IDS = Set.of(
        "minecraft:banner_pattern",
        "minecraft:instrument"
    );
    private static final String ENCHANTMENTS = "enchantments";
    private static final String STORED_ENCHANTMENTS = "stored_enchantments";
    private static final List<ScenarioSpec> SCENARIOS = List.of(
        new ScenarioSpec(
            "base",
            List.of("bacap.zip"),
            List.of("bacap_override", "bacap_rewards_item", "bacap_rewards_experience", "bacap_rewards_trophy", "bacap_cooperative_mode"),
            null,
            null
        ),
        new ScenarioSpec(
            "hardcore",
            List.of("bacap.zip", "bacap_hardcore.zip"),
            List.of("bacap_override", "bacap_hardcore_override", "bacap_rewards_item", "bacap_rewards_experience", "bacap_rewards_trophy", "bacap_cooperative_mode"),
            "bacap_hardcore.zip",
            null
        ),
        new ScenarioSpec(
            "terralith",
            List.of("bacap.zip", "terralith.zip", "bacap_terralith.zip"),
            List.of("bacap_override", "bacap_terralith_override", "bacap_rewards_item", "bacap_rewards_experience", "bacap_rewards_trophy", "bacap_cooperative_mode"),
            "bacap_terralith.zip",
            "terralith.zip"
        ),
        new ScenarioSpec(
            "amplified_nether",
            List.of("bacap.zip", "amplified_nether.zip", "bacap_amplified_nether.zip"),
            List.of("bacap_override", "bacap_amplified_nether_override", "bacap_rewards_item", "bacap_rewards_experience", "bacap_rewards_trophy", "bacap_cooperative_mode"),
            "bacap_amplified_nether.zip",
            "amplified_nether.zip"
        ),
        new ScenarioSpec(
            "nullscape",
            List.of("bacap.zip", "nullscape.zip", "bacap_nullscape.zip"),
            List.of("bacap_override", "bacap_nullscape_override", "bacap_rewards_item", "bacap_rewards_experience", "bacap_rewards_trophy", "bacap_cooperative_mode"),
            "bacap_nullscape.zip",
            "nullscape.zip"
        )
    );

    private static RegistryAccess.Frozen registryAccess;
    private static HolderLookup.Provider fullVanillaRegistryLookup;

    private PhaseACertification() {
    }

    public static void bootstrapMinecraft() {
        if (registryAccess != null && fullVanillaRegistryLookup != null) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
        fullVanillaRegistryLookup = VanillaRegistries.createLookup();
    }

    public static CertificationArtifacts generate(Path projectRoot) throws IOException {
        return generate(projectRoot, projectRoot.resolve("src/main/resources/assets/minecraft/lang/ru_ru.json"), null);
    }

    /** Historical callers supply authenticated inputs; default callers retain the current view. */
    public static CertificationArtifacts generate(Path projectRoot, Path russianDictionary, Path internalPackZip) throws IOException {
        Path frozenPacks = projectRoot.resolve("reference").resolve("phase_a_preservation").resolve("files").resolve("final");
        Path resourcepacks = projectRoot.resolve("src").resolve("main").resolve("resources").resolve("resourcepacks");

        AdvancementPackData bacap = loadZipAdvancements(frozenPacks.resolve("bacap.zip"), "bacap.zip");
        Map<String, Set<String>> variantVisibleIds = new LinkedHashMap<>();
        variantVisibleIds.put("bacap_hardcore.zip", loadZipVisibleAdvancementIds(frozenPacks.resolve("bacap_hardcore.zip")));
        variantVisibleIds.put("bacap_terralith.zip", loadZipVisibleAdvancementIds(frozenPacks.resolve("bacap_terralith.zip")));
        variantVisibleIds.put("bacap_amplified_nether.zip", loadZipVisibleAdvancementIds(frozenPacks.resolve("bacap_amplified_nether.zip")));
        variantVisibleIds.put("bacap_nullscape.zip", loadZipVisibleAdvancementIds(frozenPacks.resolve("bacap_nullscape.zip")));
        variantVisibleIds.put("terralith.zip", loadZipVisibleAdvancementIds(frozenPacks.resolve("terralith.zip")));
        variantVisibleIds.put("amplified_nether.zip", loadZipVisibleAdvancementIds(frozenPacks.resolve("amplified_nether.zip")));
        variantVisibleIds.put("nullscape.zip", loadZipVisibleAdvancementIds(frozenPacks.resolve("nullscape.zip")));

        Map<String, String> englishLang = loadBacapEnglishLang(frozenPacks.resolve("bacap.zip"));
        Map<String, String> ruOverlay = loadLangFile(russianDictionary);
        Map<String, String> achievetodoRu = loadLangFile(projectRoot.resolve("src").resolve("main").resolve("resources").resolve("assets").resolve("achievetodo").resolve("lang").resolve("ru_ru.json"));
        Map<String, String> functionSources = loadFunctionSources(frozenPacks, internalPackZip == null ? resourcepacks : null);
        if (internalPackZip != null) {
            try (ZipFile zip = new ZipFile(internalPackZip.toFile())) {
                for (var entry : zip.stream().sorted(Comparator.comparing(ZipEntry::getName)).toList()) {
                    String name = entry.getName();
                    if (!name.matches("^[^/]+/data/[^/]+/function/.+\\.mcfunction$")) continue;
                    String[] parts = name.split("/", 5);
                    String id = parts[2] + ":" + parts[4].replaceFirst("\\.mcfunction$", "");
                    functionSources.put(id, parts[0] + "::" + Path.of(name));
                }
            }
        }

        List<InventoryEntry> inventory = buildCanonicalInventory(bacap, englishLang, ruOverlay, achievetodoRu, functionSources);
        if (inventory.size() != EXPECTED_CANONICAL_ADVANCEMENTS) {
            throw new IllegalStateException("Expected " + EXPECTED_CANONICAL_ADVANCEMENTS + " canonical advancements but found " + inventory.size());
        }

        JsonObject inventoryJson = buildInventoryJson(inventory, bacap);
        JsonObject matrixJson = buildMatrixJson(variantVisibleIds);
        JsonObject reconciliationJson = buildReconciliationJson(bacap, inventory, variantVisibleIds);
        String reconciliationMarkdown = buildReconciliationMarkdown(bacap, inventory, variantVisibleIds);

        return new CertificationArtifacts(
            GSON.toJson(inventoryJson) + System.lineSeparator(),
            GSON.toJson(matrixJson) + System.lineSeparator(),
            GSON.toJson(reconciliationJson) + System.lineSeparator(),
            reconciliationMarkdown,
            inventory
        );
    }

    public static void writeArtifacts(Path projectRoot) throws IOException {
        CertificationArtifacts artifacts = generate(projectRoot);
        writeString(projectRoot.resolve(INVENTORY_SNAPSHOT), artifacts.inventoryJson());
        writeString(projectRoot.resolve(MATRIX_SNAPSHOT), artifacts.matrixJson());
        writeString(projectRoot.resolve(RECONCILIATION_JSON), artifacts.reconciliationJson());
        writeString(projectRoot.resolve(RECONCILIATION_MD), artifacts.reconciliationMarkdown());
    }

    public static void main(String[] args) throws IOException {
        Path projectRoot = args.length == 0 ? Path.of("").toAbsolutePath().normalize() : Path.of(args[0]).toAbsolutePath().normalize();
        writeArtifacts(projectRoot);
    }

    private static List<InventoryEntry> buildCanonicalInventory(
        AdvancementPackData bacap,
        Map<String, String> englishLang,
        Map<String, String> ruOverlay,
        Map<String, String> achievetodoRu,
        Map<String, String> functionSources
    ) {
        List<InventoryEntry> inventory = new ArrayList<>();
        for (RawAdvancement advancement : bacap.advancements().values()) {
            if (!isCanonical(advancement)) {
                continue;
            }
            String titleKey = extractTranslateKey(advancement.json().getAsJsonObject("display").get("title"));
            String descriptionKey = extractTranslateKey(advancement.json().getAsJsonObject("display").get("description"));
            inventory.add(new InventoryEntry(
                advancement.id(),
                advancement.namespace(),
                advancement.category(),
                advancement.packName(),
                advancement.entryName(),
                advancement.parentId(),
                titleKey,
                englishLang.containsKey(titleKey),
                ruOverlay.containsKey(titleKey) || achievetodoRu.containsKey(titleKey),
                descriptionKey,
                englishLang.containsKey(descriptionKey),
                ruOverlay.containsKey(descriptionKey) || achievetodoRu.containsKey(descriptionKey),
                advancement.criteriaCount(),
                advancement.rewardFunctionIds(),
                resolveRewardFunctionSources(advancement.rewardFunctionIds(), functionSources)
            ));
        }
        inventory.sort(Comparator.comparing(InventoryEntry::id));
        return inventory;
    }

    private static JsonObject buildInventoryJson(List<InventoryEntry> inventory, AdvancementPackData bacap) {
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", "phase_a_certified_inventory");
        root.addProperty("canonicalRule", "visible display advancements from frozen bacap.zip excluding blazeandcave:bacap/root");
        root.addProperty("frozenSourcePack", bacap.packName());
        root.addProperty("canonicalAdvancementCount", inventory.size());

        JsonObject byNamespace = new JsonObject();
        byNamespace.addProperty("blazeandcave", inventory.stream().filter(entry -> entry.namespace().equals("blazeandcave")).count());
        byNamespace.addProperty("minecraft", inventory.stream().filter(entry -> entry.namespace().equals("minecraft")).count());
        root.add("countsByNamespace", byNamespace);

        JsonObject byCategory = new JsonObject();
        Map<String, Long> categoryCounts = new TreeMap<>();
        for (InventoryEntry entry : inventory) {
            categoryCounts.merge(entry.category(), 1L, Long::sum);
        }
        for (Map.Entry<String, Long> category : categoryCounts.entrySet()) {
            byCategory.addProperty(category.getKey(), category.getValue());
        }
        root.add("countsByCategory", byCategory);

        JsonArray entries = new JsonArray();
        for (InventoryEntry entry : inventory) {
            JsonObject json = new JsonObject();
            json.addProperty("id", entry.id());
            json.addProperty("namespace", entry.namespace());
            json.addProperty("category", entry.category());
            json.addProperty("sourcePack", entry.sourcePack());
            json.addProperty("sourcePath", entry.sourcePath());
            if (entry.parentId() != null) {
                json.addProperty("parentId", entry.parentId());
            }
            json.addProperty("titleKey", entry.titleKey());
            json.addProperty("titleKeyPresentInFrozenEnglish", entry.titleKeyPresentInFrozenEnglish());
            json.addProperty("titleKeyPresentInRuntimeRussian", entry.titleKeyPresentInRuntimeRussian());
            json.addProperty("descriptionKey", entry.descriptionKey());
            json.addProperty("descriptionKeyPresentInFrozenEnglish", entry.descriptionKeyPresentInFrozenEnglish());
            json.addProperty("descriptionKeyPresentInRuntimeRussian", entry.descriptionKeyPresentInRuntimeRussian());
            json.addProperty("criteriaCount", entry.criteriaCount());

            JsonArray rewardFunctions = new JsonArray();
            for (String rewardFunctionId : entry.rewardFunctionIds()) {
                rewardFunctions.add(rewardFunctionId);
            }
            json.add("rewardFunctionIds", rewardFunctions);

            JsonObject rewardSources = new JsonObject();
            for (Map.Entry<String, String> rewardFunctionSource : entry.rewardFunctionSources().entrySet()) {
                rewardSources.addProperty(rewardFunctionSource.getKey(), rewardFunctionSource.getValue());
            }
            json.add("rewardFunctionSources", rewardSources);
            entries.add(json);
        }
        root.add("entries", entries);
        return root;
    }

    private static JsonObject buildMatrixJson(Map<String, Set<String>> variantVisibleIds) {
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", "phase_a_runtime_test_matrix");
        root.addProperty("canonicalAdvancementCount", EXPECTED_CANONICAL_ADVANCEMENTS);
        JsonArray scenarios = new JsonArray();
        for (ScenarioSpec scenario : SCENARIOS) {
            JsonObject json = new JsonObject();
            json.addProperty("id", scenario.id());

            JsonArray externalPacks = new JsonArray();
            scenario.externalPacks().forEach(externalPacks::add);
            json.add("externalPacks", externalPacks);

            JsonArray internalPacks = new JsonArray();
            scenario.internalPacks().forEach(internalPacks::add);
            json.add("internalPacks", internalPacks);

            json.addProperty("expectedCanonicalAdvancementCount", EXPECTED_CANONICAL_ADVANCEMENTS);

            JsonArray variantReplacements = new JsonArray();
            if (scenario.variantPack() != null) {
                new TreeSet<>(variantVisibleIds.getOrDefault(scenario.variantPack(), Set.of())).forEach(variantReplacements::add);
            }
            json.add("variantReplacementAdvancementIds", variantReplacements);

            JsonArray environmentPackAdvancements = new JsonArray();
            if (scenario.environmentPack() != null) {
                new TreeSet<>(variantVisibleIds.getOrDefault(scenario.environmentPack(), Set.of())).forEach(environmentPackAdvancements::add);
            }
            json.add("environmentPackAdvancementIds", environmentPackAdvancements);
            scenarios.add(json);
        }
        root.add("scenarios", scenarios);
        return root;
    }

    private static JsonObject buildReconciliationJson(
        AdvancementPackData bacap,
        List<InventoryEntry> inventory,
        Map<String, Set<String>> variantVisibleIds
    ) {
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", "phase_a_reconciliation");
        root.addProperty("resolvedCanonicalAdvancementCount", inventory.size());

        JsonObject raw = new JsonObject();
        raw.addProperty("bacapZipAllAdvancementJson", bacap.advancements().size());
        raw.addProperty("bacapZipBlazeandcaveNamespaceAdvancementJson", bacap.rawCounts().get("blazeandcave"));
        raw.addProperty("bacapZipMinecraftNamespaceAdvancementJson", bacap.rawCounts().get("minecraft"));
        raw.addProperty("bacapZipVisibleDisplayAdvancementJson", bacap.visibleCount());
        raw.addProperty("bacapZipHiddenDisplayAdvancementJson", bacap.hiddenDisplayCount());
        raw.addProperty("bacapZipNoDisplayAdvancementJson", bacap.noDisplayCount());
        raw.addProperty("excludedRootIds", 1);
        root.add("rawCounts", raw);

        JsonObject canonical = new JsonObject();
        canonical.addProperty("blazeandcaveVisibleCanonical", inventory.stream().filter(entry -> entry.namespace().equals("blazeandcave")).count());
        canonical.addProperty("minecraftVisibleCanonical", inventory.stream().filter(entry -> entry.namespace().equals("minecraft")).count());
        root.add("canonicalCounts", canonical);

        JsonObject variants = new JsonObject();
        for (Map.Entry<String, Set<String>> entry : variantVisibleIds.entrySet()) {
            variants.addProperty(entry.getKey(), entry.getValue().size());
        }
        root.add("variantVisibleCounts", variants);

        JsonArray notes = new JsonArray();
        notes.add("The previous 1106 count was a namespace-only scan of data/blazeandcave/advancement/*.json inside frozen bacap.zip.");
        notes.add("Canonical Phase A progression is resolved from visible display advancements in frozen bacap.zip, not from every helper or hidden advancement json.");
        notes.add("The exact canonical formula is 1229 total json - 61 no-display helpers - 15 hidden display nodes - 1 excluded blazeandcave:bacap/root = 1152.");
        notes.add("The final 1152 split is 1030 blazeandcave:* plus 122 minecraft:* advancements bundled in frozen bacap.zip.");
        root.add("notes", notes);
        return root;
    }

    private static String buildReconciliationMarkdown(
        AdvancementPackData bacap,
        List<InventoryEntry> inventory,
        Map<String, Set<String>> variantVisibleIds
    ) {
        long blazeandcaveCount = inventory.stream().filter(entry -> entry.namespace().equals("blazeandcave")).count();
        long minecraftCount = inventory.stream().filter(entry -> entry.namespace().equals("minecraft")).count();
        return """
            # Phase A Reconciliation

            - Frozen source of truth: `reference/phase_a_preservation/files/final/bacap.zip`
            - Canonical rule: visible display advancements from frozen `bacap.zip`, excluding `blazeandcave:bacap/root`
            - Resolved canonical total: `%d`

            ## Raw Frozen Counts

            - All advancement JSON files in `bacap.zip`: `%d`
            - `blazeandcave:*` advancement JSON files: `%d`
            - `minecraft:*` advancement JSON files: `%d`
            - Visible display advancements: `%d`
            - Hidden display advancements: `%d`
            - No-display helper advancements: `%d`

            ## Exact Reconciliation

            - Previous `1106` number was a namespace-only scan of `data/blazeandcave/advancement/*.json`.
            - That scan did not include `minecraft:*` advancements bundled by BACAP and did include hidden/helper JSON that are not canonical progression targets.
            - Final canonical inventory is `%d` `blazeandcave:*` plus `%d` `minecraft:*`.
            - Exact formula: `1229 - 61 - 15 - 1 = 1152`.

            ## Variant Visible Advancement Sets

            - `bacap_hardcore.zip`: `%d`
            - `bacap_terralith.zip`: `%d`
            - `bacap_amplified_nether.zip`: `%d`
            - `bacap_nullscape.zip`: `%d`
            - `terralith.zip`: `%d`
            - `amplified_nether.zip`: `%d`
            - `nullscape.zip`: `%d`
            """.formatted(
            inventory.size(),
            bacap.advancements().size(),
            bacap.rawCounts().get("blazeandcave"),
            bacap.rawCounts().get("minecraft"),
            bacap.visibleCount(),
            bacap.hiddenDisplayCount(),
            bacap.noDisplayCount(),
            blazeandcaveCount,
            minecraftCount,
            variantVisibleIds.get("bacap_hardcore.zip").size(),
            variantVisibleIds.get("bacap_terralith.zip").size(),
            variantVisibleIds.get("bacap_amplified_nether.zip").size(),
            variantVisibleIds.get("bacap_nullscape.zip").size(),
            variantVisibleIds.get("terralith.zip").size(),
            variantVisibleIds.get("amplified_nether.zip").size(),
            variantVisibleIds.get("nullscape.zip").size()
        );
    }

    private static Map<String, String> resolveRewardFunctionSources(List<String> rewardFunctionIds, Map<String, String> functionSources) {
        Map<String, String> resolved = new LinkedHashMap<>();
        for (String rewardFunctionId : rewardFunctionIds) {
            resolved.put(rewardFunctionId, functionSources.getOrDefault(rewardFunctionId, "MISSING"));
        }
        return resolved;
    }

    private static AdvancementPackData loadZipAdvancements(Path zipPath, String packName) throws IOException {
        Map<String, RawAdvancement> advancements = new LinkedHashMap<>();
        Map<String, Integer> rawCounts = new LinkedHashMap<>();
        rawCounts.put("blazeandcave", 0);
        rawCounts.put("minecraft", 0);
        int visibleCount = 0;
        int hiddenDisplayCount = 0;
        int noDisplayCount = 0;

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            List<? extends ZipEntry> entries = zipFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().matches("^data/[^/]+/advancement/.+\\.json$"))
                .sorted(Comparator.comparing(ZipEntry::getName))
                .toList();
            for (ZipEntry entry : entries) {
                JsonObject json = readJson(zipFile.getInputStream(entry));
                String id = entry.getName().replaceFirst("^data/([^/]+)/advancement/(.+)\\.json$", "$1:$2");
                String namespace = id.substring(0, id.indexOf(':'));
                rawCounts.computeIfPresent(namespace, (ignored, value) -> value + 1);

                JsonObject display = json.has("display") && json.get("display").isJsonObject() ? json.getAsJsonObject("display") : null;
                boolean hidden = display != null && GsonHelper.getAsBoolean(display, "hidden", false);
                if (display == null) {
                    noDisplayCount++;
                } else if (hidden) {
                    hiddenDisplayCount++;
                } else {
                    visibleCount++;
                }

                String rewardFunction = extractRewardFunctionId(json);
                List<String> rewardFunctions = rewardFunction == null ? List.of() : List.of(rewardFunction);
                advancements.put(id, new RawAdvancement(
                    id,
                    namespace,
                    pathCategory(id),
                    packName,
                    entry.getName(),
                    json,
                    display != null,
                    hidden,
                    json.has("parent") ? json.get("parent").getAsString() : null,
                    json.has("criteria") ? json.getAsJsonObject("criteria").size() : 0,
                    rewardFunctions
                ));
            }
        }

        return new AdvancementPackData(packName, advancements, rawCounts, visibleCount, hiddenDisplayCount, noDisplayCount);
    }

    private static Set<String> loadZipVisibleAdvancementIds(Path zipPath) throws IOException {
        Set<String> ids = new TreeSet<>();
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            List<? extends ZipEntry> entries = zipFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().matches("^data/[^/]+/advancement/.+\\.json$"))
                .sorted(Comparator.comparing(ZipEntry::getName))
                .toList();
            for (ZipEntry entry : entries) {
                JsonObject json = readJson(zipFile.getInputStream(entry));
                JsonObject display = json.has("display") && json.get("display").isJsonObject() ? json.getAsJsonObject("display") : null;
                boolean hidden = display != null && GsonHelper.getAsBoolean(display, "hidden", false);
                if (display == null || hidden) {
                    continue;
                }
                ids.add(entry.getName().replaceFirst("^data/([^/]+)/advancement/(.+)\\.json$", "$1:$2"));
            }
        }
        return ids;
    }

    private static Map<String, String> loadBacapEnglishLang(Path zipPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry lang = zipFile.getEntry("assets/blazeandcave/lang/en_us.json");
            if (lang == null) {
                return Map.of();
            }
            return parseFlatLang(zipFile.getInputStream(lang));
        }
    }

    private static Map<String, String> loadLangFile(Path path) throws IOException {
        if (Files.notExists(path)) {
            return Map.of();
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            return parseFlatLang(inputStream);
        }
    }

    private static Map<String, String> parseFlatLang(InputStream inputStream) throws IOException {
        JsonObject json = readJson(inputStream);
        Map<String, String> keys = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            keys.put(entry.getKey(), entry.getValue().getAsString());
        }
        return keys;
    }

    private static Map<String, String> loadFunctionSources(Path frozenPacks, Path resourcepacks) throws IOException {
        Map<String, String> sources = new LinkedHashMap<>();
        try (var zipFiles = Files.list(frozenPacks).sorted()) {
            for (Path zipPath : zipFiles.filter(path -> path.getFileName().toString().endsWith(".zip")).toList()) {
                try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                    List<? extends ZipEntry> entries = zipFile.stream()
                        .filter(entry -> !entry.isDirectory())
                        .filter(entry -> entry.getName().matches("^data/[^/]+/function/.+\\.mcfunction$"))
                        .sorted(Comparator.comparing(ZipEntry::getName))
                        .toList();
                    for (ZipEntry entry : entries) {
                        String id = entry.getName().replaceFirst("^data/([^/]+)/function/(.+)\\.mcfunction$", "$1:$2");
                        sources.putIfAbsent(id, zipPath.getFileName() + "::" + entry.getName());
                    }
                }
            }
        }
        if (resourcepacks != null && Files.exists(resourcepacks)) {
            Files.walkFileTree(resourcepacks, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!file.getFileName().toString().endsWith(".mcfunction")) {
                        return FileVisitResult.CONTINUE;
                    }
                    Path relative = resourcepacks.relativize(file);
                    if (relative.getNameCount() < 5 || !"data".equals(relative.getName(1).toString()) || !"function".equals(relative.getName(3).toString())) {
                        return FileVisitResult.CONTINUE;
                    }
                    String pack = relative.getName(0).toString();
                    String namespace = relative.getName(2).toString();
                    String functionPath = relative.subpath(4, relative.getNameCount()).toString().replace('\\', '/').replaceFirst("\\.mcfunction$", "");
                    sources.put(namespace + ":" + functionPath, pack + "::" + relative);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return sources;
    }

    private static JsonObject readJson(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static boolean isCanonical(RawAdvancement advancement) {
        return advancement.hasDisplay() && !advancement.hidden() && !CANONICAL_ROOT_EXCLUSION.equals(advancement.id());
    }

    private static String pathCategory(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        int slashIndex = path.indexOf('/');
        return slashIndex == -1 ? path : path.substring(0, slashIndex);
    }

    private static String extractTranslateKey(JsonElement component) {
        if (component == null || component.isJsonNull()) {
            return "";
        }
        if (component.isJsonObject()) {
            JsonObject object = component.getAsJsonObject();
            if (object.has("translate")) {
                return object.get("translate").getAsString();
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String key = extractTranslateKey(entry.getValue());
                if (!key.isBlank()) {
                    return key;
                }
            }
        }
        if (component.isJsonArray()) {
            for (JsonElement child : component.getAsJsonArray()) {
                String key = extractTranslateKey(child);
                if (!key.isBlank()) {
                    return key;
                }
            }
        }
        return "";
    }

    private static boolean requiresFrozenEnglishKey(JsonElement component) {
        if (component == null || component.isJsonNull() || component.isJsonPrimitive()) {
            return false;
        }
        if (component.isJsonObject()) {
            JsonObject object = component.getAsJsonObject();
            if (object.has("text")) {
                return false;
            }
            if (object.has("translate")) {
                if (object.has("fallback") && !object.get("fallback").getAsString().isBlank()) {
                    return false;
                }
                return looksLikeLocalizationKey(object.get("translate").getAsString());
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (requiresFrozenEnglishKey(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
        for (JsonElement child : component.getAsJsonArray()) {
            if (requiresFrozenEnglishKey(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeLocalizationKey(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (!(character >= 'a' && character <= 'z')
                && !(character >= '0' && character <= '9')
                && character != '_'
                && character != '.'
                && character != ':'
                && character != '/'
                && character != '-') {
                return false;
            }
        }
        return true;
    }

    static JsonObject normalizeCompatibilityJson(JsonElement jsonElement) throws ReflectiveOperationException {
        Method convertJson = ExternalPackCompatibility.class.getDeclaredMethod("convertJson", String.class);
        if (!convertJson.getDeclaringClass().equals(ExternalPackCompatibility.class)
            || convertJson.getParameterCount() != 1
            || !convertJson.getParameterTypes()[0].equals(String.class)
            || !convertJson.getReturnType().getSimpleName().equals("ConversionResult")) {
            throw new IllegalStateException("Unexpected ExternalPackCompatibility.convertJson signature");
        }
        convertJson.setAccessible(true);
        Object conversionResult = convertJson.invoke(null, jsonElement.toString());
        Method text = conversionResult.getClass().getDeclaredMethod("text");
        if (!text.getReturnType().equals(String.class) || text.getParameterCount() != 0) {
            throw new IllegalStateException("Unexpected conversion result text() signature");
        }
        text.setAccessible(true);
        return JsonParser.parseString((String) text.invoke(conversionResult)).getAsJsonObject();
    }

    private static String extractRewardFunctionId(JsonObject json) {
        if (!json.has("rewards") || !json.get("rewards").isJsonObject()) {
            return null;
        }
        JsonObject rewards = json.getAsJsonObject("rewards");
        if (!rewards.has("function")) {
            return null;
        }
        return rewards.get("function").getAsString();
    }

    private static void writeString(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    public static void certifyStaticRules(Path projectRoot) throws IOException {
        StaticCertificationReport report = analyzeStaticRules(projectRoot);
        StaticValidationEntry firstFailure = report.entries().stream()
            .filter(entry -> entry.status() == StaticValidationStatus.STATIC_FAIL)
            .findFirst()
            .orElse(null);
        if (firstFailure != null) {
            throw new IllegalStateException("STATIC_FAIL " + firstFailure.id() + ": " + firstFailure.exactCodecError());
        }
    }

    public static StaticCertificationReport analyzeStaticRules(Path projectRoot) throws IOException {
        bootstrapMinecraft();
        CertificationArtifacts artifacts = generate(projectRoot);
        RuntimeTagSourceIndex runtimeTagSourceIndex = loadBaseRuntimeTagSourceIndex(projectRoot);
        return analyzeStaticRules(projectRoot, artifacts, runtimeTagSourceIndex);
    }

    private static StaticCertificationReport analyzeStaticRules(
        Path projectRoot,
        CertificationArtifacts artifacts,
        RuntimeTagSourceIndex runtimeTagSourceIndex
    ) throws IOException {
        bootstrapMinecraft();

        Set<String> canonicalIds = new LinkedHashSet<>();
        Map<String, String> allAdvancementSources = new LinkedHashMap<>();
        AdvancementPackData bacap = loadZipAdvancements(
            projectRoot.resolve("reference").resolve("phase_a_preservation").resolve("files").resolve("final").resolve("bacap.zip"),
            "bacap.zip"
        );
        for (RawAdvancement advancement : bacap.advancements().values()) {
            allAdvancementSources.put(advancement.id(), advancement.entryName());
        }

        var registryOps = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        var fullRegistryOps = RegistryOps.create(JsonOps.INSTANCE, fullVanillaRegistryLookup);
        List<StaticValidationEntry> validationEntries = new ArrayList<>();
        for (InventoryEntry entry : artifacts.inventory()) {
            if (!canonicalIds.add(entry.id())) {
                throw new IllegalStateException("Duplicate canonical advancement id: " + entry.id());
            }
            RawAdvancement rawAdvancement = bacap.advancements().get(entry.id());
            if (rawAdvancement == null) {
                throw new IllegalStateException("Missing raw advancement for canonical id: " + entry.id());
            }
            if (entry.parentId() != null && !allAdvancementSources.containsKey(entry.parentId())) {
                throw new IllegalStateException("Missing parent advancement for " + entry.id() + ": " + entry.parentId());
            }
            JsonObject normalizedAdvancementJson;
            try {
                normalizedAdvancementJson = normalizeCompatibilityJson(rawAdvancement.json());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Failed to normalize advancement JSON for " + entry.id(), e);
            }
            StaticValidationEntry codecValidation = validateCodec(
                entry.id(),
                rawAdvancement.entryName(),
                rawAdvancement.json(),
                normalizedAdvancementJson,
                registryOps,
                fullRegistryOps,
                runtimeTagSourceIndex
            );
            if (codecValidation != null && codecValidation.status() == StaticValidationStatus.STATIC_FAIL) {
                validationEntries.add(codecValidation);
                continue;
            }

            JsonObject display = rawAdvancement.json().getAsJsonObject("display");
            JsonElement titleComponent = display.get("title");
            JsonElement descriptionComponent = display.get("description");

            if (entry.titleKey().isBlank() || (!entry.titleKeyPresentInFrozenEnglish() && requiresFrozenEnglishKey(titleComponent))) {
                throw new IllegalStateException("Missing frozen English title key for " + entry.id() + ": " + entry.titleKey());
            }
            if (entry.descriptionKey().isBlank() || (!entry.descriptionKeyPresentInFrozenEnglish() && requiresFrozenEnglishKey(descriptionComponent))) {
                throw new IllegalStateException("Missing frozen English description key for " + entry.id() + ": " + entry.descriptionKey());
            }
            for (Map.Entry<String, String> rewardSource : entry.rewardFunctionSources().entrySet()) {
                if (Objects.equals(rewardSource.getValue(), "MISSING")) {
                    throw new IllegalStateException("Missing reward function source for " + entry.id() + ": " + rewardSource.getKey());
                }
            }
            Identifier.parse(entry.id());
            validationEntries.add(codecValidation != null ? codecValidation : new StaticValidationEntry(
                entry.id(),
                StaticValidationStatus.STATIC_PASS,
                null,
                Set.of(),
                null,
                null
            ));
        }
        return new StaticCertificationReport(validationEntries);
    }

    private static StaticValidationEntry validateCodec(
        String advancementId,
        String advancementSource,
        JsonObject rawAdvancementJson,
        JsonObject normalizedAdvancementJson,
        RegistryOps<JsonElement> registryOps,
        RegistryOps<JsonElement> fullRegistryOps,
        RuntimeTagSourceIndex runtimeTagSourceIndex
    ) {
        DataResult<Advancement> parseResult = Advancement.CODEC.parse(registryOps, normalizedAdvancementJson);
        List<String> parseMessages = parseResult.error()
            .map(error -> List.of(error.message()))
            .orElseGet(List::of);
        Advancement parsed = parseResult.result().orElse(null);
        if (parseMessages.isEmpty()) {
            return parsed != null
                ? null
                : new StaticValidationEntry(
                    advancementId,
                    StaticValidationStatus.STATIC_FAIL,
                    null,
                    Set.of(),
                    "Advancement codec parse returned no error and no result",
                    null
                );
        }
        if (isPureAllowedVanillaRegistryContextFailure(parseMessages)) {
            DataResult<Advancement> fullParseResult = Advancement.CODEC.parse(fullRegistryOps, normalizedAdvancementJson);
            if (fullParseResult.error().isEmpty() && fullParseResult.result().isPresent()) {
                return null;
            }
        }
        String exactCodecError = String.join("; ", parseMessages);
        RegistryDeferredClassification registryDeferredClassification = classifyRegistryDeferredComponent(
            advancementId,
            parseMessages,
            rawAdvancementJson,
            normalizedAdvancementJson,
            fullRegistryOps,
            runtimeTagSourceIndex
        );
        if (registryDeferredClassification != null) {
            return new StaticValidationEntry(
                advancementId,
                StaticValidationStatus.RUNTIME_DEFERRED,
                registryDeferredClassification.deferredReason(),
                registryDeferredClassification.registryComponents(),
                exactCodecError,
                registryDeferredClassification
            );
        }
        return new StaticValidationEntry(
            advancementId,
            StaticValidationStatus.STATIC_FAIL,
            null,
            Set.of(),
            exactCodecError,
            null
        );
    }

    static boolean isPureAllowedVanillaRegistryContextFailure(List<String> parseMessages) {
        Set<String> missingRegistries = extractMissingRegistryIds(parseMessages);
        return missingRegistries.size() == 1
            && ALLOWED_FULL_REGISTRY_FALLBACK_IDS.contains(missingRegistries.iterator().next())
            && containsOnlyMissingRegistryDiagnostics(parseMessages, missingRegistries);
    }

    private static RegistryDeferredClassification classifyRegistryDeferredComponent(
        String advancementId,
        List<String> parseMessages,
        JsonObject rawAdvancementJson,
        JsonObject normalizedAdvancementJson,
        RegistryOps<JsonElement> fullRegistryOps,
        RuntimeTagSourceIndex runtimeTagSourceIndex
    ) {
        RegistryDeferredClassification trimClassification = classifyTrimRegistryDeferredComponent(
            advancementId,
            parseMessages,
            normalizedAdvancementJson
        );
        if (trimClassification != null) {
            return trimClassification;
        }
        RegistryDeferredClassification tagClassification = classifyTagContextDeferredComponent(
            advancementId,
            parseMessages,
            rawAdvancementJson,
            normalizedAdvancementJson,
            fullRegistryOps,
            runtimeTagSourceIndex
        );
        if (tagClassification != null) {
            return tagClassification;
        }
        RegistryDeferredClassification enchantmentClassification = classifyEnchantmentHolderSetDeferredComponent(
            advancementId,
            parseMessages,
            rawAdvancementJson,
            normalizedAdvancementJson
        );
        if (enchantmentClassification != null) {
            return enchantmentClassification;
        }
        RegistryDeferredClassification multiClassification = classifyMultiContextDeferredComponent(
            advancementId,
            parseMessages,
            rawAdvancementJson,
            normalizedAdvancementJson,
            runtimeTagSourceIndex
        );
        if (multiClassification != null) {
            return multiClassification;
        }
        List<LocationHolderFieldMatch> matches = findLocationHolderFieldMatches(parseMessages, rawAdvancementJson, normalizedAdvancementJson);
        if (matches.isEmpty()) {
            return null;
        }
        boolean biome = matches.stream().anyMatch(match -> match.field() == LocationHolderField.BIOMES);
        boolean structure = matches.stream().anyMatch(match -> match.field() == LocationHolderField.STRUCTURES);
        return new RegistryDeferredClassification(
            advancementId,
            DeferredReason.REGISTRY_CONTEXT_REQUIRED,
            singletonRegistryComponents(
                biome && structure
                    ? RegistryDependentComponent.BIOME_AND_STRUCTURE_HOLDERSET
                    : biome
                        ? RegistryDependentComponent.BIOME_HOLDERSET
                        : RegistryDependentComponent.STRUCTURE_HOLDERSET
            ),
            matches,
            List.of(),
            List.of(),
            missingRegistryLookup(LocationHolderField.BIOMES),
            missingRegistryLookup(LocationHolderField.STRUCTURES)
        );
    }

    private static RegistryDeferredClassification classifyTrimRegistryDeferredComponent(
        String advancementId,
        List<String> parseMessages,
        JsonObject normalizedAdvancementJson
    ) {
        Set<String> missingRegistries = extractMissingRegistryIds(parseMessages);
        boolean missingTrimPatternRegistry = missingRegistries.contains("minecraft:trim_pattern");
        boolean missingTrimMaterialRegistry = missingRegistries.contains("minecraft:trim_material");
        if (!(missingTrimPatternRegistry || missingTrimMaterialRegistry) || !containsTrimComponent(normalizedAdvancementJson)) {
            return null;
        }
        if (missingTrimPatternRegistry && missingTrimMaterialRegistry) {
            return new RegistryDeferredClassification(
                advancementId,
                DeferredReason.REGISTRY_CONTEXT_REQUIRED,
                singletonRegistryComponents(RegistryDependentComponent.TRIM_PATTERN_AND_MATERIAL),
                List.of(),
                List.of(),
                List.of(),
                false,
                false
            );
        }
        return new RegistryDeferredClassification(
            advancementId,
            DeferredReason.REGISTRY_CONTEXT_REQUIRED,
            singletonRegistryComponents(
                missingTrimPatternRegistry
                    ? RegistryDependentComponent.TRIM_PATTERN
                    : RegistryDependentComponent.TRIM_MATERIAL
            ),
            List.of(),
            List.of(),
            List.of(),
            false,
            false
            );
    }

    private static RegistryDeferredClassification classifyTagContextDeferredComponent(
        String advancementId,
        List<String> parseMessages,
        JsonObject rawAdvancementJson,
        JsonObject normalizedAdvancementJson,
        RegistryOps<JsonElement> fullRegistryOps,
        RuntimeTagSourceIndex runtimeTagSourceIndex
    ) {
        List<MissingTagDiagnostic> diagnostics = extractMissingTagDiagnostics(parseMessages);
        if (diagnostics.isEmpty()) {
            return null;
        }
        List<MissingTagReferenceMatch> referenceMatches = validateMissingTagReferenceMatches(
            rawAdvancementJson,
            normalizedAdvancementJson,
            diagnostics,
            runtimeTagSourceIndex
        );
        if (referenceMatches == null) {
            return null;
        }
        RegistryDependentComponent component = collapseTagComponents(referenceMatches);
        if (component == null) {
            return null;
        }
        if (!containsOnlyMissingTagDiagnostics(parseMessages)
            && !canClassifyValidatedTagWithAllowedRegistryFallback(parseMessages, normalizedAdvancementJson, fullRegistryOps, referenceMatches)) {
            return null;
        }
        return new RegistryDeferredClassification(
            advancementId,
            DeferredReason.TAG_CONTEXT_REQUIRED,
            singletonRegistryComponents(component),
            List.of(),
            referenceMatches,
            List.of(),
            false,
            false
        );
    }

    private static RegistryDeferredClassification classifyEnchantmentHolderSetDeferredComponent(
        String advancementId,
        List<String> parseMessages,
        JsonObject rawAdvancementJson,
        JsonObject normalizedAdvancementJson
    ) {
        List<EnchantmentHolderSetMatch> matches = validateEnchantmentHolderSetMatches(rawAdvancementJson, normalizedAdvancementJson);
        if (matches.isEmpty() || !containsOnlyEnchantmentHolderSetDiagnostics(parseMessages, matches)) {
            return null;
        }
        return new RegistryDeferredClassification(
            advancementId,
            DeferredReason.REGISTRY_CONTEXT_REQUIRED,
            singletonRegistryComponents(RegistryDependentComponent.ENCHANTMENT_HOLDERSET),
            List.of(),
            List.of(),
            matches,
            false,
            false
        );
    }

    private static RegistryDeferredClassification classifyMultiContextDeferredComponent(
        String advancementId,
        List<String> parseMessages,
        JsonObject rawAdvancementJson,
        JsonObject normalizedAdvancementJson,
        RuntimeTagSourceIndex runtimeTagSourceIndex
    ) {
        List<MissingTagDiagnostic> tagDiagnostics = extractMissingTagDiagnostics(parseMessages);
        List<MissingTagReferenceMatch> tagMatches = validateMissingTagReferenceMatches(
            rawAdvancementJson,
            normalizedAdvancementJson,
            tagDiagnostics,
            runtimeTagSourceIndex
        );
        List<EnchantmentHolderSetMatch> enchantmentMatches = validateEnchantmentHolderSetMatches(rawAdvancementJson, normalizedAdvancementJson);
        boolean hasEnchantmentDimension = !enchantmentMatches.isEmpty()
            && allEnchantmentMatchesUseEquipmentPredicateContext(enchantmentMatches)
            && diagnosticChunks(parseMessages).stream().anyMatch(chunk -> "minecraft:enchantment".equals(decodeMissingRegistryId(chunk)));
        Set<RegistryDependentComponent> components = new LinkedHashSet<>();
        if (tagMatches != null) {
            components.addAll(extractTagRegistryComponents(tagMatches));
        }
        if (hasEnchantmentDimension) {
            components.add(RegistryDependentComponent.ENCHANTMENT_HOLDERSET);
        }
        if (!hasEnchantmentDimension) {
            return null;
        }
        if (components.size() < 2) {
            return null;
        }
        if (!allDiagnosticChunksAccountedFor(
            parseMessages,
            tagMatches == null ? List.of() : tagMatches,
            enchantmentMatches,
            Set.of()
        )) {
            return null;
        }
        return new RegistryDeferredClassification(
            advancementId,
            DeferredReason.MULTIPLE_CONTEXT_REQUIRED,
            components,
            List.of(),
            tagMatches == null ? List.of() : tagMatches,
            enchantmentMatches,
            false,
            false
        );
    }

    private static Set<String> extractMissingRegistryIds(List<String> parseMessages) {
        Set<String> missingRegistries = new LinkedHashSet<>();
        for (String message : parseMessages) {
            for (String chunk : message.split(";")) {
                String registryId = decodeMissingRegistryId(stripMissedInputSuffix(chunk).trim());
                if (registryId != null) {
                    missingRegistries.add(registryId);
                }
            }
        }
        return missingRegistries;
    }

    private static boolean containsOnlyMissingRegistryDiagnostics(List<String> parseMessages, Set<String> expectedRegistryIds) {
        boolean sawRegistryDiagnostic = false;
        for (String message : parseMessages) {
            for (String chunk : message.split(";")) {
                String diagnostic = stripMissedInputSuffix(chunk).trim();
                if (diagnostic.isEmpty()) {
                    continue;
                }
                String registryId = decodeMissingRegistryId(diagnostic);
                if (registryId == null) {
                    return false;
                }
                if (!expectedRegistryIds.contains(registryId)) {
                    return false;
                }
                sawRegistryDiagnostic = true;
            }
        }
        return sawRegistryDiagnostic;
    }

    private static List<MissingTagDiagnostic> extractMissingTagDiagnostics(List<String> parseMessages) {
        List<MissingTagDiagnostic> diagnostics = new ArrayList<>();
        for (String message : parseMessages) {
            Matcher matcher = MISSING_TAG_DIAGNOSTIC_PATTERN.matcher(message);
            while (matcher.find()) {
                diagnostics.add(new MissingTagDiagnostic(matcher.group(1), matcher.group(2)));
            }
        }
        return diagnostics;
    }

    private static boolean containsOnlyMissingTagDiagnostics(List<String> parseMessages) {
        for (String message : parseMessages) {
            for (String chunk : message.split(";")) {
                String trimmed = chunk.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                trimmed = stripMissedInputSuffix(trimmed);
                if (!MISSING_TAG_DIAGNOSTIC_PATTERN.matcher(trimmed).matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String stripMissedInputSuffix(String message) {
        int missedInputIndex = message.indexOf(" missed input:");
        return missedInputIndex >= 0 ? message.substring(0, missedInputIndex).trim() : message.trim();
    }

    private static List<String> diagnosticChunks(List<String> parseMessages) {
        List<String> diagnostics = new ArrayList<>();
        for (String message : parseMessages) {
            for (String chunk : message.split(";")) {
                String trimmed = stripMissedInputSuffix(chunk).trim();
                if (!trimmed.isEmpty()) {
                    diagnostics.add(trimmed);
                }
            }
        }
        return diagnostics;
    }

    private static List<MissingTagReferenceMatch> validateMissingTagReferenceMatches(
        JsonObject rawAdvancementJson,
        JsonObject normalizedAdvancementJson,
        List<MissingTagDiagnostic> diagnostics,
        RuntimeTagSourceIndex runtimeTagSourceIndex
    ) {
        if (diagnostics.isEmpty()) {
            return List.of();
        }
        List<MissingTagReferenceMatch> referenceMatches = new ArrayList<>();
        for (MissingTagDiagnostic diagnostic : diagnostics) {
            RegistryDependentComponent component = mapTagRegistryComponent(diagnostic.registryId());
            if (component == null) {
                return null;
            }
            String tagReference = "#" + diagnostic.tagId();
            List<String> rawReferences = collectStringReferences(rawAdvancementJson, tagReference);
            List<String> normalizedReferences = collectStringReferences(normalizedAdvancementJson, tagReference);
            boolean runtimeExists = runtimeTagSourceIndex.contains(diagnostic.registryId(), diagnostic.tagId());
            boolean junitResolvable = isTagResolvableInJUnit(diagnostic.registryId(), diagnostic.tagId());
            boolean subtreePreserved = !rawReferences.isEmpty() && rawReferences.equals(normalizedReferences);
            if (!runtimeExists || junitResolvable || !subtreePreserved) {
                return null;
            }
            referenceMatches.add(new MissingTagReferenceMatch(
                diagnostic.tagId(),
                diagnostic.registryId(),
                tagReference,
                rawReferences,
                normalizedReferences,
                subtreePreserved,
                runtimeExists,
                runtimeTagSourceIndex.sourceFor(diagnostic.registryId(), diagnostic.tagId()).orElse(""),
                junitResolvable
            ));
        }
        return List.copyOf(referenceMatches);
    }

    private static RegistryDependentComponent collapseTagComponents(List<MissingTagReferenceMatch> referenceMatches) {
        RegistryDependentComponent component = null;
        for (MissingTagReferenceMatch match : referenceMatches) {
            RegistryDependentComponent current = mapTagRegistryComponent(match.registryId());
            if (current == null) {
                return null;
            }
            if (component == null) {
                component = current;
                continue;
            }
            if (component != current) {
                return null;
            }
        }
        return component;
    }

    private static Set<RegistryDependentComponent> extractTagRegistryComponents(List<MissingTagReferenceMatch> referenceMatches) {
        Set<RegistryDependentComponent> components = new LinkedHashSet<>();
        for (MissingTagReferenceMatch match : referenceMatches) {
            RegistryDependentComponent component = mapTagRegistryComponent(match.registryId());
            if (component == null) {
                return Set.of();
            }
            components.add(component);
        }
        return immutableRegistryComponents(components);
    }

    private static RegistryDependentComponent mapTagRegistryComponent(String registryId) {
        return switch (registryId) {
            case "minecraft:block" -> RegistryDependentComponent.BLOCK_TAG;
            case "minecraft:item" -> RegistryDependentComponent.ITEM_TAG;
            case "minecraft:entity_type" -> RegistryDependentComponent.ENTITY_TYPE_TAG;
            default -> null;
        };
    }

    private static List<String> collectStringReferences(JsonElement element, String exactValue) {
        List<String> matches = new ArrayList<>();
        collectStringReferences(element, exactValue, matches);
        return matches;
    }

    private static void collectStringReferences(JsonElement element, String exactValue, List<String> matches) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (exactValue.equals(value)) {
                matches.add(value);
            }
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                collectStringReferences(entry.getValue(), exactValue, matches);
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectStringReferences(child, exactValue, matches);
            }
        }
    }

    private static boolean isTagResolvableInJUnit(String registryId, String tagId) {
        Identifier identifier = Identifier.parse(tagId);
        return switch (registryId) {
            case "minecraft:block" -> registryAccess.lookup(Registries.BLOCK)
                .flatMap(lookup -> lookup.get(TagKey.create(Registries.BLOCK, identifier)))
                .isPresent();
            case "minecraft:item" -> registryAccess.lookup(Registries.ITEM)
                .flatMap(lookup -> lookup.get(TagKey.create(Registries.ITEM, identifier)))
                .isPresent();
            case "minecraft:entity_type" -> registryAccess.lookup(Registries.ENTITY_TYPE)
                .flatMap(lookup -> lookup.get(TagKey.create(Registries.ENTITY_TYPE, identifier)))
                .isPresent();
            default -> false;
        };
    }

    private static List<EnchantmentHolderSetMatch> findEnchantmentHolderSetMatches(
        JsonObject rawAdvancementJson,
        JsonObject normalizedAdvancementJson
    ) {
        List<EnchantmentHolderSetMatch> rawMatches = collectEnchantmentHolderSetMatches(rawAdvancementJson);
        List<EnchantmentHolderSetMatch> normalizedMatches = collectEnchantmentHolderSetMatches(normalizedAdvancementJson);
        if (rawMatches.size() != normalizedMatches.size()) {
            return List.of();
        }
        List<EnchantmentHolderSetMatch> reconciledMatches = new ArrayList<>(rawMatches.size());
        for (int i = 0; i < rawMatches.size(); i++) {
            EnchantmentHolderSetMatch rawMatch = rawMatches.get(i);
            EnchantmentHolderSetMatch normalizedMatch = normalizedMatches.get(i);
            if (!rawMatch.path().equals(normalizedMatch.path())
                || !rawMatch.predicateListKey().equals(normalizedMatch.predicateListKey())) {
                return List.of();
            }
            String normalizedSelector = reconcileMigratedEnchantmentSelector(rawMatch, normalizedMatch);
            if (normalizedSelector == null || !rawMatch.enchantmentId().equals(normalizedSelector)) {
                return List.of();
            }
            reconciledMatches.add(new EnchantmentHolderSetMatch(
                rawMatch.predicateListKey(),
                rawMatch.path(),
                rawMatch.enchantmentId(),
                normalizedSelector,
                rawMatch.subtreePreserved()
                    && normalizedMatch.subtreePreserved()
                    && rawMatch.normalizedEnchantmentId().equals(normalizedMatch.normalizedEnchantmentId())
            ));
        }
        return List.copyOf(reconciledMatches);
    }

    private static List<EnchantmentHolderSetMatch> collectEnchantmentHolderSetMatches(JsonElement element) {
        List<EnchantmentHolderSetMatch> matches = new ArrayList<>();
        collectEnchantmentHolderSetMatches(element, "$", matches);
        return matches;
    }

    private static void collectEnchantmentHolderSetMatches(JsonElement element, String path, List<EnchantmentHolderSetMatch> matches) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement predicatesElement = object.get("predicates");
            if (predicatesElement != null && predicatesElement.isJsonObject()) {
                JsonObject predicates = predicatesElement.getAsJsonObject();
                collectEnchantmentHolderSetMatches(predicates, path + ".predicates", ENCHANTMENTS, matches);
                collectEnchantmentHolderSetMatches(predicates, path + ".predicates", STORED_ENCHANTMENTS, matches);
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                collectEnchantmentHolderSetMatches(entry.getValue(), path + "." + entry.getKey(), matches);
            }
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                collectEnchantmentHolderSetMatches(array.get(i), path + "[" + i + "]", matches);
            }
        }
    }

    private static void collectEnchantmentHolderSetMatches(
        JsonObject predicates,
        String predicatePath,
        String predicateListKey,
        List<EnchantmentHolderSetMatch> matches
    ) {
        JsonElement predicateList = predicates.get(predicateListKey);
        if (predicateList == null || !predicateList.isJsonArray()) {
            return;
        }
        JsonArray array = predicateList.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement child = array.get(i);
            if (!child.isJsonObject()) {
                continue;
            }
            JsonObject predicate = child.getAsJsonObject();
            JsonElement enchantmentId = predicate.get(ENCHANTMENTS);
            String selector = decodeAcceptedEnchantmentSelector(enchantmentId);
            if (selector == null) {
                continue;
            }
            matches.add(new EnchantmentHolderSetMatch(
                predicateListKey,
                predicatePath + "." + predicateListKey + "[" + i + "]." + ENCHANTMENTS,
                selector,
                describeAcceptedEnchantmentSelector(enchantmentId),
                isScalarEnchantmentReference(enchantmentId)
            ));
        }
    }

    private static String reconcileMigratedEnchantmentSelector(
        EnchantmentHolderSetMatch rawMatch,
        EnchantmentHolderSetMatch normalizedMatch
    ) {
        if (!normalizedMatch.path().equals(rawMatch.path())
            || !normalizedMatch.predicateListKey().equals(rawMatch.predicateListKey())) {
            return null;
        }
        if (rawMatch.normalizedEnchantmentId().equals(normalizedMatch.normalizedEnchantmentId())) {
            return normalizedMatch.enchantmentId();
        }
        if (!rawMatch.subtreePreserved()) {
            return null;
        }
        return normalizedMatch.enchantmentId();
    }

    private static String decodeAcceptedEnchantmentSelector(JsonElement element) {
        if (isScalarEnchantmentReference(element)) {
            return element.getAsString();
        }
        if (!isAcceptedNormalizedEnchantmentSelectorArray(element)) {
            return null;
        }
        return element.getAsJsonArray().get(0).getAsString();
    }

    private static String describeAcceptedEnchantmentSelector(JsonElement element) {
        if (isScalarEnchantmentReference(element)) {
            return element.getAsString();
        }
        if (!isAcceptedNormalizedEnchantmentSelectorArray(element)) {
            return null;
        }
        return element.getAsJsonArray().toString();
    }

    private static boolean isScalarEnchantmentReference(JsonElement element) {
        return element != null
            && element.isJsonPrimitive()
            && element.getAsJsonPrimitive().isString()
            && !element.getAsString().startsWith("#");
    }

    private static boolean isAcceptedNormalizedEnchantmentSelectorArray(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return false;
        }
        JsonArray array = element.getAsJsonArray();
        return array.size() == 1
            && array.get(0).isJsonPrimitive()
            && array.get(0).getAsJsonPrimitive().isString()
            && !array.get(0).getAsString().startsWith("#");
    }

    private static List<EnchantmentHolderSetMatch> validateEnchantmentHolderSetMatches(
        JsonObject rawAdvancementJson,
        JsonObject normalizedAdvancementJson
    ) {
        if (registryAccess.lookup(Registries.ENCHANTMENT).isPresent()) {
            return List.of();
        }
        return findEnchantmentHolderSetMatches(rawAdvancementJson, normalizedAdvancementJson);
    }

    private static boolean containsOnlyEnchantmentHolderSetDiagnostics(
        List<String> parseMessages,
        List<EnchantmentHolderSetMatch> matches
    ) {
        Set<String> expectedIds = new LinkedHashSet<>();
        for (EnchantmentHolderSetMatch match : matches) {
            expectedIds.add(match.enchantmentId());
        }
        boolean sawDiagnostic = false;
        for (String message : parseMessages) {
            for (String chunk : message.split(";")) {
                String trimmed = chunk.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                trimmed = stripMissedInputSuffix(trimmed);
                if (decodeNotJsonArrayStringPayload(trimmed) != null) {
                    String payload = decodeNotJsonArrayStringPayload(trimmed);
                    if (!expectedIds.contains(payload)) {
                        return false;
                    }
                    sawDiagnostic = true;
                    continue;
                }
                String missingRegistryId = decodeMissingRegistryId(trimmed);
                if (missingRegistryId == null || !"minecraft:enchantment".equals(missingRegistryId)) {
                    return false;
                }
                sawDiagnostic = true;
            }
        }
        return sawDiagnostic;
    }

    private static String decodeNotJsonArrayStringPayload(String diagnostic) {
        if (!diagnostic.startsWith("Not a json array: ")) {
            return null;
        }
        try {
            JsonElement payload = JsonParser.parseString(diagnostic.substring("Not a json array: ".length()).trim());
            return payload.isJsonPrimitive() && payload.getAsJsonPrimitive().isString()
                ? payload.getAsString()
                : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String decodeMissingRegistryId(String diagnostic) {
        Matcher matcher = MISSING_REGISTRY_RESOURCE_KEY_PATTERN.matcher(diagnostic);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static boolean allDiagnosticChunksAccountedFor(
        List<String> parseMessages,
        List<MissingTagReferenceMatch> validatedTagMatches,
        List<EnchantmentHolderSetMatch> validatedEnchantmentMatches,
        Set<String> allowedFallbackRegistries
    ) {
        Set<String> validatedTagKeys = validatedTagMatches.stream()
            .map(match -> match.registryId() + "|" + match.tagId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> validatedEnchantmentIds = validatedEnchantmentMatches.stream()
            .map(EnchantmentHolderSetMatch::enchantmentId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        boolean sawMeaningfulDiagnostic = false;
        for (String chunk : diagnosticChunks(parseMessages)) {
            Matcher tagMatcher = MISSING_TAG_DIAGNOSTIC_PATTERN.matcher(chunk);
            if (tagMatcher.matches()) {
                if (!validatedTagKeys.contains(tagMatcher.group(2) + "|" + tagMatcher.group(1))) {
                    return false;
                }
                sawMeaningfulDiagnostic = true;
                continue;
            }
            String scalarPayload = decodeNotJsonArrayStringPayload(chunk);
            if (scalarPayload != null) {
                if (!validatedEnchantmentIds.contains(scalarPayload)) {
                    return false;
                }
                sawMeaningfulDiagnostic = true;
                continue;
            }
            String missingRegistryId = decodeMissingRegistryId(chunk);
            if (missingRegistryId == null) {
                return false;
            }
            if ("minecraft:enchantment".equals(missingRegistryId)) {
                if (validatedEnchantmentIds.isEmpty()) {
                    return false;
                }
                sawMeaningfulDiagnostic = true;
                continue;
            }
            if (!allowedFallbackRegistries.contains(missingRegistryId)) {
                return false;
            }
            sawMeaningfulDiagnostic = true;
        }
        return sawMeaningfulDiagnostic;
    }

    static boolean canClassifyValidatedTagWithAllowedRegistryFallback(
        List<String> parseMessages,
        JsonObject normalizedAdvancementJson,
        RegistryOps<JsonElement> fullRegistryOps,
        List<MissingTagReferenceMatch> validatedTagMatches
    ) {
        DataResult<Advancement> fullParseResult = Advancement.CODEC.parse(fullRegistryOps, normalizedAdvancementJson);
        List<String> fullParseMessages = fullParseResult.error()
            .map(error -> List.of(error.message()))
            .orElseGet(List::of);
        return canCollapseAllowedFallbackDiagnosticsToPureTag(parseMessages, fullParseMessages, validatedTagMatches);
    }

    static boolean canCollapseAllowedFallbackDiagnosticsToPureTag(
        List<String> limitedParseMessages,
        List<String> fullParseMessages,
        List<MissingTagReferenceMatch> validatedTagMatches
    ) {
        Set<String> limitedMissingRegistries = extractMissingRegistryIds(limitedParseMessages);
        if (limitedMissingRegistries.size() != 1
            || !ALLOWED_FULL_REGISTRY_FALLBACK_IDS.containsAll(limitedMissingRegistries)
            || validatedTagMatches.isEmpty()) {
            return false;
        }
        Set<RegistryDependentComponent> tagComponents = extractTagRegistryComponents(validatedTagMatches);
        if (tagComponents.size() != 1) {
            return false;
        }
        List<MissingTagDiagnostic> limitedTagDiagnostics = extractMissingTagDiagnostics(limitedParseMessages);
        if (limitedTagDiagnostics.isEmpty()) {
            return false;
        }
        Set<String> validatedTagKeys = validatedTagMatches.stream()
            .map(match -> match.registryId() + "|" + match.tagId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> limitedTagKeys = limitedTagDiagnostics.stream()
            .map(diagnostic -> diagnostic.registryId() + "|" + diagnostic.tagId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!validatedTagKeys.equals(limitedTagKeys)
            || !containsOnlyAllowedFallbackAndValidatedMissingTagDiagnostics(
                limitedParseMessages,
                limitedMissingRegistries,
                validatedTagKeys
            )) {
            return false;
        }
        Set<String> fullMissingRegistries = extractMissingRegistryIds(fullParseMessages);
        if (!fullMissingRegistries.isEmpty()) {
            return false;
        }
        if (fullParseMessages.isEmpty()) {
            return true;
        }
        if (!containsOnlyMissingTagDiagnostics(fullParseMessages)) {
            return false;
        }
        Set<String> fullTagKeys = extractMissingTagDiagnostics(fullParseMessages).stream()
            .map(diagnostic -> diagnostic.registryId() + "|" + diagnostic.tagId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return !fullTagKeys.isEmpty() && fullTagKeys.equals(validatedTagKeys);
    }

    private static boolean containsOnlyAllowedFallbackAndValidatedMissingTagDiagnostics(
        List<String> parseMessages,
        Set<String> allowedRegistryIds,
        Set<String> validatedTagKeys
    ) {
        boolean sawMissingTag = false;
        boolean sawAllowedRegistry = false;
        for (String chunk : diagnosticChunks(parseMessages)) {
            Matcher tagMatcher = MISSING_TAG_DIAGNOSTIC_PATTERN.matcher(chunk);
            if (tagMatcher.matches()) {
                String tagKey = tagMatcher.group(2) + "|" + tagMatcher.group(1);
                if (!validatedTagKeys.contains(tagKey)) {
                    return false;
                }
                sawMissingTag = true;
                continue;
            }
            String missingRegistryId = decodeMissingRegistryId(chunk);
            if (missingRegistryId == null || !allowedRegistryIds.contains(missingRegistryId)) {
                return false;
            }
            sawAllowedRegistry = true;
        }
        return sawMissingTag && sawAllowedRegistry;
    }

    private static boolean allEnchantmentMatchesUseEquipmentPredicateContext(List<EnchantmentHolderSetMatch> matches) {
        return !matches.isEmpty() && matches.stream().allMatch(match -> match.path().contains(".conditions.player[0].predicate.equipment."));
    }

    private static List<LocationHolderFieldMatch> findLocationHolderFieldMatches(
        List<String> parseMessages,
        JsonObject rawAdvancementJson,
        JsonObject normalizedAdvancementJson
    ) {
        List<LocationHolderFieldMatch> matches = new ArrayList<>();
        for (LocationHolderField field : LocationHolderField.values()) {
            List<JsonElement> rawValues = collectLocationFieldValues(rawAdvancementJson, field.jsonName());
            List<JsonElement> normalizedValues = collectLocationFieldValues(normalizedAdvancementJson, field.jsonName());
            if (normalizedValues.isEmpty()
                || rawValues.isEmpty()
                || !rawValues.equals(normalizedValues)
                || !missingRegistryLookup(field)) {
                continue;
            }
            matches.add(new LocationHolderFieldMatch(field, rawValues, normalizedValues, true));
        }
        return !matches.isEmpty() && containsOnlyLocationHolderSetDiagnostics(parseMessages, matches) ? matches : List.of();
    }

    private static boolean missingRegistryLookup(LocationHolderField field) {
        return switch (field) {
            case BIOMES -> registryAccess.lookup(Registries.BIOME).isEmpty();
            case STRUCTURES -> registryAccess.lookup(Registries.STRUCTURE).isEmpty();
        };
    }

    private static boolean containsOnlyLocationHolderSetDiagnostics(
        List<String> parseMessages,
        List<LocationHolderFieldMatch> matches
    ) {
        Map<String, Integer> expected = new LinkedHashMap<>();
        for (LocationHolderFieldMatch match : matches) {
            for (JsonElement value : match.normalizedValues()) {
                if (isHolderSetScalarOrTag(value)) {
                    expected.merge(value.getAsString(), 1, Integer::sum);
                }
            }
        }
        Map<String, Integer> actual = new LinkedHashMap<>();
        for (String message : parseMessages) {
            for (String chunk : message.split(";")) {
                String trimmed = stripMissedInputSuffix(chunk).trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String payload = decodeNotJsonArrayStringPayload(trimmed);
                if (payload == null || !expected.containsKey(payload)) {
                    return false;
                }
                actual.merge(payload, 1, Integer::sum);
            }
        }
        return !actual.isEmpty() && actual.equals(expected);
    }

    private static boolean isHolderSetScalarOrTag(JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
    }

    private static List<JsonElement> collectLocationFieldValues(JsonElement element, String fieldName) {
        List<JsonElement> values = new ArrayList<>();
        collectLocationFieldValues(element, fieldName, values);
        return values;
    }

    private static void collectLocationFieldValues(JsonElement element, String fieldName, List<JsonElement> values) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("location")) {
                JsonElement locationElement = object.get("location");
                if (locationElement.isJsonObject()) {
                    JsonObject location = locationElement.getAsJsonObject();
                    if (location.has(fieldName)) {
                        values.add(location.get(fieldName));
                    }
                } else if (locationElement.isJsonArray()) {
                    for (JsonElement locationChild : locationElement.getAsJsonArray()) {
                        if (!locationChild.isJsonObject()) {
                            continue;
                        }
                        JsonObject locationCondition = locationChild.getAsJsonObject();
                        if (!locationCondition.has("predicate") || !locationCondition.get("predicate").isJsonObject()) {
                            continue;
                        }
                        JsonObject predicate = locationCondition.getAsJsonObject("predicate");
                        if (predicate.has(fieldName)) {
                            values.add(predicate.get(fieldName));
                        }
                    }
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                collectLocationFieldValues(entry.getValue(), fieldName, values);
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectLocationFieldValues(child, fieldName, values);
            }
        }
    }

    public static HolderSetFamilyReport analyzeLocationHolderSetFamily(Path projectRoot) throws IOException {
        bootstrapMinecraft();
        StaticCertificationReport report = analyzeStaticRules(projectRoot);
        Set<String> biomeHolderSetIds = new TreeSet<>();
        Set<String> structureHolderSetIds = new TreeSet<>();
        for (StaticValidationEntry entry : report.entries()) {
            if (entry.status() != StaticValidationStatus.RUNTIME_DEFERRED
                || entry.deferredReason() != DeferredReason.REGISTRY_CONTEXT_REQUIRED
                || entry.registryDeferredClassification() == null) {
                continue;
            }
            for (LocationHolderFieldMatch match : entry.registryDeferredClassification().locationHolderMatches()) {
                if (match.field() == LocationHolderField.BIOMES) {
                    biomeHolderSetIds.add(entry.id());
                }
                if (match.field() == LocationHolderField.STRUCTURES) {
                    structureHolderSetIds.add(entry.id());
                }
            }
        }
        Set<String> biomeOnly = new TreeSet<>(biomeHolderSetIds);
        biomeOnly.removeAll(structureHolderSetIds);
        Set<String> structureOnly = new TreeSet<>(structureHolderSetIds);
        structureOnly.removeAll(biomeHolderSetIds);
        Set<String> both = new TreeSet<>(biomeHolderSetIds);
        both.retainAll(structureHolderSetIds);
        Set<String> union = new TreeSet<>(biomeHolderSetIds);
        union.addAll(structureHolderSetIds);
        return new HolderSetFamilyReport(
            biomeHolderSetIds,
            structureHolderSetIds,
            biomeOnly,
            structureOnly,
            both,
            union,
            Set.of()
        );
    }

    public static MissingTagFamilyReport analyzeMissingTagFamily(Path projectRoot) throws IOException {
        bootstrapMinecraft();
        StaticCertificationReport report = analyzeStaticRules(projectRoot);
        RuntimeTagSourceIndex runtimeTagSourceIndex = loadBaseRuntimeTagSourceIndex(projectRoot);
        return analyzeMissingTagFamily(report, runtimeTagSourceIndex);
    }

    private static MissingTagFamilyReport analyzeMissingTagFamily(
        StaticCertificationReport report,
        RuntimeTagSourceIndex runtimeTagSourceIndex
    ) {
        List<MissingTagFamilyEntry> entries = new ArrayList<>();
        for (StaticValidationEntry entry : report.entries()) {
            List<MissingTagDiagnostic> diagnostics = entry.exactCodecError() == null
                ? List.of()
                : extractMissingTagDiagnostics(List.of(entry.exactCodecError()));
            if (diagnostics.isEmpty()) {
                continue;
            }
            List<MissingTagReferenceMatch> referenceMatches = entry.registryDeferredClassification() == null
                ? List.of()
                : entry.registryDeferredClassification().missingTagReferenceMatches();
            Map<String, MissingTagReferenceMatch> matchesByKey = new LinkedHashMap<>();
            for (MissingTagReferenceMatch match : referenceMatches) {
                matchesByKey.put(match.registryId() + "|" + match.tagId(), match);
            }
            for (MissingTagDiagnostic diagnostic : diagnostics) {
                MissingTagReferenceMatch matchedReference = matchesByKey.get(diagnostic.registryId() + "|" + diagnostic.tagId());
                String tagReference = "#" + diagnostic.tagId();
                List<String> rawReferences = matchedReference != null ? matchedReference.rawReferences() : List.of();
                List<String> normalizedReferences = matchedReference != null ? matchedReference.normalizedReferences() : List.of();
                boolean subtreePreserved = matchedReference != null
                    ? matchedReference.subtreePreserved()
                    : !rawReferences.isEmpty() && rawReferences.equals(normalizedReferences);
                boolean runtimeExists = matchedReference != null
                    ? matchedReference.runtimeTagExists()
                    : runtimeTagSourceIndex.contains(diagnostic.registryId(), diagnostic.tagId());
                boolean junitResolvable = matchedReference != null
                    ? matchedReference.junitTagResolvable()
                    : isTagResolvableInJUnit(diagnostic.registryId(), diagnostic.tagId());
                String runtimeSource = matchedReference != null
                    ? matchedReference.runtimeTagSource()
                    : runtimeTagSourceIndex.sourceFor(diagnostic.registryId(), diagnostic.tagId()).orElse("");
                entries.add(new MissingTagFamilyEntry(
                    entry.id(),
                    diagnostic.tagId(),
                    diagnostic.registryId(),
                    subtreePreserved,
                    runtimeExists,
                    junitResolvable,
                    entry.status(),
                    entry.deferredReason(),
                    matchedReference != null ? matchedReference.registryComponent() : mapTagRegistryComponent(diagnostic.registryId()),
                    runtimeSource
                ));
            }
        }
        return new MissingTagFamilyReport(entries);
    }

    public static TagContextSourceStackReport analyzeTagContextSourceStack(Path projectRoot) throws IOException {
        bootstrapMinecraft();
        CertificationArtifacts artifacts = generate(projectRoot);
        RuntimeTagSourceIndex legacyIndex = loadLegacyExpandedRuntimeTagSourceIndex(projectRoot);
        RuntimeTagSourceIndex baseIndex = loadBaseRuntimeTagSourceIndex(projectRoot);
        RuntimeTagSourceIndex auditIndex = loadExpandedAuditRuntimeTagSourceIndex(projectRoot);
        StaticCertificationReport legacyReport = analyzeStaticRules(projectRoot, artifacts, legacyIndex);
        StaticCertificationReport baseReport = analyzeStaticRules(projectRoot, artifacts, baseIndex);
        MissingTagFamilyReport legacyFamilyReport = analyzeMissingTagFamily(legacyReport, auditIndex);
        Set<String> legacyDeferredIds = extractTagContextDeferredAdvancementIds(legacyReport);
        Set<String> baseDeferredIds = extractTagContextDeferredAdvancementIds(baseReport);
        Set<String> optionalFalsePositiveIds = new LinkedHashSet<>(legacyDeferredIds);
        optionalFalsePositiveIds.removeAll(baseDeferredIds);
        Set<String> baseInternalFalseNegativeIds = new LinkedHashSet<>(baseDeferredIds);
        baseInternalFalseNegativeIds.removeAll(legacyDeferredIds);
        Map<String, List<MissingTagFamilyEntry>> legacyEntriesByAdvancementId = legacyFamilyReport.entries().stream()
            .collect(java.util.stream.Collectors.groupingBy(MissingTagFamilyEntry::advancementId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
        List<TagContextSourceStackEntry> entries = new ArrayList<>();
        for (String advancementId : legacyDeferredIds.stream().sorted().toList()) {
            List<MissingTagFamilyEntry> advancementEntries = legacyEntriesByAdvancementId.getOrDefault(advancementId, List.of());
            SourceProvenance provenance = classifyAdvancementSourceProvenance(advancementEntries, auditIndex);
            List<String> sources = advancementEntries.stream()
                .flatMap(entry -> auditIndex.sourcesFor(entry.registryId(), entry.tagId()).stream())
                .map(RuntimeTagSource::displaySource)
                .distinct()
                .sorted()
                .toList();
            entries.add(new TagContextSourceStackEntry(advancementId, provenance, sources));
        }
        return new TagContextSourceStackReport(entries, legacyDeferredIds, baseDeferredIds, optionalFalsePositiveIds, baseInternalFalseNegativeIds);
    }

    public static MissingTagDisjointReport analyzeMissingTagDisjoint(Path projectRoot) throws IOException {
        bootstrapMinecraft();
        StaticCertificationReport report = analyzeStaticRules(projectRoot);
        MissingTagFamilyReport familyReport = analyzeMissingTagFamily(report, loadBaseRuntimeTagSourceIndex(projectRoot));
        Map<String, StaticValidationEntry> validationById = report.entries().stream()
            .collect(java.util.stream.Collectors.toMap(StaticValidationEntry::id, entry -> entry, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<MissingTagFamilyEntry>> entriesByAdvancementId = familyReport.entries().stream()
            .collect(java.util.stream.Collectors.groupingBy(MissingTagFamilyEntry::advancementId, LinkedHashMap::new, java.util.stream.Collectors.toList()));
        List<MissingTagAdvancementClassification> entries = new ArrayList<>();
        for (Map.Entry<String, List<MissingTagFamilyEntry>> groupedEntry : entriesByAdvancementId.entrySet()) {
            StaticValidationEntry validationEntry = validationById.get(groupedEntry.getKey());
            MissingTagStaticFailReason staticFailReason = classifyMissingTagStaticFailReason(validationEntry, groupedEntry.getValue());
            MissingTagDisposition disposition = switch (staticFailReason) {
                case null -> MissingTagDisposition.TAG_CONTEXT_DEFERRED;
                case ACTUALLY_MISSING_RUNTIME_TAG -> MissingTagDisposition.ACTUALLY_MISSING_RUNTIME_TAG;
                default -> MissingTagDisposition.MIXED_OR_OTHER_STATIC_FAIL;
            };
            entries.add(new MissingTagAdvancementClassification(
                groupedEntry.getKey(),
                disposition,
                staticFailReason,
                groupedEntry.getValue().stream().map(MissingTagFamilyEntry::tagId).distinct().sorted().toList()
            ));
        }
        return new MissingTagDisjointReport(entries);
    }

    public static MissingTagNegativeControlReport analyzeNegativeMissingTagControl(Path projectRoot) throws IOException {
        bootstrapMinecraft();
        MissingTagDisjointReport disjointReport = analyzeMissingTagDisjoint(projectRoot);
        MissingTagAdvancementClassification preferredClassification = disjointReport.entries().stream()
            .filter(entry -> entry.disposition() == MissingTagDisposition.ACTUALLY_MISSING_RUNTIME_TAG)
            .sorted(Comparator.comparing(MissingTagAdvancementClassification::advancementId))
            .findFirst()
            .orElse(null);
        if (preferredClassification == null) {
            return new MissingTagNegativeControlReport(
                false,
                "",
                "",
                "",
                false,
                false,
                "",
                null,
                null,
                null,
                ""
            );
        }
        MissingTagFamilyReport familyReport = analyzeMissingTagFamily(projectRoot);
        MissingTagFamilyEntry tagEntry = familyReport.entries().stream()
            .filter(entry -> entry.advancementId().equals(preferredClassification.advancementId()))
            .sorted(Comparator.comparing(MissingTagFamilyEntry::tagId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing family entry for control candidate " + preferredClassification.advancementId()));
        Path bacapZip = projectRoot.resolve("reference").resolve("phase_a_preservation").resolve("files").resolve("final").resolve("bacap.zip");
        AdvancementPackData bacap = loadZipAdvancements(bacapZip, "bacap.zip");
        RawAdvancement rawAdvancement = bacap.advancements().get(tagEntry.advancementId());
        if (rawAdvancement == null) {
            throw new IllegalStateException("Missing raw advancement for negative control: " + tagEntry.advancementId());
        }
        JsonObject normalizedAdvancementJson;
        try {
            normalizedAdvancementJson = normalizeCompatibilityJson(rawAdvancement.json());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to normalize negative control advancement JSON for " + tagEntry.advancementId(), e);
        }
        String tagReference = "#" + tagEntry.tagId();
        List<String> rawReferences = collectStringReferences(rawAdvancement.json(), tagReference);
        List<String> normalizedReferences = collectStringReferences(normalizedAdvancementJson, tagReference);
        StaticValidationEntry validationEntry = analyzeStaticRules(projectRoot).entries().stream()
            .filter(entry -> entry.id().equals(tagEntry.advancementId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing validation entry for negative control: " + tagEntry.advancementId()));
        return new MissingTagNegativeControlReport(
            true,
            tagEntry.advancementId(),
            tagEntry.tagId(),
            tagEntry.registryId(),
            !rawReferences.isEmpty() && rawReferences.equals(normalizedReferences),
            tagEntry.runtimeTagExists(),
            tagEntry.runtimeTagSource(),
            validationEntry.status(),
            validationEntry.deferredReason(),
            validationEntry.registryComponent(),
            validationEntry.exactCodecError()
        );
    }

    public static RemainingStaticFailBreakdownReport analyzeRemainingStaticFailBreakdown(Path projectRoot) throws IOException {
        bootstrapMinecraft();
        StaticCertificationReport report = analyzeStaticRules(projectRoot);
        MissingTagDisjointReport missingTagReport = analyzeMissingTagDisjoint(projectRoot);
        Map<String, MissingTagAdvancementClassification> missingTagClassifications = missingTagReport.entries().stream()
            .collect(java.util.stream.Collectors.toMap(MissingTagAdvancementClassification::advancementId, entry -> entry, (left, right) -> left, LinkedHashMap::new));
        List<RemainingStaticFailBreakdownEntry> entries = new ArrayList<>();
        for (StaticValidationEntry entry : report.entries()) {
            if (entry.status() != StaticValidationStatus.STATIC_FAIL) {
                continue;
            }
            String codecError = entry.exactCodecError() == null ? "" : entry.exactCodecError();
            boolean hasRegistryMissing = codecError.contains("Registry does not exist:")
                || codecError.contains("Can't access registry ResourceKey[");
            boolean hasJsonArrayShape = codecError.contains("Not a json array");
            boolean hasMissingTag = codecError.contains("Missing tag:");
            int familyFlags = (hasRegistryMissing ? 1 : 0) + (hasJsonArrayShape ? 1 : 0) + (hasMissingTag ? 1 : 0);
            if (familyFlags > 1) {
                entries.add(new RemainingStaticFailBreakdownEntry(entry.id(), StaticFailFamily.MIXED_CODEC_ERROR, null, null));
                continue;
            }
            if (hasMissingTag) {
                MissingTagAdvancementClassification classification = missingTagClassifications.get(entry.id());
                if (classification != null && classification.staticFailReason() == MissingTagStaticFailReason.ACTUALLY_MISSING_RUNTIME_TAG) {
                    entries.add(new RemainingStaticFailBreakdownEntry(entry.id(), StaticFailFamily.ACTUALLY_MISSING_TAG, null, null));
                } else if (classification != null && classification.staticFailReason() == MissingTagStaticFailReason.MIXED_CODEC_ERROR) {
                    entries.add(new RemainingStaticFailBreakdownEntry(entry.id(), StaticFailFamily.MIXED_CODEC_ERROR, null, null));
                } else {
                    entries.add(new RemainingStaticFailBreakdownEntry(entry.id(), StaticFailFamily.OTHER, null, null));
                }
                continue;
            }
            if (hasRegistryMissing) {
                entries.add(new RemainingStaticFailBreakdownEntry(
                    entry.id(),
                    StaticFailFamily.REGISTRY_MISSING,
                    classifyRegistryMissingSubtype(codecError),
                    null
                ));
                continue;
            }
            if (hasJsonArrayShape) {
                entries.add(new RemainingStaticFailBreakdownEntry(
                    entry.id(),
                    StaticFailFamily.JSON_ARRAY_SHAPE,
                    null,
                    classifyJsonArraySubtype(codecError)
                ));
                continue;
            }
            entries.add(new RemainingStaticFailBreakdownEntry(entry.id(), StaticFailFamily.OTHER, null, null));
        }
        return new RemainingStaticFailBreakdownReport(entries);
    }

    public static ControlCaseAnalysis analyzeControlCase(Path projectRoot, String advancementId) throws IOException {
        bootstrapMinecraft();
        Path bacapZip = projectRoot.resolve("reference").resolve("phase_a_preservation").resolve("files").resolve("final").resolve("bacap.zip");
        AdvancementPackData bacap = loadZipAdvancements(bacapZip, "bacap.zip");
        RuntimeTagSourceIndex runtimeTagSourceIndex = loadBaseRuntimeTagSourceIndex(projectRoot);
        RawAdvancement rawAdvancement = bacap.advancements().get(advancementId);
        if (rawAdvancement == null) {
            throw new IllegalStateException("Missing raw advancement for control case: " + advancementId);
        }
        JsonObject normalizedAdvancementJson;
        try {
            normalizedAdvancementJson = normalizeCompatibilityJson(rawAdvancement.json());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to normalize control case advancement JSON for " + advancementId, e);
        }
        var registryOps = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        var fullRegistryOps = RegistryOps.create(JsonOps.INSTANCE, fullVanillaRegistryLookup);
        StaticValidationEntry validationEntry = validateCodec(
            advancementId,
            rawAdvancement.entryName(),
            rawAdvancement.json(),
            normalizedAdvancementJson,
            registryOps,
            fullRegistryOps,
            runtimeTagSourceIndex
        );
        RegistryDeferredClassification classification = validationEntry == null ? null : validationEntry.registryDeferredClassification();
        List<LocationHolderFieldMatch> matches = classification == null ? List.of() : classification.locationHolderMatches();
        List<MissingTagReferenceMatch> missingTagMatches = classification == null ? List.of() : classification.missingTagReferenceMatches();
        boolean subtreePreserved = matches.stream().allMatch(LocationHolderFieldMatch::subtreePreserved);
        boolean tagReferencePreserved = missingTagMatches.stream().allMatch(MissingTagReferenceMatch::subtreePreserved);
        return new ControlCaseAnalysis(
            advancementId,
            rawAdvancement.entryName(),
            matches,
            missingTagMatches,
            subtreePreserved,
            tagReferencePreserved,
            validationEntry
        );
    }

    private static RuntimeTagSourceIndex loadBaseRuntimeTagSourceIndex(Path projectRoot) throws IOException {
        Map<String, List<RuntimeTagSource>> sources = new LinkedHashMap<>();
        ScenarioSpec baseScenario = requireScenario("base");
        Path frozenPacks = projectRoot.resolve("reference").resolve("phase_a_preservation").resolve("files").resolve("final");
        for (String externalPack : baseScenario.externalPacks()) {
            indexArchiveTagSources(
                frozenPacks.resolve(externalPack),
                sourceCategoryForArchive(externalPack, true),
                externalPack,
                sources
            );
        }
        Path resourcePacks = projectRoot.resolve("src").resolve("main").resolve("resources").resolve("resourcepacks");
        for (String internalPack : baseScenario.internalPacks()) {
            indexDirectoryTagSources(
                resourcePacks.resolve(internalPack),
                RuntimeTagSourceCategory.BASE_INTERNAL_PACK,
                internalPack,
                sources
            );
        }
        indexVanillaTagSources(projectRoot, sources);
        return new RuntimeTagSourceIndex(sources);
    }

    private static RuntimeTagSourceIndex loadLegacyExpandedRuntimeTagSourceIndex(Path projectRoot) throws IOException {
        Map<String, List<RuntimeTagSource>> sources = new LinkedHashMap<>();
        Path frozenPacks = projectRoot.resolve("reference").resolve("phase_a_preservation").resolve("files").resolve("final");
        if (Files.exists(frozenPacks)) {
            try (var zipFiles = Files.list(frozenPacks).sorted()) {
                for (Path zipPath : zipFiles.filter(path -> path.getFileName().toString().endsWith(".zip")).toList()) {
                    String archiveName = zipPath.getFileName().toString();
                    indexArchiveTagSources(zipPath, sourceCategoryForArchive(archiveName, false), archiveName, sources);
                }
            }
        }
        indexVanillaTagSources(projectRoot, sources);
        return new RuntimeTagSourceIndex(sources);
    }

    private static RuntimeTagSourceIndex loadExpandedAuditRuntimeTagSourceIndex(Path projectRoot) throws IOException {
        Map<String, List<RuntimeTagSource>> sources = new LinkedHashMap<>();
        Path frozenPacks = projectRoot.resolve("reference").resolve("phase_a_preservation").resolve("files").resolve("final");
        if (Files.exists(frozenPacks)) {
            try (var zipFiles = Files.list(frozenPacks).sorted()) {
                for (Path zipPath : zipFiles.filter(path -> path.getFileName().toString().endsWith(".zip")).toList()) {
                    String archiveName = zipPath.getFileName().toString();
                    indexArchiveTagSources(zipPath, sourceCategoryForArchive(archiveName, false), archiveName, sources);
                }
            }
        }
        Path resourcePacks = projectRoot.resolve("src").resolve("main").resolve("resources").resolve("resourcepacks");
        ScenarioSpec baseScenario = requireScenario("base");
        for (String internalPack : baseScenario.internalPacks()) {
            indexDirectoryTagSources(
                resourcePacks.resolve(internalPack),
                RuntimeTagSourceCategory.BASE_INTERNAL_PACK,
                internalPack,
                sources
            );
        }
        indexVanillaTagSources(projectRoot, sources);
        return new RuntimeTagSourceIndex(sources);
    }

    private static boolean usesConfiguredVanillaTagSource(Path projectRoot) {
        String configured = System.getProperty("achievetodo.minecraftJar");
        return configured != null && !configured.isBlank()
            && projectRoot.toAbsolutePath().normalize().equals(Path.of("").toAbsolutePath().normalize());
    }

    private static List<Path> vanillaTagSourceCandidates(Path projectRoot) {
        if (usesConfiguredVanillaTagSource(projectRoot)) {
            Path vanillaJar = Path.of(System.getProperty("achievetodo.minecraftJar")).toAbsolutePath().normalize();
            if (!Files.isRegularFile(vanillaJar)) {
                throw new IllegalStateException("Configured verified Minecraft tag source is missing: " + vanillaJar);
            }
            return List.of(vanillaJar);
        }
        return List.of(
            projectRoot.resolve(".gradle-user-home").resolve("caches").resolve("fabric-loom").resolve("minecraftMaven")
                .resolve("net").resolve("minecraft").resolve("minecraft-merged-deobf").resolve("26.2").resolve("minecraft-merged-deobf-26.2.jar"),
            projectRoot.resolve(".gradle").resolve("loom-cache").resolve("minecraftMaven")
                .resolve("net").resolve("minecraft").resolve("minecraft-merged-84afe0508c").resolve("26.2").resolve("minecraft-merged-84afe0508c-26.2.jar"),
            projectRoot.resolve("tmp-minecraft-merged-deobf-26.2.jar")
        );
    }

    private static void indexVanillaTagSources(Path projectRoot, Map<String, List<RuntimeTagSource>> sources) throws IOException {
        for (Path vanillaJar : vanillaTagSourceCandidates(projectRoot)) {
            if (Files.exists(vanillaJar)) {
                // Certified snapshots use this stable logical container label. The actual
                // Gradle-provided file is independently checked by strict dependency verification.
                String sourceLabel = usesConfiguredVanillaTagSource(projectRoot)
                    ? "minecraft-merged-deobf-26.2.jar" : vanillaJar.getFileName().toString();
                indexArchiveTagSources(vanillaJar, RuntimeTagSourceCategory.VANILLA_26_2, sourceLabel, sources);
                return;
            }
        }
    }

    private static void indexArchiveTagSources(
        Path archivePath,
        RuntimeTagSourceCategory category,
        String sourceLabel,
        Map<String, List<RuntimeTagSource>> sources
    ) throws IOException {
        if (!Files.exists(archivePath)) {
            return;
        }
        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            for (ZipEntry entry : zipFile.stream()
                .filter(zipEntry -> !zipEntry.isDirectory())
                .filter(zipEntry -> zipEntry.getName().matches("^data/[^/]+/tags/[^/]+/.+\\.json$"))
                .toList()) {
                TagPathMatch tagPath = parseTagPath(entry.getName());
                if (tagPath == null) {
                    continue;
                }
                addRuntimeTagSource(sources, tagPath.registryId(), tagPath.tagId(), new RuntimeTagSource(category, sourceLabel, entry.getName()));
            }
        }
    }

    private static void indexDirectoryTagSources(
        Path directoryPath,
        RuntimeTagSourceCategory category,
        String sourceLabel,
        Map<String, List<RuntimeTagSource>> sources
    ) throws IOException {
        if (!Files.exists(directoryPath)) {
            return;
        }
        try (var paths = Files.walk(directoryPath)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String relativePath = directoryPath.relativize(path).toString().replace('\\', '/');
                TagPathMatch tagPath = parseTagPath(relativePath);
                if (tagPath == null) {
                    continue;
                }
                addRuntimeTagSource(sources, tagPath.registryId(), tagPath.tagId(), new RuntimeTagSource(category, sourceLabel, relativePath));
            }
        }
    }

    private static void addRuntimeTagSource(
        Map<String, List<RuntimeTagSource>> sources,
        String registryId,
        String tagId,
        RuntimeTagSource runtimeTagSource
    ) {
        sources.computeIfAbsent(registryId + "|" + tagId, ignored -> new ArrayList<>()).add(runtimeTagSource);
    }

    private static TagPathMatch parseTagPath(String path) {
        Matcher matcher = Pattern.compile("^data/([^/]+)/tags/([^/]+)/(.+)\\.json$").matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        String registryId = switch (matcher.group(2)) {
            case "block" -> "minecraft:block";
            case "item" -> "minecraft:item";
            case "entity_type" -> "minecraft:entity_type";
            default -> null;
        };
        if (registryId == null) {
            return null;
        }
        return new TagPathMatch(registryId, matcher.group(1) + ":" + matcher.group(3));
    }

    private static RuntimeTagSourceCategory sourceCategoryForArchive(String archiveName, boolean baseScenarioOnly) {
        if (archiveName.equals("bacap.zip")) {
            return RuntimeTagSourceCategory.BACAP_BASE;
        }
        if (archiveName.equals("terralith.zip")
            || archiveName.equals("amplified_nether.zip")
            || archiveName.equals("nullscape.zip")
            || archiveName.equals("bacap_hardcore.zip")
            || archiveName.equals("bacap_terralith.zip")
            || archiveName.equals("bacap_amplified_nether.zip")
            || archiveName.equals("bacap_nullscape.zip")) {
            return RuntimeTagSourceCategory.OPTIONAL_VARIANT;
        }
        if (baseScenarioOnly) {
            throw new IllegalStateException("Unexpected non-base archive in base stack: " + archiveName);
        }
        return RuntimeTagSourceCategory.OPTIONAL_VARIANT;
    }

    private static ScenarioSpec requireScenario(String id) {
        return SCENARIOS.stream()
            .filter(scenario -> scenario.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing scenario definition: " + id));
    }

    private static Set<String> extractDeferredAdvancementIds(StaticCertificationReport report, DeferredReason deferredReason) {
        return report.entries().stream()
            .filter(entry -> entry.status() == StaticValidationStatus.RUNTIME_DEFERRED)
            .filter(entry -> entry.deferredReason() == deferredReason)
            .map(StaticValidationEntry::id)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> extractTagContextDeferredAdvancementIds(StaticCertificationReport report) {
        return report.entries().stream()
            .filter(entry -> entry.status() == StaticValidationStatus.RUNTIME_DEFERRED)
            .filter(entry -> entry.registryComponents().stream().anyMatch(PhaseACertification::isTagRegistryComponent))
            .map(StaticValidationEntry::id)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static SourceProvenance classifyAdvancementSourceProvenance(
        List<MissingTagFamilyEntry> advancementEntries,
        RuntimeTagSourceIndex runtimeTagSourceIndex
    ) {
        Set<RuntimeTagSourceCategory> categories = new LinkedHashSet<>();
        for (MissingTagFamilyEntry entry : advancementEntries) {
            for (RuntimeTagSource source : runtimeTagSourceIndex.sourcesFor(entry.registryId(), entry.tagId())) {
                categories.add(source.category());
            }
        }
        if (categories.size() > 1) {
            return SourceProvenance.MULTIPLE;
        }
        if (categories.isEmpty()) {
            throw new IllegalStateException("Missing runtime tag provenance for advancement " + advancementEntries.stream()
                .map(MissingTagFamilyEntry::advancementId)
                .findFirst()
                .orElse("unknown"));
        }
        RuntimeTagSourceCategory category = categories.iterator().next();
        return switch (category) {
            case VANILLA_26_2 -> SourceProvenance.VANILLA_26_2;
            case BACAP_BASE -> SourceProvenance.BACAP_BASE;
            case BASE_INTERNAL_PACK -> SourceProvenance.BASE_INTERNAL_PACK;
            case OPTIONAL_VARIANT -> SourceProvenance.OPTIONAL_VARIANT_ONLY;
        };
    }

    private static MissingTagStaticFailReason classifyMissingTagStaticFailReason(
        StaticValidationEntry validationEntry,
        List<MissingTagFamilyEntry> familyEntries
    ) {
        if (validationEntry == null) {
            return MissingTagStaticFailReason.OTHER_UNCLASSIFIED;
        }
        if (validationEntry.status() == StaticValidationStatus.RUNTIME_DEFERRED
            && validationEntry.registryComponents().stream().anyMatch(PhaseACertification::isTagRegistryComponent)
            && familyEntries.stream().allMatch(entry ->
                entry.registryComponent() != null
                    && entry.normalizedTagReferencePreserved()
                    && entry.runtimeTagExists()
                    && !entry.junitTagResolvable())) {
            return null;
        }
        if (validationEntry.status() != StaticValidationStatus.STATIC_FAIL) {
            return MissingTagStaticFailReason.OTHER_UNCLASSIFIED;
        }
        String codecError = validationEntry.exactCodecError() == null ? "" : validationEntry.exactCodecError();
        if (!containsOnlyMissingTagDiagnostics(List.of(codecError))) {
            return MissingTagStaticFailReason.MIXED_CODEC_ERROR;
        }
        if (familyEntries.stream().anyMatch(entry -> entry.registryComponent() == null)) {
            return MissingTagStaticFailReason.UNSUPPORTED_TAG_REGISTRY;
        }
        if (familyEntries.stream().anyMatch(entry -> !entry.normalizedTagReferencePreserved())) {
            return MissingTagStaticFailReason.TAG_REFERENCE_NOT_PRESERVED;
        }
        if (familyEntries.stream().anyMatch(MissingTagFamilyEntry::junitTagResolvable)) {
            return MissingTagStaticFailReason.JUNIT_ALREADY_RESOLVES;
        }
        if (familyEntries.stream().allMatch(entry -> !entry.runtimeTagExists())) {
            return MissingTagStaticFailReason.ACTUALLY_MISSING_RUNTIME_TAG;
        }
        return MissingTagStaticFailReason.OTHER_UNCLASSIFIED;
    }

    private static boolean isTagRegistryComponent(RegistryDependentComponent component) {
        return component == RegistryDependentComponent.BLOCK_TAG
            || component == RegistryDependentComponent.ITEM_TAG
            || component == RegistryDependentComponent.ENTITY_TYPE_TAG;
    }

    private static RegistryMissingSubtype classifyRegistryMissingSubtype(String codecError) {
        if (codecError.contains("minecraft:banner_pattern")) {
            return RegistryMissingSubtype.BANNER_PATTERN;
        }
        if (codecError.contains("minecraft:instrument")) {
            return RegistryMissingSubtype.INSTRUMENT;
        }
        return RegistryMissingSubtype.OTHER;
    }

    private static JsonArraySubtype classifyJsonArraySubtype(String codecError) {
        String normalized = codecError.toLowerCase(Locale.ROOT);
        if (normalized.contains("items")
            || normalized.contains("item")
            || normalized.contains("entity")
            || normalized.contains("entity_type")
            || normalized.contains("equipment")) {
            return JsonArraySubtype.ITEM_ENTITY_PREDICATE_SHAPE;
        }
        return JsonArraySubtype.OTHER_JSON_ARRAY;
    }

    private static Set<RegistryDependentComponent> singletonRegistryComponents(RegistryDependentComponent component) {
        return immutableRegistryComponents(Set.of(component));
    }

    private static Set<RegistryDependentComponent> immutableRegistryComponents(Set<RegistryDependentComponent> components) {
        if (components == null || components.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(components));
    }

    private static RegistryDependentComponent singleRegistryComponent(Set<RegistryDependentComponent> components) {
        if (components == null || components.size() != 1) {
            return null;
        }
        return components.iterator().next();
    }

    private static boolean containsTrimComponent(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return false;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("minecraft:trim")) {
                return true;
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (containsTrimComponent(entry.getValue())) {
                    return true;
                }
            }
            return false;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (containsTrimComponent(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    public record CertificationArtifacts(
        String inventoryJson,
        String matrixJson,
        String reconciliationJson,
        String reconciliationMarkdown,
        List<InventoryEntry> inventory
    ) {
    }

    public record InventoryEntry(
        String id,
        String namespace,
        String category,
        String sourcePack,
        String sourcePath,
        String parentId,
        String titleKey,
        boolean titleKeyPresentInFrozenEnglish,
        boolean titleKeyPresentInRuntimeRussian,
        String descriptionKey,
        boolean descriptionKeyPresentInFrozenEnglish,
        boolean descriptionKeyPresentInRuntimeRussian,
        int criteriaCount,
        List<String> rewardFunctionIds,
        Map<String, String> rewardFunctionSources
    ) {
    }

    public enum StaticValidationStatus {
        STATIC_PASS,
        STATIC_FAIL,
        RUNTIME_DEFERRED
    }

    public enum DeferredReason {
        REGISTRY_CONTEXT_REQUIRED,
        TAG_CONTEXT_REQUIRED,
        MULTIPLE_CONTEXT_REQUIRED
    }

    public enum RegistryDependentComponent {
        TRIM_PATTERN,
        TRIM_MATERIAL,
        TRIM_PATTERN_AND_MATERIAL,
        ENCHANTMENT_HOLDERSET,
        BIOME_HOLDERSET,
        STRUCTURE_HOLDERSET,
        BIOME_AND_STRUCTURE_HOLDERSET,
        BLOCK_TAG,
        ITEM_TAG,
        ENTITY_TYPE_TAG
    }

    public enum RuntimeTagSourceCategory {
        VANILLA_26_2,
        BACAP_BASE,
        BASE_INTERNAL_PACK,
        OPTIONAL_VARIANT
    }

    public enum SourceProvenance {
        VANILLA_26_2,
        BACAP_BASE,
        BASE_INTERNAL_PACK,
        OPTIONAL_VARIANT_ONLY,
        MULTIPLE
    }

    public enum MissingTagDisposition {
        TAG_CONTEXT_DEFERRED,
        ACTUALLY_MISSING_RUNTIME_TAG,
        MIXED_OR_OTHER_STATIC_FAIL
    }

    public enum MissingTagStaticFailReason {
        ACTUALLY_MISSING_RUNTIME_TAG,
        MIXED_CODEC_ERROR,
        TAG_REFERENCE_NOT_PRESERVED,
        JUNIT_ALREADY_RESOLVES,
        UNSUPPORTED_TAG_REGISTRY,
        OTHER_UNCLASSIFIED
    }

    public enum StaticFailFamily {
        REGISTRY_MISSING,
        JSON_ARRAY_SHAPE,
        ACTUALLY_MISSING_TAG,
        MIXED_CODEC_ERROR,
        OTHER
    }

    public enum RegistryMissingSubtype {
        BANNER_PATTERN,
        INSTRUMENT,
        OTHER
    }

    public enum JsonArraySubtype {
        ITEM_ENTITY_PREDICATE_SHAPE,
        OTHER_JSON_ARRAY
    }

    private enum LocationHolderField {
        BIOMES("biomes"),
        STRUCTURES("structures");

        private final String jsonName;

        LocationHolderField(String jsonName) {
            this.jsonName = jsonName;
        }

        public String jsonName() {
            return jsonName;
        }
    }

    public record StaticValidationEntry(
        String id,
        StaticValidationStatus status,
        DeferredReason deferredReason,
        Set<RegistryDependentComponent> registryComponents,
        String exactCodecError,
        RegistryDeferredClassification registryDeferredClassification
    ) {
        public StaticValidationEntry {
            registryComponents = immutableRegistryComponents(registryComponents);
        }

        public RegistryDependentComponent registryComponent() {
            return singleRegistryComponent(registryComponents);
        }
    }

    public record LocationHolderFieldMatch(
        LocationHolderField field,
        List<JsonElement> rawValues,
        List<JsonElement> normalizedValues,
        boolean subtreePreserved
    ) {
    }

    public record RegistryDeferredClassification(
        String advancementId,
        DeferredReason deferredReason,
        Set<RegistryDependentComponent> registryComponents,
        List<LocationHolderFieldMatch> locationHolderMatches,
        List<MissingTagReferenceMatch> missingTagReferenceMatches,
        List<EnchantmentHolderSetMatch> enchantmentHolderSetMatches,
        boolean biomeRegistryMissing,
        boolean structureRegistryMissing
    ) {
        public RegistryDeferredClassification {
            registryComponents = immutableRegistryComponents(registryComponents);
            locationHolderMatches = List.copyOf(locationHolderMatches);
            missingTagReferenceMatches = List.copyOf(missingTagReferenceMatches);
            enchantmentHolderSetMatches = List.copyOf(enchantmentHolderSetMatches);
        }

        public RegistryDependentComponent registryComponent() {
            return singleRegistryComponent(registryComponents);
        }
    }

    public record HolderSetFamilyReport(
        Set<String> biomeHolderSetIds,
        Set<String> structureHolderSetIds,
        Set<String> biomeOnlyIds,
        Set<String> structureOnlyIds,
        Set<String> bothIds,
        Set<String> unionIds,
        Set<String> otherLocationHolderIds
    ) {
        public int familyTotal() {
            return unionIds.size() + otherLocationHolderIds.size();
        }
    }

    public record ControlCaseAnalysis(
        String advancementId,
        String advancementSource,
        List<LocationHolderFieldMatch> locationHolderMatches,
        List<MissingTagReferenceMatch> missingTagMatches,
        boolean subtreePreserved,
        boolean tagReferencePreserved,
        StaticValidationEntry validationEntry
    ) {
    }

    public record MissingTagDiagnostic(
        String tagId,
        String registryId
    ) {
    }

    public record MissingTagReferenceMatch(
        String tagId,
        String registryId,
        String tagReference,
        List<String> rawReferences,
        List<String> normalizedReferences,
        boolean subtreePreserved,
        boolean runtimeTagExists,
        String runtimeTagSource,
        boolean junitTagResolvable
    ) {
        public RegistryDependentComponent registryComponent() {
            return mapTagRegistryComponent(registryId);
        }
    }

    public record EnchantmentHolderSetMatch(
        String predicateListKey,
        String path,
        String enchantmentId,
        String normalizedEnchantmentId,
        boolean subtreePreserved
    ) {
    }

    public record MissingTagFamilyEntry(
        String advancementId,
        String tagId,
        String registryId,
        boolean normalizedTagReferencePreserved,
        boolean runtimeTagExists,
        boolean junitTagResolvable,
        StaticValidationStatus status,
        DeferredReason deferredReason,
        RegistryDependentComponent registryComponent,
        String runtimeTagSource
    ) {
    }

    public record MissingTagFamilyReport(
        List<MissingTagFamilyEntry> entries
    ) {
        public long totalAdvancements() {
            return entries.stream().map(MissingTagFamilyEntry::advancementId).distinct().count();
        }
    }

    public record TagContextSourceStackEntry(
        String advancementId,
        SourceProvenance provenance,
        List<String> sources
    ) {
    }

    public record TagContextSourceStackReport(
        List<TagContextSourceStackEntry> entries,
        Set<String> legacyDeferredIds,
        Set<String> baseDeferredIds,
        Set<String> optionalPackFalsePositiveIds,
        Set<String> baseInternalFalseNegativeIds
    ) {
        public long deferredTagsTotal() {
            return legacyDeferredIds.size();
        }

        public long countByProvenance(SourceProvenance provenance) {
            return entries.stream().filter(entry -> entry.provenance() == provenance).count();
        }

        public long validDeferredCount() {
            return baseDeferredIds.size();
        }
    }

    public record MissingTagAdvancementClassification(
        String advancementId,
        MissingTagDisposition disposition,
        MissingTagStaticFailReason staticFailReason,
        List<String> tagIds
    ) {
    }

    public record MissingTagDisjointReport(
        List<MissingTagAdvancementClassification> entries
    ) {
        public long totalAdvancements() {
            return entries.size();
        }

        public long countByDisposition(MissingTagDisposition disposition) {
            return entries.stream().filter(entry -> entry.disposition() == disposition).count();
        }
    }

    public record MissingTagNegativeControlReport(
        boolean available,
        String advancementId,
        String tagId,
        String registryId,
        boolean tagReferencePreserved,
        boolean baseRuntimeExists,
        String baseRuntimeSource,
        StaticValidationStatus finalStatus,
        DeferredReason deferredReason,
        RegistryDependentComponent registryComponent,
        String exactCodecError
    ) {
    }

    public record RemainingStaticFailBreakdownEntry(
        String advancementId,
        StaticFailFamily family,
        RegistryMissingSubtype registryMissingSubtype,
        JsonArraySubtype jsonArraySubtype
    ) {
    }

    public record RemainingStaticFailBreakdownReport(
        List<RemainingStaticFailBreakdownEntry> entries
    ) {
        public long countByFamily(StaticFailFamily family) {
            return entries.stream().filter(entry -> entry.family() == family).count();
        }

        public long countByRegistrySubtype(RegistryMissingSubtype subtype) {
            return entries.stream().filter(entry -> entry.registryMissingSubtype() == subtype).count();
        }

        public long countByJsonArraySubtype(JsonArraySubtype subtype) {
            return entries.stream().filter(entry -> entry.jsonArraySubtype() == subtype).count();
        }
    }

    public record StaticCertificationReport(
        List<StaticValidationEntry> entries
    ) {
        public long staticPassCount() {
            return entries.stream().filter(entry -> entry.status() == StaticValidationStatus.STATIC_PASS).count();
        }

        public long staticFailCount() {
            return entries.stream().filter(entry -> entry.status() == StaticValidationStatus.STATIC_FAIL).count();
        }

        public long runtimeDeferredCount() {
            return entries.stream().filter(entry -> entry.status() == StaticValidationStatus.RUNTIME_DEFERRED).count();
        }
    }

    private record RawAdvancement(
        String id,
        String namespace,
        String category,
        String packName,
        String entryName,
        JsonObject json,
        boolean hasDisplay,
        boolean hidden,
        String parentId,
        int criteriaCount,
        List<String> rewardFunctionIds
    ) {
    }

    private record AdvancementPackData(
        String packName,
        Map<String, RawAdvancement> advancements,
        Map<String, Integer> rawCounts,
        int visibleCount,
        int hiddenDisplayCount,
        int noDisplayCount
    ) {
    }

    private record RuntimeTagSource(
        RuntimeTagSourceCategory category,
        String sourceLabel,
        String entryPath
    ) {
        public String displaySource() {
            return sourceLabel + "::" + entryPath;
        }
    }

    private record RuntimeTagSourceIndex(
        Map<String, List<RuntimeTagSource>> sources
    ) {
        public boolean contains(String registryId, String tagId) {
            return sources.containsKey(registryId + "|" + tagId);
        }

        public java.util.Optional<String> sourceFor(String registryId, String tagId) {
            return sourcesFor(registryId, tagId).stream().findFirst().map(RuntimeTagSource::displaySource);
        }

        public List<RuntimeTagSource> sourcesFor(String registryId, String tagId) {
            return sources.getOrDefault(registryId + "|" + tagId, List.of());
        }
    }

    private record TagPathMatch(
        String registryId,
        String tagId
    ) {
    }

    private record ScenarioSpec(
        String id,
        List<String> externalPacks,
        List<String> internalPacks,
        String variantPack,
        String environmentPack
    ) {
    }
}
