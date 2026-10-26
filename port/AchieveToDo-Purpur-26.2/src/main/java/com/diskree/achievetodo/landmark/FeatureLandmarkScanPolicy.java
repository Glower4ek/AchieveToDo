package com.diskree.achievetodo.landmark;

final class FeatureLandmarkScanPolicy {

    private FeatureLandmarkScanPolicy() {
    }

    static Decision decide(
        boolean featureScanComplete,
        boolean hasPersistedFeatureSourceRecord,
        Trigger trigger
    ) {
        if (trigger == Trigger.NONE || featureScanComplete) {
            return Decision.SKIP;
        }
        if (hasPersistedFeatureSourceRecord) {
            return Decision.MARK_ONLY;
        }
        return Decision.SCAN_AND_MARK;
    }

    enum Trigger {
        NONE,
        COMPATIBILITY,
        POPULATION
    }

    enum Decision {
        SKIP,
        MARK_ONLY,
        SCAN_AND_MARK
    }
}
