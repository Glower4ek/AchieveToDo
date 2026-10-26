package com.diskree.achievetodo.ui.bukkit;

import com.diskree.achievetodo.tracking.bukkit.BukkitProgressionRuntime;
import com.diskree.achievetodo.ui.UiAbilityBrowserService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class BukkitUiRuntime implements Listener, AutoCloseable {

    private static final long REFRESH_INTERVAL_TICKS = 10L;

    private final JavaPlugin plugin;
    private final BukkitProgressionRuntime progressionRuntime;
    private final BukkitUiSessionManager sessionManager;
    private BukkitTask refreshTask;

    public BukkitUiRuntime(JavaPlugin plugin, BukkitProgressionRuntime progressionRuntime) {
        this.plugin = plugin;
        this.progressionRuntime = progressionRuntime;
        this.sessionManager = new BukkitUiSessionManager(
            plugin,
            progressionRuntime,
            new UiAbilityBrowserService(),
            new BukkitUiItemFactory()
        );
    }

    public void start() {
        progressionRuntime.addListener("ui-runtime", sessionManager::markDirty);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        refreshTask = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            sessionManager::refreshDirtySessions,
            REFRESH_INTERVAL_TICKS,
            REFRESH_INTERVAL_TICKS
        );
    }

    public boolean open(Player player) {
        return sessionManager.open(player);
    }

    public UiPresentationMode getPresentationMode(Player player) {
        return sessionManager.getPresentationMode(player);
    }

    public void setPresentationMode(Player player, UiPresentationMode presentationMode) {
        sessionManager.setPresentationMode(player, presentationMode);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        sessionManager.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        sessionManager.handleDrag(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        sessionManager.handleClose(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessionManager.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
        sessionManager.handleLocaleChange(event.getPlayer());
    }

    @Override
    public void close() {
        progressionRuntime.removeListener("ui-runtime");
        HandlerList.unregisterAll(this);
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        sessionManager.closeAll();
    }
}
