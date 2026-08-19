package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public class AbstractHorseEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;openCustomInventoryScreen(Lnet/minecraft/world/entity/player/Player;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse abstractHorseEntity = (AbstractHorse) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;equipBodyArmor(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse abstractHorseEntity = (AbstractHorse) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;doPlayerRide(Lnet/minecraft/world/entity/player/Player;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractHorse abstractHorseEntity = (AbstractHorse) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "handleEating",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;setInLove(Lnet/minecraft/world/entity/player/Player;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract4(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        AbstractHorse abstractHorseEntity = (AbstractHorse) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "handleEating",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;heal(F)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract5(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        AbstractHorse abstractHorseEntity = (AbstractHorse) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "handleEating",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract6(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        AbstractHorse abstractHorseEntity = (AbstractHorse) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "handleEating",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;modifyTemper(I)I",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract7(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        AbstractHorse abstractHorseEntity = (AbstractHorse) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, abstractHorseEntity)) {
            cir.setReturnValue(false);
        }
    }
}
