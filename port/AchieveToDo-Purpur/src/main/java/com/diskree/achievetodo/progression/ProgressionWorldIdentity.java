package com.diskree.achievetodo.progression;

import java.util.Objects;
import java.util.UUID;

/** S3A deliberately binds one dimension, not an inferred group of similarly named worlds. */
public record ProgressionWorldIdentity(UUID uuid, String key) {
    public ProgressionWorldIdentity {
        Objects.requireNonNull(uuid, "uuid");
        if (uuid.equals(new UUID(0, 0)) || key == null || !key.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) {
            throw new IllegalArgumentException("Explicit nonzero world UUID and resource key required");
        }
    }

    public boolean matches(UUID candidateUuid, String candidateKey) {
        return uuid.equals(candidateUuid) && key.equals(candidateKey);
    }

    public void verifySaved(String savedUuid, String savedKey) {
        if (!uuid.toString().equals(savedUuid) || !key.equals(savedKey)) {
            throw new IllegalStateException("WORLD_IDENTITY_MISMATCH: progression selection belongs to another world or lacks an explicit binding");
        }
    }
}
