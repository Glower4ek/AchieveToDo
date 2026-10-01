package com.diskree.achievetodo.certification;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

@SuppressWarnings("UnstableApiUsage")
public final class PhaseAClientSmokeGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitFor(client -> client.player != null && client.level != null);
            context.computeOnClient(client -> {
                if (client.player == null) {
                    throw new IllegalStateException("Client player was not created");
                }
                if (client.level == null) {
                    throw new IllegalStateException("Client level was not created");
                }
                return client.player.getName().getString();
            });
            if (singleplayer.getConnection() == null) {
                throw new IllegalStateException("Integrated server connection is unavailable");
            }
            context.takeScreenshot("achievetodo-phase-a-client-smoke");
        }
    }
}
