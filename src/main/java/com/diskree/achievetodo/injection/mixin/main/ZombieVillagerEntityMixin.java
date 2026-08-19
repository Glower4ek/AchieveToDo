package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieVillager.class)
public class ZombieVillagerEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockAnimalsInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ZombieVillager zombieVillagerEntity = (ZombieVillager) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, zombieVillagerEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
