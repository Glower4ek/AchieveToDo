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

/** Frozen singleton contract for the Meadows jukebox item-on-block criterion. */
public final class PhaseAItemUsedOnBlockContextualCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/item_used_on_block_contextual_case_catalog.json");
    public static final Path PERSISTENT_EVIDENCE = Path.of("src/test/resources/phase_a_certification/item_used_on_block_contextual_execution_evidence.json");
    public static final String FAMILY = "ITEM_USED_ON_BLOCK_CONTEXTUAL";
    public static final String SOURCE = "PhaseAItemUsedOnBlockContextualGameTest";
    public static final String ADVANCEMENT_ID = "minecraft:adventure/play_jukebox_in_meadows";
    public static final String CRITERION = "play_jukebox_in_meadows";
    public static final String BIOME = "minecraft:meadow";
    public static final String BLOCK = "minecraft:jukebox";
    public static final String DISC = "minecraft:music_disc_13";
    public static final String BOUNDARY = "ServerPlayerGameMode.useItemOn->JukeboxBlock.useItemOn->JukeboxPlayable.tryInsertIntoJukebox->CriteriaTriggers.ITEM_USED_ON_BLOCK";
    private static final String FROZEN_PATH = "data/minecraft/advancement/adventure/play_jukebox_in_meadows.json";
    private static final String FROZEN_SHA256 = "870fa39f9ae028b81f1a82fdf4c9da864380c2750c528d4b1e420aa798dbbc73";
    private static final String BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseAItemUsedOnBlockContextualCertification() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected project root");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve(SNAPSHOT).getParent());
        Files.writeString(root.resolve(SNAPSHOT), generate(root), StandardCharsets.UTF_8);
    }
    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)), "Frozen-source catalog mismatch");
    }
    public static String generate(Path root) throws Exception {
        // The archive hash is verified independently, and the exact entry hash protects the criterion contract.
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(BACAP_SHA256.equals(sha(Files.readAllBytes(archive))), "BACAP archive changed");
        JsonObject frozen;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            var entry = zip.getEntry(FROZEN_PATH); require(entry != null, "Missing frozen jukebox advancement");
            byte[] bytes; try (InputStream in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
            require(FROZEN_SHA256.equals(sha(bytes)), "Frozen jukebox advancement changed");
            frozen = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject criteria = frozen.getAsJsonObject("criteria"); require(criteria != null && criteria.size() == 1, "Wrong criterion cardinality");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION); require(criterion != null && "minecraft:item_used_on_block".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions"); require(conditions != null && conditions.size() == 1, "Wrong conditions");
        JsonArray location = conditions.getAsJsonArray("location"); require(location != null && location.size() == 2, "Wrong location predicate count");
        JsonObject block = location.get(0).getAsJsonObject(); require("minecraft:location_check".equals(block.get("condition").getAsString()), "Wrong location condition");
        JsonObject predicate = block.getAsJsonObject("predicate");
        require(predicate.size() == 2 && BIOME.equals(predicate.get("biomes").getAsString()), "Wrong biome predicate");
        JsonArray blocks = predicate.getAsJsonObject("block").getAsJsonArray("blocks");
        require(blocks.size() == 1 && BLOCK.equals(blocks.get(0).getAsString()), "Wrong block predicate");
        JsonObject tool = location.get(1).getAsJsonObject(); require("minecraft:match_tool".equals(tool.get("condition").getAsString()), "Wrong tool condition");
        JsonObject toolPredicates = tool.getAsJsonObject("predicate").getAsJsonObject("predicates");
        require(toolPredicates.size() == 1 && toolPredicates.getAsJsonObject("minecraft:jukebox_playable").size() == 0, "Wrong jukebox-playable component predicate");
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");
        JsonObject catalog = new JsonObject(); catalog.addProperty("snapshot", "phase_a_item_used_on_block_contextual_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("minecraftVersion", "26.2");
        catalog.addProperty("compatibilityMarker", "compat_26_2_r15"); catalog.addProperty("frozenBacapSha256", BACAP_SHA256);
        catalog.addProperty("frozenAdvancementPath", FROZEN_PATH); catalog.addProperty("frozenAdvancementSha256", FROZEN_SHA256);
        catalog.addProperty("advancementId", ADVANCEMENT_ID); catalog.addProperty("criterion", CRITERION);
        catalog.addProperty("requirementGroup", 0); catalog.addProperty("trigger", "minecraft:item_used_on_block");
        catalog.addProperty("biome", BIOME); catalog.addProperty("block", BLOCK); catalog.addProperty("toolComponent", "minecraft:jukebox_playable");
        catalog.addProperty("selectedDisc", DISC); catalog.addProperty("productionGate", "USE_JUKEBOX"); catalog.addProperty("runtimeBoundary", BOUNDARY);
        return GSON.toJson(catalog) + "\n";
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
