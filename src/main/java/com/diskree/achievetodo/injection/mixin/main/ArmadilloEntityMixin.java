package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Armadillo.class)
public class ArmadilloEntityMixin {

    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/armadillo/Armadillo;brushOffScute(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Z"
        )
    )
    public boolean lockBrush(
        @NotNull Armadillo armadilloEntity,
        net.minecraft.world.entity.Entity brushUser,
        ItemStack brushStack,
        Operation<Boolean> original,
        @Local(argsOnly = true) Player player
    ) {
        return !armadilloEntity.isBaby() &&
            !AchieveToDoMod.isTargetInLockedLandmark(player, armadilloEntity) &&
            !AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_BRUSH) &&
            original.call(armadilloEntity, brushUser, brushStack);
    }
}
