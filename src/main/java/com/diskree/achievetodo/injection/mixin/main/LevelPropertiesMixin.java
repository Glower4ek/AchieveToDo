package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.server.Constants;
import com.diskree.achievetodo.util.MixinCasting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimaryLevelData.class)
public class LevelPropertiesMixin {

    @Shadow
    private LevelSettings settings;

    @Inject(
        method = "setTagData",
        at = @At("RETURN")
    )
    private void saveConfigName(
        CompoundTag levelNbt,
        java.util.UUID singlePlayerUuid,
        CallbackInfo ci
    ) {
        LevelInfoExtension levelInfoExtension = MixinCasting.levelInfo(settings);
        String configName = levelInfoExtension.achievetodo$getConfigName();
        if (configName != null) {
            levelNbt.putString(Constants.NbtKey.LEVEL_CONFIG_NAME, configName);
        }
    }
}
