package com.diskree.achievetodo.injection.mixin.gametest;

import com.diskree.achievetodo.ability.ProgressionModeType;
import com.diskree.achievetodo.client.ExternalPack;
import com.diskree.achievetodo.client.ExternalPackCompatibility;
import com.diskree.achievetodo.client.InternalPack;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.util.MixinCasting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
    private static final String BACAP_ZIP = "bacap.zip";
    private static final String BACAP_SOURCE_SHA1 = "14da3f07b5467e8b59ffc0253fd8212c938cd739";
    private static final String BACAP_SOURCE_SHA256 = "c71d1aa1a84dbe00a3f85a42144b46214c4669a3cccf07ff66631d28f16a99b2";

    private static boolean normalCombatFixture() {
        return Boolean.getBoolean("achievetodo.s03.normalFixture");
    }

    private static List<String> normalPacks() {
        return List.of("vanilla", "achievetodo", "fabric-convention-tags-v2", "fabric-gametest-api-v1",
            ExternalPack.BACAP.getDatapackName(), InternalPack.BACAP_OVERRIDE.getDatapackName());
    }

    // Native GameTest selects every available builtin. Optional worldgen overrides are invalid
    // without their companion packs. Opt in only for this disposable normal D-S03 fixture.
    @Redirect(method = "create", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/packs/repository/PackRepository;getAvailableIds()Ljava/util/Collection;"))
    private static Collection<String> selectNormalCombatPacks(PackRepository repository) {
        if (!normalCombatFixture()) return repository.getAvailableIds();
        List<String> selected = normalPacks();
        for (String id : selected) {
            if (!repository.isAvailable(id)) throw new IllegalStateException("Missing normal GameTest pack: " + id);
        }
        System.out.println("D_S03_NORMAL_PACKS selected=" + selected);
        return selected;
    }

    @ModifyArg(method = "create", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/WorldLoader$PackConfig;<init>(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/WorldDataConfiguration;ZZ)V"), index = 1)
    private static WorldDataConfiguration normalCombatPackOrder(WorldDataConfiguration configuration) {
        if (!normalCombatFixture()) return configuration;
        List<String> selected = normalPacks();
        List<String> actual = configuration.dataPacks().getEnabled();
        if (actual.size() != selected.size() || !new HashSet<>(actual).equals(new HashSet<>(selected))) {
            throw new IllegalStateException("Unexpected normal GameTest pack selection: " + actual);
        }
        return new WorldDataConfiguration(new DataPackConfig(selected, configuration.dataPacks().getDisabled()),
            configuration.enabledFeatures());
    }

    @Inject(
        method = "lambda$create$1",
        at = @At("HEAD")
    )
    private static void seedDefaultConfigName(
        LevelSettings levelSettings,
        WorldLoader.DataLoadContext dataLoadContext,
        CallbackInfoReturnable<WorldLoader.DataLoadOutput> cir
    ) {
        LevelInfoExtension levelInfoExtension = MixinCasting.levelInfo(levelSettings);
        levelInfoExtension.achievetodo$setConfigName(ProgressionModeType.getDefaultMode().getName());
        Path configDirectory = FabricLoader.getInstance().getConfigDir().resolve("achievetodo");
        // The disposable harness supplies an existing production-written preset. Keep UNKNOWN/SAVED
        // unchanged: ConfigurationIO must reject a missing preset rather than reclassifying a world.
        var configuration = LevelInfoExtension.ConfigurationIO.load(configDirectory,
            levelInfoExtension.achievetodo$getConfigName(), 0L,
            levelInfoExtension.achievetodo$getConfigurationOrigin());
        System.out.println("D_S03_CONFIG_BOOTSTRAP origin=" + levelInfoExtension.achievetodo$getConfigurationOrigin()
            + " directory=" + configDirectory + " abilities=" + configuration.size());
    }

    @Inject(
        method = "create",
        at = @At("HEAD")
    )
    private static void installBacapPack(
        Thread serverThread,
        LevelStorageSource.LevelStorageAccess levelStorageAccess,
        PackRepository packRepository,
        Optional<String> testSelection,
        boolean verify,
        int repeatCount,
        CallbackInfoReturnable<GameTestServer> cir
    ) {
        Path sourcePack = resolveBacapPack();
        Path datapacksDirectory = levelStorageAccess.getLevelPath(LevelResource.DATAPACK_DIR);
        Path targetPack = datapacksDirectory.resolve(BACAP_ZIP);
        try {
            byte[] sourceBytes = Files.readAllBytes(sourcePack);
            if (sourceBytes.length != 3135424
                || !BACAP_SOURCE_SHA1.equals(hash(sourceBytes, "SHA-1"))
                || !BACAP_SOURCE_SHA256.equals(hash(sourceBytes, "SHA-256"))
                || !BACAP_SOURCE_SHA1.equals(ExternalPack.BACAP.getSha1())) {
                throw new IllegalStateException("Canonical Phase B BACAP source identity mismatch");
            }
            verifySourceZip(sourcePack);
            System.out.println("D_S03_BACAP_SOURCE sha1=" + BACAP_SOURCE_SHA1
                + " sha256=" + BACAP_SOURCE_SHA256 + " bytes=" + sourceBytes.length + " entries=7091 CRC=PASS");
            ExternalPackCompatibility.copyForWorld(sourcePack, targetPack, ExternalPack.BACAP);
            if (!Files.isRegularFile(targetPack) || !ExternalPackCompatibility.isCurrentWorldPack(targetPack, ExternalPack.BACAP)) {
                throw new IllegalStateException("Invalid production-compatible GameTest BACAP copy");
            }
            System.out.println("D_S03_BACAP_WORLD_COPY compatible="
                + ExternalPackCompatibility.isCompatibleWorldCopy(targetPack, ExternalPack.BACAP)
                + " nativeSource=" + ExternalPackCompatibility.isPinnedHistoricalSource(targetPack, ExternalPack.BACAP)
                + " path=" + targetPack);
        } catch (IOException e) {
            throw new RuntimeException("Failed to install BACAP datapack into GameTest world", e);
        }
    }

    private static Path resolveBacapPack() {
        String root = System.getProperty("achievetodo.phaseA.projectRoot");
        if (root == null || root.isBlank()) {
            throw new IllegalStateException("Missing trusted GameTest projectRoot");
        }
        Path configuredRoot = Path.of(root).toAbsolutePath().normalize();
        Path source = configuredRoot.resolve("src/test/resources/phase_b_certification/bacap_1_21_pinned.zip");
        if (Files.isRegularFile(source) && Files.isReadable(source)) {
            return source;
        }
        throw new IllegalStateException("Missing canonical Phase B BACAP under trusted root: " + configuredRoot);
    }

    private static void verifySourceZip(Path source) throws IOException {
        HashSet<String> names = new HashSet<>();
        try (ZipFile zip = new ZipFile(source.toFile())) {
            var entries = zip.entries();
            byte[] buffer = new byte[8192];
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!names.add(entry.getName())) throw new IOException("Duplicate BACAP ZIP entry");
                CRC32 crc = new CRC32();
                long size = 0;
                try (InputStream input = zip.getInputStream(entry)) {
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        crc.update(buffer, 0, read);
                        size += read;
                    }
                }
                if (size != entry.getSize() || crc.getValue() != entry.getCrc()) {
                    throw new IOException("Invalid BACAP ZIP entry: " + entry.getName());
                }
            }
        }
        if (names.size() != 7091) throw new IOException("Incorrect BACAP ZIP entry count");
    }

    private static String hash(byte[] bytes, String algorithm) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing source hash algorithm: " + algorithm, e);
        }
    }
}
