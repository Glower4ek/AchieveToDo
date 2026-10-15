package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AchieveToDoServer;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PiercingWeapon.class)
public class PiercingWeaponMixin {
    @WrapMethod(method = "attack(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V")
    private void lockPiercingAttack(LivingEntity actor, EquipmentSlot slot, Operation<Void> original) {
        ItemStack stack = actor.getItemBySlot(slot);
        try (var invocation = AchieveToDoServer.openCombatInvocation(actor, slot, stack, true)) {
            if (!(actor instanceof Player player) || !AchieveToDoServer.isRegisteredSpear(stack.getItem()) ||
                !AchieveToDoServer.isCombatAbilityLocked(
                    player, AbilityType.findToolMaterialUsageAbility(stack.getItem()), true
                )) {
                original.call(actor, slot);
            }
            if (invocation != null) {
                invocation.reportDenial();
            }
        }
    }

    @ModifyReturnValue(
        method = "canHitEntity(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("RETURN")
    )
    private static boolean lockPiercingTarget(boolean nativeResult,
        @Local(argsOnly = true, ordinal = 0) Entity actor,
        @Local(argsOnly = true, ordinal = 1) Entity target) {
        return nativeResult && !AchieveToDoServer.isScopedCombatTargetLocked(actor, target);
    }
}
