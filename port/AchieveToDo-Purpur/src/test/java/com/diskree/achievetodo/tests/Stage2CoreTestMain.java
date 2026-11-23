package com.diskree.achievetodo.tests;

import com.diskree.achievetodo.ability.AbilityModelTest;
import com.diskree.achievetodo.ability.AbilityResolversTest;
import com.diskree.achievetodo.bootstrap.BootstrapValidationTest;
import com.diskree.achievetodo.datapack.EmbeddedDatapackResourcesTest;
import com.diskree.achievetodo.landmark.FeatureLandmarkDetectorTest;
import com.diskree.achievetodo.landmark.FeatureLandmarkScanPolicyTest;
import com.diskree.achievetodo.landmark.LandmarkEngineTest;
import com.diskree.achievetodo.landmark.LandmarkModelAndPersistenceTest;
import com.diskree.achievetodo.landmark.StructureLandmarkDetectorTest;
import com.diskree.achievetodo.progression.AbilityStateResolverTest;
import com.diskree.achievetodo.progression.ChaosProgressionGeneratorTest;
import com.diskree.achievetodo.progression.ProgressionConfigServiceTest;
import com.diskree.achievetodo.progression.ProgressionsTest;
import com.diskree.achievetodo.tracking.TrackingCoreTest;

public final class Stage2CoreTestMain {

    private Stage2CoreTestMain() {
    }

    public static void main(String[] args) {
        AbilityModelTest.run();
        AbilityResolversTest.run();
        AbilityStateResolverTest.run();
        ProgressionsTest.run();
        ChaosProgressionGeneratorTest.run();
        ProgressionConfigServiceTest.run();
        EmbeddedDatapackResourcesTest.run();
        BootstrapValidationTest.run();
        LandmarkModelAndPersistenceTest.run();
        StructureLandmarkDetectorTest.run();
        FeatureLandmarkDetectorTest.run();
        FeatureLandmarkScanPolicyTest.run();
        LandmarkEngineTest.run();
        TrackingCoreTest.run();
        System.out.println("Stage 2 core, Stage 3 datapack, Stage 4 landmark, Stage 5 restriction-core, and Stage 6 tracking-core tests passed.");
    }
}
