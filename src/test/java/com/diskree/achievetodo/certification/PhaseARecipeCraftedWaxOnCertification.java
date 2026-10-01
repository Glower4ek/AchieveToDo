package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Frozen-definition catalog for the one-group wax_on recipe-crafted frontier. */
public final class PhaseARecipeCraftedWaxOnCertification {
    public static final Path SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "recipe_crafted_wax_on_case_catalog.json");
    public static final String ADVANCEMENT_ID = "minecraft:husbandry/wax_on";
    public static final String SOURCE_PATH = "data/minecraft/advancement/husbandry/wax_on.json";
    public static final String SELECTED_CRITERION = "minecraft:waxed_copper_block_from_honeycomb";
    public static final String SELECTED_RECIPE = "minecraft:waxed_copper_block_from_honeycomb";
    private static final String BACAP = "reference/phase_a_preservation/files/final/bacap.zip";
    private static final String BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseARecipeCraftedWaxOnCertification() { }

    public static String generateSnapshot(Path root) throws Exception {
        Path archive = root.resolve(BACAP);
        require(BACAP_SHA256.equals(hash(Files.readAllBytes(archive))), "Frozen BACAP hash changed");
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entry = zip.getEntry(SOURCE_PATH);
            require(entry != null, "Missing wax_on frozen definition");
            JsonObject advancement;
            byte[] source;
            try (InputStream input = zip.getInputStream(entry)) { source = input.readAllBytes(); advancement = JsonParser.parseString(new String(source, StandardCharsets.UTF_8)).getAsJsonObject(); }
            JsonObject criteria = advancement.getAsJsonObject("criteria");
            JsonArray requirements = advancement.getAsJsonArray("requirements");
            require(criteria != null && requirements != null && requirements.size() == 1, "wax_on requirements changed");
            Set<String> recipeCriteria = new LinkedHashSet<>();
            for (var criterion : criteria.entrySet()) {
                JsonObject definition = criterion.getValue().getAsJsonObject();
                if ("minecraft:recipe_crafted".equals(definition.get("trigger").getAsString())) recipeCriteria.add(criterion.getKey());
            }
            require(recipeCriteria.size() == 36, "Expected 36 recipe-crafted wax criteria");
            require(recipeCriteria.contains(SELECTED_CRITERION), "Selected wax recipe criterion changed");
            require(SELECTED_RECIPE.equals(criteria.getAsJsonObject(SELECTED_CRITERION).getAsJsonObject("conditions").get("recipe_id").getAsString()), "Selected recipe changed");
            require(requirements.get(0).getAsJsonArray().size() == 37, "Expected one 37-way OR requirement group");
            JsonObject snapshot = new JsonObject();
            snapshot.addProperty("snapshot", "phase_a_recipe_crafted_wax_on_case_catalog");
            snapshot.addProperty("family", "RECIPE_CRAFTED_WAX_ON");
            snapshot.addProperty("advancementId", ADVANCEMENT_ID);
            snapshot.addProperty("sourcePath", SOURCE_PATH);
            snapshot.addProperty("sourceJsonSha256", hash(source));
            snapshot.addProperty("frozenBacapSha256", BACAP_SHA256);
            snapshot.addProperty("criteriaCount", criteria.size());
            snapshot.addProperty("recipeCraftedCriteriaCount", recipeCriteria.size());
            snapshot.addProperty("requirementGroupCount", requirements.size());
            snapshot.addProperty("requirementGroupAlternativeCount", requirements.get(0).getAsJsonArray().size());
            snapshot.addProperty("selectedCriterion", SELECTED_CRITERION);
            snapshot.addProperty("selectedRecipeId", SELECTED_RECIPE);
            snapshot.addProperty("runtimeBoundary", "ServerGamePacketListenerImpl.handleContainerClick->ResultSlot.onTake->CriteriaTriggers.RECIPE_CRAFTED");
            snapshot.addProperty("completionSemantics", "one requirement group; one legitimate selected recipe criterion is sufficient");
            return GSON.toJson(snapshot) + "\n";
        }
    }

    private static String hash(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
