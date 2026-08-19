package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.entity.vault.VaultServerData;
import net.minecraft.world.level.block.entity.vault.VaultSharedData;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VaultBlockEntity.Server.class)
public class VaultBlockEntityServerMixin {

    @Inject(
        method = "tryInsertKey",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/vault/VaultBlockEntity$Server;resolveItemsToEject(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/entity/vault/VaultConfig;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private static void lockVault(
        ServerLevel world,
        BlockPos pos,
        BlockState state,
        VaultConfig config,
        VaultServerData serverData,
        VaultSharedData sharedData,
        Player player,
        ItemStack stack,
        CallbackInfo ci
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.UNLOCK_VAULT)
        ) {
            ci.cancel();
        }
    }
}
