package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractChestedHorse.class)
public class AbstractDonkeyEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/AbstractChestedHorse;makeMad()V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractChestedHorse abstractDonkeyEntity = (AbstractChestedHorse) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractDonkeyEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/AbstractChestedHorse;equipChest(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractChestedHorse abstractDonkeyEntity = (AbstractChestedHorse) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractDonkeyEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
