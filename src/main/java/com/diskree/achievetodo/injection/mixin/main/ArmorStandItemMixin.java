package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

@Mixin(ArmorStandItem.class)
public class ArmorStandItemMixin {

    @WrapOperation(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"
        )
    )
    public boolean lockArmorStandPlace(
        List<Entity> entities,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) UseOnContext context,
        @Local AABB boundingBox
    ) {
        return original.call(entities) &&
            !AchieveToDoMod.isTargetInLockedLandmark(context.getPlayer(), context.getLevel(), boundingBox);
    }
}
