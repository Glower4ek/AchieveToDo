package com.diskree.achievetodo.restriction;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record RestrictionContext(
    UUID actorId,
    RestrictionActionKind actionKind,
    RestrictionActorMode actorMode,
    AbilityType abilityCandidate,
    RestrictionTarget target,
    RestrictionHand hand,
    RestrictionAutomationSource automationSource,
    RestrictionVehicleContext vehicleContext,
    RestrictionProjectileContext projectileContext,
    Set<RestrictionFlag> flags
) {

    public RestrictionContext {
        actionKind = Objects.requireNonNull(actionKind, "actionKind");
        actorMode = actorMode == null ? RestrictionActorMode.UNKNOWN : actorMode;
        target = target == null ? RestrictionTarget.none() : target;
        hand = hand == null ? RestrictionHand.NONE : hand;
        automationSource = automationSource == null ? RestrictionAutomationSource.UNKNOWN : automationSource;
        vehicleContext = vehicleContext == null ? RestrictionVehicleContext.none() : vehicleContext;
        projectileContext = projectileContext == null ? RestrictionProjectileContext.none() : projectileContext;
        flags = Set.copyOf(new LinkedHashSet<>(flags == null ? Set.of() : flags));
    }

    public boolean hasFlag(RestrictionFlag flag) {
        return flags.contains(flag);
    }

    public static Builder builder(UUID actorId, RestrictionActionKind actionKind) {
        return new Builder(actorId, actionKind);
    }

    public static final class Builder {

        private final UUID actorId;
        private final RestrictionActionKind actionKind;
        private RestrictionActorMode actorMode = RestrictionActorMode.UNKNOWN;
        private AbilityType abilityCandidate;
        private RestrictionTarget target = RestrictionTarget.none();
        private RestrictionHand hand = RestrictionHand.NONE;
        private RestrictionAutomationSource automationSource = RestrictionAutomationSource.UNKNOWN;
        private RestrictionVehicleContext vehicleContext = RestrictionVehicleContext.none();
        private RestrictionProjectileContext projectileContext = RestrictionProjectileContext.none();
        private final Set<RestrictionFlag> flags = new LinkedHashSet<>();

        private Builder(UUID actorId, RestrictionActionKind actionKind) {
            this.actorId = actorId;
            this.actionKind = Objects.requireNonNull(actionKind, "actionKind");
        }

        public Builder actorMode(RestrictionActorMode actorMode) {
            this.actorMode = actorMode;
            return this;
        }

        public Builder abilityCandidate(AbilityType abilityCandidate) {
            this.abilityCandidate = abilityCandidate;
            return this;
        }

        public Builder target(RestrictionTarget target) {
            this.target = target;
            return this;
        }

        public Builder hand(RestrictionHand hand) {
            this.hand = hand;
            return this;
        }

        public Builder automationSource(RestrictionAutomationSource automationSource) {
            this.automationSource = automationSource;
            return this;
        }

        public Builder vehicleContext(RestrictionVehicleContext vehicleContext) {
            this.vehicleContext = vehicleContext;
            return this;
        }

        public Builder projectileContext(RestrictionProjectileContext projectileContext) {
            this.projectileContext = projectileContext;
            return this;
        }

        public Builder addFlag(RestrictionFlag flag) {
            if (flag != null) {
                flags.add(flag);
            }
            return this;
        }

        public Builder flags(Set<RestrictionFlag> flags) {
            this.flags.clear();
            if (flags != null) {
                this.flags.addAll(flags);
            }
            return this;
        }

        public RestrictionContext build() {
            return new RestrictionContext(
                actorId,
                actionKind,
                actorMode,
                abilityCandidate,
                target,
                hand,
                automationSource,
                vehicleContext,
                projectileContext,
                flags
            );
        }
    }
}
