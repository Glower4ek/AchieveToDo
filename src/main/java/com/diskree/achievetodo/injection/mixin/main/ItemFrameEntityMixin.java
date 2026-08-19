package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public class ItemFrameEntityMixin {

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setItem(Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockPlace(
        Player player,
        InteractionHand hand,
        Vec3 hitPosition,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        ItemFrame itemFrameEntity = (ItemFrame) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, itemFrameEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(
        method = "interact",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/ItemFrame;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockRotation(
        Player player,
        InteractionHand hand,
        Vec3 hitPosition,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        ItemFrame itemFrameEntity = (ItemFrame) (Object) this;
        if (AchieveToDoMod.isTargetInLockedLandmark(player, itemFrameEntity)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(
        method = "hurtServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/ItemFrame;dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockDrop(
        ServerLevel world,
        @NotNull DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> cir
    ) {
        ItemFrame itemFrameEntity = (ItemFrame) (Object) this;
        if (source.getEntity() instanceof Player player &&
            AchieveToDoMod.isTargetInLockedLandmark(player, itemFrameEntity)
        ) {
            cir.setReturnValue(false);
        }
    }
}
