package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import com.diskree.achievetodo.ability.AbilityType;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/** Native transaction guards use S3C authority directly and store no unlock state. */
final class NativeTransactionBindings implements AutoCloseable {
    private final Object token = new Object();
    private final Method retire;

    NativeTransactionBindings(PlayerProgressionLookup progression, NativeFoodAbilities foods) {
        try {
            Class<?> nativePlayer = Class.forName("net.minecraft.world.entity.player.Player");
            Method bukkit = nativePlayer.getMethod("getBukkitEntity");
            var equipment = new NativeEquipmentAbilities();
            Function<Function<Object, AbilityType>, BiPredicate<Object, Object>> guard = resolver -> (actor, stack) -> {
                try {
                    Player player = (Player) bukkit.invoke(actor);
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
                    var ability = resolver.apply(stack);
                    if (ability == null) return false;
                    return !GameplayGate.allows(progression.getProgressionState(player.getUniqueId()),
                        player.getUniqueId(), false, ability);
                } catch (ReflectiveOperationException unavailable) {
                    throw new IllegalStateException("Native actor unavailable", unavailable);
                }
            };
            Class<?> agent = ClassLoader.getSystemClassLoader().loadClass(
                "com.diskree.achievetodo.gameplay.nativeguard.NativeTransactionAgent");
            retire = agent.getMethod("retire", Object.class);
            agent.getMethod("install", Object.class, Map.class, ClassLoader.class)
                .invoke(null, token, Map.of("food", guard.apply(foods::nativeAbility),
                    "equipment", guard.apply(equipment::apply),
                    "bundle", guard.apply(unused -> AbilityType.PUT_IN_BUNDLE),
                    "ender", guard.apply(unused -> AbilityType.OPEN_ENDER_CHEST),
                    "shulker", guard.apply(unused -> AbilityType.OPEN_SHULKER_BOX)), nativePlayer.getClassLoader());
        } catch (ReflectiveOperationException unavailable) {
            throw new IllegalStateException("S4F native guards unavailable; start pinned Purpur with -javaagent:<plugin JAR>", unavailable);
        }
    }

    @Override public void close() {
        try { retire.invoke(null, token); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Native guard retirement failed", failure); }
    }
}
