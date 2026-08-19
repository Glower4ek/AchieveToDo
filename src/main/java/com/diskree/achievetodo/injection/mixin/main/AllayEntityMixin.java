package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Allay.class)
public class AllayEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/allay/Allay;duplicateAllay()V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Allay allayEntity = (Allay) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, allayEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract2(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Allay allayEntity = (Allay) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, allayEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/allay/Allay;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Allay allayEntity = (Allay) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, allayEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
