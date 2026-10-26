package com.diskree.achievetodo.bridge;

/** Runs the same frozen logical vectors against the independently built client copy. */
public final class ClientCodecFixtureTest {
    public static void main(String[] args) { BridgeProjectionTest.main(args); BridgeLandmarksTest.main(args); }
}
