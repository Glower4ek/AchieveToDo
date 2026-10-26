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

    @Override
    public void onEnable() {
        try {
            readiness.beginStart();
            var build = io.papermc.paper.ServerBuildInfo.buildInfo();
            PinnedStartupContract.runtime(Runtime.version().feature(), build.minecraftVersionId(),
                build.brandName(), build.buildNumber().orElse(-1));
            PinnedStartupContract.installation(getFile().toPath());
            readiness.startService("bridge", () -> {
                bridgeRuntime = new BukkitBridgeRuntime(this, () -> progressionRuntime);
                bridgeRuntime.setLandmarks(id -> gameplayRuntime==null ? com.diskree.achievetodo.bridge.BridgeLandmarks.Snapshot.disabled() : gameplayRuntime.landmarkPresentation(id));
                bridgeRuntime.start();
            }, () -> { if (bridgeRuntime != null) bridgeRuntime.close(); });
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
            getLogger().log(Level.SEVERE, "Foundation startup failed; releasing partial services", failure);
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

    private void startGameplay() throws Exception {
        readiness.startService("gameplay-s4a", () -> {
            gameplayRuntime = new BukkitGameplayRuntime(this, progressionRuntime);
            gameplayRuntime.start();
        }, () -> { if (gameplayRuntime != null) gameplayRuntime.close(); });
    }
}
