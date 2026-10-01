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

/** Frozen three-bee nest and Silk Touch contract. */
public final class PhaseASingletonSilkTouchNestCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/singleton_silk_touch_nest_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/singleton_silk_touch_nest_execution_evidence.json");
    public static final String FAMILY = "SINGLETON:minecraft:husbandry/silk_touch_nest";
    public static final String SOURCE = "PhaseASingletonSilkTouchNestGameTest";
    public static final String ADVANCEMENT = "minecraft:husbandry/silk_touch_nest";
    public static final String CRITERION = "silk_touch_nest";
    public static final String BLOCK = "minecraft:bee_nest";
    public static final String ENCHANTMENT = "minecraft:silk_touch";
    public static final String BOUNDARY = "ServerboundPlayerActionPacket.handle->ServerGamePacketListenerImpl.handlePlayerAction->ServerPlayerGameMode.handleBlockBreakAction->ServerPlayerGameMode.destroyBlock->BeehiveBlock.playerDestroy->CriteriaTriggers.BEE_NEST_DESTROYED";
    private static final String FROZEN_PATH = "data/minecraft/advancement/husbandry/silk_touch_nest.json";
    private static final String FROZEN_HASH = "6191682bd841af5c00405a13f10d9fdfaeb2596c1b983d169534ee113ef2e3bf";
    private static final String BACAP_HASH = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonSilkTouchNestCertification() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected root");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve(SNAPSHOT).getParent());
        Files.writeString(root.resolve(SNAPSHOT), generate(root), StandardCharsets.UTF_8);
    }
    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)),
            "Frozen ancient-restoration catalog mismatch");
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
        require(criteria.size() == 1, "Wrong criterion count");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION);
        require("minecraft:bee_nest_destroyed".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions.size() == 3 && conditions.getAsJsonArray("blocks").size() == 1
            && BLOCK.equals(conditions.getAsJsonArray("blocks").get(0).getAsString())
            && conditions.get("num_bees_inside").getAsInt() == 3, "Wrong nest/count");
        JsonArray enchantments = conditions.getAsJsonObject("item").getAsJsonObject("predicates").getAsJsonArray("enchantments");
        require(enchantments.size() == 1 && ENCHANTMENT.equals(enchantments.get(0).getAsJsonObject().get("enchantments").getAsString())
            && enchantments.get(0).getAsJsonObject().getAsJsonObject("levels").get("min").getAsInt() == 1, "Wrong enchantment");
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");
        JsonObject catalog = new JsonObject();
        catalog.addProperty("snapshot", "phase_a_singleton_silk_touch_nest_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        catalog.addProperty("frozenBacapSha256", BACAP_HASH); catalog.addProperty("frozenAdvancementPath", FROZEN_PATH);
        catalog.addProperty("frozenAdvancementSha256", FROZEN_HASH); catalog.addProperty("advancementId", ADVANCEMENT);
        catalog.addProperty("criterion", CRITERION); catalog.addProperty("requirementGroup", 0);
        catalog.addProperty("trigger", "minecraft:bee_nest_destroyed"); catalog.addProperty("block", BLOCK);
        catalog.addProperty("beesInside", 3); catalog.addProperty("enchantment", ENCHANTMENT);
        catalog.addProperty("minimumEnchantmentLevel", 1); catalog.addProperty("selectedTool", "minecraft:wooden_axe");
        catalog.addProperty("runtimeBoundary", BOUNDARY);
        return GSON.toJson(catalog) + "\n";
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
}
