package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.tracking.TrackedStatisticsDataType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Player;

@Mixin(StatsCounter.class)
public class StatHandlerMixin {

    @Inject(
        method = "setValue",
        at = @At("RETURN")
    )
    private void trackStatChange(Player player, Stat<?> stat, int value, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            Set<TrackedStatisticsDataType> trackedStatisticsDataTypes = TrackedStatisticsDataType.findByStat(stat);
            if (trackedStatisticsDataTypes != null) {
                for (TrackedStatisticsDataType trackedStatisticsDataType : trackedStatisticsDataTypes) {
                    AchieveToDoMod.getServer().setStat(serverPlayer, trackedStatisticsDataType, value);
                }
            }
        }
    }
}
