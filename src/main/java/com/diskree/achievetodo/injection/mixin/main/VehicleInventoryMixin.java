package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEntity.class)
public interface VehicleInventoryMixin {

    @Shadow
    Level level();

    @Shadow
    AABB getBoundingBox();

    @Inject(
        method = "interactWithContainerVehicle",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    default void lockVehicleInventory(Player player, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, level(), getBoundingBox())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
