package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAttachedEntity.class)
public class BlockAttachedEntityMixin {

    @Inject(
        method = "hurtServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill(Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockKillAttachedEntity(
        ServerLevel world,
        @NotNull DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (source.getEntity() instanceof Player player) {
            BlockAttachedEntity blockAttachedEntity = (BlockAttachedEntity) (Object) this;
            if (AchieveToDoMod.isTargetInLockedLandmark(player, blockAttachedEntity)) {
                cir.setReturnValue(false);
            }
        }
    }
}
