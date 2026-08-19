package com.diskree.achievetodo;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.ability.DimensionType;
import com.diskree.achievetodo.client.AchieveToDoClient;
import com.diskree.achievetodo.networking.c2s.DemystifyAbilityPayload;
import com.diskree.achievetodo.networking.s2c.*;
import com.diskree.achievetodo.server.AchieveToDoServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AchieveToDoMod implements ModInitializer {

    public static Logger logger = LoggerFactory.getLogger(BuildConfig.MOD_NAME);

    private static AchieveToDoServer server;

    @Override
    public void onInitialize() {
        registerPayloads();

        server = new AchieveToDoServer();
        server.onInitializeServer();
    }

    public static AchieveToDoServer getServer() {
        return server;
    }

    public static @NotNull Identifier getIdentifier(String path) {
        return Identifier.fromNamespaceAndPath(BuildConfig.MOD_ID, path);
    }

    public static boolean isAbilityLocked(@Nullable Player player, @Nullable AbilityType abilityType) {
        return isAbilityLocked(player, abilityType, false);
    }

    public static boolean isAbilityLocked(
        @Nullable Player player,
        @Nullable AbilityType abilityType,
        boolean checkOnly
    ) {
        if (player == null || abilityType == null) {
            return false;
        }
        if (player.level().isClientSide()) {
            return AchieveToDoClient.isAbilityLocked(abilityType, checkOnly);
        }
        return player instanceof ServerPlayer serverPlayer &&
            server != null &&
            server.isAbilityLocked(serverPlayer, abilityType);
    }

    public static boolean isTargetInLockedLandmark(@Nullable Player actor, @NotNull Entity target) {
        return isTargetInLockedLandmark(actor, target.level(), target.getBoundingBox());
    }

    public static boolean isTargetInLockedLandmark(@NotNull UseOnContext context) {
        return isTargetInLockedLandmark(context.getPlayer(), context.getLevel(), context.getClickedPos());
    }

    public static boolean isTargetInLockedLandmark(
        @Nullable Player actor,
        @NotNull Level targetWorld,
        @NotNull BlockPos targetBlockPos
    ) {
        return isTargetInLockedLandmark(actor, targetWorld, new AABB(targetBlockPos));
    }

    public static boolean isTargetInLockedLandmark(
        @Nullable Player actor,
        @NotNull Level targetWorld,
        @NotNull AABB targetBox
    ) {
        if (actor == null) {
            return false;
        }
        DimensionType targetDimensionType = DimensionType.findByWorld(targetWorld.dimension());
        if (targetDimensionType == null) {
            return false;
        }
        if (actor.level().isClientSide()) {
            return AchieveToDoClient.isTargetInLockedLandmark(targetDimensionType, targetBox);
        }
        if (server != null && actor instanceof ServerPlayer serverPlayer) {
            return server.isTargetInLockedLandmark(serverPlayer, targetDimensionType, targetBox);
        }
        return false;
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(
            DemystifyAbilityPayload.ID,
            DemystifyAbilityPayload.CODEC
        );

        PayloadTypeRegistry.clientboundPlay().register(
            SyncAbilitiesConfigurationPayload.ID,
            SyncAbilitiesConfigurationPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            SyncObtainedAdvancementsCountPayload.ID,
            SyncObtainedAdvancementsCountPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            LandmarksLockedStatusChangedPayload.ID,
            LandmarksLockedStatusChangedPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            LandmarkTypesUnlockedPayload.ID,
            LandmarkTypesUnlockedPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            LockedLandmarkResizedPayload.ID,
            LockedLandmarkResizedPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            ScoreProgressChangedPayload.ID,
            ScoreProgressChangedPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            StatisticsDataProgressChangedPayload.ID,
            StatisticsDataProgressChangedPayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            CheckTargetInLockedLandmarkPayload.ID,
            CheckTargetInLockedLandmarkPayload.CODEC
        );
    }
}
