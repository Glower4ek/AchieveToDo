package com.diskree.achievetodo.core;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Release launch, agent half. Runs first thing in premain, before any native agent reads a property.
 *
 * <p>When the operator gave no {@code achievetodo.gameplay.mode}, this fills in the fixed switches of the
 * certified composition (mode {@code certification} plus the five {@code true} switches) as ordinary system
 * properties. Every agent and the plugin then read exactly the values the certified 14-flag command passed, so
 * the enforcement code itself is untouched. A launch that names the mode explicitly is left completely alone.
 *
 * <p>An explicitly given switch that disagrees with the certified value is never overridden: the profile is then
 * not applied, the mode stays unset, and the plugin refuses to start with the reason. Only java.* is used here,
 * because this class is loaded by the system class loader during premain.
 */
public final class ReleaseLaunchDefaults {
    public static final String MODE = "achievetodo.gameplay.mode";
    /** {@code release} when applied; {@code refused:<property>=<value>} when an explicit switch conflicted. */
    public static final String PROFILE = "achievetodo.launch.profile";
    public static final String RELEASE = "release";
    public static final String REFUSED_PREFIX = "refused:";
    /** Same list and order as {@link StartupModePolicy#CERTIFIED_SWITCHES}; a test keeps them equal. */
    public static final List<String> SWITCHES = List.of(
        "achievetodo.bridge.real",
        "achievetodo.progression.foundation",
        "achievetodo.bridge.groupScope",
        "achievetodo.landmarks.exactPrototype",
        "achievetodo.bridge.spike");

    /** @return the resulting value of {@link #PROFILE}, or null when the launch was explicit. */
    public static String apply(Function<String, String> get, BiConsumer<String, String> set) {
        if (get.apply(MODE) != null) return null; // explicit launch: certified rc.1 behaviour, nothing derived
        for (String name : SWITCHES) {
            String given = get.apply(name);
            // Boolean.getBoolean parity: only "true" in any case counts as true.
            if (given != null && !"true".equalsIgnoreCase(given)) {
                String refused = REFUSED_PREFIX + name + "=" + given;
                set.accept(PROFILE, refused);
                return refused;
            }
        }
        for (String name : SWITCHES) if (get.apply(name) == null) set.accept(name, "true");
        set.accept(MODE, "certification");
        set.accept(PROFILE, RELEASE);
        return RELEASE;
    }

    public static boolean active(Function<String, String> get) {
        return RELEASE.equals(get.apply(PROFILE)) && "certification".equals(get.apply(MODE));
    }

    private ReleaseLaunchDefaults() {}
}
