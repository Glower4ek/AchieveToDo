package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import com.diskree.achievetodo.server.AdvancementsMode;
import com.diskree.achievetodo.tracking.TrackedScoreType;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Set;

@Mixin(Scoreboard.class)
public class ScoreboardMixin {

    @Mixin(targets = "net/minecraft/world/scores/Scoreboard$1")
    public static class ScoreMixin {

        @WrapOperation(
            method = "sendScoreToPlayers",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/scores/Scoreboard;onScoreChanged(Lnet/minecraft/world/scores/ScoreHolder;Lnet/minecraft/world/scores/Objective;Lnet/minecraft/world/scores/Score;)V"
            )
        )
        private void trackScoreChanges(
            Scoreboard scoreboard,
            @NotNull ScoreHolder scoreHolder,
            @NotNull Objective objective,
            Score scoreboardScore,
            @NotNull Operation<Void> original
        ) {
            original.call(scoreboard, scoreHolder, objective, scoreboardScore);

            if (scoreboard instanceof ServerScoreboard serverScoreboard) {
                int score = scoreboardScore.value();
                String objectiveName = objective.getName();
                AdvancementsMode advancementsMode = AchieveToDoMod.getServer().currentAdvancementsMode;
                if (advancementsMode != null &&
                    objective == AchieveToDoMod.getServer().currentScoreboardObjective
                ) {
                    AchieveToDoMod.getServer().reconcileScoreHolder(
                        serverScoreboard, scoreHolder.getScoreboardName()
                    );
                } else if (scoreHolder instanceof ServerPlayer serverPlayer) {
                    Set<TrackedScoreType> progressTypes = TrackedScoreType.findByObjectiveName(objectiveName);
                    if (progressTypes != null) {
                        for (TrackedScoreType progressType : progressTypes) {
                            AchieveToDoMod.getServer().setScore(
                                serverPlayer,
                                progressType,
                                progressType.fixScore(scoreboard, scoreHolder, score)
                            );
                        }
                    }
                }
            }
        }
    }
}
