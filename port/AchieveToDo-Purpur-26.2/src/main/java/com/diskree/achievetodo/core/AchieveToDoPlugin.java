package com.diskree.achievetodo.core;

import com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime;
import com.diskree.achievetodo.bridge.BukkitBridgeRuntime;
import com.diskree.achievetodo.gameplay.BukkitGameplayRuntime;
import java.util.logging.Level;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class AchieveToDoPlugin extends JavaPlugin {

    private final ServerRuntimeReadiness readiness = new ServerRuntimeReadiness();
    private BukkitProgressionRuntime progressionRuntime;
    private BukkitBridgeRuntime bridgeRuntime;
    private BukkitGameplayRuntime gameplayRuntime;
    private GameplayAdmission productAdmission;

    @Override
    public void onEnable() {
        try {
            readiness.beginStart();
            var build = io.papermc.paper.ServerBuildInfo.buildInfo();
            PinnedStartupContract.runtime(Runtime.version().feature(), build.minecraftVersionId(),
                build.brandName(), build.buildNumber().orElse(-1));
            // No agent (or a switch contradicting the certified value): refuse with the one-line start command
            // before the agent identity check, which would otherwise fail with a bare ClassNotFoundException.
            if (System.getProperty(StartupModePolicy.MODE) == null) StartupModePolicy.decide(System::getProperty, startupHints());
            PinnedStartupContract.installation(getFile().toPath());
            com.diskree.achievetodo.ui.advancement.AdvancementVisibilityAgent.verify(getServer().getClass().getClassLoader());
            if (ReleaseLaunchDefaults.active(System::getProperty)) {
                // Release launch: fill in only the per-installation values the operator did not give.
                var profile = new ReleaseLaunchProfile(System::getProperty, System::setProperty);
                profile.deriveInstallation(getDataFolder().toPath(), loadedWorlds());
                getLogger().warning("ATD_LAUNCH_PROFILE=release; derived=" + profile.derived());
            }
            // No silent default: a launch without an explicit mode is refused (see StartupModePolicy).
            var startup = StartupModePolicy.decide(System::getProperty, startupHints());
            startup.warnings().forEach(getLogger()::severe);
            // The native enforcement composition is the only one; the 0.2.x legacy diagnostic path is gone.
            if (!Boolean.getBoolean("achievetodo.landmarks.exactPrototype") || !Boolean.getBoolean("achievetodo.bridge.real")
                || !Boolean.getBoolean("achievetodo.bridge.groupScope")
                || !Boolean.getBoolean("achievetodo.bridge.spike") || !Boolean.getBoolean("achievetodo.progression.foundation"))
                throw new IllegalStateException("ATD_GAMEPLAY_MODE_PRECONDITIONS");
            productAdmission = new GameplayAdmission(startup.mode());
            readiness.startService("bridge", () -> {
                bridgeRuntime = new BukkitBridgeRuntime(this, () -> progressionRuntime,
                    this::gameplayOperational, this::productGameplayReady, () -> {});
                bridgeRuntime.setLandmarks(id -> gameplayRuntime==null ? com.diskree.achievetodo.bridge.BridgeLandmarks.Snapshot.disabled() : gameplayRuntime.landmarkPresentation(id));
                bridgeRuntime.start();
            }, () -> { if (bridgeRuntime != null) bridgeRuntime.close(); });
            readiness.startService("progression", () -> {
                progressionRuntime = new BukkitProgressionRuntime(this); progressionRuntime.start();
            }, () -> { if (progressionRuntime != null) progressionRuntime.close(); });
            startGameplay();
            readiness.markRunning();
            if (!gameplayRuntime.enforcementReady()) throw new IllegalStateException("ATD_REQUIRED_ENFORCEMENT_UNAVAILABLE");
            getLogger().info("ATD_GAMEPLAY_MODE=" + productAdmission.mode());
            if (!chatMigrationInstalled())
                getLogger().warning("ATD_CHAT_MIGRATION_UNAVAILABLE: advancement messages in chat have no hover text");
            reportReadiness();
        } catch (Exception | LinkageError failure) {
            readiness.fail();
            if (failure instanceof StartupModePolicy.Refusal refusal) {
                // An operator error, not a defect: print the instructions without a stack trace.
                refusal.guidance().forEach(getLogger()::severe);
            } else {
                getLogger().log(Level.SEVERE, "Foundation startup failed; releasing partial services", failure);
            }
            stopServices();
            // Disabling this plugin alone would leave an unguarded vanilla server.
            // A required authority/agent failure must terminate admission altogether.
            getLogger().severe("ATD_STARTUP_FAIL_CLOSED: stopping server before unprotected play");
            getServer().shutdown();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        stopServices();
    }

    public ServerRuntimeReadiness.Snapshot runtimeStatus() {
        return readiness.snapshot();
    }

    private void stopServices() {
        // Stop callbacks before closing the state they reference, including partial enable.
        try {
            HandlerList.unregisterAll(this);
        } catch (RuntimeException failure) {
            readiness.fail();
            getLogger().log(Level.SEVERE, "Failed to unregister plugin listeners", failure);
        }
        try {
            getServer().getScheduler().cancelTasks(this);
        } catch (RuntimeException failure) {
            readiness.fail();
            getLogger().log(Level.SEVERE, "Failed to cancel plugin tasks", failure);
        }
        for (ServerRuntimeReadiness.CleanupFailure failure : readiness.stop()) {
            getLogger().log(Level.SEVERE, "Failed to close service " + failure.service(), failure.cause());
        }
        bridgeRuntime = null;
        gameplayRuntime = null;
        progressionRuntime = null;
        long pendingTasks = getServer().getScheduler().getPendingTasks().stream()
            .filter(task -> task.getOwner() == this).count();
        getLogger().info("Foundation stopped: state=" + readiness.snapshot().state()
            + ", retainedServices=" + readiness.snapshot().retainedServices().size()
            + ", ownedPendingTasks=" + pendingTasks
            + ", registeredListeners=" + HandlerList.getRegisteredListeners(this).size());
    }

    private boolean gameplayOperational() {
        try {
            return productAdmission != null && productAdmission.operational(
                readiness.snapshot().state() == ServerRuntimeReadiness.State.RUNNING
                    && bridgeRuntime != null && bridgeRuntime.transportReady(),
                progressionRuntime != null && progressionRuntime.isReady(),
                gameplayRuntime != null && gameplayRuntime.enforcementReady(),
                Boolean.getBoolean("achievetodo.landmarks.exactPrototype"), Boolean.getBoolean("achievetodo.bridge.real"),
                Boolean.getBoolean("achievetodo.bridge.groupScope"));
        } catch (RuntimeException | LinkageError unavailable) { return false; }
    }
    private static boolean chatMigrationInstalled() {
        try {
            return (boolean) Class.forName("com.diskree.achievetodo.gameplay.nativeguard.NativeChatAgent", false, null)
                .getMethod("installed").invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError unavailable) { return false; }
    }

    public boolean productGameplayReady() {
        return gameplayOperational();
    }

    /**
     * Progression becomes ready only once the server has started and the BACAP objective exists, so readiness is
     * reported from the main thread after startup: once when players can be admitted, or loudly when it does not
     * happen within a minute.
     */
    private void reportReadiness() {
        int[] attempts = {0};
        getServer().getScheduler().runTaskTimer(this, task -> {
            if (productGameplayReady()) {
                getLogger().info("ATD_PRODUCT_GAMEPLAY_READY=true; players with the AchieveToDo " + getPluginMeta().getVersion()
                    + " client mod can join");
                task.cancel();
            } else if (++attempts[0] == 60) {
                getLogger().severe("ATD_PRODUCT_GAMEPLAY_READY=false after 60 s; players cannot join yet (progression="
                    + (progressionRuntime != null && progressionRuntime.isReady()) + ", bridge="
                    + (bridgeRuntime != null && bridgeRuntime.transportReady()) + ", guards="
                    + (gameplayRuntime != null && gameplayRuntime.enforcementReady()) + ")");
            }
        }, 1L, 20L);
    }

    /** Values the operator may need for the start command. Read-only and best effort: never a binding. */
    private StartupModePolicy.Hints startupHints() {
        String uuid = null, key = null, groupUuid = null, groupKey = null, datapacks = null;
        var loaded = new java.util.ArrayList<String>();
        try {
            for (var world : getServer().getWorlds()) {
                String role = switch (world.getEnvironment()) {
                    case NORMAL -> "overworld"; case NETHER -> "nether"; case THE_END -> "end"; default -> "other";
                };
                loaded.add(role + ".uuid=" + world.getUID() + "  " + role + ".key=" + world.getKey());
                if (uuid != null || world.getEnvironment() != org.bukkit.World.Environment.NORMAL) continue;
                uuid = world.getUID().toString();
                key = world.getKey().toString();
                datapacks = ReleaseLaunchProfile.levelRoot(world.getWorldFolder().toPath()).resolve("datapacks").toAbsolutePath().normalize().toString();
            }
        } catch (RuntimeException | LinkageError unavailable) { /* hints only */ }
        try {
            var group = com.diskree.achievetodo.progression.ProgressionWorldGroup.load(
                getDataFolder().toPath().resolve("progression").resolve("world-group.properties"));
            groupUuid = group.worlds().get("overworld").uuid().toString();
            groupKey = group.worlds().get("overworld").key();
        } catch (RuntimeException | LinkageError unavailable) { /* missing or invalid: reported by the guidance */ }
        String jar = null;
        try { jar = "plugins/" + getFile().getName(); } catch (RuntimeException | LinkageError unavailable) { /* hints only */ }
        return new StartupModePolicy.Hints(uuid, key, groupUuid, groupKey, datapacks, loaded, jar);
    }

    private java.util.List<ReleaseLaunchProfile.LoadedWorld> loadedWorlds() {
        var worlds = new java.util.ArrayList<ReleaseLaunchProfile.LoadedWorld>();
        for (var world : getServer().getWorlds()) {
            String role = switch (world.getEnvironment()) {
                case NORMAL -> "overworld"; case NETHER -> "nether"; case THE_END -> "end"; default -> "other";
            };
            worlds.add(new ReleaseLaunchProfile.LoadedWorld(role, world.getUID(), world.getKey().toString(),
                world.getWorldFolder().toPath()));
        }
        return worlds;
    }

    private void startGameplay() throws Exception {
        readiness.startService("gameplay-s4a", () -> {
            gameplayRuntime = new BukkitGameplayRuntime(this, progressionRuntime);
            gameplayRuntime.start();
        }, () -> { if (gameplayRuntime != null) gameplayRuntime.close(); });
    }
}
