package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.BuildConfig;
import com.diskree.achievetodo.client.Utils;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.server.Constants;
import com.diskree.achievetodo.util.MixinCasting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.Main;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Main.class)
public class MainMixin {

    @WrapOperation(
        method = "createNewWorldData(Lnet/minecraft/server/dedicated/DedicatedServerSettings;Lnet/minecraft/server/WorldLoader$DataLoadContext;Lnet/minecraft/core/Registry;ZZ)Lnet/minecraft/server/WorldLoader$DataLoadOutput;",
        at = @At(
            value = "NEW",
            target = "(Ljava/lang/String;Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/LevelSettings$DifficultySettings;ZLnet/minecraft/world/level/WorldDataConfiguration;)Lnet/minecraft/world/level/LevelSettings;"
        )
    )
    private static LevelSettings readConfigNameFromServerProperties(
        String name,
        GameType gameMode,
        LevelSettings.DifficultySettings difficultySettings,
        boolean allowCommands,
        WorldDataConfiguration dataConfiguration,
        @NotNull Operation<LevelSettings> original,
        @Local @NotNull DedicatedServerProperties serverPropertiesHandler
    ) {
        String configName = serverPropertiesHandler.get(Constants.ConfigKey.SERVER_CONFIG_PROPERTY_NAME, "");
        if (configName == null || configName.isEmpty()) {
            throw new IllegalStateException(
                "You must set " + Constants.ConfigKey.SERVER_CONFIG_PROPERTY_NAME +
                    " with selected configuration in your `server.properties` file!" +
                    " Check out the `Server-side setup` section in the mod description: " +
                    Utils.buildModrinthModUrl(BuildConfig.MOD_ID)
            );
        }
        LevelSettings levelInfo = original.call(
            name, gameMode, difficultySettings, allowCommands, dataConfiguration
        );
        LevelInfoExtension levelInfoExtension = MixinCasting.levelInfo(levelInfo);
        levelInfoExtension.achievetodo$setConfigName(configName);
        return levelInfo;
    }
}
