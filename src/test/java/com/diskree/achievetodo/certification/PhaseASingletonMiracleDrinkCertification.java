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

/** Frozen poison-cure consumption and live half-heart scoreboard contract. */
public final class PhaseASingletonMiracleDrinkCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/singleton_miracle_drink_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/singleton_miracle_drink_execution_evidence.json");
    public static final String FAMILY = "SINGLETON:blazeandcave:monsters/miracle_drink";
    public static final String SOURCE = "PhaseASingletonMiracleDrinkGameTest";
    public static final String ADVANCEMENT = "blazeandcave:monsters/miracle_drink";
    public static final String CRITERION = "miracle_drink";
    public static final String ITEM_TAG = "blazeandcave:poison_cures";
    public static final String SCORE = "bac_health";
    public static final String BOUNDARY = "ServerboundUseItemPacket.handle->ServerGamePacketListenerImpl.handleUseItem->LivingEntity.completeUsingItem->Consumable.onConsume->CriteriaTriggers.CONSUME_ITEM";
    private static final String FROZEN_PATH = "data/blazeandcave/advancement/monsters/miracle_drink.json";
    private static final String FROZEN_HASH = "071f20e4c422fc0facdc9c37a8704e08b9354a3fb0f8d18f8eb7fa29d34c1067";
    private static final String TAG_PATH = "data/blazeandcave/tags/item/poison_cures.json";
    private static final String TAG_HASH = "d42e15905bd518c2f763c50ce752944cc4c10a4c2b2e0a08f66b53268fb70bb5";
    private static final String BACAP_HASH = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonMiracleDrinkCertification() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected root");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve(SNAPSHOT).getParent());
        Files.writeString(root.resolve(SNAPSHOT), generate(root), StandardCharsets.UTF_8);
    }
    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)),
            "Frozen miracle-drink catalog mismatch");
    }
    public static String generate(Path root) throws Exception {
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(BACAP_HASH.equals(sha(Files.readAllBytes(archive))), "BACAP archive changed");
        JsonObject frozen, tag;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            frozen = load(zip, FROZEN_PATH, FROZEN_HASH);
            tag = load(zip, TAG_PATH, TAG_HASH);
        }
        JsonObject criteria = frozen.getAsJsonObject("criteria");
        require(criteria != null && criteria.size() == 1, "Wrong criterion count");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION);
        require(criterion != null && "minecraft:consume_item".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions != null && conditions.size() == 2, "Wrong condition shape");
        JsonObject item = conditions.getAsJsonObject("item");
        require(item != null && item.size() == 1 && ("#" + ITEM_TAG).equals(item.get("items").getAsString()), "Wrong cure tag");
        JsonArray player = conditions.getAsJsonArray("player");
        require(player != null && player.size() == 2, "Wrong player predicate count");
        JsonObject effect = player.get(0).getAsJsonObject();
        require("minecraft:entity_properties".equals(effect.get("condition").getAsString())
            && "this".equals(effect.get("entity").getAsString())
            && effect.getAsJsonObject("predicate").getAsJsonObject("effects")
                .getAsJsonObject("minecraft:poison").getAsJsonObject("amplifier").get("min").getAsInt() == 0,
            "Wrong poison predicate");
        JsonObject score = player.get(1).getAsJsonObject();
        require("minecraft:entity_scores".equals(score.get("condition").getAsString())
            && "this".equals(score.get("entity").getAsString())
            && score.getAsJsonObject("scores").size() == 1
            && score.getAsJsonObject("scores").get(SCORE).getAsInt() == 1, "Wrong half-heart score predicate");
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");
        require(!tag.get("replace").getAsBoolean(), "Cure tag replacement changed");
        JsonArray values = tag.getAsJsonArray("values");
        require(values != null && values.size() == 2
            && "minecraft:milk_bucket".equals(values.get(0).getAsString())
            && "minecraft:honey_bottle".equals(values.get(1).getAsString()), "Frozen cure membership changed");
        JsonObject catalog = new JsonObject();
        catalog.addProperty("snapshot", "phase_a_singleton_miracle_drink_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        catalog.addProperty("frozenBacapSha256", BACAP_HASH);
        catalog.addProperty("frozenAdvancementPath", FROZEN_PATH); catalog.addProperty("frozenAdvancementSha256", FROZEN_HASH);
        catalog.addProperty("frozenTagPath", TAG_PATH); catalog.addProperty("frozenTagSha256", TAG_HASH);
        catalog.addProperty("advancementId", ADVANCEMENT); catalog.addProperty("criterion", CRITERION);
        catalog.addProperty("requirementGroup", 0); catalog.addProperty("trigger", "minecraft:consume_item");
        catalog.addProperty("itemTag", ITEM_TAG); catalog.add("members", values.deepCopy());
        catalog.addProperty("requiredEffect", "minecraft:poison"); catalog.addProperty("minEffectAmplifier", 0);
        catalog.addProperty("scoreObjective", SCORE); catalog.addProperty("requiredScore", 1);
        catalog.addProperty("runtimeBoundary", BOUNDARY);
        return GSON.toJson(catalog) + "\n";
    }
    private static JsonObject load(ZipFile zip, String path, String expectedHash) throws Exception {
        var entry = zip.getEntry(path); require(entry != null, "Missing frozen " + path);
        byte[] bytes; try (InputStream in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
        require(expectedHash.equals(sha(bytes)), "Frozen hash changed for " + path);
        return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
}
