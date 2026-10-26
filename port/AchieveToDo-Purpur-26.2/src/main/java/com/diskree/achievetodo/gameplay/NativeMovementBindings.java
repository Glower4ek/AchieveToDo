package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BiPredicate;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/** S4L transactions query the existing S3C source synchronously; no permission snapshot is retained. */
final class NativeMovementBindings implements AutoCloseable {
    static final Set<AbilityType> ABILITIES = Set.of(AbilityType.JUMP, AbilityType.SNEAK, AbilityType.SPRINT, AbilityType.SWIM);
    private final Object token = new Object();
    private final Method retire;

    NativeMovementBindings(PlayerProgressionLookup progression) {
        try {
            Class<?> nativePlayer = Class.forName("net.minecraft.world.entity.player.Player");
            Method bukkit = nativePlayer.getMethod("getBukkitEntity");
            BiPredicate<Object, String> guard = (actor, id) -> {
                try {
                    Player player = (Player) bukkit.invoke(actor);
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
                    AbilityType ability = AbilityType.valueOf(id);
                    if (!ABILITIES.contains(ability)) throw new IllegalArgumentException("Unknown native action " + id);
                    return !GameplayGate.allows(progression.getProgressionState(player.getUniqueId()),
                        player.getUniqueId(), false, ability);
                } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            };
            Class<?> agent = ClassLoader.getSystemClassLoader().loadClass(
                "com.diskree.achievetodo.gameplay.nativeguard.NativeMovementAgent");
            retire = agent.getMethod("retire", Object.class);
            agent.getMethod("install", Object.class, BiPredicate.class, ClassLoader.class)
                .invoke(null, token, guard, nativePlayer.getClassLoader());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("S4L native actions unavailable; pinned same-JAR startup agent required", failure);
        }
    }

    @Override public void close() {
        try { retire.invoke(null, token); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("S4L retirement failed", failure); }
    }
}
