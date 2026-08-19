package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import com.diskree.achievetodo.AchieveToDoMod;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Equippable.class)
public class EquippableComponentMixin {

    @Inject(
        method = "swapWithEquipmentSlot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;level()Lnet/minecraft/world/level/Level;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockEquip(
        @NotNull ItemStack stack,
        Player player,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
