package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartFurnace.class)
public class FurnaceMinecartEntityMixin {

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;consume(ILnet/minecraft/world/entity/LivingEntity;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFurnaceMinecart(
        Player player,
        InteractionHand hand,
        Vec3 hitPosition,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        MinecartFurnace furnaceMinecartEntity = (MinecartFurnace) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, furnaceMinecartEntity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
