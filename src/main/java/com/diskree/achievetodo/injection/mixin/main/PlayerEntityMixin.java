package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AchieveToDoServer;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerEntityMixin {

    @Inject(
        method = "startSleepInBed",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        Player player = (Player) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, player.level(), pos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.SLEEP)
        ) {
            cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
        }
    }

    @ModifyReturnValue(
        method = "blockActionRestricted(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/GameType;)Z",
        at = @At("RETURN")
    )
    public boolean lockBlockBreak(
        boolean original,
        @Local(argsOnly = true) Level world,
        @Local(argsOnly = true) @NotNull BlockPos pos
    ) {
        if (original) {
            return true;
        }
        Player player = (Player) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos)) {
            return true;
        }
        if (pos.getY() < 0 && AchieveToDoMod.isAbilityLocked(player, AbilityType.BREAK_BLOCKS_IN_NEGATIVE_Y)) {
            return true;
        }
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.BREAK_BLOCKS)) {
            return true;
        }
        Item item = player.getMainHandItem().getItem();
        AbilityType toolAbility = AbilityType.findToolMaterialUsageAbility(item);
        if (toolAbility != null && AchieveToDoMod.isAbilityLocked(player, toolAbility)) {
            return true;
        }
        if (item instanceof ShearsItem && AchieveToDoMod.isAbilityLocked(player, AbilityType.USE_SHEARS)) {
            return true;
        }
        return false;
    }

    @Inject(
        method = "attack(Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockEntityAttack(@NotNull Entity target, CallbackInfo info) {
        Player player = (Player) (Object) this;
        if (player instanceof ServerPlayer ? AchieveToDoServer.isCombatTargetLocked(player, target, true) :
            AchieveToDoMod.isTargetInLockedLandmark(player, target)) {
            info.cancel();
            return;
        }
        Item item = player.getWeaponItem().getItem();
        AbilityType ability = item == Items.TRIDENT ? AbilityType.ATTACK_WITH_TRIDENT :
            item == Items.MACE ? AbilityType.ATTACK_WITH_MACE : AbilityType.findToolMaterialUsageAbility(item);
        if (player instanceof ServerPlayer ? AchieveToDoServer.isCombatAbilityLocked(player, ability, true) :
            AchieveToDoMod.isAbilityLocked(player, ability)) {
            info.cancel();
        }
    }

    @Inject(
        method = "stabAttack(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockSpearStab(EquipmentSlot slot, Entity target, float damage, boolean dealsDamage,
                              boolean dealsKnockback, boolean dismounts, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        Item item = player.getItemBySlot(slot).getItem();
        if (AchieveToDoServer.isCombatTargetLocked(player, target, true) ||
            AchieveToDoServer.isRegisteredSpear(item) && AchieveToDoServer.isCombatAbilityLocked(
                player, AbilityType.findToolMaterialUsageAbility(item), true
            )) {
            cir.setReturnValue(false);
        }
    }

    @ModifyReturnValue(
        method = "mayUseItemAt",
        at = @At(value = "RETURN", ordinal = 0)
    )
    public boolean lockDecorationItemPlace(
        boolean original,
        @Local(argsOnly = true) BlockPos blockPos,
        @Local(argsOnly = true) Direction facing,
        @Local(argsOnly = true) ItemStack stack
    ) {
        if (!original) {
            return false;
        }
        if (stack.getItem() instanceof HangingEntityItem) {
            Player player = (Player) (Object) this;
            if (AchieveToDoMod.isTargetInLockedLandmark(player, player.level(), blockPos)) {
                return false;
            }
        }
        return true;
    }
}
