package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import java.lang.reflect.Method;
import java.util.*;

/** Canonical findEquipmentEquipAbility uses these exact registered Item references, not EQUIPPABLE. */
final class NativeEquipmentAbilities {
    static final Map<AbilityType, List<String>> ITEMS;
    static {
        var items = new EnumMap<AbilityType, List<String>>(AbilityType.class);
        for (String material : List.of("COPPER", "GOLDEN", "LEATHER", "CHAINMAIL", "IRON", "DIAMOND", "NETHERITE"))
            items.put(AbilityType.valueOf("EQUIP_" + material + "_ARMOR"),
                List.of(material + "_HELMET", material + "_CHESTPLATE", material + "_LEGGINGS", material + "_BOOTS"));
        items.put(AbilityType.EQUIP_TURTLE_HELMET, List.of("TURTLE_HELMET"));
        items.put(AbilityType.EQUIP_ELYTRA, List.of("ELYTRA"));
        ITEMS = Map.copyOf(items);
    }
    private final IdentityHashMap<Object, AbilityType> identities = new IdentityHashMap<>();
    private final Method getItem;

    NativeEquipmentAbilities() {
        try {
            getItem = Class.forName("net.minecraft.world.item.ItemStack").getMethod("getItem");
            var items = Class.forName("net.minecraft.world.item.Items");
            for (var entry : ITEMS.entrySet()) for (String item : entry.getValue())
                if (identities.put(items.getField(item).get(null), entry.getKey()) != null)
                    throw new IllegalStateException("Duplicate equipment identity: " + item);
        } catch (ReflectiveOperationException unavailable) { throw new IllegalStateException("Pinned equipment contract unavailable", unavailable); }
    }

    AbilityType apply(Object stack) {
        try { return identities.get(getItem.invoke(stack)); }
        catch (ReflectiveOperationException unavailable) { throw new IllegalStateException("Native equipment lookup failed", unavailable); }
    }

    static Set<AbilityType> abilities() {
        var result = EnumSet.copyOf(ITEMS.keySet());
        result.addAll(List.of(AbilityType.PUT_IN_BUNDLE, AbilityType.OPEN_ENDER_CHEST, AbilityType.OPEN_SHULKER_BOX));
        return Set.copyOf(result);
    }
}
