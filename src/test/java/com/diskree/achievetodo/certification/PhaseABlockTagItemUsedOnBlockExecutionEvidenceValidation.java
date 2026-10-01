package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import static com.diskree.achievetodo.certification.PhaseABlockTagItemUsedOnBlockCertification.require;

/** Shared receipt semantics; persistent evidence is independent of TEMP and recorder state. */
public final class PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation {
    public enum Mode { TEMP_DIAGNOSTIC, TEMP_PROMOTABLE, PERSISTENT }
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/block_tag_item_used_on_block_execution_evidence.json");
    public static final Path PERSISTENT_ARTIFACT = Path.of("src/test/resources/phase_a_certification/block_tag_item_used_on_block_execution_evidence.json");
    public static final String ACCEPTED_TEMP_SHA256 = "11e7e8061958beaec7c7c6932c959c65de9b7c600e9e3028f8aae8aeeb7762ff";

    public static void loadTemporary(Path root, Mode mode) throws IOException {
        require(mode == Mode.TEMP_DIAGNOSTIC || mode == Mode.TEMP_PROMOTABLE, "Expected TEMP validation mode");
        validate(root, JsonParser.parseString(Files.readString(root.resolve(TEMP))).getAsJsonObject(), mode);
    }

    public static JsonObject loadValidatedPersistentArtifact(Path root) throws IOException {
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT))).getAsJsonObject();
        validate(root, artifact, Mode.PERSISTENT);
        return artifact;
    }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        JsonObject artifact = loadValidatedPersistentArtifact(root);
        Map<String, RuntimeEvidenceData> evidence = new LinkedHashMap<>();
        for (JsonElement element : artifact.getAsJsonArray("entries")) {
            JsonObject receipt = element.getAsJsonObject();
            RuntimeEvidenceData data = evidence.computeIfAbsent(string(receipt, "advancementId"), RuntimeEvidenceData::empty);
            String criterion = string(receipt, "criterion");
            String family = string(receipt, "family"), source = string(receipt, "source");
            data.greenCriteria().add(criterion);
            data.criteriaByFamily().computeIfAbsent(family, ignored -> new TreeSet<>()).add(criterion);
            data.criteriaBySource().computeIfAbsent(source, ignored -> new TreeSet<>()).add(criterion);
            data.families().add(family);
            data.sources().add(source);
        }
        return evidence;
    }

    /** First promotion only. Never serialize receipts or replace an existing destination. */
    public static void promoteTemporaryArtifact(Path root) throws IOException {
        Path source = root.resolve(TEMP);
        byte[] initialBytes = Files.readAllBytes(source);
        String sourceHash = sha256(initialBytes);
        require(ACCEPTED_TEMP_SHA256.equals(sourceHash), "PROMOTION_SOURCE_TEMP_CHANGED");
        loadTemporary(root, Mode.TEMP_PROMOTABLE);
        byte[] tempBytes = Files.readAllBytes(source);
        require(Arrays.equals(initialBytes, tempBytes), "PROMOTION_SOURCE_TEMP_CHANGED");
        Path destination = root.resolve(PERSISTENT_ARTIFACT);
        require(!Files.exists(destination, LinkOption.NOFOLLOW_LINKS), "Persistent destination already exists");
        Files.createDirectories(destination.getParent());
        Path sibling = destination.resolveSibling(destination.getFileName() + ".promotion.tmp");
        Files.write(sibling, tempBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try {
            require(!Files.exists(destination, LinkOption.NOFOLLOW_LINKS), "Persistent destination already exists");
            try {
                Files.move(sibling, destination, StandardCopyOption.ATOMIC_MOVE);
                System.out.println("BLOCK_TAG_PROMOTION_MOVE=ATOMIC_MOVE");
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(sibling, destination);
                System.out.println("BLOCK_TAG_PROMOTION_MOVE=SAME_FILESYSTEM_NO_REPLACE_FALLBACK");
            }
        } finally {
            Files.deleteIfExists(sibling);
        }
        byte[] persistentBytes = Files.readAllBytes(destination);
        require(Arrays.equals(tempBytes, persistentBytes), "Persistent bytes differ from accepted TEMP");
        require(sourceHash.equals(sha256(persistentBytes)), "Persistent hash mismatch");
        loadValidatedPersistentArtifact(root);
        require(Arrays.equals(tempBytes, Files.readAllBytes(source)), "PROMOTION_SOURCE_TEMP_CHANGED");
    }

    static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public record RuntimeEvidenceData(
        String advancementId, Set<String> greenCriteria,
        Map<String, Set<String>> criteriaByFamily, Map<String, Set<String>> criteriaBySource,
        Set<String> families, Set<String> sources
    ) {
        static RuntimeEvidenceData empty(String id) {
            return new RuntimeEvidenceData(id, new TreeSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
                new LinkedHashSet<>(), new LinkedHashSet<>());
        }
    }

    public static void validate(Path root, JsonObject artifact, Mode mode) throws IOException {
        Objects.requireNonNull(mode, "mode");
        String generated = PhaseABlockTagItemUsedOnBlockCertification.generateSnapshot(root);
        require(generated.equals(Files.readString(root.resolve(PhaseABlockTagItemUsedOnBlockCertification.SNAPSHOT))), "Stale catalog snapshot");
        String fingerprint = PhaseABlockTagItemUsedOnBlockCertification.fingerprint(root);
        Map<String, JsonObject> catalog = new TreeMap<>();
        for (JsonElement e : JsonParser.parseString(generated).getAsJsonObject().getAsJsonArray("cases")) {
            JsonObject c = e.getAsJsonObject(); catalog.put(key(c), c);
        }
        identity(artifact, fingerprint);
        String run = string(artifact, "runId");
        Set<String> seen = new TreeSet<>();
        require(artifact.has("entries") && artifact.get("entries").isJsonArray(), "Missing entries");
        for (JsonElement element : artifact.getAsJsonArray("entries")) {
            JsonObject receipt = element.getAsJsonObject(); String key = key(receipt);
            require(catalog.containsKey(key), "Unknown criterion: " + key);
            require(seen.add(key), "Duplicate criterion: " + key);
            identity(receipt, fingerprint); equal(receipt, "runId", run);
            equal(receipt, "result", "GREEN");
            equal(receipt, "boundary", "ServerPlayerGameMode.useItemOn");
            equal(receipt, "playerClass", "net.minecraft.server.level.ServerPlayer");
            equal(receipt, "gameMode", "SURVIVAL");
            equal(receipt, "hand", "MAIN_HAND");
            equal(receipt, "blockTagSampling", "POST_USE_CLICKED_POSITION");
            string(receipt, "playerUuid"); string(receipt, "clickedPosition");
            string(receipt, "blockStateBefore"); string(receipt, "blockStateAfter");
            string(receipt, "interactionResult");
            for (String field : List.of("joined", "connectionRegistered", "clientLoaded", "normalScheduler", "buildPermission", "productionPreconditions", "runtimeTagMembership", "interactionConsumesAction", "semanticMutation", "criterionAfter")) bool(receipt, field, true);
            bool(receipt, "criterionBefore", false);
            JsonObject c = catalog.get(key);
            for (String field : List.of("blockTag", "heldItem", "action", "trigger")) equal(receipt, field, string(c, field));
            require(receipt.get("requirementGroupIndex").getAsInt() == c.get("requirementGroupIndex").getAsInt(), "Wrong requirement group");
            int before = receipt.get("heldCountBefore").getAsInt(), after = receipt.get("heldCountAfter").getAsInt();
            require(before > 0 && after >= 0, "Invalid held counts");
            JsonObject proof = receipt.getAsJsonObject("actionProof");
            switch (string(c, "action")) {
                case "WAX_SIGN" -> { bool(proof, "waxedBefore", false); bool(proof, "waxedAfter", true); require(after == before - 1, "Honeycomb not consumed"); }
                case "DYE_SIGN" -> {
                    bool(proof, "textPresent", true); bool(proof, "waxedBefore", false);
                    require(!string(proof, "colorBefore").equals(string(proof, "colorAfter")), "No dye mutation");
                    equal(proof, "colorAfter", string(c, "heldItem").replace("minecraft:", "").replace("_dye", ""));
                    require(after == before - 1, "Dye not consumed");
                }
                case "GLOW_SIGN" -> { bool(proof, "textPresent", true); bool(proof, "waxedBefore", false); bool(proof, "glowingBefore", false); bool(proof, "glowingAfter", true); require(after == before - 1, "Ink not consumed"); }
                case "MAP_BANNER" -> { bool(proof, "mapCoversBanner", true); bool(proof, "markerBefore", false); bool(proof, "markerAfter", true); string(proof, "mapId"); require(after == before, "Map count changed"); }
                case "PLACE_FOOD" -> {
                    bool(proof, "placementAllowed", true); bool(proof, "campfireRecipe", true);
                    bool(proof, "useCampfireUnlocked", true); bool(proof, "lockedLandmark", false);
                    bool(proof, "slotEmptyBefore", true); equal(proof, "slotItemAfter", string(c, "heldItem"));
                    require(proof.get("slotCountAfter").getAsInt() == 1 && proof.get("slotIndex").getAsInt() >= 0 && proof.get("slotIndex").getAsInt() < 4, "No actual food placement");
                    require(after == before - 1, "Food not consumed");
                }
                case "GROW_OAK_SAPLING" -> {
                    bool(proof, "planted", true); bool(proof, "validSupport", true); bool(proof, "clearGrowthVolume", true);
                    bool(proof, "successfulTreeGrowth", true);
                    equal(proof, "mutationPosition", string(receipt, "clickedPosition"));
                    equal(proof, "mutationBlockBefore", "minecraft:oak_sapling");
                    require(proof.get("saplingStageBefore").getAsInt() == 1, "Expected stage-1 oak sapling");
                    equal(proof, "mutationBlockAfter", "minecraft:oak_log");
                    require(after == before - 1, "Bone meal not consumed");
                }
                case "BOTTLE_HONEY" -> {
                    bool(proof, "litCampfireBelow", true); bool(proof, "smokeyBefore", true); bool(proof, "smokeyAfter", true);
                    require(proof.get("honeyLevelBefore").getAsInt() == 5 && proof.get("honeyLevelAfter").getAsInt() == 0, "Honey level unchanged");
                    bool(proof, "honeyBottleProduced", true);
                }
                default -> throw new IllegalStateException("Unsupported semantic proof");
            }
        }
        if (mode == Mode.TEMP_PROMOTABLE) require(seen.size() == 30 && seen.equals(PhaseABlockTagItemUsedOnBlockCertification.expectedKeys()), "TEMP promotable requires exact30");
        if (mode == Mode.PERSISTENT) require(seen.size() == 30 && seen.equals(PhaseABlockTagItemUsedOnBlockCertification.expectedKeys()), "Persistent requires exact30");
    }
    private static void identity(JsonObject o, String fingerprint) {
        equal(o, "family", PhaseABlockTagItemUsedOnBlockCertification.FAMILY);
        equal(o, "source", PhaseABlockTagItemUsedOnBlockCertification.SOURCE);
        equal(o, "catalogFingerprint", fingerprint); equal(o, "minecraftVersion", "26.2");
        equal(o, "compatibilityMarker", "compat_26_2_r15"); string(o, "runId");
    }
    static String key(JsonObject o) { return string(o, "advancementId") + "#" + string(o, "criterion"); }
    static String string(JsonObject o, String field) {
        require(o != null && o.has(field) && o.get(field).isJsonPrimitive() && o.getAsJsonPrimitive(field).isString(), "Missing string: " + field);
        String value = o.get(field).getAsString(); require(!value.isBlank(), "Blank " + field); return value;
    }
    static void equal(JsonObject o, String field, String expected) { require(expected.equals(string(o, field)), "Wrong " + field); }
    static void bool(JsonObject o, String field, boolean expected) {
        require(o != null && o.has(field) && o.get(field).isJsonPrimitive() && o.getAsJsonPrimitive(field).isBoolean() && o.get(field).getAsBoolean() == expected, "Wrong " + field);
    }
}
