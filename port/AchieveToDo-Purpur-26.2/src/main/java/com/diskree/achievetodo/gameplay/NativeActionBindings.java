package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.BiPredicate;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/** S4H transactions query the existing S3C source synchronously; no permission snapshot is retained. */
final class NativeActionBindings implements AutoCloseable {
    static final Set<AbilityType> ABILITIES = Set.of(
        AbilityType.ATTACK_WITH_MACE, AbilityType.ATTACK_WITH_TRIDENT,
        AbilityType.SHOOT_BOW, AbilityType.SHOOT_CROSSBOW, AbilityType.USE_SHIELD,
        AbilityType.USE_OMINOUS_BOTTLE, AbilityType.USE_ENDER_EYE,
        AbilityType.GLIDE_WITH_FIREWORKS, AbilityType.USE_COMPOSTER, AbilityType.USE_CAMPFIRE,
        AbilityType.CHARGE_RESPAWN_ANCHOR, AbilityType.USE_JUKEBOX, AbilityType.PLACE_END_CRYSTAL);
    private final Object token = new Object();
    private final Method retire;

    NativeActionBindings(PlayerProgressionLookup progression) {
        try {
            Class<?> nativePlayer = Class.forName("net.minecraft.world.entity.player.Player");
            Method bukkit = nativePlayer.getMethod("getBukkitEntity");
            BiPredicate<Object, String> guard = (actor, id) -> {
                try {
                    Player player = (Player) bukkit.invoke(actor);
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
                    AbilityType ability = AbilityType.valueOf(id);
                    if (!ABILITIES.contains(ability) && ability != AbilityType.USE_SPYGLASS)
                        throw new IllegalArgumentException("Unknown native action " + id);
                    return !GameplayGate.allows(progression.getProgressionState(player.getUniqueId()),
                        player.getUniqueId(), false, ability);
                } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            };
            Class<?> agent = ClassLoader.getSystemClassLoader().loadClass(
                "com.diskree.achievetodo.gameplay.nativeguard.NativeActionAgent");
            retire = agent.getMethod("retire", Object.class);
            agent.getMethod("install", Object.class, BiPredicate.class, ClassLoader.class)
                .invoke(null, token, guard, nativePlayer.getClassLoader());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("S4H native actions unavailable; pinned same-JAR startup agent required", failure);
        }
    }

    @Override public void close() {
        try { retire.invoke(null, token); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("S4H retirement failed", failure); }
    }
}
