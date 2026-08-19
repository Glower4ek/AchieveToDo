package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Unique
    private boolean isEndGatewayOnCentralIsland(@NotNull BlockPos pos) {
        return pos.getY() == 75 &&
            pos.getX() >= -96 && pos.getX() <= 96 &&
            pos.getZ() >= -96 && pos.getZ() <= 96;
    }

    @Inject(
        method = "setShiftKeyDown",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockSneaking(boolean isSneaking, CallbackInfo ci) {
        if (isSneaking) {
            Entity entity = (Entity) (Object) this;
            if (entity instanceof Player player &&
                player.onGround() &&
                AchieveToDoMod.isAbilityLocked(player, AbilityType.SNEAK)
            ) {
                ci.cancel();
            }
        }
    }

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Leashable;canHaveALeashAttachedTo(Lnet/minecraft/world/entity/Entity;)Z"
        ),
        cancellable = true
    )
    public void lockLeashAttach(Player player, InteractionHand hand, Vec3 hitPosition, CallbackInfoReturnable<InteractionResult> cir) {
        Entity entity = (Entity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, entity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Leashable;dropLeash()V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockLeashDetach(Player player, InteractionHand hand, Vec3 hitPosition, CallbackInfoReturnable<InteractionResult> cir) {
        Entity entity = (Entity) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, entity)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @ModifyReturnValue(
        method = "canAddPassenger",
        at = @At(value = "TAIL")
    )
    public boolean lockMinecart(
        boolean original,
        @Local(argsOnly = true, ordinal = 1) Entity passenger
    ) {
        if (!original) {
            return false;
        }
        Entity entity = (Entity) (Object) this;
        if (passenger instanceof Player player && entity instanceof Minecart) {
            if (AchieveToDoMod.isTargetInLockedLandmark(player, entity) ||
                AchieveToDoMod.isAbilityLocked(player, AbilityType.GET_INTO_MINECART)
            ) {
                return false;
            }
        }
        return true;
    }

    @Inject(
        method = "setAsInsidePortal",
        at = @At("HEAD"),
        cancellable = true
    )
    private void lockPortal(Portal portal, BlockPos pos, CallbackInfo ci) {
        Entity teleportEntity = (Entity) (Object) this;
        ThrownEnderpearl enderPearl = null;
        if (teleportEntity instanceof ThrownEnderpearl enderPearlEntity) {
            teleportEntity = enderPearlEntity.getOwner();
            enderPearl = enderPearlEntity;
        }
        if (teleportEntity == null) {
            return;
        }
        Level currentWorld = enderPearl != null ? enderPearl.level() : teleportEntity.level();
        ResourceKey<Level> currentWorldRegistryKey = currentWorld.dimension();
        AbilityType abilityType = AbilityType.findPortalTeleportAbility(portal);
        if (currentWorldRegistryKey == Level.NETHER && abilityType == AbilityType.ENTER_NETHER) {
            return;
        }
        if (currentWorldRegistryKey == Level.END) {
            if (abilityType == AbilityType.ENTER_END) {
                return;
            }
            if (abilityType == AbilityType.TELEPORT_OUTER_ISLANDS && !isEndGatewayOnCentralIsland(pos)) {
                return;
            }
        }

        if (teleportEntity instanceof Player playerEntity) {
            if (AchieveToDoMod.isTargetInLockedLandmark(playerEntity, currentWorld, pos) ||
                AchieveToDoMod.isAbilityLocked(playerEntity, abilityType)
            ) {
                if (enderPearl != null) {
                    enderPearl.remove(Entity.RemovalReason.DISCARDED);
                }
                ci.cancel();
                return;
            }
        }
        if (enderPearl != null || !teleportEntity.isVehicle()) {
            return;
        }
        if (teleportEntity.getControllingPassenger() instanceof Player controllingPlayer &&
            AchieveToDoMod.isAbilityLocked(controllingPlayer, abilityType)
        ) {
            ci.cancel();
            return;
        }
        for (Entity passengerEntity : teleportEntity.getPassengers()) {
            if (passengerEntity instanceof Player passenger &&
                AchieveToDoMod.isAbilityLocked(passenger, abilityType)
            ) {
                passenger.stopRiding();
            }
        }
    }
}
