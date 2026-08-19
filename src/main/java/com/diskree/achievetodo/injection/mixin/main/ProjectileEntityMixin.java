package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Projectile.class)
public abstract class ProjectileEntityMixin {

    @Shadow
    public abstract @Nullable Entity getOwner();

    @ModifyReturnValue(
        method = "canHitEntity(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(value = "TAIL")
    )
    public boolean lockProjectileHit(boolean original, @Local(argsOnly = true) Entity target) {
        if (!original) {
            return false;
        }
        if (getOwner() instanceof Player player &&
            target != player &&
            AchieveToDoMod.isTargetInLockedLandmark(player, target)
        ) {
            return false;
        }
        return true;
    }
}
