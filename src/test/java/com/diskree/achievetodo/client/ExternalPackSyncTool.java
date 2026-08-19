package com.diskree.achievetodo.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class ExternalPackSyncTool {

    private ExternalPackSyncTool() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Expected arguments: <globalPacksDir> <savesDir>");
        }
        Path globalPacksDirectory = Path.of(args[0]);
        Path savesDirectory = Path.of(args[1]);
        if (Files.notExists(savesDirectory)) {
            return;
        }
        try (Stream<Path> worlds = Files.list(savesDirectory)) {
            worlds
                .filter(Files::isDirectory)
                .forEach(world -> syncWorld(globalPacksDirectory, world.resolve("datapacks")));
        }
    }

    private static void syncWorld(Path globalPacksDirectory, Path worldPacksDirectory) {
        if (Files.notExists(worldPacksDirectory) || !Files.isDirectory(worldPacksDirectory)) {
            return;
        }
        for (ExternalPack externalPack : ExternalPack.values()) {
            Path worldPack = worldPacksDirectory.resolve(externalPack.getFileName());
            if (Files.notExists(worldPack)) {
                continue;
            }
            Path globalPack = globalPacksDirectory.resolve(externalPack.getFileName());
            if (Files.notExists(globalPack)) {
                continue;
            }
            try {
                ExternalPackCompatibility.copyForWorld(globalPack, worldPack, externalPack);
            } catch (IOException e) {
                throw new RuntimeException("Failed to sync " + worldPack, e);
            }
        }
    }
}
