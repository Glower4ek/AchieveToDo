package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class ChaosProgressionGenerator {

    private static final int MAX_ADVANCEMENTS_COUNT = 1000;
    private static final double CHAOS_INITIALLY_UNLOCKED_CHANCE_PERCENT = 1.0;
    private static final double CHAOS_PERMANENTLY_LOCKED_CHANCE_PERCENT = 0.2;

    private final Random random;
    private Map<AbilityType, Integer> progression;

    private ChaosProgressionGenerator(long seed, Map<AbilityType, Integer> baseProgression) {
        this.random = new Random(seed);
        this.progression = Map.copyOf(baseProgression);
    }

    public static Map<AbilityType, Integer> generateChaosProgression(long seed) {
        ChaosProgressionGenerator generator = new ChaosProgressionGenerator(seed, Progressions.getHardProgression());
        generator.shuffle();
        generator.shift();
        generator.fixPriority();
        generator.applySpecialFlags();
        return generator.progression;
    }

    private void shuffle() {
        ArrayList<AbilityType> abilityTypes = new ArrayList<>(progression.keySet());
        ArrayList<Integer> countValues = new ArrayList<>(progression.values());
        Collections.shuffle(abilityTypes, random);
        Collections.shuffle(countValues, random);
        Map<AbilityType, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < abilityTypes.size(); i++) {
            result.put(abilityTypes.get(i), countValues.get(i));
        }
        progression = result;
    }

    private void shift() {
        Map<AbilityType, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<AbilityType, Integer> entry : progression.entrySet()) {
            AbilityType abilityType = entry.getKey();
            int oldCount = entry.getValue();
            int newCount;
            if (abilityType.getChaosPriority() == 100) {
                newCount = oldCount;
            } else {
                int shiftRange = Math.round((100 - abilityType.getChaosPriority()) * 0.5f);
                int shiftMagnitude = shiftRange == 0 ? 0 : random.nextInt(shiftRange);
                int shift = random.nextBoolean() ? shiftMagnitude : -shiftMagnitude;
                newCount = oldCount + shift;
            }
            if (newCount < ProgressionConstants.MIN_ADVANCEMENTS_COUNT) {
                newCount = ProgressionConstants.MIN_ADVANCEMENTS_COUNT;
            }
            if (newCount > MAX_ADVANCEMENTS_COUNT) {
                newCount = MAX_ADVANCEMENTS_COUNT;
            }
            result.put(abilityType, newCount);
        }
        progression = result;
    }

    private void fixPriority() {
        Map<AbilityType, Integer> result = new LinkedHashMap<>();
        Map<AbilityType, Integer> baseProgression = Progressions.getHardProgression();
        for (Map.Entry<AbilityType, Integer> entry : progression.entrySet()) {
            AbilityType abilityType = entry.getKey();
            int baseCount = baseProgression.get(abilityType);
            int oldCount = entry.getValue();
            int newCount;
            if (abilityType.getChaosPriority() == 0) {
                newCount = oldCount;
            } else if (abilityType.getChaosPriority() == 100) {
                newCount = baseCount;
            } else {
                double ratio = abilityType.getChaosPriority() / 100.0 * random.nextDouble();
                newCount = (int) Math.round(oldCount + ratio * (baseCount - oldCount));
            }
            result.put(abilityType, newCount);
        }
        progression = result;
    }

    private void applySpecialFlags() {
        Map<AbilityType, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<AbilityType, Integer> entry : progression.entrySet()) {
            AbilityType abilityType = entry.getKey();
            int oldCount = entry.getValue();
            int newCount;
            if (abilityType == AbilityType.VISION || random.nextDouble() * 100.0 < CHAOS_INITIALLY_UNLOCKED_CHANCE_PERCENT) {
                newCount = ProgressionConstants.INITIALLY_UNLOCKED_FLAG;
            } else if (abilityType.getChaosPriority() != 100 &&
                random.nextDouble() * 100.0 < CHAOS_PERMANENTLY_LOCKED_CHANCE_PERCENT) {
                newCount = ProgressionConstants.PERMANENTLY_LOCKED_FLAG;
            } else {
                newCount = oldCount;
            }
            result.put(abilityType, newCount);
        }
        progression = result;
    }
}
