package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.progression.DemystificationSink;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionActorMode;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionContext;
import com.diskree.achievetodo.restriction.RestrictionDecision;
import com.diskree.achievetodo.restriction.RestrictionDecisionKind;
import com.diskree.achievetodo.restriction.RestrictionEngine;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionHand;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class BukkitRestrictionService {

    private final JavaPlugin plugin;
    private final RestrictionEngine restrictionEngine;
    private final PlayerProgressionLookup progressionLookup;
    private final DemystificationSink demystificationSink;

    public BukkitRestrictionService(
        JavaPlugin plugin,
        RestrictionEngine restrictionEngine,
        PlayerProgressionLookup progressionLookup,
        DemystificationSink demystificationSink
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.restrictionEngine = Objects.requireNonNull(restrictionEngine, "restrictionEngine");
        this.progressionLookup = Objects.requireNonNull(progressionLookup, "progressionLookup");
        this.demystificationSink = Objects.requireNonNull(demystificationSink, "demystificationSink");
    }

    public RestrictionDecision check(Player player, RestrictionContext context) {
        return restrictionEngine.check(resolveState(player), context);
    }

    public boolean denyIfRestricted(Player player, RestrictionContext context, Cancellable cancellable) {
        RestrictionDecision decision = check(player, context);
        if (decision.isAllowed()) {
            return false;
        }
        cancellable.setCancelled(true);
        demystify(player, decision);
        sendFeedback(player, decision);
        return true;
    }

    public boolean denyIfRestricted(
        Player player,
        RestrictionActionKind actionKind,
        Collection<AbilityType> abilityCandidates,
        RestrictionTarget target,
        RestrictionHand hand,
        RestrictionAutomationSource automationSource,
        Set<RestrictionFlag> flags,
        Cancellable cancellable
    ) {
        RestrictionDecision decision = check(player, actionKind, abilityCandidates, target, hand, automationSource, flags);
        if (decision.isAllowed()) {
            return false;
        }
        cancellable.setCancelled(true);
        demystify(player, decision);
        sendFeedback(player, decision);
        return true;
    }

    public boolean denySilentlyIfRestricted(
        Player player,
        RestrictionActionKind actionKind,
        Collection<AbilityType> abilityCandidates,
        RestrictionTarget target,
        RestrictionHand hand,
        RestrictionAutomationSource automationSource,
        Set<RestrictionFlag> flags,
        Cancellable cancellable
    ) {
        RestrictionDecision decision = check(player, actionKind, abilityCandidates, target, hand, automationSource, flags);
        if (decision.isAllowed()) {
            return false;
        }
        cancellable.setCancelled(true);
        return true;
    }

    public RestrictionDecision check(
        Player player,
        RestrictionActionKind actionKind,
        Collection<AbilityType> abilityCandidates,
        RestrictionTarget target,
        RestrictionHand hand,
        RestrictionAutomationSource automationSource,
        Set<RestrictionFlag> flags
    ) {
        PlayerProgressionState state = resolveState(player);
        RestrictionActorMode actorMode = BukkitRestrictionResolver.resolveActorMode(player.getGameMode());

        RestrictionDecision landmarkDecision = restrictionEngine.checkLandmark(
            state,
            target == null ? null : target.world(),
            target == null ? null : target.blockBox(),
            actorMode
        );
        RestrictionDecision abilityDecision = RestrictionDecision.allow();
        for (AbilityType abilityCandidate : abilityCandidates) {
            if (abilityCandidate == null) {
                continue;
            }
            RestrictionContext context = RestrictionContext.builder(player.getUniqueId(), actionKind)
                .actorMode(actorMode)
                .abilityCandidate(abilityCandidate)
                .target(target)
                .hand(hand)
                .automationSource(automationSource)
                .flags(flags)
                .build();
            RestrictionDecision currentDecision = restrictionEngine.checkAbility(state, context.abilityCandidate(), actorMode);
            if (!currentDecision.isAllowed()) {
                abilityDecision = currentDecision;
                break;
            }
        }
        if (abilityDecision.isAllowed() && landmarkDecision.isAllowed()) {
            return RestrictionDecision.allow();
        }
        if (abilityDecision.isAllowed()) {
            return landmarkDecision;
        }
        if (landmarkDecision.isAllowed()) {
            return abilityDecision;
        }
        return RestrictionDecision.denyBoth(
            abilityDecision.ability(),
            landmarkDecision.landmark(),
            landmarkDecision.landmarkAbility(),
            landmarkDecision.deniedLandmarks(),
            "ability_and_landmark_locked"
        );
    }

    public void handleDeniedDecision(Player player, RestrictionDecision decision) {
        if (decision == null || decision.isAllowed()) {
            return;
        }
        demystify(player, decision);
        sendFeedback(player, decision);
    }

    public List<AbilityType> abilityList(AbilityType... candidates) {
        List<AbilityType> resolved = new ArrayList<>();
        for (AbilityType candidate : candidates) {
            if (candidate != null) {
                resolved.add(candidate);
            }
        }
        return resolved;
    }

    private PlayerProgressionState resolveState(Player player) {
        return progressionLookup.getProgressionState(player.getUniqueId());
    }

    private void demystify(Player player, RestrictionDecision decision) {
        if (decision.kind() == RestrictionDecisionKind.DENY_ABILITY || decision.kind() == RestrictionDecisionKind.DENY_BOTH) {
            AbilityType abilityType = decision.ability();
            if (abilityType != null) {
                demystificationSink.demystify(player.getUniqueId(), abilityType);
            }
        }
    }

    public void sendFeedback(Player player, RestrictionDecision decision) {
        String message = switch (decision.kind()) {
            case DENY_ABILITY -> "Locked ability: " + decision.ability().getName();
            case DENY_LANDMARK -> "Locked landmark: " + decision.landmark().getName();
            case DENY_BOTH -> "Locked ability and landmark";
            case ALLOW -> null;
        };
        if (message != null) {
            player.sendActionBar(Component.text(message));
        }
    }
}
