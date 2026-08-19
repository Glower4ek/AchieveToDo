package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Llama.class)
public class LlamaEntityMixin {

    @Inject(
        method = "handleEating",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/Llama;setInLove(Lnet/minecraft/world/entity/player/Player;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        Llama llamaEntity = (Llama) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, llamaEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "handleEating",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/equine/Llama;heal(F)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        Llama llamaEntity = (Llama) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, llamaEntity)) {
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
    public void lockInteract3(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        Llama llamaEntity = (Llama) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, llamaEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "handleEating",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isClientSide()Z",
            shift = At.Shift.BEFORE,
            ordinal = 1
        ),
        cancellable = true
    )
    public void lockInteract4(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        Llama llamaEntity = (Llama) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, llamaEntity)) {
            cir.setReturnValue(false);
        }
    }
}
