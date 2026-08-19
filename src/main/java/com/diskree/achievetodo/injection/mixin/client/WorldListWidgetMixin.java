package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.client.ExternalPack;
import com.diskree.achievetodo.client.ExternalPackCompatibility;
import com.diskree.achievetodo.client.CompatibilityJoinGate;
import com.diskree.achievetodo.client.Utils;
import com.diskree.achievetodo.client.gui.ErrorScreen;
import com.diskree.achievetodo.client.gui.ExternalPackDownloader;
import com.diskree.achievetodo.server.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListWidgetMixin {

    @Unique
    private final CompatibilityJoinGate achievetodo$compatibilityJoinGate = new CompatibilityJoinGate();

    @Unique
    private void showUnknownError() {
        minecraft.setScreenAndShow(new ErrorScreen(screen, "error.unknown"));
    }

    @Unique
    private void showIntegrityCheckFailed() {
        minecraft.setScreenAndShow(new ErrorScreen(screen, "error.integrity_check_failed"));
    }

    @Unique
    private void showMissingRequiredPack() {
        minecraft.setScreenAndShow(new ErrorScreen(screen, "error.missing_required_pack"));
    }

    @Shadow
    @Final
    LevelSummary summary;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private Screen screen;

    @Shadow
    public abstract void joinWorld();

    @Inject(
        method = "joinWorld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;createWorldOpenFlows()Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void checkPacks(@NotNull CallbackInfo ci) {
        if (!achievetodo$compatibilityJoinGate.shouldRunPreparation()) {
            return;
        }

        Path worldPacksDirectory;
        Path levelDataFile;
        try (LevelStorageSource.LevelStorageAccess session = minecraft.getLevelSource().validateAndCreateAccess(summary.getLevelId())) {
            worldPacksDirectory = session.getLevelPath(LevelResource.DATAPACK_DIR);
            levelDataFile = session.getLevelPath(LevelResource.LEVEL_DATA_FILE);
        } catch (Exception e) {
            showUnknownError();
            ci.cancel();
            return;
        }
        if (!ExternalPackCompatibility.requiresHistoricalCompatibility(levelDataFile)) {
            return;
        }
        Path globalPacksDirectory = new File(minecraft.gameDirectory, "datapacks").toPath();
        if (Files.notExists(globalPacksDirectory)) {
            try {
                Files.createDirectory(globalPacksDirectory);
            } catch (IOException e) {
                showUnknownError();
                ci.cancel();
                return;
            }
        }
        List<ExternalPack> requiredPacks = ExternalPackCompatibility.findRequiredExternalPacks(worldPacksDirectory);
        if (requiredPacks.isEmpty()) {
            showMissingRequiredPack();
            ci.cancel();
            return;
        }
        prepareHistoricalSourcesAndJoin(requiredPacks, 0, globalPacksDirectory, worldPacksDirectory);
        ci.cancel();
    }

    @Unique
    private void prepareHistoricalSourcesAndJoin(
        @NotNull List<ExternalPack> requiredPacks,
        int index,
        @NotNull Path globalPacksDirectory,
        @NotNull Path worldPacksDirectory
    ) {
        if (index >= requiredPacks.size()) {
            ExternalPackCompatibility.WorldPackSyncResult result = ExternalPackCompatibility.ensureWorldPacksUpToDate(
                globalPacksDirectory,
                worldPacksDirectory,
                true
            );
            if (result == ExternalPackCompatibility.WorldPackSyncResult.ALREADY_CURRENT
                || result == ExternalPackCompatibility.WorldPackSyncResult.UPDATED) {
                achievetodo$compatibilityJoinGate.runPreparedJoin(this::joinWorld);
                return;
            }
            if (result == ExternalPackCompatibility.WorldPackSyncResult.INTEGRITY_CHECK_FAILED) {
                achievetodo$compatibilityJoinGate.reset();
                showIntegrityCheckFailed();
                return;
            }
            if (result == ExternalPackCompatibility.WorldPackSyncResult.MISSING_REQUIRED_PACK
                || result == ExternalPackCompatibility.WorldPackSyncResult.NO_KNOWN_PACKS) {
                achievetodo$compatibilityJoinGate.reset();
                showMissingRequiredPack();
                return;
            }
            achievetodo$compatibilityJoinGate.reset();
            showUnknownError();
            return;
        }

        ExternalPack externalPack = requiredPacks.get(index);
        Path globalPack = globalPacksDirectory.resolve(externalPack.getFileName());
        if (ExternalPackCompatibility.isPinnedHistoricalSource(globalPack, externalPack)) {
            prepareHistoricalSourcesAndJoin(requiredPacks, index + 1, globalPacksDirectory, worldPacksDirectory);
            return;
        }

        minecraft.setScreenAndShow(new ExternalPackDownloader(screen, externalPack, isFileDownloaded -> {
            if (!isFileDownloaded) {
                achievetodo$compatibilityJoinGate.reset();
                return;
            }
            if (!ExternalPackCompatibility.isPinnedHistoricalSource(globalPack, externalPack)) {
                achievetodo$compatibilityJoinGate.reset();
                showMissingRequiredPack();
                return;
            }
            prepareHistoricalSourcesAndJoin(requiredPacks, index + 1, globalPacksDirectory, worldPacksDirectory);
        }, true));
    }
}
