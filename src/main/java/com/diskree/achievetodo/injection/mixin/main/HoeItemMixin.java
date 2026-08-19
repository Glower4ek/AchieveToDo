package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public class HoeItemMixin {

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
    public void lockHoe(
        UseOnContext context,
        CallbackInfoReturnable<InteractionResult> cir,
        @Local Player player
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(context) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findToolMaterialUsageAbility(material))
        ) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
