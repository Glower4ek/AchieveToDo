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
        assertTrue(ExternalPackCompatibility.isPinnedHistoricalSource(PhaseBPackTestFixtures.current(), ExternalPack.BACAP));
        assertFalse(ExternalPackCompatibility.isPinnedHistoricalSource(PhaseBPackTestFixtures.historical(), ExternalPack.BACAP));
        Path valid = PhaseBPackTestFixtures.currentCopy(tempDir);
        PhaseBPackTestFixtures.assertIsolatedMarkerNegatives(tempDir, valid);
    }

    @Test
    void ensureWorldPacksUpToDateRepairsStaleWorldPackToCurrentCompatibleCopy() throws Exception {
        Path global = Files.createDirectories(tempDir.resolve("global"));
        Path world = Files.createDirectories(tempDir.resolve("world"));
        Files.copy(PhaseBPackTestFixtures.current(), global.resolve(ExternalPack.BACAP.getFileName()));
        Path valid = PhaseBPackTestFixtures.currentCopy(tempDir);
        var stale = PhaseBPackTestFixtures.marker(valid); stale.setProperty("version", "compat_26_2_r18");
        Path target = world.resolve(ExternalPack.BACAP.getFileName());
        PhaseBPackTestFixtures.writeMarker(target, PhaseBPackTestFixtures.MARKER, stale);
        assertFalse(ExternalPackCompatibility.isCurrentWorldPack(target, ExternalPack.BACAP));
        assertEquals(ExternalPackCompatibility.WorldPackSyncResult.UPDATED,
            ExternalPackCompatibility.ensureWorldPacksUpToDate(global, world, true));
        PhaseBPackTestFixtures.assertCurrentMarker(target);
        assertTrue(ExternalPackCompatibility.isCurrentWorldPack(target, ExternalPack.BACAP));
        assertEquals(ExternalPackCompatibility.WorldPackSyncResult.ALREADY_CURRENT,
            ExternalPackCompatibility.ensureWorldPacksUpToDate(global, world, true));
        Path oldGlobal = Files.createDirectories(tempDir.resolve("old-global"));
        Path oldWorld = Files.createDirectories(tempDir.resolve("old-world"));
        Files.copy(PhaseBPackTestFixtures.historical(), oldGlobal.resolve(ExternalPack.BACAP.getFileName()));
        Path staleTarget = oldWorld.resolve(ExternalPack.BACAP.getFileName());
        PhaseBPackTestFixtures.writeMarker(staleTarget, PhaseBPackTestFixtures.MARKER, stale);
        byte[] before = Files.readAllBytes(staleTarget);
        assertEquals(ExternalPackCompatibility.WorldPackSyncResult.MISSING_REQUIRED_PACK,
            ExternalPackCompatibility.ensureWorldPacksUpToDate(oldGlobal, oldWorld, true));
        org.junit.jupiter.api.Assertions.assertArrayEquals(before, Files.readAllBytes(staleTarget));
        PhaseBPackTestFixtures.historical(); PhaseBPackTestFixtures.current();
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
