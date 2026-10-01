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

/** Frozen effects-changed source and location contract for Thanks a lotl. */
public final class PhaseASingletonAxolotlMonumentCertification {
    public static final Path SNAPSHOT = Path.of("src/test/resources/phase_a_certification/singleton_axolotl_monument_case_catalog.json");
    public static final Path PERSISTENT = Path.of("src/test/resources/phase_a_certification/singleton_axolotl_monument_execution_evidence.json");
    public static final String FAMILY = "SINGLETON:blazeandcave:animal/thanks_a_lotl";
    public static final String SOURCE = "PhaseASingletonAxolotlMonumentGameTest";
    public static final String ADVANCEMENT = "blazeandcave:animal/thanks_a_lotl";
    public static final String CRITERION = "kill_axolotl_target";
    public static final String STRUCTURE = "minecraft:monument";
    public static final String SOURCE_ENTITY = "minecraft:axolotl";
    public static final String BOUNDARY = "ServerboundAttackPacket.handle->ServerGamePacketListenerImpl.handleAttack->ServerPlayer.attack->AxolotlAi.StopAttackingIfTargetInvalid->Axolotl.onStopAttacking->Axolotl.applySupportingEffects->Player.addEffect->ServerPlayer.onEffectAdded->CriteriaTriggers.EFFECTS_CHANGED";
    private static final String FROZEN_PATH = "data/blazeandcave/advancement/animal/thanks_a_lotl.json";
    private static final String FROZEN_HASH = "02f5fc9bff59785263ea96e146b15ed26185875f8687569aefb6b8fd8a3afb2f";
    private static final String BACAP_HASH = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonAxolotlMonumentCertification() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected root");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve(SNAPSHOT).getParent());
        Files.writeString(root.resolve(SNAPSHOT), generate(root), StandardCharsets.UTF_8);
    }
    public static void validate(Path root) throws Exception {
        require(generate(root).equals(Files.readString(root.resolve(SNAPSHOT), StandardCharsets.UTF_8)), "Frozen axolotl monument catalog mismatch");
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
        require(criterion != null && "minecraft:effects_changed".equals(criterion.get("trigger").getAsString()), "Wrong trigger");
        JsonObject conditions = criterion.getAsJsonObject("conditions");
        require(conditions != null && conditions.size() == 2, "Wrong condition count");
        var source = conditions.getAsJsonArray("source");
        require(source != null && source.size() == 1, "Wrong source alternatives");
        JsonObject sourceCheck = source.get(0).getAsJsonObject();
        require(sourceCheck.size() == 3 && "minecraft:entity_properties".equals(sourceCheck.get("condition").getAsString())
            && "this".equals(sourceCheck.get("entity").getAsString()), "Wrong source check");
        JsonObject sourcePredicate = sourceCheck.getAsJsonObject("predicate");
        require(sourcePredicate.size() == 2 && SOURCE_ENTITY.equals(sourcePredicate.get("type").getAsString())
            && "monument".equals(sourcePredicate.getAsJsonObject("location").get("structures").getAsString()), "Wrong source predicate");
        JsonObject player = conditions.getAsJsonObject("player");
        require(player != null && player.size() == 3 && "minecraft:entity_properties".equals(player.get("condition").getAsString())
            && "this".equals(player.get("entity").getAsString())
            && "monument".equals(player.getAsJsonObject("predicate").getAsJsonObject("location").get("structures").getAsString()), "Wrong player monument predicate");
        require(!frozen.has("requirements"), "Expected implicit singleton requirement group");
        JsonObject catalog = new JsonObject();
        catalog.addProperty("snapshot", "phase_a_singleton_axolotl_monument_case_catalog");
        catalog.addProperty("family", FAMILY); catalog.addProperty("source", SOURCE);
        catalog.addProperty("minecraftVersion", "26.2"); catalog.addProperty("compatibilityMarker", "compat_26_2_r15");
        catalog.addProperty("frozenBacapSha256", BACAP_HASH);
        catalog.addProperty("frozenAdvancementPath", FROZEN_PATH); catalog.addProperty("frozenAdvancementSha256", FROZEN_HASH);
        catalog.addProperty("advancementId", ADVANCEMENT); catalog.addProperty("criterion", CRITERION); catalog.addProperty("requirementGroup", 0);
        catalog.addProperty("trigger", "minecraft:effects_changed"); catalog.addProperty("sourceEntity", SOURCE_ENTITY);
        catalog.addProperty("sourceStructure", STRUCTURE); catalog.addProperty("playerStructure", STRUCTURE);
        catalog.addProperty("productionGate", "LANDMARK_ONLY_FOR_PLAYER_ATTACK"); catalog.addProperty("runtimeBoundary", BOUNDARY);
        return GSON.toJson(catalog) + "\n";
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
}
