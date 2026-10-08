package com.diskree.achievetodo.injection.mixin.main;

import com.diskree.achievetodo.AchieveToDoMod;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerScoreboard.class)
public class ServerScoreboardMixin {

    @Inject(
        method = "setDisplayObjective",
        at = @At("RETURN")
    )
    private void trackScoreboardChanged(CallbackInfo ci) {
        ServerScoreboard scoreboard = (ServerScoreboard) (Object) this;
        AchieveToDoMod.getServer().prepareScoreboard(scoreboard);
    }

    @Inject(
        method = "onPlayerScoreRemoved(Lnet/minecraft/world/scores/ScoreHolder;Lnet/minecraft/world/scores/Objective;)V",
        at = @At("RETURN")
    )
    private void trackPlayerScoreRemoved(ScoreHolder holder, Objective objective, CallbackInfo ci) {
        if (objective == AchieveToDoMod.getServer().currentScoreboardObjective) {
            AchieveToDoMod.getServer().reconcileScoreHolder(
                (ServerScoreboard) (Object) this, holder.getScoreboardName()
            );
        }
    }

    @Inject(
        method = "onPlayerRemoved(Lnet/minecraft/world/scores/ScoreHolder;)V",
        at = @At("RETURN")
    )
    private void trackPlayerRemoved(ScoreHolder holder, CallbackInfo ci) {
        AchieveToDoMod.getServer().reconcileScoreHolder(
            (ServerScoreboard) (Object) this, holder.getScoreboardName()
        );
    }

    @Inject(
        method = "addPlayerToTeam(Ljava/lang/String;Lnet/minecraft/world/scores/PlayerTeam;)Z",
        at = @At("RETURN")
    )
    private void trackPlayerAddedToTeam(String playerName, PlayerTeam team, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            AchieveToDoMod.getServer().reconcileTeamMembers((ServerScoreboard) (Object) this, team);
        }
    }

    @Inject(
        method = "removePlayerFromTeam(Ljava/lang/String;Lnet/minecraft/world/scores/PlayerTeam;)V",
        at = @At("RETURN")
    )
    private void trackPlayerRemovedFromTeam(String playerName, PlayerTeam team, CallbackInfo ci) {
        ServerScoreboard scoreboard = (ServerScoreboard) (Object) this;
        AchieveToDoMod.getServer().reconcileScoreHolder(scoreboard, playerName);
        AchieveToDoMod.getServer().reconcileTeamMembers(scoreboard, team);
    }

    @Inject(
        method = "onTeamRemoved(Lnet/minecraft/world/scores/PlayerTeam;)V",
        at = @At("RETURN")
    )
    private void trackTeamRemoved(PlayerTeam team, CallbackInfo ci) {
        AchieveToDoMod.getServer().reconcileTeamMembers((ServerScoreboard) (Object) this, team);
    }
}
