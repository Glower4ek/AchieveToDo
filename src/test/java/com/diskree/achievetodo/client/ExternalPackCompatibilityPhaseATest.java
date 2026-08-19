package com.diskree.achievetodo.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalPackCompatibilityPhaseATest {

    @TempDir
    Path tempDir;

    @Test
    void treatsPinnedHistoricalSourceAsSourceButMarkerCopyAsWorldCopyOnly() throws Exception {
        ExternalPack externalPack = ExternalPack.BACAP;
        Path rawPack = Path.of("reference", "phase_a_preservation", "files", "final", externalPack.getFileName());
        assertTrue(Files.exists(rawPack));
        assertTrue(ExternalPackCompatibility.isPinnedHistoricalSource(rawPack, externalPack));

        Path currentCompatiblePack = tempDir.resolve("bacap-current.zip");
        writeMarkerOnlyPack(
            currentCompatiblePack,
            "achievetodo_compatibility/compat_26_2.properties",
            "compat_26_2_r15",
            externalPack.getFileName(),
            externalPack.getSha1(),
            ExternalPackCompatibility.currentRootOverrideSha1()
        );
        assertFalse(ExternalPackCompatibility.isPinnedHistoricalSource(currentCompatiblePack, externalPack));
        assertTrue(ExternalPackCompatibility.isCompatibleWorldCopy(currentCompatiblePack, externalPack));

        Path staleCompatiblePack = tempDir.resolve("bacap-stale.zip");
        writeMarkerOnlyPack(
            staleCompatiblePack,
            "achievetodo_compatibility/phase_b_26_2.properties",
            "phase_b_26_2_r6",
            externalPack.getFileName(),
            externalPack.getSha1(),
            ExternalPackCompatibility.currentRootOverrideSha1()
        );
        assertFalse(ExternalPackCompatibility.isCompatibleWorldCopy(staleCompatiblePack, externalPack));

        Path wrongSourceCompatiblePack = tempDir.resolve("bacap-wrong-source.zip");
        writeMarkerOnlyPack(
            wrongSourceCompatiblePack,
            "achievetodo_compatibility/compat_26_2.properties",
            "compat_26_2_r15",
            externalPack.getFileName(),
            "deadbeef",
            ExternalPackCompatibility.currentRootOverrideSha1()
        );
        assertFalse(ExternalPackCompatibility.isCompatibleWorldCopy(wrongSourceCompatiblePack, externalPack));

        Path wrongRootsCompatiblePack = tempDir.resolve("bacap-wrong-roots.zip");
        writeMarkerOnlyPack(
            wrongRootsCompatiblePack,
            "achievetodo_compatibility/compat_26_2.properties",
            "compat_26_2_r13",
            externalPack.getFileName(),
            externalPack.getSha1(),
            "deadbeef"
        );
        assertFalse(ExternalPackCompatibility.isCompatibleWorldCopy(wrongRootsCompatiblePack, externalPack));
    }

    @Test
    void ensureWorldPacksUpToDateRepairsStaleWorldPackToCurrentCompatibleCopy() throws Exception {
        ExternalPack externalPack = ExternalPack.BACAP;
        Path globalDir = tempDir.resolve("global");
        Path worldDir = tempDir.resolve("world");
        Files.createDirectories(globalDir);
        Files.createDirectories(worldDir);

        Path rawPack = Path.of("reference", "phase_a_preservation", "files", "final", externalPack.getFileName());
        Path globalPack = globalDir.resolve(externalPack.getFileName());
        Files.copy(rawPack, globalPack);
        assertTrue(ExternalPackCompatibility.isPinnedHistoricalSource(globalPack, externalPack));

        Path worldPack = worldDir.resolve(externalPack.getFileName());
        writeMarkerOnlyPack(
            worldPack,
            "achievetodo_compatibility/phase_b_26_2.properties",
            "phase_b_26_2_r6",
            externalPack.getFileName(),
            externalPack.getSha1(),
            ExternalPackCompatibility.currentRootOverrideSha1()
        );

        assertEquals(
            ExternalPackCompatibility.WorldPackSyncResult.UPDATED,
            ExternalPackCompatibility.ensureWorldPacksUpToDate(globalDir, worldDir, true)
        );
        assertTrue(ExternalPackCompatibility.isCompatibleWorldCopy(worldPack, externalPack));
    }

    @Test
    void ensureWorldPacksUpToDateKeepsMissingPackSemanticsDistinct() throws Exception {
        Path globalDir = tempDir.resolve("global");
        Path worldDir = tempDir.resolve("world");
        Files.createDirectories(globalDir);
        Files.createDirectories(worldDir);

        assertEquals(
            ExternalPackCompatibility.WorldPackSyncResult.NO_KNOWN_PACKS,
            ExternalPackCompatibility.ensureWorldPacksUpToDate(globalDir, worldDir, false)
        );
        assertEquals(
            ExternalPackCompatibility.WorldPackSyncResult.MISSING_REQUIRED_PACK,
            ExternalPackCompatibility.ensureWorldPacksUpToDate(globalDir, worldDir, true)
        );
    }

    @Test
    void rewritesOnlyKnownLegacyGameruleCasesAndPreservesUnrelatedCommands() throws Exception {
        String converted = convertFunction(String.join("\n",
            "say first",
            "gamerule commandBlockOutput false",
            "execute in minecraft:overworld run gamerule maxCommandForkCount 128",
            "execute as @p run gamerule keepInventory true",
            "execute store result score time bac_current_time run time query daytime",
            "say last"
        ));

        String[] lines = converted.split("\\R", -1);
        assertEquals("say first", lines[0]);
        assertEquals("# achievetodo compatibility: removed unsupported gamerule command", lines[1]);
        assertEquals("# achievetodo compatibility: removed unsupported gamerule command", lines[2]);
        assertEquals("execute as @p run gamerule keepInventory true", lines[3]);
        assertEquals("execute store result score time bac_current_time run time of minecraft:overworld query minecraft:day", lines[4]);
        assertEquals("say last", lines[5]);
    }

    private static String convertFunction(String function) throws Exception {
        Method convertFunction = ExternalPackCompatibility.class.getDeclaredMethod("convertFunction", String.class);
        convertFunction.setAccessible(true);
        Object conversionResult = convertFunction.invoke(null, function);
        Method text = conversionResult.getClass().getDeclaredMethod("text");
        text.setAccessible(true);
        return (String) text.invoke(conversionResult);
    }

    private static void writeMarkerOnlyPack(
        Path pack,
        String markerEntry,
        String version,
        String fileName,
        String sourceSha1,
        String rootOverrideSha1
    ) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("version", version);
        properties.setProperty("fileName", fileName);
        properties.setProperty("sourceSha1", sourceSha1);
        properties.setProperty("rootOverrideSha1", rootOverrideSha1);

        ByteArrayOutputStream markerBytes = new ByteArrayOutputStream();
        properties.store(markerBytes, null);

        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(pack))) {
            output.putNextEntry(new ZipEntry("pack.mcmeta"));
            output.write("{}".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();

            output.putNextEntry(new ZipEntry(markerEntry));
            output.write(markerBytes.toByteArray());
            output.closeEntry();
        }
    }
}
