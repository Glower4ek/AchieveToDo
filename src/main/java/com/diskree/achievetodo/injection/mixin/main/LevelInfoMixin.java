package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.BuildConfig;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension;
import com.diskree.achievetodo.server.Constants;
import com.diskree.achievetodo.util.MixinCasting;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Dynamic;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.LevelSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.Objects;

@Mixin(LevelSettings.class)
public abstract class LevelInfoMixin implements LevelInfoExtension {
    @Unique
    private String configName;

    @Unique
    private ConfigurationOrigin achievetodo$configurationOrigin = ConfigurationOrigin.UNKNOWN;

    @Override
    public synchronized String achievetodo$getConfigName() { return configName; }

    @Override
    public synchronized void achievetodo$setConfigName(String configName) { this.configName = configName; }

    @Override
    public synchronized ConfigurationOrigin achievetodo$getConfigurationOrigin() { return achievetodo$configurationOrigin; }

    @Override
    public synchronized void achievetodo$setConfigurationOrigin(ConfigurationOrigin origin) {
        achievetodo$configurationOrigin = Objects.requireNonNull(origin);
    }

    @Override
    public synchronized Map<AbilityType, Integer> achievetodo$getAbilitiesConfiguration(long seed) {
        AchieveToDoMod.logger.info("Loading '{}' abilities configuration...", configName);
        return ConfigurationIO.load(FabricLoader.getInstance().getConfigDir().resolve(BuildConfig.MOD_ID),
            configName, seed, achievetodo$configurationOrigin);
    }

    @ModifyReturnValue(
        method = "parse(Lcom/mojang/serialization/Dynamic;Lnet/minecraft/world/level/WorldDataConfiguration;)Lnet/minecraft/world/level/LevelSettings;",
        at = @At("RETURN")
    )
    private static LevelSettings readConfigName(LevelSettings levelInfo, @Local(argsOnly = true) Dynamic<?> dynamic) {
        LevelInfoExtension extension = MixinCasting.levelInfo(levelInfo);
        extension.achievetodo$setConfigName(dynamic.get(Constants.NbtKey.LEVEL_CONFIG_NAME).asString(""));
        extension.achievetodo$setConfigurationOrigin(ConfigurationOrigin.SAVED);
        return levelInfo;
    }

    @ModifyReturnValue(
        method = {
            "withGameType(Lnet/minecraft/world/level/GameType;)Lnet/minecraft/world/level/LevelSettings;",
            "withAllowCommands(Z)Lnet/minecraft/world/level/LevelSettings;",
            "withDifficulty(Lnet/minecraft/world/Difficulty;)Lnet/minecraft/world/level/LevelSettings;",
            "withDifficultyLock(Z)Lnet/minecraft/world/level/LevelSettings;",
            "withDataConfiguration(Lnet/minecraft/world/level/WorldDataConfiguration;)Lnet/minecraft/world/level/LevelSettings;",
            "copy()Lnet/minecraft/world/level/LevelSettings;"
        },
        at = @At("RETURN")
    )
    private synchronized LevelSettings keepConfigNameOnRecreate(LevelSettings levelInfo) {
        LevelInfoExtension extension = MixinCasting.levelInfo(levelInfo);
        extension.achievetodo$setConfigName(configName);
        extension.achievetodo$setConfigurationOrigin(achievetodo$configurationOrigin);
        return levelInfo;
    }
}
