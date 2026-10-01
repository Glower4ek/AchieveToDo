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
import java.util.zip.ZipFile;

/** Frozen Warden-hit equipment and effect contract for Maximum Resistance. */
public final class PhaseASingletonMaximumResistanceCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/singleton_maximum_resistance_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/singleton_maximum_resistance_execution_evidence.json");
    public static final String FAMILY = "SINGLETON:blazeandcave:monsters/maximum_resistance";
    public static final String SOURCE = "PhaseASingletonMaximumResistanceGameTest";
    public static final String ADVANCEMENT = "blazeandcave:monsters/maximum_resistance";
    public static final String CRITERION = "phantom";
    public static final String WARDEN = "minecraft:warden";
    public static final String ENCHANTMENT = "minecraft:protection";
    public static final String EFFECT = "minecraft:resistance";
    public static final String BOUNDARY = "WardenAi.MeleeAttack->Warden.doHurtTarget->LivingEntity.hurtServer->CriteriaTriggers.ENTITY_HURT_PLAYER";
    public static final String[] ARMOR = {"minecraft:netherite_helmet", "minecraft:netherite_chestplate",
        "minecraft:netherite_leggings", "minecraft:netherite_boots"};
    private static final String[] SLOTS = {"head", "chest", "legs", "feet"};
    private static final String FROZEN_PATH = "data/blazeandcave/advancement/monsters/maximum_resistance.json";
    private static final String FROZEN_HASH = "a6cbc45aefde5db109f1e9b599ac9754dc803f95044b05c22318414812da4cbf";
    private static final String BACAP_HASH = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonMaximumResistanceCertification() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected root");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve(SNAPSHOT).getParent());
        Files.writeString(root.resolve(SNAPSHOT), generate(root), StandardCharsets.UTF_8);
    }
    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)),
            "Frozen maximum-resistance catalog mismatch");
    }
    public static String generate(Path root) throws Exception {
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(BACAP_HASH.equals(sha(Files.readAllBytes(archive))), "BACAP archive changed");
        JsonObject frozen;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            var entry = zip.getEntry(FROZEN_PATH); require(entry != null, "Missing frozen advancement");
            byte[] bytes; try (InputStream in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
            require(FROZEN_HASH.equals(sha(bytes)), "Frozen advancement changed");
            frozen = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject criteria = frozen.getAsJsonObject("criteria");
        require(criteria != null && criteria.size() == 1, "Wrong criterion count");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION);
        require(criterion != null && "minecraft:entity_hurt_player".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions != null && conditions.size() == 2, "Wrong condition count");
        JsonObject damage = conditions.getAsJsonObject("damage");
        require(damage != null && damage.size() == 2 && !damage.get("blocked").getAsBoolean()
            && "warden".equals(damage.getAsJsonObject("source_entity").get("type").getAsString()), "Wrong Warden damage predicate");
        JsonArray player = conditions.getAsJsonArray("player");
        require(player != null && player.size() == 1, "Wrong player predicate count");
        JsonObject playerCheck = player.get(0).getAsJsonObject();
        require(playerCheck.size() == 3 && "minecraft:entity_properties".equals(playerCheck.get("condition").getAsString())
            && "this".equals(playerCheck.get("entity").getAsString()), "Wrong player check");
        JsonObject predicate = playerCheck.getAsJsonObject("predicate");
        require(predicate.size() == 2, "Wrong player predicate shape");
        JsonObject equipment = predicate.getAsJsonObject("equipment");
        require(equipment != null && equipment.size() == 4, "Wrong equipment slots");
        for (int i = 0; i < SLOTS.length; i++) {
            JsonObject stack = equipment.getAsJsonObject(SLOTS[i]);
            require(stack != null && stack.size() == 2, "Wrong " + SLOTS[i] + " item predicate");
            JsonArray items = stack.getAsJsonArray("items");
            require(items != null && items.size() == 1 && ARMOR[i].equals(items.get(0).getAsString()), "Wrong armor item");
            JsonObject enchantments = stack.getAsJsonObject("predicates");
            require(enchantments != null && enchantments.size() == 1, "Wrong enchantment predicate map");
            JsonArray values = enchantments.getAsJsonArray("enchantments");
            require(values != null && values.size() == 1, "Wrong enchantment alternatives");
            JsonObject requirement = values.get(0).getAsJsonObject();
            require(requirement.size() == 2 && ENCHANTMENT.equals(requirement.get("enchantments").getAsString())
                && requirement.getAsJsonObject("levels").get("min").getAsInt() == 4,
                "Wrong Protection IV requirement");
        }
        JsonObject effects = predicate.getAsJsonObject("effects");
        require(effects != null && effects.size() == 1
            && effects.getAsJsonObject(EFFECT).getAsJsonObject("amplifier").get("min").getAsInt() == 3,
            "Wrong Resistance IV requirement");
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");
        JsonObject catalog = new JsonObject();
        catalog.addProperty("snapshot", "phase_a_singleton_maximum_resistance_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        catalog.addProperty("frozenBacapSha256", BACAP_HASH);
        catalog.addProperty("frozenAdvancementPath", FROZEN_PATH); catalog.addProperty("frozenAdvancementSha256", FROZEN_HASH);
        catalog.addProperty("advancementId", ADVANCEMENT); catalog.addProperty("criterion", CRITERION);
        catalog.addProperty("requirementGroup", 0); catalog.addProperty("trigger", "minecraft:entity_hurt_player");
        catalog.addProperty("sourceEntity", WARDEN); catalog.addProperty("blocked", false);
        JsonArray armor = new JsonArray(); for (String item : ARMOR) armor.add(item);
        catalog.add("armor", armor); catalog.addProperty("enchantment", ENCHANTMENT); catalog.addProperty("minEnchantmentLevel", 4);
        catalog.addProperty("effect", EFFECT); catalog.addProperty("minEffectAmplifier", 3);
        catalog.addProperty("productionGate", "NO_WARDEN_DAMAGE_GATE"); catalog.addProperty("runtimeBoundary", BOUNDARY);
        return GSON.toJson(catalog) + "\n";
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
}
