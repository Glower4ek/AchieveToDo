package com.diskree.achievetodo.ui.bukkit;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

final class BukkitUiPresentationPreferenceRepository {

    private static final String KEY_PREFIX = "player.";
    private final Path file;
    private final Map<UUID, UiPresentationMode> preferencesByPlayer = new LinkedHashMap<>();

    BukkitUiPresentationPreferenceRepository(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        load();
    }

    UiPresentationMode get(UUID playerId) {
        return preferencesByPlayer.getOrDefault(playerId, UiPresentationMode.FALLBACK);
    }

    void set(UUID playerId, UiPresentationMode mode) {
        preferencesByPlayer.put(Objects.requireNonNull(playerId, "playerId"), Objects.requireNonNull(mode, "mode"));
        save();
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read UI presentation preferences " + file, exception);
        }
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(KEY_PREFIX)) {
                continue;
            }
            String rawPlayerId = key.substring(KEY_PREFIX.length());
            Optional<UiPresentationMode> mode = UiPresentationMode.fromCommandValue(properties.getProperty(key));
            if (mode.isEmpty()) {
                continue;
            }
            try {
                preferencesByPlayer.put(UUID.fromString(rawPlayerId), mode.get());
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed persisted keys from older or external edits.
            }
        }
    }

    private void save() {
        Properties properties = new Properties();
        for (Map.Entry<UUID, UiPresentationMode> entry : preferencesByPlayer.entrySet()) {
            properties.setProperty(KEY_PREFIX + entry.getKey(), entry.getValue().commandValue());
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                properties.store(writer, "AchieveToDo UI presentation preferences");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write UI presentation preferences " + file, exception);
        }
    }
}
