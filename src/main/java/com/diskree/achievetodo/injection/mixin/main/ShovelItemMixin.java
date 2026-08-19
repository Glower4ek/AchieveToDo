package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShovelItem.class)
public class ShovelItemMixin {

    @Unique
    private ToolMaterial material;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void saveMaterial(
        ToolMaterial material,
        float attackDamage,
        float attackSpeed,
        Item.Properties settings,
        CallbackInfo ci
    ) {
        this.material = material;
    }

    @Inject(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockFlattenUsage(@NotNull UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
            AchieveToDoMod.isAbilityLocked(context.getPlayer(), AbilityType.findToolMaterialUsageAbility(material))
        ) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isClientSide()Z",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockCampfireExtinguishUsage(
        @NotNull UseOnContext context,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
            AchieveToDoMod.isAbilityLocked(context.getPlayer(), AbilityType.findToolMaterialUsageAbility(material))
        ) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
