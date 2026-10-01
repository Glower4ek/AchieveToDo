package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.ZipFile;

/** Frozen parent/partner contract for the independent llama-breeding singleton. */
public final class PhaseASingletonLlamaBreedingCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/singleton_llama_breeding_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/singleton_llama_breeding_execution_evidence.json");
    public static final String FAMILY = "SINGLETON:blazeandcave:animal/so_i_got_that_going_for_me";
    public static final String SOURCE = "PhaseASingletonLlamaBreedingGameTest";
    public static final String ADVANCEMENT = "blazeandcave:animal/so_i_got_that_going_for_me";
    public static final String CRITERION = "llama";
    public static final String ENTITY_TAG = "blazeandcave:llamas";
    public static final String SELECTED_ENTITY = "minecraft:llama";
    public static final String FEED_ITEM = "minecraft:hay_block";
    public static final String BOUNDARY = "ServerboundInteractPacket.handle->ServerGamePacketListenerImpl.handleInteract->Llama.mobInteract->Llama.handleEating->Animal.setInLove->BreedGoal.breed->Animal.spawnChildFromBreeding->Animal.finalizeSpawnChildFromBreeding->CriteriaTriggers.BRED_ANIMALS";
    private static final String FROZEN_PATH = "data/blazeandcave/advancement/animal/so_i_got_that_going_for_me.json";
    private static final String FROZEN_HASH = "c20819c76041948bc8c0e648656782a0e49f50793b0cebba7f658985f3c3c6a0";
    private static final String TAG_PATH = "data/blazeandcave/tags/entity_type/llamas.json";
    private static final String TAG_HASH = "502c2a965bf6bdb960388a945f12e26d8505a778a3f5e9afc20a8bbee56cca3d";
    private static final String BACAP_HASH = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonLlamaBreedingCertification() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected root");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve(SNAPSHOT).getParent());
        Files.writeString(root.resolve(SNAPSHOT), generate(root), StandardCharsets.UTF_8);
    }
    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)), "Frozen llama breeding catalog mismatch");
    }
    public static String generate(Path root) throws Exception {
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(BACAP_HASH.equals(sha(Files.readAllBytes(archive))), "BACAP archive changed");
        JsonObject frozen;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            frozen = JsonParser.parseString(readFrozen(zip, FROZEN_PATH, FROZEN_HASH)).getAsJsonObject();
            JsonObject tag = JsonParser.parseString(readFrozen(zip, TAG_PATH, TAG_HASH)).getAsJsonObject();
            require(!tag.get("replace").getAsBoolean() && tag.getAsJsonArray("values").size() == 2
                && "minecraft:llama".equals(tag.getAsJsonArray("values").get(0).getAsString())
                && "minecraft:trader_llama".equals(tag.getAsJsonArray("values").get(1).getAsString()), "Frozen llama tag changed");
        }
        JsonObject criteria = frozen.getAsJsonObject("criteria");
        require(criteria != null && criteria.size() == 1, "Wrong criterion count");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION);
        require(criterion != null && "minecraft:bred_animals".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions != null && conditions.size() == 2, "Wrong condition count");
        for (String role : new String[]{"parent", "partner"}) {
            JsonObject entity = conditions.getAsJsonObject(role);
            require(entity != null && entity.size() == 1 && ("#" + ENTITY_TAG).equals(entity.get("type").getAsString()), "Wrong " + role + " predicate");
        }
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");
        JsonObject catalog = new JsonObject();
        catalog.addProperty("snapshot", "phase_a_singleton_llama_breeding_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        catalog.addProperty("frozenBacapSha256", BACAP_HASH);
        catalog.addProperty("frozenAdvancementPath", FROZEN_PATH); catalog.addProperty("frozenAdvancementSha256", FROZEN_HASH);
        catalog.addProperty("frozenEntityTagPath", TAG_PATH); catalog.addProperty("frozenEntityTagSha256", TAG_HASH);
        catalog.addProperty("advancementId", ADVANCEMENT); catalog.addProperty("criterion", CRITERION); catalog.addProperty("requirementGroup", 0);
        catalog.addProperty("trigger", "minecraft:bred_animals"); catalog.addProperty("parentTag", ENTITY_TAG);
        catalog.addProperty("partnerTag", ENTITY_TAG); catalog.addProperty("selectedParent", SELECTED_ENTITY);
        catalog.addProperty("selectedPartner", SELECTED_ENTITY); catalog.addProperty("feedItem", FEED_ITEM);
        catalog.addProperty("productionGate", "LANDMARK_ONLY_FOR_LLAMA_FEEDING"); catalog.addProperty("runtimeBoundary", BOUNDARY);
        return GSON.toJson(catalog) + "\n";
    }
    private static String readFrozen(ZipFile zip, String path, String expectedHash) throws Exception {
        var entry = zip.getEntry(path); require(entry != null, "Missing frozen " + path);
        byte[] bytes; try (InputStream in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
        require(expectedHash.equals(sha(bytes)), "Frozen source changed: " + path);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
}
