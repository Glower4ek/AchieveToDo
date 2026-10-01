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

/** Frozen redstone/click contract, kept separate from every other singleton front. */
public final class PhaseASingletonRedstoneClickCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/singleton_redstone_click_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/singleton_redstone_click_execution_evidence.json");
    public static final String FAMILY = "SINGLETON:blazeandcave:redstone/click";
    public static final String SOURCE = "PhaseASingletonRedstoneClickGameTest";
    public static final String ADVANCEMENT = "blazeandcave:redstone/click";
    public static final String CRITERION = "oak_button";
    public static final String TAG = "minecraft:buttons";
    public static final String BUTTON = "minecraft:oak_button";
    public static final String BOUNDARY = "ServerPlayerGameMode.useItemOn->BlockState.useWithoutItem->ButtonBlock.press->CriteriaTriggers.DEFAULT_BLOCK_USE";
    private static final String FROZEN_PATH = "data/blazeandcave/advancement/redstone/click.json";
    private static final String FROZEN_HASH = "e089c1c8fd7f009b327a8e10a5845996635c01214a13c2ceb326a9282314514d";
    private static final String BACAP_HASH = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonRedstoneClickCertification() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected root");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve(SNAPSHOT).getParent());
        Files.writeString(root.resolve(SNAPSHOT), generate(root), StandardCharsets.UTF_8);
    }
    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)), "Frozen button catalog mismatch");
    }
    public static String generate(Path root) throws Exception {
        Path archive = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        require(BACAP_HASH.equals(sha(Files.readAllBytes(archive))), "BACAP archive changed");
        JsonObject frozen;
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            var entry = zip.getEntry(FROZEN_PATH); require(entry != null, "Missing frozen click advancement");
            byte[] bytes; try (InputStream in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
            require(FROZEN_HASH.equals(sha(bytes)), "Frozen click advancement changed");
            frozen = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonObject criteria = frozen.getAsJsonObject("criteria");
        require(criteria != null && criteria.size() == 1, "Wrong singleton criterion count");
        JsonObject criterion = criteria.getAsJsonObject(CRITERION);
        require(criterion != null && "minecraft:default_block_use".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions != null && conditions.size() == 1, "Wrong condition count");
        var location = conditions.getAsJsonArray("location");
        require(location != null && location.size() == 1, "Wrong location condition count");
        JsonObject check = location.get(0).getAsJsonObject();
        require(check.size() == 2 && "minecraft:location_check".equals(check.get("condition").getAsString()), "Wrong location check");
        JsonObject predicate = check.getAsJsonObject("predicate");
        require(predicate.size() == 1 && ("#" + TAG).equals(predicate.getAsJsonObject("block").get("blocks").getAsString()), "Wrong button tag");
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");
        JsonObject catalog = new JsonObject(); catalog.addProperty("snapshot", "phase_a_singleton_redstone_click_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE); catalog.addProperty("minecraftVersion", "26.2");
        catalog.addProperty("compatibilityMarker", "compat_26_2_r15"); catalog.addProperty("frozenBacapSha256", BACAP_HASH);
        catalog.addProperty("frozenAdvancementPath", FROZEN_PATH); catalog.addProperty("frozenAdvancementSha256", FROZEN_HASH);
        catalog.addProperty("advancementId", ADVANCEMENT); catalog.addProperty("criterion", CRITERION); catalog.addProperty("requirementGroup", 0);
        catalog.addProperty("trigger", "minecraft:default_block_use"); catalog.addProperty("blockTag", TAG);
        catalog.addProperty("selectedBlock", BUTTON); catalog.addProperty("productionGate", "NONE"); catalog.addProperty("runtimeBoundary", BOUNDARY);
        return GSON.toJson(catalog) + "\n";
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
}
