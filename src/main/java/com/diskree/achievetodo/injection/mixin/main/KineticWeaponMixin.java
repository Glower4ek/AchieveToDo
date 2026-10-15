package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AchieveToDoServer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KineticWeapon.class)
public class KineticWeaponMixin {
    @WrapMethod(method = "damageEntities(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V")
    private void scopeKineticContacts(ItemStack stack, int ticksRemaining, LivingEntity actor,
                                       EquipmentSlot slot, Operation<Void> original) {
        try (var invocation = AchieveToDoServer.openCombatInvocation(actor, slot, stack, false)) {
            original.call(stack, ticksRemaining, actor, slot);
        }
    }

    @Inject(
        method = "damageEntities(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"),
        cancellable = true
    )
    private void lockKineticContacts(ItemStack stack, int ticksRemaining, LivingEntity actor,
                                     EquipmentSlot slot, CallbackInfo ci) {
        if (actor instanceof Player player && AchieveToDoServer.isRegisteredSpear(stack.getItem()) &&
            AchieveToDoServer.isCombatAbilityLocked(
                player, AbilityType.findToolMaterialUsageAbility(stack.getItem()), false
            )) {
            ci.cancel();
        }
    }
}
