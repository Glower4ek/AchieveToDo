package com.diskree.achievetodo.tracking;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

public enum TrackedScoreType {

    AN_APPLE_A_DAY("blazeandcave:farming/an_apple_a_day", 30, false, "bac_apple_days"),
    JUST_KEEPS_GOING("blazeandcave:animal/just_keeps_going", 3600, true, "bac_just_keep"),
    CAPTAIN_AMERICA("blazeandcave:biomes/captain_america", 1200, true, "bac_captain_america"),
    LET_ME_OUT("blazeandcave:nether/let_me_out", 24000, true, "bac_let_me_out"),
    I_YEARNED_FOR_THE_MINES("blazeandcave:mining/i_yearned_for_the_mines", 6000, true, "bac_i_yearned_for_the_mines"),
    CASTAWAY("blazeandcave:farming/castaway", 3600, true, "bac_castaway"),
    FREE_DIVER("blazeandcave:biomes/free_diver", 120, true, "bac_underwater"),
    SLEEP_WITH_THE_FISHES("blazeandcave:biomes/sleep_with_the_fishes", 1200, true, "bac_underwater"),
    PASSING_THE_TIME("blazeandcave:statistics/passing_the_time", 100, false, "bac_day_count"),
    HAPPY_NEW_YEAR("blazeandcave:statistics/happy_new_year", 365, false, "bac_day_count"),
    HALF_HEART_LIFE("blazeandcave:weaponry/half_heart_life", 60, false, "bac_hh_life"),
    HOUSE_OF_FREAKS("blazeandcave:monsters/house_of_freaks", 5, false, "bac_warden_count"),
    OVERWARDEN("blazeandcave:challenges/overwarden", 50, false, "bac_overwarden_count"),
    DIVERS_DOZEN("blazeandcave:end/divers_dozen", 12, false, "bac_divers_dozen_count"),
    FACTORIO("blazeandcave:redstone/factorio", 64, false, "bac_factorio_count"),
    A_PIGLINS_BEST_FRIEND("blazeandcave:nether/a_piglins_best_friend", 500, false, "bac_pigling"),
    THE_WORLD_IS_ENDING("blazeandcave:challenges/the_world_is_ending", 10, false, "bac_ten_withers"),
    VAULT_HUNTER("blazeandcave:adventure/vault_hunter", 25, false, "bac_vault_hunter_count"),
    WHACK_A_MOLE("blazeandcave:enchanting/whack_a_mole", 8, false, "bac_whack_a_mole_count"),
    OM_NOM_NOM("blazeandcave:statistics/om_nom_nom", 200, false, "bac_stat_food"),
    YUM_YUM_YUMMO("blazeandcave:statistics/yum_yum_yummo", 1000, false, "bac_stat_food"),
    FOOD_GLORIOUS_FOOD("blazeandcave:statistics/food_glorious_food", 5000, false, "bac_stat_food"),
    LOOT_EM("blazeandcave:statistics/loot_em", 10, false, "bac_stat_loot_chest"),
    MORE_FOR_ME("blazeandcave:statistics/more_for_me", 100, false, "bac_stat_loot_chest"),
    I_HEART_CHESTS("blazeandcave:statistics/i_heart_chests", 500, false, "bac_stat_loot_chest"),
    ON_A_RAIL(
        "blazeandcave:redstone/on_a_rail",
        1000,
        true,
        "bac_oar_eligible_x",
        "bac_oar_eligible_z",
        "bac_oar_current_x",
        "bac_oar_current_z"
    ),
    ARTILLERY("blazeandcave:weaponry/artillery", 9, false, "bac_inv_artillery"),
    CHESTFUL_OF_COBBLESTONE("blazeandcave:mining/chestful_of_cobblestone", 1728, true, "bac_inv_chestful_of_cobblestone"),
    IMMORTAL("blazeandcave:challenges/immortal", 37, false, "bac_inv_immortal"),
    ROCKETMAN("blazeandcave:end/rocketman", 2368, true, "bac_inv_rocketman"),
    LOSER("blazeandcave:weaponry/loser", 2, false, "bac_loser");

