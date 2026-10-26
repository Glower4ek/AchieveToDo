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
