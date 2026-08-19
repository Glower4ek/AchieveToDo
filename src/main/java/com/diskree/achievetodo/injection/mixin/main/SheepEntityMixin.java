package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheep.class)
public class SheepEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockInteract1(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Sheep sheepEntity = (Sheep) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, sheepEntity) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)
        ) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
