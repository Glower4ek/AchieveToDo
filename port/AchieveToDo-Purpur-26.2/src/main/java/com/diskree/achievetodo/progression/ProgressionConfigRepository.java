package com.diskree.achievetodo.progression;

import java.util.Optional;

public interface ProgressionConfigRepository {
    Optional<String> read(String fileName);

    void write(String fileName, String content);
    default void compareAndWrite(String fileName, Optional<String> expected, String content) {
        synchronized (this) {
            if (!read(fileName).equals(expected)) throw new IllegalStateException("Configuration changed concurrently");
            write(fileName, content);
        }
    }
}
