package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Villager.class)
public abstract class VillagerEntityMixin {

    @Shadow
    public abstract VillagerData getVillagerData();

    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/trading/MerchantOffers;isEmpty()Z"
        )
    )
    public boolean lockVillager(
        MerchantOffers tradeOffers,
        @NotNull Operation<Boolean> original,
        @Local(argsOnly = true) Player player
    ) {
        if (original.call(tradeOffers)) {
            return true;
        }
        Villager villagerEntity = (Villager) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, villagerEntity) ||
            AchieveToDoMod.isAbilityLocked(
                player,
                AbilityType.findTradeAbility(getVillagerData().profession().unwrapKey().orElse(null))
            )
        ) {
            return true;
        }
        return false;
    }
}
