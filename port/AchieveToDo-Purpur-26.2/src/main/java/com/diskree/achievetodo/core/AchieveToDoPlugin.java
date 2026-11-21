package com.diskree.achievetodo.core;

import com.diskree.achievetodo.landmark.bukkit.BukkitLandmarkRuntime;
import com.diskree.achievetodo.restriction.bukkit.BukkitRestrictionRuntime;
import com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime;
import com.diskree.achievetodo.ui.bukkit.BukkitUiRuntime;
import com.diskree.achievetodo.bridge.BukkitBridgeRuntime;
import com.diskree.achievetodo.gameplay.BukkitGameplayRuntime;
import java.util.logging.Level;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class AchieveToDoPlugin extends JavaPlugin {

    private final ServerRuntimeReadiness readiness = new ServerRuntimeReadiness();
    private BukkitLandmarkRuntime landmarkRuntime;
    private BukkitProgressionRuntime progressionRuntime;
    private BukkitRestrictionRuntime restrictionRuntime;
    private BukkitUiRuntime uiRuntime;
    private BukkitBridgeRuntime bridgeRuntime;
    private BukkitGameplayRuntime gameplayRuntime;
    private ProductGameplayAdmission productAdmission;

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
            var gameplayMode = startup.mode();
            if (gameplayMode != ProductGameplayAdmission.Mode.DIAGNOSTIC) {
                if (!Boolean.getBoolean("achievetodo.landmarks.exactPrototype") || !Boolean.getBoolean("achievetodo.bridge.real")
                    || !Boolean.getBoolean("achievetodo.bridge.groupScope")
                    || (gameplayMode == ProductGameplayAdmission.Mode.CERTIFICATION &&
                        (!Boolean.getBoolean("achievetodo.bridge.spike") || !Boolean.getBoolean("achievetodo.progression.foundation")))
                    || (gameplayMode == ProductGameplayAdmission.Mode.PRODUCT && Boolean.getBoolean("achievetodo.bridge.spike")))
                    throw new IllegalStateException("ATD_GAMEPLAY_MODE_PRECONDITIONS");
                String certificate = System.getProperty("achievetodo.gameplay.certificate");
                productAdmission = new ProductGameplayAdmission(gameplayMode, getFile().toPath(),
                    certificate == null ? null : java.nio.file.Path.of(certificate), System.getProperty("achievetodo.gameplay.certificate.sha256"));
            }
            readiness.startService("bridge", () -> {
                bridgeRuntime = new BukkitBridgeRuntime(this, () -> progressionRuntime,
                    productAdmission == null ? null : this::gameplayOperational, this::productGameplayReady,
                    () -> { if (productAdmission != null) productAdmission.refreshIntegrity(); });
                bridgeRuntime.setLandmarks(id -> gameplayRuntime==null ? com.diskree.achievetodo.bridge.BridgeLandmarks.Snapshot.disabled() : gameplayRuntime.landmarkPresentation(id));
                bridgeRuntime.start();
            }, () -> { if (bridgeRuntime != null) bridgeRuntime.close(); });
            if (productAdmission != null) {
                // The exact Fabric-parity native path is the sole enforcement composition here.
                readiness.startService("progression", () -> {
                    progressionRuntime = new BukkitProgressionRuntime(this); progressionRuntime.start();
                }, () -> { if (progressionRuntime != null) progressionRuntime.close(); });
                startGameplay();
                readiness.markRunning();
                if (!gameplayRuntime.enforcementReady()) throw new IllegalStateException("ATD_REQUIRED_ENFORCEMENT_UNAVAILABLE");
                getLogger().warning("ATD_GAMEPLAY_MODE=" + productAdmission.mode() + "; PRODUCT_GAMEPLAY_READY=" + productGameplayReady());
                return;
            }
            if (Boolean.getBoolean("achievetodo.bridge.spike")) {
                if (Boolean.getBoolean("achievetodo.progression.foundation")) {
                    readiness.startService("progression", () -> {
                        progressionRuntime = new BukkitProgressionRuntime(this);
                        progressionRuntime.start();
                    }, () -> { if (progressionRuntime != null) progressionRuntime.close(); });
                    startGameplay();
                    getLogger().warning("S3C PROGRESSION PREVIEW: authoritative BACAP/config projection; PRODUCT_GAMEPLAY_READY=false");
                }
                readiness.markRunning();
                getLogger().warning("S2P TEST ONLY: diagnostic bridge; ability enforcement not certified.");
                return;
            }
            readiness.startService("landmarks", () -> {
                landmarkRuntime = new BukkitLandmarkRuntime(this);
                landmarkRuntime.start();
            }, () -> {
                if (landmarkRuntime != null) landmarkRuntime.close();
            });
            readiness.startService("progression", () -> {
                progressionRuntime = new BukkitProgressionRuntime(this);
                progressionRuntime.start();
            }, () -> {
                if (progressionRuntime != null) progressionRuntime.close();
            });
            readiness.startService("restrictions", () -> {
                restrictionRuntime = new BukkitRestrictionRuntime(this, landmarkRuntime.engine(), progressionRuntime, progressionRuntime);
                restrictionRuntime.start();
            }, () -> restrictionRuntime = null);
            startGameplay();
            readiness.startService("ui", () -> {
                uiRuntime = new BukkitUiRuntime(this, progressionRuntime);
                uiRuntime.start();
            }, () -> {
                if (uiRuntime != null) uiRuntime.close();
            });
            registerCommands();
            readiness.markRunning();
            getLogger().info("Foundation lifecycle RUNNING: " + readiness.snapshot().retainedServices());
            getLogger().warning("Experimental bridge admission closed outside explicit S2P spike mode.");
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
        uiRuntime = null;
        bridgeRuntime = null;
        gameplayRuntime = null;
        restrictionRuntime = null;
        progressionRuntime = null;
        landmarkRuntime = null;
        long pendingTasks = getServer().getScheduler().getPendingTasks().stream()
            .filter(task -> task.getOwner() == this).count();
        getLogger().info("Foundation stopped: state=" + readiness.snapshot().state()
            + ", retainedServices=" + readiness.snapshot().retainedServices().size()
            + ", ownedPendingTasks=" + pendingTasks
            + ", registeredListeners=" + HandlerList.getRegisteredListeners(this).size());
    }

    private void registerCommands() {
        registerCommand(
            "atd",
            "Open the AchieveToDo ability browser.",
            new AchieveToDoUiCommand(uiRuntime)
        );
        registerCommand(
            "achievetodo",
            "Open the AchieveToDo ability browser.",
            new AchieveToDoUiCommand(uiRuntime)
        );
        registerCommand(
            "atdinspect",
            "Read-only AchieveToDo runtime inspection for live validation.",
            new AchieveToDoInspectCommand(landmarkRuntime, progressionRuntime)
        );
        registerCommand(
            "atdsetup",
            "Controlled AchieveToDo live-validation setup helpers.",
            new AchieveToDoSetupCommand()
        );
    }

    private boolean gameplayOperational() {
        try {
            return productAdmission != null && productAdmission.operational(
                readiness.snapshot().state() == ServerRuntimeReadiness.State.RUNNING
                    && bridgeRuntime != null && bridgeRuntime.transportReady(),
                progressionRuntime != null && progressionRuntime.isReady(),
                gameplayRuntime != null && gameplayRuntime.enforcementReady(),
                Boolean.getBoolean("achievetodo.landmarks.exactPrototype"), Boolean.getBoolean("achievetodo.bridge.real"),
                Boolean.getBoolean("achievetodo.bridge.groupScope"), Boolean.getBoolean("achievetodo.bridge.spike"));
        } catch (RuntimeException | LinkageError unavailable) { return false; }
    }
    public boolean productGameplayReady() {
        return productAdmission != null && productAdmission.productReady(gameplayOperational());
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
