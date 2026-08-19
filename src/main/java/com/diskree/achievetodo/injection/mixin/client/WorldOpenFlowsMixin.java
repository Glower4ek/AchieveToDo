package com.diskree.achievetodo.injection.mixin.client;

import com.diskree.achievetodo.client.ExternalPackCompatibility;
import com.diskree.achievetodo.client.gui.ErrorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.nio.file.Path;

@Mixin(WorldOpenFlows.class)
public abstract class WorldOpenFlowsMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private LevelStorageSource levelSource;

    @Inject(
        method = "openWorld",
        at = @At("HEAD"),
        cancellable = true
    )
    private void achievetodo$refreshWorldDatapacks(@NotNull String levelId, Runnable onCancel, CallbackInfo ci) {
        Path worldPacksDirectory;
        Path levelDataFile;
        try (LevelStorageSource.LevelStorageAccess session = levelSource.validateAndCreateAccess(levelId)) {
            worldPacksDirectory = session.getLevelPath(LevelResource.DATAPACK_DIR);
            levelDataFile = session.getLevelPath(LevelResource.LEVEL_DATA_FILE);
        } catch (Exception e) {
            minecraft.setScreenAndShow(new ErrorScreen(null, "error.unknown"));
            ci.cancel();
            return;
        }
        boolean compatibilityRequired = ExternalPackCompatibility.requiresHistoricalCompatibility(levelDataFile);

        ExternalPackCompatibility.WorldPackSyncResult result = ExternalPackCompatibility.ensureWorldPacksUpToDate(
            new File(minecraft.gameDirectory, "datapacks").toPath(),
            worldPacksDirectory,
            compatibilityRequired
        );
        if (result == ExternalPackCompatibility.WorldPackSyncResult.ALREADY_CURRENT
            || result == ExternalPackCompatibility.WorldPackSyncResult.NO_KNOWN_PACKS
            || result == ExternalPackCompatibility.WorldPackSyncResult.UPDATED) {
            return;
        }

        String errorKey;
        if (result == ExternalPackCompatibility.WorldPackSyncResult.INTEGRITY_CHECK_FAILED) {
            errorKey = "error.integrity_check_failed";
        } else if (result == ExternalPackCompatibility.WorldPackSyncResult.MISSING_REQUIRED_PACK) {
            errorKey = "error.missing_required_pack";
        } else {
            errorKey = "error.unknown";
        }
        minecraft.setScreenAndShow(new ErrorScreen(null, errorKey));
        ci.cancel();
    }
}
