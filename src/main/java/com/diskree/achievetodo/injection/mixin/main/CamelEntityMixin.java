package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camel.class)
public class CamelEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/camel/Camel;doPlayerRide(Lnet/minecraft/world/entity/player/Player;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Camel camelEntity = (Camel) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, camelEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "handleEating",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/camel/Camel;heal(F)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        Camel camelEntity = (Camel) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, camelEntity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "handleEating",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/camel/Camel;setInLove(Lnet/minecraft/world/entity/player/Player;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        Camel camelEntity = (Camel) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, camelEntity)) {
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
    public void lockInteract4(Player player, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        Camel camelEntity = (Camel) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, camelEntity)) {
            cir.setReturnValue(false);
        }
    }
}
