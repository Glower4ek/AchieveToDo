package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(
        method = "isEquippableInSlot",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockEquip(ItemStack stack, EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof Player player &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))
        ) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "setSprinting",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockSprint(boolean sprinting, CallbackInfo ci) {
        if (sprinting) {
            LivingEntity livingEntity = (LivingEntity) (Object) this;
            if (livingEntity instanceof Player player) {
                AbilityType abilityType = player.isUnderWater() ? AbilityType.SWIM : AbilityType.SPRINT;
                if (AchieveToDoMod.isAbilityLocked(player, abilityType)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(
        method = "canEquipWithDispenser",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockEquip(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof Player player &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))
        ) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "jumpFromGround",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockJump(CallbackInfo ci) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof Player player &&
            !player.isInWater() &&
            AchieveToDoMod.isAbilityLocked(player, AbilityType.JUMP)
        ) {
            ci.cancel();
        }
    }
}
