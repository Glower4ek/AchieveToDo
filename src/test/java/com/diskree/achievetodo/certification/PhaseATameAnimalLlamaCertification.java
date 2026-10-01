package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Exact frozen-definition catalog for the narrow llama TAME_ANIMAL frontier family. */
public final class PhaseATameAnimalLlamaCertification {
    public static final Path SNAPSHOT = Path.of(
        "src", "test", "resources", "phase_a_certification", "tame_animal_llama_case_catalog.json"
    );
    public static final String SNAPSHOT_ID = "phase_a_tame_animal_llama_case_catalog";
    public static final String FAMILY = "TAME_ANIMAL_LLAMA";
    public static final String CANONICAL_SEMANTICS_SOURCE = "reference/phase_a_preservation/files/final/bacap.zip";
    public static final String FROZEN_BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String TRIGGER = "minecraft:tame_animal";
    public static final String BOUNDARY = "ServerGamePacketListenerImpl.handleInteract";
    public static final String PACKET_PATH = "ServerboundInteractPacket.handle->ServerGamePacketListenerImpl.handleInteract"
        + "->AbstractHorse.mobInteract->Player.startRiding->RunAroundLikeCrazyGoal.tick"
        + "->AbstractHorse.tameWithName->CriteriaTriggers.TAME_ANIMAL";
    public static final List<Case> CASES = List.of(
        new Case("blazeandcave:animal/heavy_duty_caravan", "data/blazeandcave/advancement/animal/heavy_duty_caravan.json", "strength", 0, "strength", 5, null, "ff42c3d13a868fc40e22a842dd2f2ebc6d033baf8350078353f096efd3111d8d"),
        new Case("blazeandcave:animal/llama_llama_duck_king", "data/blazeandcave/advancement/animal/llama_llama_duck_king.json", "creamy", 0, "variant", 0, "CREAMY", "d286f447dc9e117632310fd24ae368b421d005ed77c93351b98bc189e1734e8b"),
        new Case("blazeandcave:animal/llama_llama_duck_king", "data/blazeandcave/advancement/animal/llama_llama_duck_king.json", "white", 1, "variant", 1, "WHITE", "d286f447dc9e117632310fd24ae368b421d005ed77c93351b98bc189e1734e8b"),
        new Case("blazeandcave:animal/llama_llama_duck_king", "data/blazeandcave/advancement/animal/llama_llama_duck_king.json", "brown", 2, "variant", 2, "BROWN", "d286f447dc9e117632310fd24ae368b421d005ed77c93351b98bc189e1734e8b"),
        new Case("blazeandcave:animal/llama_llama_duck_king", "data/blazeandcave/advancement/animal/llama_llama_duck_king.json", "gray", 3, "variant", 3, "GRAY", "d286f447dc9e117632310fd24ae368b421d005ed77c93351b98bc189e1734e8b"),
        new Case("blazeandcave:animal/stay_calmer", "data/blazeandcave/advancement/animal/stay_calmer.json", "tamed_animal", 0, "any_llama", 0, null, "47cb4d34c7fc4e44a42d61d679cec089b3c65b32d568b7dd5113e2c333da0de4")
    );
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseATameAnimalLlamaCertification() { }

    public static String generateSnapshot(Path projectRoot) throws IOException {
        Path bacap = projectRoot.resolve(CANONICAL_SEMANTICS_SOURCE);
        require(Files.isRegularFile(bacap), "Missing frozen BACAP");
        require(FROZEN_BACAP_SHA256.equals(hash(Files.readAllBytes(bacap))), "Frozen BACAP hash changed");
        Set<String> keys = new LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(bacap.toFile(), StandardCharsets.UTF_8)) {
            for (Case definition : CASES) {
                ZipEntry entry = zip.getEntry(definition.sourcePath());
                require(entry != null, "Missing frozen advancement " + definition.sourcePath());
                try (InputStream input = zip.getInputStream(entry)) {
                    require(definition.sourceJsonSha256().equals(hash(input.readAllBytes())),
                        "Frozen advancement hash changed for " + definition.key());
                }
                require(keys.add(definition.key()), "Duplicate llama criterion " + definition.key());
            }
        }
        require(keys.size() == 6, "Expected exactly six llama criteria");
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT_ID);
        root.addProperty("schemaVersion", 1);
        root.addProperty("family", FAMILY);
        root.addProperty("canonicalAdvancementCount", 1152);
        root.addProperty("minecraftVersion", MINECRAFT_VERSION);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("canonicalSemanticsSource", CANONICAL_SEMANTICS_SOURCE);
        root.addProperty("frozenBacapSha256", FROZEN_BACAP_SHA256);
        root.addProperty("runtimeBoundary", BOUNDARY);
        root.addProperty("runtimePacketPath", PACKET_PATH);
        JsonObject summary = new JsonObject();
        summary.addProperty("totalCases", 6);
        summary.addProperty("uniqueKeys", 6);
        summary.addProperty("uniqueAdvancements", 3);
        summary.addProperty("requirementGroups", 6);
        summary.addProperty("boundedNativeTamingTicks", 600);
        root.add("summary", summary);
        JsonArray cases = new JsonArray();
        CASES.forEach(c -> cases.add(c.toJson()));
        root.add("cases", cases);
        return GSON.toJson(root) + "\n";
    }

    public static void writeSnapshot(Path projectRoot) throws IOException {
        Path output = projectRoot.resolve(SNAPSHOT);
        Files.createDirectories(output.getParent());
        Files.writeString(output, generateSnapshot(projectRoot), StandardCharsets.UTF_8);
    }

    private static String hash(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public record Case(String advancementId, String sourcePath, String criterion, int requirementGroupIndex,
                       String predicateKind, int predicateValue, String variant, String sourceJsonSha256) {
        String key() { return advancementId + "#" + criterion; }
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("advancementId", advancementId);
            json.addProperty("sourcePath", sourcePath);
            json.addProperty("criterion", criterion);
            json.addProperty("requirementGroupIndex", requirementGroupIndex);
            json.addProperty("trigger", TRIGGER);
            JsonObject predicate = new JsonObject();
            predicate.addProperty("entityType", "#blazeandcave:llamas");
            predicate.addProperty("kind", predicateKind);
            if ("strength".equals(predicateKind)) predicate.addProperty("strength", predicateValue);
            if ("variant".equals(predicateKind)) { predicate.addProperty("variant", variant); predicate.addProperty("variantId", predicateValue); }
            json.add("entityPredicate", predicate);
            json.addProperty("action", "MOUNT_UNTAMED_LLAMA");
            json.addProperty("boundary", BOUNDARY);
            json.addProperty("packetPath", PACKET_PATH);
            json.addProperty("nativeTamingStrategy", "set max temper, mount through real interaction packet, wait at most 600 normal server ticks for RunAroundLikeCrazyGoal");
            json.addProperty("automationEligibility", "SUPPORTED_BOUNDED");
            json.addProperty("semanticsSource", "frozenBacap+official26_2Bytecode");
            json.addProperty("sourceJsonSha256", sourceJsonSha256);
            return json;
        }
    }
}
