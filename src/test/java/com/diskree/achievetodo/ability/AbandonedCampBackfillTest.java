package com.diskree.achievetodo.ability;

import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension.ConfigurationIO;
import com.diskree.achievetodo.injection.extension.main.LevelInfoExtension.ConfigurationOrigin;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 0.4.0 adds the abandoned camp (Minecraft 26.3). A world configuration written by 0.3.0 has no key for it: the
 * built-in presets get the preset's fixed value appended, CHAOS a seed-derived value, a custom configuration keeps
 * it permanently locked (-1) without touching the file. Every other value stays as it was.
 */
final class AbandonedCampBackfillTest {
    // A literal: touching AbilityType before Bootstrap.bootStrap() would poison the class for the whole test JVM.
    private static final String CAMP = "interact_inside_abandoned_camp";

    @TempDir Path root;

    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    /** A 0.3.0 configuration: every ability except the camp, each set to 7. */
    private static String release030Config(int version) {
        StringBuilder text = new StringBuilder("version = " + version + "\n\n[abilities]\n");
        for (AbilityType ability : AbilityType.values()) {
            if (ability != AbilityType.INTERACT_INSIDE_ABANDONED_CAMP) text.append(ability.getName()).append(" = 7\n");
        }
        return text.toString();
    }

    @Test
    void freshPresetsOpenTheCampMidwayBetweenBuriedTreasureAndIgloo() {
        for (Map<AbilityType, Integer> preset : java.util.List.of(Progressions.getEasyProgression(),
            Progressions.getNormalProgression(), Progressions.getHardProgression())) {
            int treasure = preset.get(AbilityType.INTERACT_INSIDE_BURIED_TREASURE), igloo = preset.get(AbilityType.INTERACT_INSIDE_IGLOO);
            // Midpoint, rounded up when the gap is odd (normal: 110..131).
            assertEquals((treasure + igloo + 1) / 2, preset.get(AbilityType.INTERACT_INSIDE_ABANDONED_CAMP));
        }
        assertEquals(93, Progressions.ABANDONED_CAMP_EASY);
        assertEquals(121, Progressions.ABANDONED_CAMP_NORMAL);
        assertEquals(148, Progressions.ABANDONED_CAMP_HARD);
    }

    @Test
    void builtInPresetsAppendTheirFixedValue() throws Exception {
        Map<String, Integer> expected = Map.of("easy", 93, "normal", 121, "hard", 148);
        for (var entry : expected.entrySet()) {
            Path file = root.resolve(entry.getKey() + ".toml");
            String before = release030Config(ProgressionModeType.findByName(entry.getKey()).getVersion());
            Files.writeString(file, before);
            Map<AbilityType, Integer> map = ConfigurationIO.load(root, entry.getKey(), 0, ConfigurationOrigin.SAVED);
            assertEquals(entry.getValue(), map.get(AbilityType.INTERACT_INSIDE_ABANDONED_CAMP), entry.getKey());
            for (AbilityType ability : AbilityType.values()) {
                if (ability != AbilityType.INTERACT_INSIDE_ABANDONED_CAMP) assertEquals(7, map.get(ability), ability.getName());
            }
            assertEquals(before + CAMP + " = " + entry.getValue() + "\n", Files.readString(file));
            assertEquals(map, ConfigurationIO.load(root, entry.getKey(), 0, ConfigurationOrigin.SAVED));
        }
    }

    @Test
    void chaosSamplesFromTheSeedAndIsStable() throws Exception {
        for (long seed : new long[]{0L, 42L}) {
            Path file = root.resolve("chaos_" + seed + ".toml");
            String before = release030Config(ProgressionModeType.CHAOS.getVersion());
            Files.writeString(file, before);
            int value = ConfigurationIO.load(root, "chaos", seed, ConfigurationOrigin.SAVED).get(AbilityType.INTERACT_INSIDE_ABANDONED_CAMP);
            assertEquals(seed == 0L ? 407 : CHAOS_SEED_42, value, "seed " + seed);
            assertEquals(before + CAMP + " = " + value + "\n", Files.readString(file));
        }
    }

    /** The plugin's AbandonedCampBackfillTest pins the same values (seed 0: 407, seed 42: 2). */
    static final int CHAOS_SEED_42 = 2;

    @Test
    void customConfigurationKeepsTheCampLockedAndTheFileUntouched() throws Exception {
        Path file = root.resolve("custom.toml");
        Files.writeString(file, "[abilities]\njump = 17\n");
        byte[] bytes = Files.readAllBytes(file);
        Map<AbilityType, Integer> map = ConfigurationIO.load(root, "custom", 0, ConfigurationOrigin.FRESH);
        assertEquals(-1, map.get(AbilityType.INTERACT_INSIDE_ABANDONED_CAMP));
        assertEquals(17, map.get(AbilityType.JUMP));
        assertArrayEquals(bytes, Files.readAllBytes(file));
        Files.writeString(file, "[abilities]\n" + CAMP + " = 12\n");
        assertEquals(12, ConfigurationIO.load(root, "custom", 0, ConfigurationOrigin.FRESH).get(AbilityType.INTERACT_INSIDE_ABANDONED_CAMP));
        assertEquals("[abilities]\n" + CAMP + " = 12\n", new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }
}
