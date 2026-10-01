package com.diskree.achievetodo.certification;

import com.diskree.achievetodo.server.Constants;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;

public final class PhaseASmokeGameTest implements CustomTestMethodInvoker {

    @GameTest
    public void phaseAStaticInventoryLoads(GameTestHelper helper) throws Exception {
        if (Constants.TOTAL_ADVANCEMENTS_COUNT != PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS) {
            throw new IllegalStateException("Pinned total advancements count drifted from certified Phase A inventory");
        }
        if (PhaseACertification.generate(java.nio.file.Path.of("").toAbsolutePath().normalize()).inventory().isEmpty()) {
            throw new IllegalStateException("Certified Phase A inventory unexpectedly resolved to zero advancements");
        }
        helper.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        helper.setBlock(0, 0, 0, Blocks.AIR);
        method.invoke(this, helper);
    }
}
