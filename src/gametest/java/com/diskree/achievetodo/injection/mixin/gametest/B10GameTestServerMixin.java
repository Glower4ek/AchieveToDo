package com.diskree.achievetodo.injection.mixin.gametest;

import com.diskree.achievetodo.certification.PhaseBRuntimeSmoke;
import com.diskree.achievetodo.ability.ProgressionModeType;
import com.diskree.achievetodo.util.MixinCasting;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import net.minecraft.world.level.WorldDataConfiguration;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;
import java.util.Optional;

@Mixin(GameTestServer.class)
public abstract class B10GameTestServerMixin {
    @Inject(method = "create", at = @At("HEAD"))
    private static void install(Thread thread, LevelStorageSource.LevelStorageAccess storage,
        PackRepository packs, Optional<String> selection, boolean verify, int repeats,
        CallbackInfoReturnable<GameTestServer> cir) {
        PhaseBRuntimeSmoke.install(storage);
    }
    @Redirect(method = "create", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/packs/repository/PackRepository;getAvailableIds()Ljava/util/Collection;"))
    private static Collection<String> selectProductionPacks(PackRepository repository) {
        return PhaseBRuntimeSmoke.select(repository);
    }
    // Fabric's GameTest-only settings factory moves builtin packs after file packs.
    // Restore the production-derived D2 order before any resources are loaded.
    @ModifyArg(method = "create", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/WorldLoader$PackConfig;<init>(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/WorldDataConfiguration;ZZ)V"), index = 1)
    private static WorldDataConfiguration companionProductionOrder(WorldDataConfiguration configuration) {
        return PhaseBRuntimeSmoke.companionProductionOrder(configuration);
    }
    @Inject(method = "lambda$create$1", at = @At("HEAD"))
    private static void config(LevelSettings settings, WorldLoader.DataLoadContext context,
        CallbackInfoReturnable<WorldLoader.DataLoadOutput> cir) {
        MixinCasting.levelInfo(settings).achievetodo$setConfigName(ProgressionModeType.getDefaultMode().getName());
    }
}
