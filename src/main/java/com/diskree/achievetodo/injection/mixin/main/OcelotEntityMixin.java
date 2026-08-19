package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ocelot.class)
public class OcelotEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/feline/Ocelot;usePlayerItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Ocelot ocelotEntity = (Ocelot) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, ocelotEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
