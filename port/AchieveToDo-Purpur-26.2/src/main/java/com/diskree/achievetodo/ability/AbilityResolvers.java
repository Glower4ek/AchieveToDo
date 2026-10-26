package com.diskree.achievetodo.ability;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class AbilityResolvers {

    private static final Map<String, AbilityType> ITEM_ABILITIES;
    private static final Map<String, AbilityType> BLOCK_ABILITIES;
    private static final Map<String, AbilityType> FOOD_ABILITIES;
    private static final Map<String, AbilityType> TOOL_MATERIAL_ABILITIES;
    private static final Map<String, AbilityType> EQUIPMENT_ITEM_ABILITIES;
    private static final Map<String, AbilityType> EQUIPMENT_MATERIAL_ABILITIES;
    private static final Map<String, AbilityType> TRADE_ABILITIES;
    private static final Map<LandmarkType, AbilityType> LANDMARK_ABILITIES;
    private static final Map<String, AbilityType> PORTAL_ABILITIES;

    static {
        Map<String, AbilityType> itemAbilities = new HashMap<>();
        Map<String, AbilityType> blockAbilities = new HashMap<>();
        Map<String, AbilityType> foodAbilities = new HashMap<>();
        Map<String, AbilityType> toolMaterialAbilities = new HashMap<>();
        Map<String, AbilityType> equipmentItemAbilities = new HashMap<>();
        Map<String, AbilityType> equipmentMaterialAbilities = new HashMap<>();
        Map<String, AbilityType> tradeAbilities = new HashMap<>();
        Map<LandmarkType, AbilityType> landmarkAbilities = new EnumMap<>(LandmarkType.class);
        Map<String, AbilityType> portalAbilities = new HashMap<>();
        for (AbilityType abilityType : AbilityType.values()) {
            AbilityBinding binding = abilityType.getBinding();
            switch (binding.type()) {
                case ITEM -> itemAbilities.put(binding.key(), abilityType);
                case BLOCK -> blockAbilities.put(binding.key(), abilityType);
                case FOOD -> foodAbilities.put(binding.key(), abilityType);
                case TOOL_MATERIAL -> toolMaterialAbilities.put(binding.key(), abilityType);
                case EQUIPMENT_ITEM -> equipmentItemAbilities.put(binding.key(), abilityType);
                case EQUIPMENT_MATERIAL -> equipmentMaterialAbilities.put(binding.key(), abilityType);
                case TRADE_TARGET -> tradeAbilities.put(binding.key(), abilityType);
                case LANDMARK -> landmarkAbilities.put(abilityType.getLandmarkType(), abilityType);
                case PORTAL -> portalAbilities.put(binding.key(), abilityType);
                default -> {
                }
            }
        }
        ITEM_ABILITIES = Collections.unmodifiableMap(itemAbilities);
        BLOCK_ABILITIES = Collections.unmodifiableMap(blockAbilities);
        FOOD_ABILITIES = Collections.unmodifiableMap(foodAbilities);
        TOOL_MATERIAL_ABILITIES = Collections.unmodifiableMap(toolMaterialAbilities);
        EQUIPMENT_ITEM_ABILITIES = Collections.unmodifiableMap(equipmentItemAbilities);
        EQUIPMENT_MATERIAL_ABILITIES = Collections.unmodifiableMap(equipmentMaterialAbilities);
        TRADE_ABILITIES = Collections.unmodifiableMap(tradeAbilities);
        LANDMARK_ABILITIES = Collections.unmodifiableMap(landmarkAbilities);
        PORTAL_ABILITIES = Collections.unmodifiableMap(portalAbilities);
    }

    private AbilityResolvers() {
    }

    public static Optional<AbilityType> findItemAbility(String itemKey) {
        return findByNormalizedKey(itemKey, ITEM_ABILITIES);
    }

    public static Optional<AbilityType> findBlockAbility(String blockKey) {
        return findByNormalizedKey(blockKey, BLOCK_ABILITIES);
    }

    public static Optional<AbilityType> findFoodAbility(String foodKey) {
        return findByNormalizedKey(foodKey, FOOD_ABILITIES);
    }

    public static Optional<AbilityType> findToolMaterialAbility(String toolMaterialKey) {
        return findByNormalizedKey(toolMaterialKey, TOOL_MATERIAL_ABILITIES);
    }

    public static Optional<AbilityType> findEquipmentAbility(EquipmentDescriptor descriptor) {
        if (descriptor == null) {
            return Optional.empty();
        }
        Optional<AbilityType> byItem = findByNormalizedKey(descriptor.itemKey(), EQUIPMENT_ITEM_ABILITIES);
        if (byItem.isPresent()) {
            return byItem;
        }
        return findByNormalizedKey(descriptor.materialKey(), EQUIPMENT_MATERIAL_ABILITIES);
    }

    public static Optional<AbilityType> findTradeAbility(String tradeTargetKey) {
        return findByNormalizedKey(tradeTargetKey, TRADE_ABILITIES);
    }

    public static Optional<AbilityType> findLandmarkAbility(LandmarkType landmarkType) {
        if (landmarkType == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(LANDMARK_ABILITIES.get(landmarkType));
    }

    public static Optional<AbilityType> findPortalAbility(String portalKey) {
        return findByNormalizedKey(portalKey, PORTAL_ABILITIES);
    }

    public static Optional<AbilityType> findAbilityByAdvancementId(String advancementId) {
        return AbilityType.findByAdvancementId(advancementId);
    }

    public static String findAdvancementId(AbilityType abilityType) {
        return abilityType.getAdvancementId();
    }

    private static Optional<AbilityType> findByNormalizedKey(String key, Map<String, AbilityType> abilities) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(abilities.get(AbilityBinding.normalizeKey(key)));
    }
}
