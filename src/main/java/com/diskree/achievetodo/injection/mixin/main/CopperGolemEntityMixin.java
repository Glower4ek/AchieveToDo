package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.WeatheringCopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperGolem.class)
public abstract class CopperGolemEntityMixin {
    @Shadow private long nextWeatheringTick;

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void lockManualInteraction(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        CopperGolem golem = (CopperGolem) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        AbilityType ability = null;
        if (stack.isEmpty() && !golem.getMainHandItem().isEmpty()) {
            // Retrieval takes precedence over every later item branch.
        } else if (stack.is(Items.SHEARS) && golem.readyForShearing()) {
            ability = AbilityType.USE_SHEARS;
        } else if (golem.level().isClientSide()) {
            return;
        } else if (stack.is(Items.HONEYCOMB) && nextWeatheringTick != -2L) {
            // Waxing has no tool-material permission.
        } else if (stack.is(ItemTags.AXES) && (nextWeatheringTick == -2L ||
            golem.getWeatherState() != WeatheringCopper.WeatherState.UNAFFECTED)) {
            ability = AbilityType.findToolMaterialUsageAbility(stack.getItem());
        } else {
            return;
        }
        if (AchieveToDoMod.isTargetInLockedLandmark(player, golem) || AchieveToDoMod.isAbilityLocked(player, ability)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
