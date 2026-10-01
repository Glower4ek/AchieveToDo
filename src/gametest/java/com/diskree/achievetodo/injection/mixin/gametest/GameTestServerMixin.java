package com.diskree.achievetodo.injection.mixin.gametest;

import com.diskree.achievetodo.ability.ProgressionModeType;
import com.diskree.achievetodo.client.ExternalPack;
import com.diskree.achievetodo.client.ExternalPackCompatibility;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.util.MixinCasting;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Mixin(GameTestServer.class)
public abstract class GameTestServerMixin {
    private static final String BACAP_ZIP = "bacap.zip";

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
            if (!"45b8bb0076bbf5b92fde7dc9590c6686937abbc0".equals(hash(sourceBytes, "SHA-1"))
                || !"8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70".equals(hash(sourceBytes, "SHA-256"))) {
                throw new IllegalStateException("Frozen BACAP source identity mismatch");
            }
            ExternalPackCompatibility.copyForWorld(sourcePack, targetPack, ExternalPack.BACAP);
            if (!Files.isRegularFile(targetPack) || !ExternalPackCompatibility.isCompatibleWorldCopy(targetPack, ExternalPack.BACAP)) {
                throw new IllegalStateException("Invalid production-compatible GameTest BACAP copy");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to install BACAP datapack into GameTest world", e);
        }
    }

    private static Path resolveBacapPack() {
        String root = System.getProperty("achievetodo.phaseA.projectRoot");
        if (root == null || root.isBlank()) {
            throw new IllegalStateException("Missing Phase A projectRoot");
        }
        Path configuredRoot = Path.of(root).toAbsolutePath().normalize();
        for (Path candidateRoot = configuredRoot; candidateRoot != null; candidateRoot = candidateRoot.getParent()) {
            Path source = candidateRoot.resolve("reference/phase_a_preservation/files/final/" + BACAP_ZIP);
            if (Files.isRegularFile(source)) return source;
        }
        throw new IllegalStateException("Missing frozen BACAP under configured root or its ancestors: " + configuredRoot);
    }

    private static String hash(byte[] bytes, String algorithm) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing source hash algorithm: " + algorithm, e);
        }
    }
}
