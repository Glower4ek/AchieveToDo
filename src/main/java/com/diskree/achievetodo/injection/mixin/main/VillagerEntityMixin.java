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

    /**
     * A denied trade sees an empty offer list, so vanilla refuses it (the villager shakes its head).
     * The lock is checked before {@code getOffers()}: generating a cartographer's offers searches for structures
     * (explorer maps) and stalled the server for seconds, which delayed the lock message (0.3.0 note, fixed in 0.4.0).
     */
    @WrapOperation(
        method = "mobInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/villager/Villager;getOffers()Lnet/minecraft/world/item/trading/MerchantOffers;"
        )
    )
    public MerchantOffers lockVillager(
        Villager villager,
        @NotNull Operation<MerchantOffers> original,
        @Local(argsOnly = true) Player player
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, villager) ||
            AchieveToDoMod.isAbilityLocked(
                player,
                AbilityType.findTradeAbility(getVillagerData().profession().unwrapKey().orElse(null))
            )
        ) {
            return new MerchantOffers();
        }
        return original.call(villager);
    }
}