    private static final Map<String, TrackedScoreType> BY_ADVANCEMENT_ID;
    private static final Map<String, Set<TrackedScoreType>> BY_OBJECTIVE_NAME;

    static {
        Map<String, TrackedScoreType> advancementMap = new HashMap<>();
        Map<String, Set<TrackedScoreType>> objectiveMap = new HashMap<>();
        for (TrackedScoreType type : values()) {
            advancementMap.put(type.advancementId.toLowerCase(Locale.ROOT), type);
            for (String objectiveName : type.objectiveNames) {
                objectiveMap.computeIfAbsent(objectiveName.toLowerCase(Locale.ROOT), ignored -> EnumSet.noneOf(TrackedScoreType.class)).add(type);
            }
        }
        Map<String, Set<TrackedScoreType>> normalizedObjectiveMap = new HashMap<>();
        for (Map.Entry<String, Set<TrackedScoreType>> entry : objectiveMap.entrySet()) {
            normalizedObjectiveMap.put(entry.getKey(), Collections.unmodifiableSet(EnumSet.copyOf(entry.getValue())));
        }
        BY_ADVANCEMENT_ID = Collections.unmodifiableMap(advancementMap);
        BY_OBJECTIVE_NAME = Collections.unmodifiableMap(normalizedObjectiveMap);
    }

    private final String advancementId;
    private final int finalValue;
    private final boolean percentage;
    private final Set<String> objectiveNames;

    TrackedScoreType(String advancementId, int finalValue, boolean percentage, String... objectiveNames) {
        this.advancementId = advancementId;
        this.finalValue = finalValue;
        this.percentage = percentage;
        this.objectiveNames = Arrays.stream(objectiveNames)
            .map(name -> name.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }

    public String getAdvancementId() {
        return advancementId;
    }

    public int getFinalValue() {
        return finalValue;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public Set<String> getObjectiveNames() {
        return objectiveNames;
    }

    public int normalizeProgress(int progress) {
        if (!percentage) {
            return progress;
        }
        return Math.max(0, Math.min(100, (int) ((progress * 100.0) / finalValue)));
    }

    public int fixScore(ScoreAccess scoreAccess, String holderEntry, int score) {
        if (this == ON_A_RAIL) {
            OptionalInt eligibleX = scoreAccess.getScore(holderEntry, "bac_oar_eligible_x");
            if (eligibleX.isPresent()) {
                if (eligibleX.getAsInt() == 1) {
                    OptionalInt currentX = scoreAccess.getScore(holderEntry, "bac_oar_current_x");
                    if (currentX.isPresent()) {
                        score = Math.abs(currentX.getAsInt());
                    }
                } else {
                    OptionalInt eligibleZ = scoreAccess.getScore(holderEntry, "bac_oar_eligible_z");
                    if (eligibleZ.isPresent()) {
                        if (eligibleZ.getAsInt() == 1) {
                            OptionalInt currentZ = scoreAccess.getScore(holderEntry, "bac_oar_current_z");
                            if (currentZ.isEmpty()) {
                                return score;
                            }
                            score = Math.abs(currentZ.getAsInt());
                        } else {
                            score = 0;
                        }
                    }
                }
            }
        } else if (this == LOSER) {
            score = score <= 10 ? 1 : 0;
        }
        return score;
    }

    public static Optional<TrackedScoreType> findByAdvancementId(String advancementId) {
        if (advancementId == null || advancementId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ADVANCEMENT_ID.get(advancementId.trim().toLowerCase(Locale.ROOT)));
    }

    public static Set<TrackedScoreType> findByObjectiveName(String objectiveName) {
        if (objectiveName == null || objectiveName.isBlank()) {
            return Set.of();
        }
        Set<TrackedScoreType> trackedScoreTypes = BY_OBJECTIVE_NAME.get(objectiveName.trim().toLowerCase(Locale.ROOT));
        if (trackedScoreTypes == null) {
            return Set.of();
        }
        return trackedScoreTypes;
    }
}
