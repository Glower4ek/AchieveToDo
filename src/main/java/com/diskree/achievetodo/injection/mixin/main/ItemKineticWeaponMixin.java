package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.server.AchieveToDoServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemKineticWeaponMixin {
    @Inject(
        method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;startUsingItem(Lnet/minecraft/world/InteractionHand;)V", ordinal = 1),
        cancellable = true
    )
    private void lockKineticStart(Level level, Player player, InteractionHand hand,
                                  CallbackInfoReturnable<InteractionResult> cir) {
        Item item = player.getItemInHand(hand).getItem();
        if (AchieveToDoServer.isRegisteredSpear(item) && AchieveToDoServer.isCombatAbilityLocked(
            player, AbilityType.findToolMaterialUsageAbility(item), true
        )) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
