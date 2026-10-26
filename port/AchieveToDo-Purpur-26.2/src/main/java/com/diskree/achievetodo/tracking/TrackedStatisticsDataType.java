package com.diskree.achievetodo.tracking;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public enum TrackedStatisticsDataType {

    ICEOLOGER_SHOULDVE_WON("blazeandcave:animal/iceologer_shouldve_won", TrackedStatisticKey.entityKilled("glow_squid"), 100, false),
    WHERES_THE_HONEY_LEBOWSKI("blazeandcave:animal/wheres_the_honey_lebowski", TrackedStatisticKey.itemUsed("honey_bottle"), 200, false),
    INSOMNIAC("blazeandcave:building/insomniac", TrackedStatisticKey.custom("time_since_rest"), 720_000, true),
    RING_OF_THE_END("blazeandcave:end/ring_of_the_end", TrackedStatisticKey.entityKilled("ender_dragon"), 20, false),
    MUST_BE_YOUR_BIRTHDAY("blazeandcave:farming/must_be_your_birthday", TrackedStatisticKey.custom("eat_cake_slice"), 100, false),
    BULLDOZER("blazeandcave:mining/bulldozer", TrackedStatisticKey.blockMined("stone"), 10_000, true),
    PUPIL_POPPERS("blazeandcave:monsters/pupil_poppers", TrackedStatisticKey.itemUsed("spider_eye"), 1_000, false),
    OUT_FOR_A_STROLL("blazeandcave:statistics/out_for_a_stroll", TrackedStatisticKey.custom("walk_one_cm"), 1_000_000, true),
    WHO_NEEDS_CARS("blazeandcave:statistics/who_needs_cars", TrackedStatisticKey.custom("walk_one_cm"), 5_000_000, true),
    I_ENJOY_LONG_WALKS_AND_PLAYING_MINECRAFT("blazeandcave:statistics/i_enjoy_long_walks_and_playing_minecraft", TrackedStatisticKey.custom("walk_one_cm"), 25_000_000, true),
    NATURAL_SPRINTER("blazeandcave:statistics/natural_sprinter", TrackedStatisticKey.custom("sprint_one_cm"), 1_000_000, true),
    MARATHON("blazeandcave:statistics/marathon", TrackedStatisticKey.custom("sprint_one_cm"), 4_219_500, true),
    YOUR_LEGS_MUST_BE_TIRED("blazeandcave:statistics/your_legs_must_be_tired", TrackedStatisticKey.custom("sprint_one_cm"), 25_000_000, true),
    SNEAKY_SNITCH("blazeandcave:statistics/sneaky_snitch", TrackedStatisticKey.custom("crouch_one_cm"), 10_000, true),
    BLACK_BELT_NINJA("blazeandcave:statistics/black_belt_ninja", TrackedStatisticKey.custom("crouch_one_cm"), 100_000, true),
    ANCIENT_KUNG_FU_MASTER("blazeandcave:statistics/ancient_kung_fu_master", TrackedStatisticKey.custom("crouch_one_cm"), 1_000_000, true),
    LAPS_IN_THE_POOL("blazeandcave:statistics/laps_in_the_pool", TrackedStatisticKey.custom("swim_one_cm"), 100_000, true),
    OLYMPIC_ATHLETE("blazeandcave:statistics/olympic_athlete", TrackedStatisticKey.custom("swim_one_cm"), 1_000_000, true),
    OLYMPIC_GOLD_MEDALLIST("blazeandcave:statistics/olympic_gold_medallist", TrackedStatisticKey.custom("swim_one_cm"), 5_000_000, true),
    LUXURY_CRUISE("blazeandcave:statistics/luxury_cruise", TrackedStatisticKey.custom("boat_one_cm"), 100_000, true),
    PIRATE_CAPTAIN("blazeandcave:statistics/pirate_captain", TrackedStatisticKey.custom("boat_one_cm"), 1_000_000, true),
    SAILOR_OF_THE_SEVEN_SEAS("blazeandcave:statistics/sailor_of_the_seven_seas", TrackedStatisticKey.custom("boat_one_cm"), 5_000_000, true),
    MINECART_RIDER("blazeandcave:statistics/minecart_rider", TrackedStatisticKey.custom("minecart_one_cm"), 100_000, true),
    I_LIKE_TRAINS("blazeandcave:statistics/i_like_trains", TrackedStatisticKey.custom("minecart_one_cm"), 1_000_000, true),
    GLOBAL_RAILWAY_NETWORK("blazeandcave:statistics/global_railway_network", TrackedStatisticKey.custom("minecart_one_cm"), 5_000_000, true),
    PIG_TRAINING("blazeandcave:statistics/pig_training", TrackedStatisticKey.custom("pig_one_cm"), 10_000, true),
    SNOUT_500("blazeandcave:statistics/snout_500", TrackedStatisticKey.custom("pig_one_cm"), 100_000, true),
    LIGHTNING_MCPIG("blazeandcave:statistics/lightning_mcpig", TrackedStatisticKey.custom("pig_one_cm"), 1_000_000, true),
    TAKING_IT_IN_STRIDE("blazeandcave:statistics/taking_it_in_stride", TrackedStatisticKey.custom("strider_one_cm"), 10_000, true),
    WHO_NEEDS_BOATS("blazeandcave:statistics/who_needs_boats", TrackedStatisticKey.custom("strider_one_cm"), 100_000, true),
    THIS_RIDE_IS_LIT_YO("blazeandcave:statistics/this_ride_is_lit_yo", TrackedStatisticKey.custom("strider_one_cm"), 1_000_000, true),
    HORSE_TRAINING("blazeandcave:statistics/horse_training", TrackedStatisticKey.custom("horse_one_cm"), 100_000, true),
    SHERIFF_FIREARM("blazeandcave:statistics/sheriff_firearm", TrackedStatisticKey.custom("horse_one_cm"), 1_000_000, true),
    SCOURGE_OF_THE_WEST("blazeandcave:statistics/scourge_of_the_west", TrackedStatisticKey.custom("horse_one_cm"), 5_000_000, true),
    TAKE_TO_THE_SKIES("blazeandcave:statistics/take_to_the_skies", TrackedStatisticKey.custom("aviate_one_cm"), 1_000_000, true),
    SUPERSONIC("blazeandcave:statistics/supersonic", TrackedStatisticKey.custom("aviate_one_cm"), 10_000_000, true),
    FREQUENT_FLYER("blazeandcave:statistics/frequent_flyer", TrackedStatisticKey.custom("aviate_one_cm"), 100_000_000, true),
    SPRING_IN_YOUR_STEP("blazeandcave:statistics/spring_in_your_step", TrackedStatisticKey.custom("jump"), 1_000, true),
    BOING_BOING("blazeandcave:statistics/boing_boing", TrackedStatisticKey.custom("jump"), 10_000, true),
    JUMPING_JACKS("blazeandcave:statistics/jumping_jacks", TrackedStatisticKey.custom("jump"), 100_000, true),
    MOB_HUNTER("blazeandcave:statistics/mob_hunter", TrackedStatisticKey.custom("mob_kills"), 250, false),
    BLOODTHIRSTY("blazeandcave:statistics/bloodthirsty", TrackedStatisticKey.custom("mob_kills"), 2_500, true),
    KILL_OR_BE_KILLED("blazeandcave:statistics/kill_or_be_killed", TrackedStatisticKey.custom("mob_kills"), 25_000, true),
    THE_PARROTS_AND_THE_BATS("blazeandcave:statistics/the_parrots_and_the_bats", TrackedStatisticKey.custom("animals_bred"), 100, false),
    CUPID("blazeandcave:statistics/cupid", TrackedStatisticKey.custom("animals_bred"), 500, false),
    TWO_BY_TWO("blazeandcave:statistics/two_by_two", TrackedStatisticKey.custom("animals_bred"), 2_500, false),
    NOVICE_ENCHANTER("blazeandcave:statistics/novice_enchanter", TrackedStatisticKey.custom("enchant_item"), 10, false),
    JOURNEYMAN_ENCHANTER("blazeandcave:statistics/journeyman_enchanter", TrackedStatisticKey.custom("enchant_item"), 50, false),
    ELDERLY_ENCHANTER("blazeandcave:statistics/elderly_enchanter", TrackedStatisticKey.custom("enchant_item"), 250, false),
    THE_HAGGLER("blazeandcave:statistics/the_haggler", TrackedStatisticKey.custom("traded_with_villager"), 100, false),
    BIG_SPENDER("blazeandcave:statistics/big_spender", TrackedStatisticKey.custom("traded_with_villager"), 500, false),
    STONKS("blazeandcave:statistics/stonks", TrackedStatisticKey.custom("traded_with_villager"), 2_500, false);

    private static final Map<String, TrackedStatisticsDataType> BY_ADVANCEMENT_ID;
    private static final Map<TrackedStatisticKey, Set<TrackedStatisticsDataType>> BY_STATISTIC_KEY;

    static {
        Map<String, TrackedStatisticsDataType> advancementMap = new HashMap<>();
        Map<TrackedStatisticKey, Set<TrackedStatisticsDataType>> keyMap = new HashMap<>();
        for (TrackedStatisticsDataType type : values()) {
            advancementMap.put(type.advancementId.toLowerCase(Locale.ROOT), type);
            keyMap.computeIfAbsent(type.statisticKey, ignored -> new java.util.LinkedHashSet<>()).add(type);
        }
        Map<TrackedStatisticKey, Set<TrackedStatisticsDataType>> immutableKeyMap = new HashMap<>();
        for (Map.Entry<TrackedStatisticKey, Set<TrackedStatisticsDataType>> entry : keyMap.entrySet()) {
            immutableKeyMap.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }
        BY_ADVANCEMENT_ID = Collections.unmodifiableMap(advancementMap);
        BY_STATISTIC_KEY = Collections.unmodifiableMap(immutableKeyMap);
    }

    private final String advancementId;
    private final TrackedStatisticKey statisticKey;
    private final int finalValue;
    private final boolean percentage;

    TrackedStatisticsDataType(String advancementId, TrackedStatisticKey statisticKey, int finalValue, boolean percentage) {
        this.advancementId = advancementId;
        this.statisticKey = statisticKey;
        this.finalValue = finalValue;
        this.percentage = percentage;
    }

    public String getAdvancementId() {
        return advancementId;
    }

    public TrackedStatisticKey getStatisticKey() {
        return statisticKey;
    }

    public int getFinalValue() {
        return finalValue;
    }

    public boolean isPercentage() {
        return percentage;
    }

    public int normalizeProgress(int progress) {
        if (!percentage) {
            return progress;
        }
        return Math.max(0, Math.min(100, (int) ((progress * 100.0) / finalValue)));
    }

    public static Set<TrackedStatisticsDataType> findByStatisticKey(TrackedStatisticKey statisticKey) {
        if (statisticKey == null) {
            return Set.of();
        }
        Set<TrackedStatisticsDataType> trackedStatisticsDataTypes = BY_STATISTIC_KEY.get(statisticKey);
        if (trackedStatisticsDataTypes == null) {
            return Set.of();
        }
        return trackedStatisticsDataTypes;
    }

    public static Optional<TrackedStatisticsDataType> findByAdvancementId(String advancementId) {
        if (advancementId == null || advancementId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ADVANCEMENT_ID.get(advancementId.trim().toLowerCase(Locale.ROOT)));
    }
}
