package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.player.Player;

@Mixin(Bucketable.class)
public interface BucketableMixin {

    @Inject(
        method = "bucketMobPickup",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private static <T extends LivingEntity & Bucketable> void lockInteract1(
        Player player,
        InteractionHand hand,
        T entity,
        CallbackInfoReturnable<Optional<InteractionResult>> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, entity)) {
            cir.setReturnValue(Optional.of(InteractionResult.FAIL));
        }
    }
}
