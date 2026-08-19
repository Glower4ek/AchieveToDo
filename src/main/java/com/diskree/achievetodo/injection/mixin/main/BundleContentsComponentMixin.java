package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.component.BundleContents;
import com.diskree.achievetodo.AchieveToDoMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BundleContents.Mutable.class)
public class BundleContentsComponentMixin {

    @Inject(
        method = "tryTransfer(Lnet/minecraft/world/inventory/Slot;Lnet/minecraft/world/entity/player/Player;)I",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    public void lockBundle(
        Slot slot,
        Player player,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.PUT_IN_BUNDLE)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
            cir.setReturnValue(0);
        }
    }
}
