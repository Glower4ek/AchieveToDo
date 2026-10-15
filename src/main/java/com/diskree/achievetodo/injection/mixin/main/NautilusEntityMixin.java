package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractNautilus.class)
public class NautilusEntityMixin {
    @Inject(method = "mobInteract", at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/nautilus/AbstractNautilus;openCustomInventoryScreen(Lnet/minecraft/world/entity/player/Player;)V"),
        @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/nautilus/AbstractNautilus;usePlayerItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"),
        @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/nautilus/AbstractNautilus;feed(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;FF)V")
    }, cancellable = true)
    private void lockNautilusEffects(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, (AbstractNautilus) (Object) this)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/animal/nautilus/AbstractNautilus;doPlayerRide(Lnet/minecraft/world/entity/player/Player;)V"), cancellable = true)
    private void lockNautilusRide(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractNautilus target = (AbstractNautilus) (Object) this;
        if (!target.isVehicle() && AchieveToDoMod.isTargetInLockedLandmark(player, target)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
