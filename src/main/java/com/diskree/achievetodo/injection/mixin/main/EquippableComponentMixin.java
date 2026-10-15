package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.server.level.ServerPlayer;
import com.diskree.achievetodo.ability.DimensionType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import com.diskree.achievetodo.AchieveToDoMod;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Equippable.class)
public class EquippableComponentMixin {

    @Inject(method = "equipOnTarget", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/player/Player;level()Lnet/minecraft/world/level/Level;"), cancellable = true)
    private void lockTargetEquipment(Player player, LivingEntity target, ItemStack stack,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        // Native recipient/slot/occupied/alive checks precede this injection.
        var server = AchieveToDoMod.getServer();
        DimensionType dimension = DimensionType.findByWorld(target.level().dimension());
        boolean denied;
        if (player instanceof ServerPlayer actor && server != null && dimension != null) {
            denied = server.isTargetInLockedLandmark(actor, dimension, target.getBoundingBox(),
                !atd$reportsEquipmentDenial(player, target, stack));
        } else {
            denied = AchieveToDoMod.isTargetInLockedLandmark(player, target);
        }
        if (denied) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Unique
    private static boolean atd$reportsEquipmentDenial(Player player, LivingEntity target, ItemStack stack) {
        var context = StackWalker.getInstance().walk(frames -> frames
            .filter(frame -> frame.getClassName().startsWith("net.minecraft.world.entity."))
            .map(frame -> frame.getClassName() + "#" + frame.getMethodName()).toList());
        boolean outerInteraction = context.contains("net.minecraft.world.entity.player.Player#interactOn");
        boolean subtypeInteraction = context.stream().anyMatch(frame -> frame.endsWith("#mobInteract"));
        // An outer native item fallback repeats an eligible equip after nonconsuming FAIL.
        if (outerInteraction && subtypeInteraction) {
            return false;
        }
        boolean branchReporter = false;
        if (target instanceof Camel camel && !camel.isBaby()) {
            branchReporter = player.isSecondaryUseActive() ||
                (!camel.isFood(stack) && camel.getPassengers().size() < 2);
        } else if (target instanceof AbstractNautilus nautilus && !nautilus.isBaby() && nautilus.isTame()) {
            branchReporter = player.isSecondaryUseActive() || (!nautilus.isFood(stack) && !nautilus.isVehicle());
        } else if (target instanceof AbstractHorse horse && horse.isTamed() && !horse.isBaby() && !horse.isVehicle()) {
            branchReporter = !horse.isFood(stack);
        }
        // Direct equip has no native mount branch reporter; never suppress its notification.
        return !(branchReporter && (outerInteraction || subtypeInteraction));
    }

    @Inject(
        method = "swapWithEquipmentSlot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;level()Lnet/minecraft/world/level/Level;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockEquip(
        @NotNull ItemStack stack,
        Player player,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isAbilityLocked(player, AbilityType.findEquipmentEquipAbility(stack.getItem()))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
