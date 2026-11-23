package com.diskree.achievetodo.core;

import java.util.Objects;

/**
 * Live gameplay admission. Players are admitted only while every part of the enforcement composition is up:
 * the plugin lifecycle, progression, the native guards, exact landmarks, the real bridge protocol and the group
 * scope.
 */
public final class GameplayAdmission {
    public enum Mode { RELEASE }
    private final Mode mode;

    /** {@code release}; {@code certification} is the pre-0.3.0 name of the same mode and stays accepted. */
    public static Mode mode(String value) {
        return switch (value) {
            case "release", "certification" -> Mode.RELEASE;
            default -> throw new IllegalArgumentException("ATD_UNKNOWN_GAMEPLAY_MODE");
        };
    }
    public GameplayAdmission(Mode mode) { this.mode = Objects.requireNonNull(mode); }
    public Mode mode() { return mode; }
    public boolean operational(boolean lifecycle, boolean progression, boolean hooks, boolean exact,
                               boolean realProtocol, boolean groupScope) {
        return lifecycle && progression && hooks && exact && realProtocol && groupScope;
    }
}
