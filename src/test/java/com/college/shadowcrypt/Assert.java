package com.college.shadowcrypt;

/**
 * Lightweight zero-dependency assertion utilities for pure Java automated testing.
 */
public final class Assert {

    private Assert() {}

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    public static void equals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    public static void equals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    public static void notNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message + " (was null)");
        }
    }
}
