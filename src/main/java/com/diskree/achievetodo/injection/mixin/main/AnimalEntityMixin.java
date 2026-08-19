package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Animal.class)
public class AnimalEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Animal;usePlayerItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockAnimalsInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Animal animalEntity = (Animal) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, animalEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
