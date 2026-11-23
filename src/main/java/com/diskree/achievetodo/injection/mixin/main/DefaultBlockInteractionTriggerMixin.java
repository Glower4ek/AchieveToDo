package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.server.DeniedCriteria;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.DefaultBlockInteractionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** DEFAULT_BLOCK_USE after a denial does not count. */
@Mixin(DefaultBlockInteractionTrigger.class)
public abstract class DefaultBlockInteractionTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true)
    private void suppressDeniedUse(ServerPlayer player, BlockPos pos, CallbackInfo ci) {
        if ((Object) this == CriteriaTriggers.DEFAULT_BLOCK_USE
            && DeniedCriteria.consume(player.getUUID(), DeniedCriteria.Kind.DEFAULT_BLOCK_USE)) {
            ci.cancel();
        }
    }
}
