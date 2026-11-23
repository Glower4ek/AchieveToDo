package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Tadpole.class)
public class TadpoleEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/frog/Tadpole;feed(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Tadpole tadpoleEntity = (Tadpole) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, tadpoleEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    // 0.4.0: the 26.3 golden dandelion locks the tadpole's age (Tadpole overrides AgeableMob's branch).
    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/AgeableMob;setAgeLocked(Lnet/minecraft/world/entity/Mob;Ljava/util/function/Supplier;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockGoldenDandelion(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Tadpole tadpoleEntity = (Tadpole) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, tadpoleEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
