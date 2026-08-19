package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBoat.class)
public class AbstractBoatEntityMixin {

    @ModifyReturnValue(
        method = "canAddPassenger",
        at = @At("TAIL")
    )
    private boolean lockBoat(
        boolean original,
        @Local(argsOnly = true) Entity passenger
    ) {
        if (!original) {
            return false;
        }
        if (passenger instanceof Player player) {
            AbstractBoat boatEntity = (AbstractBoat) (Object) this;
            if (AchieveToDoMod.isTargetInLockedLandmark(player, boatEntity) ||
                AchieveToDoMod.isAbilityLocked(player, AbilityType.GET_INTO_BOAT)
            ) {
                return false;
            }
        }
        return true;
    }
}
