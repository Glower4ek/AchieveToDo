package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.ability.AbilityType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

@Mixin(AxeItem.class)
public abstract class AxeItemMixin {

    @Shadow
    protected abstract Optional<BlockState> getStripped(BlockState state);

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
        method = "evaluateNewBlockState",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockStrip(
        Level world,
        BlockPos pos,
        @Nullable Player player,
        BlockState state,
        CallbackInfoReturnable<Optional<BlockState>> cir
    ) {
        // The evaluator plays sounds. Establish the native candidate using only pure lookups.
        Optional<BlockState> candidate = getStripped(state)
            .or(() -> WeatheringCopper.getPrevious(state))
            .or(() -> Optional.ofNullable(HoneycombItem.WAX_OFF_BY_BLOCK.get().get(state.getBlock()))
                .map(block -> block.withPropertiesOf(state)));
        if (candidate.isEmpty()) {
            return;
        }
        if (AchieveToDoMod.isTargetInLockedLandmark(player, world, pos) ||
            AchieveToDoMod.isAbilityLocked(player, AbilityType.findToolMaterialUsageAbility(material)) ||
            isLinkedTargetLocked(player, world, pos, state)
        ) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Unique
    private static boolean isLinkedTargetLocked(Player player, Level world, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            BlockPos other = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState partner = world.getBlockState(other);
            if (partner.getBlock() instanceof DoorBlock && partner.getValue(DoorBlock.HALF) != state.getValue(DoorBlock.HALF) &&
                AchieveToDoMod.isTargetInLockedLandmark(player, world, other)) {
                return true;
            }
        }
        if (state.getBlock() instanceof CopperChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return AchieveToDoMod.isTargetInLockedLandmark(player, world, ChestBlock.getConnectedBlockPos(pos, state));
        }
        return false;
    }
}
