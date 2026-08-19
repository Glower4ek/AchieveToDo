package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorSlot.class)
public class ArmorSlotMixin {

    @Shadow
    @Final
    private LivingEntity owner;

    @Inject(
        method = "mayPlace",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void lockEquip(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (owner instanceof Player player &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))
        ) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(
        method = "mayPickup",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;mayPickup(Lnet/minecraft/world/entity/player/Player;)Z"
        )
    )
    private boolean lockEquip(
        ArmorSlot armorSlot,
        Player player,
        @NotNull Operation<Boolean> original,
        @Local @NotNull ItemStack stack
    ) {
        if (!original.call(armorSlot, player)) {
            return false;
        }
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.closeContainer();
            }
            return false;
        }
        return true;
    }
}
