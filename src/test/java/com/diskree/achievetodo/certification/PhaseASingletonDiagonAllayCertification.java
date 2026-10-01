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
import java.util.Set;
import java.util.zip.ZipFile;

/** Exact frozen predicate for an Allay-delivered potion picked up in Deep Dark. */
public final class PhaseASingletonDiagonAllayCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/singleton_diagon_allay_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/singleton_diagon_allay_execution_evidence.json");
    public static final String FAMILY = "SINGLETON:blazeandcave:adventure/diagon_allay";
    public static final String SOURCE = "PhaseASingletonDiagonAllayGameTest";
    public static final String ADVANCEMENT = "blazeandcave:adventure/diagon_allay";
    public static final String CRITERION = "diagon_allay";
    public static final String BIOME = "minecraft:deep_dark";
    public static final String ENTITY = "minecraft:allay";
    public static final String ITEM = "minecraft:potion";
    public static final String BOUNDARY = "AllayAi.GoAndGiveItemsToTarget->BehaviorUtils.throwItem->ItemEntity.playerTouch->ServerPlayer.onItemPickup->CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER";
    private static final String FROZEN_PATH = "data/blazeandcave/advancement/adventure/diagon_allay.json";
    private static final String FROZEN_HASH = "6cd5238ac062614baeb05fb7a4c2cbb9ede1b4c1cb9094da0b51c824b431accd";
    private static final String BACAP_HASH = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonDiagonAllayCertification() { }

    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)), "Frozen Allay catalog mismatch");
    }

    public static String generate(Path root) throws Exception {
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(BACAP_HASH.equals(sha(Files.readAllBytes(archive))), "BACAP archive changed");
        JsonObject frozen;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            var entry = zip.getEntry(FROZEN_PATH);
            require(entry != null, "Missing frozen Allay advancement");
            byte[] bytes;
            try (InputStream in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
            require(FROZEN_HASH.equals(sha(bytes)), "Frozen Allay advancement changed");
            frozen = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject criteria = frozen.getAsJsonObject("criteria");
        require(criteria != null && criteria.size() == 1, "Wrong singleton criterion count");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION);
        require(criterion != null && "minecraft:thrown_item_picked_up_by_player".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions != null && conditions.size() == 3, "Wrong condition count");
        JsonArray entity = conditions.getAsJsonArray("entity");
        require(entity != null && entity.size() == 1, "Wrong entity condition count");
        JsonObject entityCheck = entity.get(0).getAsJsonObject();
        require("minecraft:entity_properties".equals(entityCheck.get("condition").getAsString())
            && "this".equals(entityCheck.get("entity").getAsString())
            && ENTITY.equals(entityCheck.getAsJsonObject("predicate").get("type").getAsString()), "Wrong Allay predicate");
        JsonArray itemAlternatives = conditions.getAsJsonObject("item").getAsJsonArray("items");
        require(itemAlternatives != null && itemAlternatives.size() == 3
            && Set.of("minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion")
                .equals(Set.of(itemAlternatives.get(0).getAsString(), itemAlternatives.get(1).getAsString(), itemAlternatives.get(2).getAsString())),
            "Wrong potion alternatives");
        JsonArray player = conditions.getAsJsonArray("player");
        require(player != null && player.size() == 1, "Wrong player condition count");
        JsonObject anyOf = player.get(0).getAsJsonObject();
        require("minecraft:any_of".equals(anyOf.get("condition").getAsString()), "Wrong player OR predicate");
        JsonArray terms = anyOf.getAsJsonArray("terms");
        require(terms != null && terms.size() == 2, "Wrong player alternative count");
        require("minecraft:deep_dark".equals(terms.get(0).getAsJsonObject().getAsJsonObject("predicate")
            .getAsJsonObject("location").get("biomes").getAsString()), "Wrong Deep Dark biome");
        require("minecraft:ancient_city".equals(terms.get(1).getAsJsonObject().getAsJsonObject("predicate")
            .getAsJsonObject("location").get("structures").getAsString()), "Wrong Ancient City structure");
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");

        JsonObject catalog = new JsonObject();
        catalog.addProperty("snapshot", "phase_a_singleton_diagon_allay_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        catalog.addProperty("frozenBacapSha256", BACAP_HASH); catalog.addProperty("frozenAdvancementPath", FROZEN_PATH);
        catalog.addProperty("frozenAdvancementSha256", FROZEN_HASH); catalog.addProperty("advancementId", ADVANCEMENT);
        catalog.addProperty("criterion", CRITERION); catalog.addProperty("requirementGroup", 0);
        catalog.addProperty("trigger", "minecraft:thrown_item_picked_up_by_player");
        catalog.addProperty("selectedEntity", ENTITY); catalog.addProperty("selectedItem", ITEM);
        catalog.addProperty("selectedPlayerLocationAlternative", BIOME);
        catalog.addProperty("productionGate", "LANDMARK_ONLY_FOR_ALLAY_INTERACTION");
        catalog.addProperty("runtimeBoundary", BOUNDARY);
        return GSON.toJson(catalog) + "\n";
    }

    private static String sha(byte[] bytes) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
}
