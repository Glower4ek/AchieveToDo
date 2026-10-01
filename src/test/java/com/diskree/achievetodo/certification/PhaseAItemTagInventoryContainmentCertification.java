package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.zip.ZipFile;

/** Verifies the frozen four-case component-containment frontier without widening its old supported catalog. */
public final class PhaseAItemTagInventoryContainmentCertification {
    public static final Path SNAPSHOT = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_inventory_containment_case_catalog.json");
    private static final String BACAP = "reference/phase_a_preservation/files/final/bacap.zip";
    private static final String BACAP_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";
    public static final List<Case> CASES = List.of(
        new Case("blazeandcave:animal/flamboyant_range", "bundle", "minecraft:bundles", "bundle_contents contains 4 of each of 16 dyes", "data/blazeandcave/advancement/animal/flamboyant_range.json", "e2d07a16067b9a7fff0b7bcdf2d3733764851e7fb58fdf9fe25ce3012296d1ee"),
        new Case("blazeandcave:animal/fractal", "bundle", "minecraft:bundles", "bundle_contents contains a 16-level nested bundle chain", "data/blazeandcave/advancement/animal/fractal.json", "11028bbd6a217cf4da45ede0d2469b04e26974fa1208c5f5167d97b056605747"),
        new Case("blazeandcave:end/organizational_wizard", "shulker_box", "minecraft:shulker_boxes", "custom_name equals Blocks", "data/blazeandcave/advancement/end/organizational_wizard.json", "5d39597e70ef53dd1d3ca89b7e1e4ce2ce40bbc3025471e7d5df637f48cd5328"),
        new Case("blazeandcave:redstone/sculker_box", "sculker_box", "minecraft:shulker_boxes", "container has 27 slots of 64 sculk", "data/blazeandcave/advancement/redstone/sculker_box.json", "221530a4f87b368a26261ab93a5bffe848b8030116fbb2aa638168a9f534787d")
    );
    private PhaseAItemTagInventoryContainmentCertification() { }

    public static void validate(Path root) throws Exception {
        require(BACAP_SHA256.equals(sha(Files.readAllBytes(root.resolve(BACAP)))), "Frozen BACAP hash changed");
        JsonObject snapshot = JsonParser.parseString(Files.readString(root.resolve(SNAPSHOT))).getAsJsonObject();
        require("ITEM_TAG_INVENTORY_CONTAINMENT".equals(snapshot.get("family").getAsString()), "family mismatch");
        JsonArray entries = snapshot.getAsJsonArray("cases"); require(entries.size() == CASES.size(), "case count mismatch");
        try (ZipFile zip = new ZipFile(root.resolve(BACAP).toFile(), StandardCharsets.UTF_8)) {
            for (int index = 0; index < CASES.size(); index++) {
                Case definition = CASES.get(index); JsonObject entry = entries.get(index).getAsJsonObject();
                require(definition.key().equals(entry.get("advancementId").getAsString() + "#" + entry.get("criterion").getAsString()), "snapshot key mismatch");
                require(definition.itemTag().equals(entry.get("itemTag").getAsString()) && definition.predicate().equals(entry.get("predicate").getAsString()), "snapshot predicate mismatch");
                try (InputStream input = zip.getInputStream(zip.getEntry(definition.sourcePath()))) {
                    require(definition.sourceSha256().equals(sha(input.readAllBytes())), "frozen source changed " + definition.key());
                }
            }
        }
    }
    private static String sha(byte[] bytes) throws Exception { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    public record Case(String advancementId, String criterion, String itemTag, String predicate, String sourcePath, String sourceSha256) { String key() { return advancementId + "#" + criterion; } }
}
