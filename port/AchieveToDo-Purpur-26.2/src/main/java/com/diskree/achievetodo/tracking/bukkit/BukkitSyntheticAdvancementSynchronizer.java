package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.datapack.EmbeddedDatapack;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.tracking.PlayerProgressionUpdate;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class BukkitSyntheticAdvancementSynchronizer {

    private static final String DEMYSTIFIED_CRITERION = EmbeddedDatapack.DEMYSTIFIED_CRITERION;
    private static final String UNLOCKED_CRITERION = EmbeddedDatapack.UNLOCKED_CRITERION;

    private final Map<AbilityType, NamespacedKey> advancementKeys = new EnumMap<>(AbilityType.class);
    private final java.util.function.Function<NamespacedKey, Advancement> resolver;

    BukkitSyntheticAdvancementSynchronizer() { this(Bukkit::getAdvancement); }

    BukkitSyntheticAdvancementSynchronizer(java.util.function.Function<NamespacedKey, Advancement> resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    Set<AbilityType> readDemystifiedAbilities(Player player) {
        Objects.requireNonNull(player, "player");
        EnumSet<AbilityType> demystifiedAbilities = EnumSet.noneOf(AbilityType.class);
        for (AbilityType abilityType : AbilityType.values()) {
            Advancement advancement = resolveAdvancement(abilityType);
            if (advancement == null) {
                continue;
            }
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            if (progress.getAwardedCriteria().contains(DEMYSTIFIED_CRITERION)) {
                demystifiedAbilities.add(abilityType);
            }
        }
        return demystifiedAbilities;
    }

    void fullSync(Player player, PlayerProgressionState progressionState) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(progressionState, "progressionState");
        for (AbilityType abilityType : AbilityType.values()) {
            Advancement advancement = resolveAdvancement(abilityType);
            if (advancement == null) {
                continue;
            }
            if (progressionState.getDemystifiedAbilities().contains(abilityType)) {
                awardCriterion(player, advancement, DEMYSTIFIED_CRITERION);
            }
            int requiredCount = progressionState.getRequirements().getRequiredCount(abilityType);
            if (isNumericThreshold(requiredCount) && requiredCount != Integer.MIN_VALUE && progressionState.getAbilityState(abilityType).isUnlocked()) {
                awardRemainingCriteria(player, advancement);
            } else if (isNumericThreshold(requiredCount)) {
                revokeCriterion(player, advancement, UNLOCKED_CRITERION);
            }
        }
    }

    void applyUpdate(Player player, PlayerProgressionUpdate update) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(update, "update");
        PlayerProgressionState currentState = update.currentState();
        for (AbilityType abilityType : update.relockedAbilities()) {
            Advancement advancement = resolveAdvancement(abilityType);
            if (advancement == null) {
                continue;
            }
            if (isNumericThreshold(currentState.getRequirements().getRequiredCount(abilityType))) {
                revokeCriterion(player, advancement, UNLOCKED_CRITERION);
            }
        }
        for (AbilityType abilityType : update.unlockedAbilities()) {
            Advancement advancement = resolveAdvancement(abilityType);
            if (advancement == null) {
                continue;
            }
            int requiredCount = currentState.getRequirements().getRequiredCount(abilityType);
            if (requiredCount == Integer.MIN_VALUE) {
                // Fabric uses MIN_VALUE as its old-count sentinel: no representable count crosses this threshold.
                revokeCriterion(player, advancement, UNLOCKED_CRITERION);
            } else if (isNumericThreshold(requiredCount)) {
                awardRemainingCriteria(player, advancement);
            } else if (currentState.getDemystifiedAbilities().contains(abilityType)) {
                awardCriterion(player, advancement, DEMYSTIFIED_CRITERION);
            }
        }
    }

    private static boolean isNumericThreshold(int value) { return value != 0 && value != -1; }

    void demystify(Player player, AbilityType abilityType) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(abilityType, "abilityType");
        Advancement advancement = resolveAdvancement(abilityType);
        if (advancement != null) {
            awardCriterion(player, advancement, DEMYSTIFIED_CRITERION);
        }
    }

    private Advancement resolveAdvancement(AbilityType abilityType) {
        NamespacedKey key = advancementKeys.computeIfAbsent(abilityType, this::toNamespacedKey);
        return resolver.apply(key);
    }

    private NamespacedKey toNamespacedKey(AbilityType abilityType) {
        String advancementId = abilityType.getAdvancementId();
        int separatorIndex = advancementId.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == advancementId.length() - 1) {
            throw new IllegalStateException("Invalid advancement id: " + advancementId);
        }
        return new NamespacedKey(
            advancementId.substring(0, separatorIndex),
            advancementId.substring(separatorIndex + 1)
        );
    }

    private void awardRemainingCriteria(Player player, Advancement advancement) {
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            progress.awardCriteria(criterion);
        }
    }

    private void awardCriterion(Player player, Advancement advancement, String criterion) {
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (!progress.getAwardedCriteria().contains(criterion)) {
            progress.awardCriteria(criterion);
        }
    }

    private void revokeCriterion(Player player, Advancement advancement, String criterion) {
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (progress.getAwardedCriteria().contains(criterion)) {
            progress.revokeCriteria(criterion);
        }
    }
}
