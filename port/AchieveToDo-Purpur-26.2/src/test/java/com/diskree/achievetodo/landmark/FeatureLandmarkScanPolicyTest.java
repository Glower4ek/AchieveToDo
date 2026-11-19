package com.diskree.achievetodo.landmark;

import com.diskree.achievetodo.tests.TestSupport;

public final class FeatureLandmarkScanPolicyTest {

    private FeatureLandmarkScanPolicyTest() {
    }

    public static void run() {
        verifyPopulateScansUnmarkedChunk();
        verifyCompatibilityScansUnmarkedChunk();
        verifyMarkedChunkDoesNotRescan();
        verifyExistingFeatureRecordOnlyMarksComplete();
    }

    private static void verifyPopulateScansUnmarkedChunk() {
        TestSupport.assertEquals(
            FeatureLandmarkScanPolicy.Decision.SCAN_AND_MARK,
            FeatureLandmarkScanPolicy.decide(false, false, FeatureLandmarkScanPolicy.Trigger.POPULATION)
        );
    }

    private static void verifyCompatibilityScansUnmarkedChunk() {
        TestSupport.assertEquals(
            FeatureLandmarkScanPolicy.Decision.SCAN_AND_MARK,
            FeatureLandmarkScanPolicy.decide(false, false, FeatureLandmarkScanPolicy.Trigger.COMPATIBILITY)
        );
    }

    private static void verifyMarkedChunkDoesNotRescan() {
        TestSupport.assertEquals(
            FeatureLandmarkScanPolicy.Decision.SKIP,
            FeatureLandmarkScanPolicy.decide(true, false, FeatureLandmarkScanPolicy.Trigger.COMPATIBILITY)
        );
    }

    private static void verifyExistingFeatureRecordOnlyMarksComplete() {
        TestSupport.assertEquals(
            FeatureLandmarkScanPolicy.Decision.MARK_ONLY,
            FeatureLandmarkScanPolicy.decide(false, true, FeatureLandmarkScanPolicy.Trigger.COMPATIBILITY)
        );
    }
}
