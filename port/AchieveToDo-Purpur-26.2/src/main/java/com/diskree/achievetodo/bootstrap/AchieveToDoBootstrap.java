package com.diskree.achievetodo.bootstrap;

import io.papermc.paper.datapack.DatapackRegistrar;
import com.diskree.achievetodo.datapack.BacapPacks;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

public final class AchieveToDoBootstrap implements PluginBootstrap {

    private static final String EMBEDDED_DATAPACK_PATH = "/datapack";
    private static final String EMBEDDED_DATAPACK_ID = "embedded";

    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            DatapackRegistrar registrar = event.registrar();
            BacapPacks.beginDiscovery();
            try {
                // The accepted S2P-only fixture remains independent of gameplay content.
                if (!Boolean.getBoolean("achievetodo.bridge.spike") || Boolean.getBoolean("achievetodo.progression.foundation")) {
                    var directory = BacapPacks.directory();
                    BacapPacks.validate(directory);
                    // Operator installs these files in the level's standard datapacks directory.
                    // Native discovery owns loading/order; no second registration of BACAP.
                    BacapPacks.completeDiscovery();
                    context.getLogger().info("BACAP canonical 1.21/MC26.2 compat_26_2_r19 preflight verified for native world discovery; generation=" + BacapPacks.generation());
                }
                URI uri = Objects.requireNonNull(
                    AchieveToDoBootstrap.class.getResource(EMBEDDED_DATAPACK_PATH),
                    "Embedded datapack folder is missing from plugin resources"
                ).toURI();
                Objects.requireNonNull(
                    registrar.discoverPack(uri, EMBEDDED_DATAPACK_ID),
                    "Embedded synthetic datapack discovery failed: " + EMBEDDED_DATAPACK_ID
                );
                context.getLogger().info("Discovered embedded synthetic datapack: " + EMBEDDED_DATAPACK_ID);
            } catch (URISyntaxException | IOException exception) {
                throw new IllegalStateException("Failed to register embedded datapack", exception);
            }
        });
    }
}
