package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.client.ExternalPack;
import com.diskree.achievetodo.client.ExternalPackCompatibility;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;

/** Verified frozen copy followed by the unchanged accepted production conversion. */
public final class PublicationRuntimeSetup {
    private static final String FROZEN_SHA256 = "8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70";

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path frozen = root.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        byte[] source = Files.readAllBytes(frozen);
        require(sha(source).equals(FROZEN_SHA256), "Frozen BACAP source drift");
        Path copy = root.resolve("build/tmp/publication_runtime_input/bacap.zip");
        Files.createDirectories(copy.getParent());
        Files.copy(frozen, copy, StandardCopyOption.REPLACE_EXISTING);
        require(sha(Files.readAllBytes(copy)).equals(FROZEN_SHA256), "Frozen BACAP copy drift");

        Path runtime = root.resolve("build/run/pre26Smoke01/world/datapacks/bacap.zip");
        Files.createDirectories(runtime.getParent());
        ExternalPackCompatibility.copyForWorld(copy, runtime, ExternalPack.BACAP);
        require(ExternalPackCompatibility.isCompatibleWorldCopy(runtime, ExternalPack.BACAP), "Runtime compatibility marker mismatch");
        var expected = JsonParser.parseString(Files.readString(root.resolve(
            "reference/phase_a_planning/final19/raider_production_output_scope.json")))
            .getAsJsonObject().getAsJsonObject("outputFingerprints");
        require(expected.size() == 1229, "Wrong semantic bridge size");
        try (var zip = new ZipFile(runtime.toFile())) {
            for (var row : expected.entrySet()) {
                var entry = zip.getEntry(row.getKey());
                require(entry != null, "Missing runtime definition: " + row.getKey());
                try (var input = zip.getInputStream(entry)) {
                    require(sha(input.readAllBytes()).equals(row.getValue().getAsJsonObject()
                        .get("productionOutputSha256").getAsString()), "Runtime definition drift: " + row.getKey());
                }
            }
        }
        require(sha(Files.readAllBytes(frozen)).equals(FROZEN_SHA256), "Frozen BACAP changed");
        System.out.println("PUBLICATION_RUNTIME_READY frozen=" + FROZEN_SHA256
            + " copy=" + sha(Files.readAllBytes(copy)) + " runtime=" + sha(Files.readAllBytes(runtime))
            + " verifiedDefinitions=1229");
    }
}
