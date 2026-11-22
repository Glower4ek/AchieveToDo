package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.AchieveToDoMod;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {

    /**
     * A locked portal denies the teleport (EntityMixin.lockPortal), so touching it must not grant the vanilla
     * enter_block advancement either ("We Need to Go Deeper", "The End?", "Remote Getaway"). Same exemptions as the
     * teleport guard: a nether portal in the Nether, the exit portal in the End and the outer-island gateways are never
     * locked. Known issue 3, fixed in 0.3.0.
     */
    @Inject(
        method = "onInsideBlock",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockPortalContactAdvancement(BlockState state, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        AbilityType abilityType;
        if (state.is(net.minecraft.world.level.block.Blocks.NETHER_PORTAL)) {
            if (player.level().dimension() == net.minecraft.world.level.Level.NETHER) return;
            abilityType = AbilityType.ENTER_NETHER;
        } else if (state.is(net.minecraft.world.level.block.Blocks.END_PORTAL)) {
            if (player.level().dimension() == net.minecraft.world.level.Level.END) return;
            abilityType = AbilityType.ENTER_END;
        } else if (state.is(net.minecraft.world.level.block.Blocks.END_GATEWAY)) {
            BlockPos pos = player.blockPosition();
            boolean centralIsland = pos.getX() >= -96 && pos.getX() <= 96 && pos.getZ() >= -96 && pos.getZ() <= 96
                && pos.getY() >= 74 && pos.getY() <= 76;
            if (player.level().dimension() == net.minecraft.world.level.Level.END && !centralIsland) return;
            abilityType = AbilityType.TELEPORT_OUTER_ISLANDS;
        } else {
            return;
        }
        if (AchieveToDoMod.isAbilityLocked(player, abilityType, true)
            || AchieveToDoMod.isTargetInLockedLandmark(player, player.level(), player.blockPosition())) {
            ci.cancel();
        }
    }

    @Inject(
        method = "jumpFromGround",
        at = @At("HEAD"),
        cancellable = true
    )
    public void lockJump(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!player.isInWater() && AchieveToDoMod.isAbilityLocked(player, AbilityType.JUMP)) {
            ci.cancel();
        }
    }

    @ModifyReturnValue(
        method = "adjustSpawnLocation",
        at = @At("RETURN")
    )
    public BlockPos setSafeWorldSpawn(
        @NotNull BlockPos original,
        @Local(argsOnly = true) @NotNull ServerLevel world
    ) {
        int maxY = world.getMinY() - 1;
        BlockPos highestBlockPos = original;

        int centerX = original.getX();
        int centerZ = original.getZ();
        int startX = centerX - 50;
        int endX = centerX + 50;
        int startZ = centerZ - 50;
        int endZ = centerZ + 50;

        int chunkStartX = startX >> 4;
        int chunkEndX = endX >> 4;
        int chunkStartZ = startZ >> 4;
        int chunkEndZ = endZ >> 4;

        for (int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++) {
            for (int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++) {
                LevelChunk chunk = world.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk == null) {
                    continue;
                }
                Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);
                int chunkMaxY;
                if (heightmap == null) {
                    chunkMaxY = world.getMinY();
                } else {
                    int tmpMax = world.getMinY();
                    for (int localX = 0; localX < 16; localX++) {
                        for (int localZ = 0; localZ < 16; localZ++) {
                            int y = heightmap.getFirstAvailable(localX, localZ);
                            if (y > tmpMax) {
                                tmpMax = y;
                            }
                        }
                    }
                    chunkMaxY = tmpMax;
                }
                if (chunkMaxY <= maxY) {
                    continue;
                }
                int blockMinX = chunkX << 4;
                int blockMaxX = blockMinX + 15;
                int blockMinZ = chunkZ << 4;
                int blockMaxZ = blockMinZ + 15;

                int realStartX = Math.max(blockMinX, startX);
                int realEndX = Math.min(blockMaxX, endX);
                int realStartZ = Math.max(blockMinZ, startZ);
                int realEndZ = Math.min(blockMaxZ, endZ);

                for (int x = realStartX; x <= realEndX; x++) {
                    for (int z = realStartZ; z <= realEndZ; z++) {
                        int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                        if (topY > maxY) {
                            maxY = topY;
                            BlockPos topPos = new BlockPos(x, topY, z);
                            BlockState state = world.getBlockState(topPos);
                            if (!state.getFluidState().isEmpty()) {
                                continue;
                            }
                            VoxelShape shape = state.getCollisionShape(world, topPos);
                            if (shape.isEmpty() || shape.max(Direction.Axis.Y) < 1.0D) {
                                continue;
                            }
                            double maxCollisionY = shape.max(Direction.Axis.Y);
                            if (maxCollisionY >= 1.0D) {
                                topY++;
                            }
                            highestBlockPos = new BlockPos(x, topY, z);
                        }
                    }
                }
            }
        }
        return highestBlockPos;
    }
}
