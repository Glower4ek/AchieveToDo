package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import org.bukkit.inventory.ItemStack;

/** Pinned 26.2 native component identity, matching Fabric AbilityType.findEatFoodAbility's ==. */
public final class NativeFoodAbilities implements Function<ItemStack, AbilityType> {
    private static final Map<AbilityType, String> FOODS = Map.ofEntries(
        Map.entry(AbilityType.EAT_SALMON, "SALMON"), Map.entry(AbilityType.EAT_COD, "COD"),
        Map.entry(AbilityType.EAT_TROPICAL_FISH, "TROPICAL_FISH"), Map.entry(AbilityType.EAT_ROTTEN_FLESH, "ROTTEN_FLESH"),
        Map.entry(AbilityType.EAT_SPIDER_EYE, "SPIDER_EYE"), Map.entry(AbilityType.EAT_POISONOUS_POTATO, "POISONOUS_POTATO"),
        Map.entry(AbilityType.EAT_SUSPICIOUS_STEW, "SUSPICIOUS_STEW"), Map.entry(AbilityType.EAT_CHICKEN, "CHICKEN"),
        Map.entry(AbilityType.EAT_BEETROOT, "BEETROOT"), Map.entry(AbilityType.EAT_DRIED_KELP, "DRIED_KELP"),
        Map.entry(AbilityType.EAT_APPLE, "APPLE"), Map.entry(AbilityType.EAT_MUSHROOM_STEW, "MUSHROOM_STEW"),
        Map.entry(AbilityType.EAT_HONEY, "HONEY_BOTTLE"), Map.entry(AbilityType.EAT_GOLDEN_APPLE, "GOLDEN_APPLE"),
        Map.entry(AbilityType.EAT_ENCHANTED_GOLDEN_APPLE, "ENCHANTED_GOLDEN_APPLE"), Map.entry(AbilityType.EAT_CHORUS_FRUIT, "CHORUS_FRUIT"),
        Map.entry(AbilityType.EAT_BREAD, "BREAD"), Map.entry(AbilityType.EAT_COOKED_BEEF, "COOKED_BEEF"),
        Map.entry(AbilityType.EAT_COOKED_COD, "COOKED_COD"), Map.entry(AbilityType.EAT_RABBIT_STEW, "RABBIT_STEW"),
        Map.entry(AbilityType.EAT_MELON_SLICE, "MELON_SLICE"),
        Map.entry(AbilityType.EAT_COOKIE, "COOKIE"),
        Map.entry(AbilityType.EAT_BEETROOT_SOUP, "BEETROOT_SOUP"),
        Map.entry(AbilityType.EAT_MUTTON, "MUTTON"),
        Map.entry(AbilityType.EAT_PUFFERFISH, "PUFFERFISH"),
        Map.entry(AbilityType.EAT_PUMPKIN_PIE, "PUMPKIN_PIE"),
        Map.entry(AbilityType.EAT_RABBIT, "RABBIT"),
        Map.entry(AbilityType.EAT_BEEF, "BEEF"),
        Map.entry(AbilityType.EAT_PORKCHOP, "PORKCHOP"),
        Map.entry(AbilityType.EAT_BAKED_POTATO, "BAKED_POTATO"),
        Map.entry(AbilityType.EAT_COOKED_SALMON, "COOKED_SALMON"),
        Map.entry(AbilityType.EAT_COOKED_RABBIT, "COOKED_RABBIT"),
        Map.entry(AbilityType.EAT_COOKED_CHICKEN, "COOKED_CHICKEN"),
        Map.entry(AbilityType.EAT_COOKED_MUTTON, "COOKED_MUTTON"),
        Map.entry(AbilityType.EAT_COOKED_PORKCHOP, "COOKED_PORKCHOP"),
        Map.entry(AbilityType.EAT_GOLDEN_CARROT, "GOLDEN_CARROT"),
        Map.entry(AbilityType.EAT_SWEET_BERRIES, "SWEET_BERRIES"),
        Map.entry(AbilityType.EAT_GLOW_BERRIES, "GLOW_BERRIES"),
        Map.entry(AbilityType.EAT_CARROT, "CARROT"),
        Map.entry(AbilityType.EAT_POTATO, "POTATO")
    );
    private final IdentityHashMap<Object, AbilityType> identities;
    private final Function<ItemStack, Object> food;
    private Function<Object, Object> nativeFood;

    public NativeFoodAbilities() {
        try {
            Class<?> stack = Class.forName("net.minecraft.world.item.ItemStack");
            Class<?> component = Class.forName("net.minecraft.core.component.DataComponentType");
            Object key = Class.forName("net.minecraft.core.component.DataComponents").getField("FOOD").get(null);
            Method copy = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack")
                .getMethod("asNMSCopy", ItemStack.class);
            Method get = stack.getMethod("get", component);
            nativeFood = item -> {
                try { return get.invoke(item, key); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException("Native FOOD lookup failed", e); }
            };
            identities = new IdentityHashMap<>();
            Class<?> foods = Class.forName("net.minecraft.world.food.Foods");
            for (var entry : FOODS.entrySet()) {
                Object value = foods.getField(entry.getValue()).get(null);
                if (value == null || identities.put(value, entry.getKey()) != null)
                    throw new IllegalStateException("Non-unique native food identity: " + entry.getKey());
            }
            food = item -> {
                try { return get.invoke(copy.invoke(null, item), key); }
                catch (ReflectiveOperationException e) { throw new IllegalStateException("Native FOOD lookup failed", e); }
            };
        } catch (ReflectiveOperationException e) {
            // Fail startup through the existing readiness rollback; never substitute Material/equality matching.
            throw new IllegalStateException("S4D requires the pinned 26.2 native FOOD contract", e);
        }
    }

    NativeFoodAbilities(Map<Object, AbilityType> identities, Function<ItemStack, Object> food) {
        this.identities = new IdentityHashMap<>(identities);
        this.food = food;
    }

    public static Set<AbilityType> abilities() { return FOODS.keySet(); }

    static boolean plantable(AbilityType ability) {
        return ability == AbilityType.EAT_SWEET_BERRIES || ability == AbilityType.EAT_GLOW_BERRIES
            || ability == AbilityType.EAT_CARROT || ability == AbilityType.EAT_POTATO;
    }

    AbilityType nativeAbility(Object stack) { return identities.get(nativeFood.apply(stack)); }

    @Override public AbilityType apply(ItemStack item) {
        return item == null || item.isEmpty() ? null : identities.get(food.apply(item));
    }
}
