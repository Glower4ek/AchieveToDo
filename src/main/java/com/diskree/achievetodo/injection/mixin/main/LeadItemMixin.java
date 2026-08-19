package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(LeadItem.class)
public class LeadItemMixin {

    @WrapOperation(
        method = "bindPlayerMobs",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Leashable;leashableInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Predicate;)Ljava/util/List;"
        )
    )
    private static @NotNull List<Leashable> lockAttachHeldMobs(
        Level world,
        Vec3 center,
        Predicate<Leashable> predicate,
        @NotNull Operation<List<Leashable>> original,
        @Local(argsOnly = true) Player player,
        @Local(argsOnly = true) BlockPos pos
    ) {
        List<Leashable> result = original.call(world, center, predicate);
        if (!result.isEmpty() && AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            result.clear();
        }
        return result;
    }
}
