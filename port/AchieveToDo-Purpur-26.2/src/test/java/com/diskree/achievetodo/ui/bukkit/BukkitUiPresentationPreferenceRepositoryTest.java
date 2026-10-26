package com.diskree.achievetodo.ui.bukkit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;

public final class BukkitUiPresentationPreferenceRepositoryTest {

    private BukkitUiPresentationPreferenceRepositoryTest() {
    }

    public static void run() {
        try {
            Path directory = Files.createTempDirectory("atd-ui-presentation-test");
            Path file = directory.resolve("presentation.properties");
            UUID playerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            BukkitUiPresentationPreferenceRepository repository = new BukkitUiPresentationPreferenceRepository(file);
            assertEquals(UiPresentationMode.FALLBACK, repository.get(playerId));

            repository.set(playerId, UiPresentationMode.ENHANCED);
            BukkitUiPresentationPreferenceRepository reloaded = new BukkitUiPresentationPreferenceRepository(file);
            assertEquals(UiPresentationMode.ENHANCED, reloaded.get(playerId));
        } catch (IOException exception) {
            throw new AssertionError("Unexpected UI presentation preference repository IO failure", exception);
        }
    }
}
