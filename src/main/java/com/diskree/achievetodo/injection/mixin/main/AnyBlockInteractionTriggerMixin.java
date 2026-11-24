package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.server.DeniedCriteria;
import net.minecraft.advancements.triggers.AnyBlockInteractionTrigger;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ANY_BLOCK_USE after a denial does not count. */
@Mixin(AnyBlockInteractionTrigger.class)
public abstract class AnyBlockInteractionTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true)
    private void suppressDeniedUse(ServerPlayer player, BlockPos pos, ItemInstance item, CallbackInfo ci) {
        if ((Object) this == CriteriaTriggers.ANY_BLOCK_USE
            && DeniedCriteria.consume(player.getUUID(), DeniedCriteria.Kind.ANY_BLOCK_USE)) {
            ci.cancel();
        }
    }
}
