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

/** Frozen contract for the village-bell singleton. */
public final class PhaseASingletonVillageBellCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/singleton_village_bell_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/singleton_village_bell_execution_evidence.json");
    public static final String FAMILY = "SINGLETON:blazeandcave:adventure/oh_look_it_dings";
    public static final String SOURCE = "PhaseASingletonVillageBellGameTest";
    public static final String ADVANCEMENT = "blazeandcave:adventure/oh_look_it_dings";
    public static final String CRITERION = "bell";
    public static final String BLOCK = "minecraft:bell";
    public static final String STRUCTURE_TAG = "blazeandcave:village";
    public static final String SELECTED_STRUCTURE = "minecraft:village_plains";
    public static final String BOUNDARY = "ServerboundUseItemOnPacket.handle->ServerGamePacketListenerImpl.handleUseItemOn->ServerPlayerGameMode.useItemOn->BellBlock.useWithoutItem->BellBlock.attemptToRing->CriteriaTriggers.ANY_BLOCK_USE";
    private static final String FROZEN_PATH = "data/blazeandcave/advancement/adventure/oh_look_it_dings.json";
    private static final String FROZEN_HASH = "98888ba1856f1f85a6cd0e5e121a02939c62c3cdf9a0389f990234e1f063ca29";
    private static final String TAG_PATH = "data/blazeandcave/tags/worldgen/structure/village.json";
    private static final String TAG_HASH = "da3e50311b2df908f525973fda99e6bf0a92652bd62b589a6540e4ce59593e57";
    private static final String BACAP_HASH = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonVillageBellCertification() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected root");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve(SNAPSHOT).getParent());
        Files.writeString(root.resolve(SNAPSHOT), generate(root), StandardCharsets.UTF_8);
    }
    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)), "Frozen village bell catalog mismatch");
    }
    public static String generate(Path root) throws Exception {
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(BACAP_HASH.equals(sha(Files.readAllBytes(archive))), "BACAP archive changed");
        JsonObject frozen;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            frozen = JsonParser.parseString(readFrozen(zip, FROZEN_PATH, FROZEN_HASH)).getAsJsonObject();
            JsonObject tag = JsonParser.parseString(readFrozen(zip, TAG_PATH, TAG_HASH)).getAsJsonObject();
            require(!tag.get("replace").getAsBoolean() && tag.getAsJsonArray("values").size() == 1
                && "#minecraft:village".equals(tag.getAsJsonArray("values").get(0).getAsString()), "Frozen village tag changed");
        }
        JsonObject criteria = frozen.getAsJsonObject("criteria");
        require(criteria != null && criteria.size() == 1, "Wrong criterion count");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION);
        require(criterion != null && "minecraft:any_block_use".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions != null && conditions.size() == 1, "Wrong condition count");
        var locations = conditions.getAsJsonArray("location");
        require(locations != null && locations.size() == 1, "Wrong location alternatives");
        JsonObject check = locations.get(0).getAsJsonObject();
        require(check.size() == 2 && "minecraft:location_check".equals(check.get("condition").getAsString()), "Wrong location check");
        JsonObject predicate = check.getAsJsonObject("predicate");
        require(predicate.size() == 2 && ("#" + STRUCTURE_TAG).equals(predicate.get("structures").getAsString()), "Wrong structure predicate");
        JsonObject block = predicate.getAsJsonObject("block");
        require(block.size() == 1 && block.getAsJsonArray("blocks").size() == 1
            && BLOCK.equals(block.getAsJsonArray("blocks").get(0).getAsString()), "Wrong bell predicate");
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");
        JsonObject catalog = new JsonObject();
        catalog.addProperty("snapshot", "phase_a_singleton_village_bell_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        catalog.addProperty("frozenBacapSha256", BACAP_HASH);
        catalog.addProperty("frozenAdvancementPath", FROZEN_PATH); catalog.addProperty("frozenAdvancementSha256", FROZEN_HASH);
        catalog.addProperty("frozenStructureTagPath", TAG_PATH); catalog.addProperty("frozenStructureTagSha256", TAG_HASH);
        catalog.addProperty("advancementId", ADVANCEMENT); catalog.addProperty("criterion", CRITERION); catalog.addProperty("requirementGroup", 0);
        catalog.addProperty("trigger", "minecraft:any_block_use"); catalog.addProperty("block", BLOCK);
        catalog.addProperty("structureTag", STRUCTURE_TAG); catalog.addProperty("selectedStructure", SELECTED_STRUCTURE);
        catalog.addProperty("productionGate", "LANDMARK_ONLY_FOR_BELL_RING"); catalog.addProperty("runtimeBoundary", BOUNDARY);
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
