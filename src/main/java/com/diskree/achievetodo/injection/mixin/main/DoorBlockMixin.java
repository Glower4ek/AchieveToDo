package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorBlock.class)
public class DoorBlockMixin {

    @Shadow
    @Final
    public static BooleanProperty OPEN;

    @Inject(
        method = "useWithoutItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;cycle(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Object;",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    public void lockDoor(
        @NotNull BlockState state,
        Level world,
        BlockPos pos,
        Player player,
        BlockHitResult hit,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            !state.getValue(OPEN) && AchieveToDoMod.isAbilityLocked(player, AbilityType.OPEN_DOOR)
        ) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
