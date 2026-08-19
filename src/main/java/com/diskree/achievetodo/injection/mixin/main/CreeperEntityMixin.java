package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Creeper.class)
public class CreeperEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFlintAndSteel(
        Player player,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir,
        @Local @NotNull ItemStack stack
    ) {
        Creeper creeperEntity = (Creeper) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, creeperEntity) ||
            stack.is(Items.FLINT_AND_STEEL) && AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_FLINT_AND_STEEL)
        ) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
