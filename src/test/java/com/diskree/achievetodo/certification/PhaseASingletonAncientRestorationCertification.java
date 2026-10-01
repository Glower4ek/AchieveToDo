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

/** Frozen portal-entry and ruined-portal structure contract. */
public final class PhaseASingletonAncientRestorationCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/singleton_ancient_restoration_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/singleton_ancient_restoration_execution_evidence.json");
    public static final String FAMILY = "SINGLETON:blazeandcave:nether/ancient_restoration";
    public static final String SOURCE = "PhaseASingletonAncientRestorationGameTest";
    public static final String ADVANCEMENT = "blazeandcave:nether/ancient_restoration";
    public static final String CRITERION = "entered_nether_portal";
    public static final String BLOCK = "minecraft:nether_portal";
    public static final String[] STRUCTURES = {"minecraft:ruined_portal", "minecraft:ruined_portal_desert",
        "minecraft:ruined_portal_jungle", "minecraft:ruined_portal_mountain", "minecraft:ruined_portal_nether",
        "minecraft:ruined_portal_ocean", "minecraft:ruined_portal_swamp"};
    public static final String BOUNDARY = "Entity.checkInsideBlocks->ServerPlayer.onInsideBlock->CriteriaTriggers.ENTER_BLOCK";
    private static final String FROZEN_PATH = "data/blazeandcave/advancement/nether/ancient_restoration.json";
    private static final String FROZEN_HASH = "e2fad1c3275fef2039856fa4e58ed0c724f2bfdb5d5a4816d556c7169d31c107";
    private static final String BACAP_HASH = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonAncientRestorationCertification() { }

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
        require(criteria != null && criteria.size() == 1, "Wrong criterion count");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION);
        require(criterion != null && "minecraft:enter_block".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions != null && conditions.size() == 2 && BLOCK.equals(conditions.get("block").getAsString()), "Wrong block condition");
        JsonArray player = conditions.getAsJsonArray("player");
        require(player != null && player.size() == 2, "Wrong player condition count");
        JsonObject any = player.get(0).getAsJsonObject();
        require(any.size() == 2 && "minecraft:any_of".equals(any.get("condition").getAsString()), "Wrong structure alternative condition");
        JsonArray terms = any.getAsJsonArray("terms"); require(terms != null && terms.size() == STRUCTURES.length, "Wrong structure count");
        for (int i = 0; i < STRUCTURES.length; i++) {
            JsonObject term = terms.get(i).getAsJsonObject();
            require(term.size() == 3 && "minecraft:entity_properties".equals(term.get("condition").getAsString())
                && "this".equals(term.get("entity").getAsString())
                && STRUCTURES[i].equals(term.getAsJsonObject("predicate").getAsJsonObject("location")
                    .get("structures").getAsString()), "Wrong ruined-portal structure alternative");
        }
        JsonObject inverted = player.get(1).getAsJsonObject();
        JsonObject exclusion = inverted.getAsJsonObject("term");
        JsonArray modes = exclusion.getAsJsonObject("predicate").getAsJsonObject("type_specific").getAsJsonArray("gamemode");
        require("minecraft:inverted".equals(inverted.get("condition").getAsString())
            && "minecraft:entity_properties".equals(exclusion.get("condition").getAsString())
            && "this".equals(exclusion.get("entity").getAsString())
            && modes.size() == 1 && "spectator".equals(modes.get(0).getAsString()), "Wrong spectator exclusion");
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");
        JsonObject catalog = new JsonObject();
        catalog.addProperty("snapshot", "phase_a_singleton_ancient_restoration_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        catalog.addProperty("frozenBacapSha256", BACAP_HASH); catalog.addProperty("frozenAdvancementPath", FROZEN_PATH);
        catalog.addProperty("frozenAdvancementSha256", FROZEN_HASH); catalog.addProperty("advancementId", ADVANCEMENT);
        catalog.addProperty("criterion", CRITERION); catalog.addProperty("requirementGroup", 0);
        catalog.addProperty("trigger", "minecraft:enter_block"); catalog.addProperty("block", BLOCK);
        JsonArray structures = new JsonArray(); for (String id : STRUCTURES) structures.add(id);
        catalog.add("structures", structures); catalog.addProperty("excludedGameMode", "spectator");
        catalog.addProperty("runtimeBoundary", BOUNDARY);
        return GSON.toJson(catalog) + "\n";
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
}
