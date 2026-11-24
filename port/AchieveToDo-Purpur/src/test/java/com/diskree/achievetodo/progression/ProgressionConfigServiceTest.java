package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;
import static com.diskree.achievetodo.tests.TestSupport.assertFalse;
import static com.diskree.achievetodo.tests.TestSupport.assertTrue;

public final class ProgressionConfigServiceTest {

    private final ProgressionConfigService service = new ProgressionConfigService();

    public static void run() {
        ProgressionConfigServiceTest test = new ProgressionConfigServiceTest();
        test.builtInModeGeneratesPersistedConfigWhenMissing();
        test.chaosModeUsesSeedSpecificConfigName();
        test.customConfigNormalizesMissingAbilitiesAndClampsUpperBound();
        test.builtInModeReusesUpToDateExistingFile();
        test.abandonedCampIsBackfilledLikeTheFabricMod();
    }

    /**
     * 0.4.0: a 0.3.0 configuration has no abandoned camp key. Same values as the Fabric mod's
     * AbandonedCampBackfillTest: presets 93/121/148, CHAOS seed 0 -> 407 and seed 42 -> 2, custom -1 (file untouched).
     */
    private void abandonedCampIsBackfilledLikeTheFabricMod() {
        String camp = AbilityType.INTERACT_INSIDE_ABANDONED_CAMP.getName();
        StringBuilder old = new StringBuilder("version = 1\n\n[abilities]\n");
        for (AbilityType ability : AbilityType.values()) {
            if (ability != AbilityType.INTERACT_INSIDE_ABANDONED_CAMP) old.append(ability.getName()).append(" = 7\n");
        }
        Map<String, Integer> presets = Map.of("easy", 93, "normal", 121, "hard", 148);
        for (var entry : presets.entrySet()) {
            InMemoryRepository repository = new InMemoryRepository();
            repository.write(entry.getKey() + ".toml", old.toString());
            var resolved = service.resolve(new WorldProgressionConfig(entry.getKey()), 0L, repository, false);
            assertEquals(entry.getValue(), resolved.definition().requirements().getRequiredCount(AbilityType.INTERACT_INSIDE_ABANDONED_CAMP));
            assertEquals(7, resolved.definition().requirements().getRequiredCount(AbilityType.USE_COPPER_TOOLS));
            assertEquals(old + camp + " = " + entry.getValue() + "\n", repository.files.get(entry.getKey() + ".toml"));
        }
        for (long seed : new long[]{0L, 42L}) {
            InMemoryRepository repository = new InMemoryRepository();
            repository.write("chaos_" + seed + ".toml", old.toString());
            var resolved = service.resolve(new WorldProgressionConfig("chaos"), seed, repository, false);
            assertEquals(seed == 0L ? 407 : 2, resolved.definition().requirements().getRequiredCount(AbilityType.INTERACT_INSIDE_ABANDONED_CAMP));
        }
        InMemoryRepository repository = new InMemoryRepository();
        repository.write("custom.toml", "[abilities]\njump = 17\n");
        var custom = service.resolve(new WorldProgressionConfig("custom"), 0L, repository, false);
        assertEquals(-1, custom.definition().requirements().getRequiredCount(AbilityType.INTERACT_INSIDE_ABANDONED_CAMP));
        assertEquals("[abilities]\njump = 17\n", repository.files.get("custom.toml"));
    }

    private void builtInModeGeneratesPersistedConfigWhenMissing() {
        InMemoryRepository repository = new InMemoryRepository();
        ResolvedProgressionConfig resolved = service.resolve(new WorldProgressionConfig("normal"), 77L, repository);
        assertEquals("normal.toml", resolved.fileName());
        assertEquals(ProgressionModeType.NORMAL, resolved.definition().builtInMode());
        assertTrue(resolved.generated());
        assertTrue(repository.files.containsKey("normal.toml"));
    }

    private void chaosModeUsesSeedSpecificConfigName() {
        InMemoryRepository repository = new InMemoryRepository();
        ResolvedProgressionConfig resolved = service.resolve(new WorldProgressionConfig("chaos"), 99L, repository);
        assertEquals("chaos_99.toml", resolved.fileName());
        assertEquals("chaos_99", resolved.definition().id());
        assertEquals(ProgressionModeType.CHAOS, resolved.definition().builtInMode());
    }

    private void customConfigNormalizesMissingAbilitiesAndClampsUpperBound() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.write("custom.toml", """
            version = 7

            [abilities]
            open_chest = 999999
            vision = -1
            unknown_ability = 17
            """);
        ResolvedProgressionConfig resolved = service.resolve(new WorldProgressionConfig("custom"), 0L, repository);
        assertFalse(resolved.generated());
        assertEquals(7, resolved.definition().version());
        assertEquals(ProgressionConstants.TOTAL_ADVANCEMENTS_COUNT, resolved.definition().requirements().getRequiredCount(AbilityType.OPEN_CHEST));
        assertEquals(-1, resolved.definition().requirements().getRequiredCount(AbilityType.VISION));
        assertEquals(0, resolved.definition().requirements().getRequiredCount(AbilityType.JUMP));
    }

    private void builtInModeReusesUpToDateExistingFile() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.write("easy.toml", """
            version = 1

            [abilities]
            vision = 123
            """);
        ResolvedProgressionConfig resolved = service.resolve(new WorldProgressionConfig("easy"), 0L, repository);
        assertFalse(resolved.generated());
        assertEquals(123, resolved.definition().requirements().getRequiredCount(AbilityType.VISION));
    }

    private static final class InMemoryRepository implements ProgressionConfigRepository {
        private final Map<String, String> files = new HashMap<>();

        @Override
        public Optional<String> read(String fileName) {
            return Optional.ofNullable(files.get(fileName));
        }

        @Override
        public void write(String fileName, String content) {
            files.put(fileName, content);
        }
    }
}
