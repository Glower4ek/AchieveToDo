package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CertifiedContainerLootCatalog {
    private static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "container_loot_case_catalog.json");
    private static final String SNAPSHOT = "phase_a_container_loot_case_catalog";
    private static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static volatile List<CaseDefinition> cachedCases;

    private CertifiedContainerLootCatalog() {
    }

    public static List<CaseDefinition> allCases() {
        List<CaseDefinition> local = cachedCases;
        if (local != null) {
            return local;
        }
        synchronized (CertifiedContainerLootCatalog.class) {
            if (cachedCases == null) {
                cachedCases = List.copyOf(loadCases());
            }
            return cachedCases;
        }
    }

    private static List<CaseDefinition> loadCases() {
        try {
            Path catalogPath = PhaseAContainerLootExecutionEvidence.projectRoot().resolve(CATALOG_PATH);
            JsonObject root = JsonParser.parseString(Files.readString(catalogPath, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!SNAPSHOT.equals(root.get("snapshot").getAsString())) {
                throw new IllegalStateException("Unexpected container loot catalog snapshot: " + root.get("snapshot").getAsString());
            }
            JsonArray casesJson = root.getAsJsonArray("cases");
            List<CaseDefinition> cases = new ArrayList<>();
            if (casesJson == null) {
                return cases;
            }
            for (JsonElement caseElement : casesJson) {
                JsonObject caseJson = caseElement.getAsJsonObject();
                if (!AUTOMATION_SUPPORTED.equals(caseJson.get("automationEligibility").getAsString())) {
                    continue;
                }
                String lootTableId = caseJson.get("lootTable").getAsString();
                cases.add(new CaseDefinition(
                    Identifier.parse(caseJson.get("advancementId").getAsString()),
                    caseJson.get("criterion").getAsString(),
                    ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(lootTableId)),
                    lootTableId
                ));
            }
            cases.sort(Comparator
                .comparing((CaseDefinition value) -> value.advancementId().toString())
                .thenComparing(CaseDefinition::criterion)
                .thenComparing(CaseDefinition::lootTableId));
            return cases;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load container loot catalog", e);
        }
    }

    public record CaseDefinition(
        Identifier advancementId,
        String criterion,
        ResourceKey<LootTable> lootTable,
        String lootTableId
    ) {
    }
}
