package com.diskree.achievetodo.tests;

import java.util.Objects;

public final class TestSupport {

    private TestSupport() {
    }

    public static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    public static void assertTrue(boolean condition, MessageSupplier messageSupplier) {
        if (!condition) {
            throw new AssertionError(messageSupplier.message());
        }
    }

    public static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("Expected condition to be false");
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void assertNotEquals(Object unexpected, Object actual) {
        if (Objects.equals(unexpected, actual)) {
            throw new AssertionError("Did not expect <" + actual + ">");
        }
    }

    public static void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new AssertionError("Expected same instance <" + expected + "> but was <" + actual + ">");
        }
    }

    @FunctionalInterface
    public interface MessageSupplier {
        String message();
    }
}
