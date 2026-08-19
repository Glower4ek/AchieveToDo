package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderEntityMixin {

    @Inject(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/wanderingtrader/WanderingTrader;getOffers()Lnet/minecraft/world/item/trading/MerchantOffers;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockWanderingTrader(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        WanderingTrader wanderingTraderEntity = (WanderingTrader) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, wanderingTraderEntity) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.TRADE_WITH_WANDERING_TRADER)
        ) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
