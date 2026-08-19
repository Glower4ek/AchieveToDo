package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Panda.class)
public class PandaEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/panda/Panda;usePlayerItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE,
            ordinal = 0
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Panda pandaEntity = (Panda) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, pandaEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/panda/Panda;usePlayerItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE,
            ordinal = 1
        ),
        cancellable = true
    )
    public void lockInteract2(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Panda pandaEntity = (Panda) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, pandaEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/panda/Panda;tryToSit()V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract3(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Panda pandaEntity = (Panda) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, pandaEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
