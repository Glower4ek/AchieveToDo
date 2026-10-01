package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

/** Frozen-source contract for the two trim-material inventory advancements. */
public final class PhaseATrimMaterialInventoryChangedCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/trim_material_inventory_changed_case_catalog.json");
    private static final Path BACAP = Path.of("reference/phase_a_preservation/files/final/bacap.zip");
    private static final String BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final String[] MATERIALS = {"amethyst", "copper", "diamond", "emerald", "gold", "iron", "lapis", "quartz", "netherite", "redstone", "resin"};
    private static final String[] ADVANCEMENTS = {"chromatic_armory", "coordinated_flair"};
    private static final String[] SOURCE_SHA256 = {"7376fd0207d820eec342c762d20fe4639c40c61390b665d016d9c74c604f1e8e", "47b3aaf25cf068d6d7e60aa961a88a13e29b43425cee0ea644a5825e1d309321"};
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseATrimMaterialInventoryChangedCertification() { }

    public record Case(String advancementId, String criterion, String materialId, int requirementGroup, String witness, String productionGate, String sourcePath, String sourceSha256) {
        public String key() { return advancementId + "#" + criterion; }
    }

    public static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        for (int a = 0; a < ADVANCEMENTS.length; a++) {
            String source = "data/blazeandcave/advancement/adventure/" + ADVANCEMENTS[a] + ".json";
            for (int m = 0; m < MATERIALS.length; m++) {
                cases.add(new Case("blazeandcave:adventure/" + ADVANCEMENTS[a], MATERIALS[m], "minecraft:" + MATERIALS[m], a == 0 ? m : 0,
                    a == 0 ? "native_item_entity_pickup" : "native_inventory_armor_click",
                    a == 0 ? "NONE" : "EQUIP_IRON_ARMOR", source, SOURCE_SHA256[a]));
            }
        }
        return List.copyOf(cases);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected project root");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve(SNAPSHOT).getParent());
        Files.writeString(root.resolve(SNAPSHOT), generate(root), StandardCharsets.UTF_8);
    }

    public static void validate(Path root) throws Exception {
        String expected = generate(root);
        String actual = Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8);
        require(expected.equals(actual), "Trim-material catalog differs from frozen-source derivation");
    }

    public static String generate(Path root) throws Exception {
        require(BACAP_SHA256.equals(sha(Files.readAllBytes(root.resolve(BACAP)))), "Frozen BACAP SHA-256 changed");
        try (ZipFile zip = new ZipFile(root.resolve(BACAP).toFile(), StandardCharsets.UTF_8)) {
            for (int a = 0; a < ADVANCEMENTS.length; a++) {
                String path = "data/blazeandcave/advancement/adventure/" + ADVANCEMENTS[a] + ".json";
                var entry = zip.getEntry(path);
                require(entry != null, "Missing frozen advancement " + path);
                byte[] bytes;
                try (InputStream in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                require(SOURCE_SHA256[a].equals(sha(bytes)), "Frozen advancement SHA changed " + path);
                JsonObject advancement = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject criteria = advancement.getAsJsonObject("criteria");
                require(criteria != null && criteria.size() == MATERIALS.length, "Expected exact 11 criteria in " + path);
                Set<String> expectedNames = new LinkedHashSet<>(List.of(MATERIALS));
                require(criteria.keySet().equals(expectedNames), "Unexpected criteria in " + path);
                for (String material : MATERIALS) {
                    JsonObject criterion = criteria.getAsJsonObject(material);
                    require(criterion != null && "minecraft:inventory_changed".equals(criterion.get("trigger").getAsString()), "Wrong trigger " + path + "#" + material);
                    JsonObject conditions = criterion.getAsJsonObject("conditions");
                    require(conditions != null && conditions.size() == 1, "Wrong conditions " + path + "#" + material);
                    if (a == 0) {
                        require(conditions.has("items"), "Expected item predicate");
                        JsonArray items = conditions.getAsJsonArray("items");
                        require(items.size() == 1, "Expected one inventory item predicate");
                        requireTrim(items.get(0).getAsJsonObject(), material, path);
                    } else {
                        require(conditions.has("player"), "Expected player equipment predicate");
                        JsonArray player = conditions.getAsJsonArray("player");
                        require(player.size() == 1, "Expected one player predicate");
                        JsonObject entity = player.get(0).getAsJsonObject();
                        require("minecraft:entity_properties".equals(entity.get("condition").getAsString()) && "this".equals(entity.get("entity").getAsString()), "Wrong player loot context");
                        JsonObject equipment = entity.getAsJsonObject("predicate").getAsJsonObject("equipment");
                        require(equipment != null && equipment.size() == 4, "Expected four equipment slots");
                        for (String slot : List.of("head", "chest", "legs", "feet")) requireTrim(equipment.getAsJsonObject(slot), material, path + "/" + slot);
                    }
                }
                if (a == 0) {
                    require(!advancement.has("requirements"), "Chromatic armory must use implicit singleton AND groups");
                } else {
                    JsonArray groups = advancement.getAsJsonArray("requirements");
                    require(groups != null && groups.size() == 1, "Coordinated flair must have one OR group");
                    Set<String> alternatives = new LinkedHashSet<>();
                    for (JsonElement element : groups.get(0).getAsJsonArray()) require(alternatives.add(element.getAsString()), "Duplicate OR alternative");
                    require(alternatives.equals(expectedNames), "Unexpected OR alternatives");
                }
            }
        }
        JsonObject catalog = new JsonObject();
        catalog.addProperty("snapshot", "phase_a_trim_material_inventory_changed_case_catalog");
        catalog.addProperty("family", "TRIM_MATERIAL_INVENTORY_CHANGED");
        catalog.addProperty("minecraftVersion", "26.2");
        catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        catalog.addProperty("frozenBacapSha256", BACAP_SHA256);
        catalog.addProperty("advancementCount", 2);
        catalog.addProperty("criterionCount", 22);
        catalog.addProperty("requirementGroupCount", 12);
        JsonArray cases = new JsonArray();
        for (Case c : cases()) {
            JsonObject item = new JsonObject();
            item.addProperty("advancementId", c.advancementId()); item.addProperty("criterion", c.criterion());
            item.addProperty("trigger", "minecraft:inventory_changed"); item.addProperty("materialId", c.materialId());
            item.addProperty("requirementGroup", c.requirementGroup()); item.addProperty("witness", c.witness());
            item.addProperty("productionGate", c.productionGate());
            item.addProperty("sourcePath", c.sourcePath()); item.addProperty("sourceSha256", c.sourceSha256());
            cases.add(item);
        }
        catalog.add("cases", cases);
        return GSON.toJson(catalog) + "\n";
    }

    private static void requireTrim(JsonObject item, String material, String context) {
        require(item != null && item.size() == 1, "Wrong item predicate " + context);
        JsonObject predicates = item.getAsJsonObject("predicates");
        require(predicates != null && predicates.size() == 1, "Wrong predicates " + context);
        JsonObject trim = predicates.getAsJsonObject("minecraft:trim");
        require(trim != null && trim.size() == 1 && ("minecraft:" + material).equals(trim.get("material").getAsString()), "Wrong trim material " + context);
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
