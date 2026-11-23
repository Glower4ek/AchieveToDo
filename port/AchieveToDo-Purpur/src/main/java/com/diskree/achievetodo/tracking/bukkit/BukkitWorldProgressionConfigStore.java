package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.progression.WorldProgressionConfig;
import com.diskree.achievetodo.progression.WorldProgressionConfigStore;
import com.diskree.achievetodo.progression.ProgressionWorldIdentity;
import org.bukkit.World;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

final class BukkitWorldProgressionConfigStore implements WorldProgressionConfigStore {

    private static final String CONFIG_NAME_KEY = "configName";
    private static final String WORLD_NAME_KEY = "worldName";
    private static final String WORLD_UID_KEY = "worldUid";

    private final Path file;
    private final World world;
    private final com.diskree.achievetodo.progression.ProgressionWorldGroup group;

    BukkitWorldProgressionConfigStore(Path file, World world) {
        this(file, world, null);
    }

    BukkitWorldProgressionConfigStore(Path file, World world, com.diskree.achievetodo.progression.ProgressionWorldGroup group) {
        this.group = group;
        this.file = Objects.requireNonNull(file, "file");
        this.world = Objects.requireNonNull(world, "world");
    }

    @Override
    public Optional<WorldProgressionConfig> load() {
        if (Files.notExists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        Properties properties = new Properties() {
            @Override public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) throw new IllegalArgumentException("Duplicate selection property: " + key);
                return super.put(key, value);
            }
        };
        try (InputStream inputStream = Files.newInputStream(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read world progression config from " + file, exception);
        }
        if (group != null && (!group.groupId().equals(properties.getProperty("groupId")) ||
            !group.configId().toString().equals(properties.getProperty("configId")))) throw new IllegalStateException("CONFIG_OWNER_MISMATCH");
        String configName = properties.getProperty(CONFIG_NAME_KEY);
        new ProgressionWorldIdentity(world.getUID(), world.getKey().toString())
            .verifySaved(properties.getProperty(WORLD_UID_KEY), properties.getProperty("worldKey"));
        if (configName == null || configName.isBlank()) {
            throw new IllegalStateException("Persisted world progression selection has no configName: " + file);
        }
        return Optional.of(new WorldProgressionConfig(configName));
    }

    @Override
    public void save(WorldProgressionConfig config) {
        Objects.requireNonNull(config, "config");
        Properties properties = new Properties();
        if (group != null) { properties.setProperty("groupId",group.groupId()); properties.setProperty("configId",group.configId().toString()); }
        properties.setProperty(CONFIG_NAME_KEY, config.configName());
        properties.setProperty(WORLD_NAME_KEY, world.getName());
        properties.setProperty(WORLD_UID_KEY, world.getUID().toString());
        properties.setProperty("worldKey", world.getKey().toString());
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "world-selection-", ".tmp");
            try {
                try (OutputStream outputStream = Files.newOutputStream(temporary)) {
                    properties.store(outputStream, "AchieveToDo world progression selection");
                }
                try (var channel = java.nio.channels.FileChannel.open(temporary, java.nio.file.StandardOpenOption.WRITE)) { channel.force(true); }
                Files.move(temporary, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally { Files.deleteIfExists(temporary); }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to persist world progression config into " + file, exception);
        }
    }
}
