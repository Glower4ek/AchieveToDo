package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.server.DeniedCriteria;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.PlayerInteractTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PLAYER_INTERACTED_WITH_ENTITY after a denial does not count; PLAYER_SHEARED_EQUIPMENT shares the class. */
@Mixin(PlayerInteractTrigger.class)
public abstract class PlayerInteractTriggerMixin {

    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true)
    private void suppressDeniedInteraction(ServerPlayer player, ItemStack item, Entity entity, CallbackInfo ci) {
        if ((Object) this == CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY
            && DeniedCriteria.consume(player.getUUID(), DeniedCriteria.Kind.PLAYER_INTERACTED_WITH_ENTITY)) {
            ci.cancel();
        }
    }
}
