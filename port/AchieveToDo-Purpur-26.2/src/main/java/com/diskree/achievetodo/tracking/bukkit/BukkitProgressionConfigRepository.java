package com.diskree.achievetodo.tracking.bukkit;

import com.diskree.achievetodo.progression.ProgressionConfigRepository;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.*;

final class BukkitProgressionConfigRepository implements ProgressionConfigRepository {
    private final Path directory;
    BukkitProgressionConfigRepository(Path directory) { this.directory = directory.toAbsolutePath().normalize(); }
    private Path target(String fileName) throws IOException {
        if (!fileName.matches("[A-Za-z0-9_-]+\\.toml")) throw new IllegalArgumentException("Unsafe config path");
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS) && !directory.toRealPath().equals(directory)) throw new IOException("Aliased config root");
        Path file = directory.resolve(fileName);
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS) && (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file))) throw new IOException("Unsafe config file");
        return file;
    }
    @Override public Optional<String> read(String fileName) {
        try {
            Path file = target(fileName);
            if (Files.notExists(file, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
            if (Files.size(file) > 16*1024*1024) throw new IOException("Configuration too large");
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) { throw new IllegalStateException("Unable to read progression config", e); }
    }
    @Override public void write(String fileName, String content) { compareAndWrite(fileName, read(fileName), content); }
    @Override public void compareAndWrite(String fileName, Optional<String> expected, String content) {
        try {
            Path file = target(fileName);
            Files.createDirectories(directory);
            file = target(fileName);
            Path lockPath = directory.resolve(".progression.lock");
            try (FileChannel lockChannel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                 var lock = lockChannel.tryLock()) {
                if (lock == null || !read(fileName).equals(expected)) throw new IOException("Concurrent configuration transaction");
                Path temporary = Files.createTempFile(directory, "progression-", ".tmp");
                try {
                    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                    try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        while (buffer.hasRemaining()) channel.write(buffer);
                        channel.force(true);
                    }
                    if (!Files.readString(temporary, StandardCharsets.UTF_8).equals(content)) throw new IOException("Candidate readback mismatch");
                    if (!read(fileName).equals(expected)) throw new IOException("Configuration changed before publish");
                    // Unsupported atomic replacement is an error; never truncate authoritative input.
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } finally { Files.deleteIfExists(temporary); }
            }
        } catch (IOException e) { throw new IllegalStateException("Unable to publish progression config", e); }
    }
}
