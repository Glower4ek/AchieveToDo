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
    private final GameplayDenialEffects denialEffects;

    NativeActionBindings(PlayerProgressionLookup progression) {
        denialEffects=new GameplayDenialEffects(progression);
        try {
            Class<?> nativePlayer = Class.forName("net.minecraft.world.entity.player.Player");
            Method bukkit = nativePlayer.getMethod("getBukkitEntity");
            Method combatRecord=Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeToolCombatInvocation",false,null).getMethod("record",Object.class,Runnable.class,boolean.class);
            BiPredicate<Object, String> guard = (actor, id) -> {
                try {
                    Player player = (Player) bukkit.invoke(actor);
                    if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
                    boolean combat=id.startsWith("COMBAT:");
                    AbilityType ability = AbilityType.valueOf(combat?id.substring(7):id);
                    if (!ABILITIES.contains(ability) && ability != AbilityType.USE_SPYGLASS)
                        throw new IllegalArgumentException("Unknown native action " + id);
                    boolean denied=!GameplayGate.allows(progression.getProgressionState(player.getUniqueId()),
                        player.getUniqueId(), false, ability);
                    if(denied && combat)combatRecord.invoke(null,actor,(Runnable)()->denialEffects.report(player,ability),true);
                    else if(denied)denialEffects.report(player,ability);
                    return denied;
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
        denialEffects.clear();
        try { retire.invoke(null, token); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("S4H retirement failed", failure); }
    }
}
