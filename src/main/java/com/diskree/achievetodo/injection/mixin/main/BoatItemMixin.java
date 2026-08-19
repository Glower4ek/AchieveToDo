package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BoatItem.class)
public class BoatItemMixin {

    @WrapOperation(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"
        )
    )
    public boolean lockBoatPlace(
        Level world,
        Entity entity,
        AABB boundingBox,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) Player player
    ) {
        return original.call(world, entity, boundingBox) &&
            !AchieveToDoMod.isTargetInLockedLandmark(player, world, boundingBox);
    }
}
