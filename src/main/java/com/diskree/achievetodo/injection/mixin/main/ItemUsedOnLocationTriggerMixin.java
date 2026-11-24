package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.server.DeniedCriteria;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.ItemUsedOnLocationTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ITEM_USED_ON_BLOCK after a denial does not count; PLACED_BLOCK and ALLAY_DROP_ITEM_ON_BLOCK share the class. */
@Mixin(ItemUsedOnLocationTrigger.class)
public abstract class ItemUsedOnLocationTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true)
    private void suppressDeniedUse(ServerPlayer player, BlockPos pos, ItemInstance item, CallbackInfo ci) {
        if ((Object) this == CriteriaTriggers.ITEM_USED_ON_BLOCK
            && DeniedCriteria.consume(player.getUUID(), DeniedCriteria.Kind.ITEM_USED_ON_BLOCK)) {
            ci.cancel();
        }
    }
}
