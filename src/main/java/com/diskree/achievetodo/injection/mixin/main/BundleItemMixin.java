package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.AchieveToDoMod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BundleItem.class)
public class BundleItemMixin {

    @WrapOperation(
        method = "overrideOtherStackedOnMe",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;allowModification(Lnet/minecraft/world/entity/player/Player;)Z",
            ordinal = 0
        )
    )
    public boolean lockBundle(
        Slot instance,
        Player player,
        @NotNull Operation<Boolean> original
    ) {
        if (!original.call(instance, player)) {
            return false;
        }
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.PUT_IN_BUNDLE)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
        } else {
            return true;
        }
        return false;
    }
}
