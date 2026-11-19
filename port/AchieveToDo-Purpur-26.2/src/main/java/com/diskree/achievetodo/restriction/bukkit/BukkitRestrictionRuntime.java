package com.diskree.achievetodo.restriction.bukkit;

import com.diskree.achievetodo.adapters.BlockBreakAdapter;
import com.diskree.achievetodo.adapters.BlockInteractionAdapter;
import com.diskree.achievetodo.adapters.BukkitRestrictionService;
import com.diskree.achievetodo.adapters.BundleAdapter;
import com.diskree.achievetodo.adapters.AttachedEntityAdapter;
import com.diskree.achievetodo.adapters.EntityInteractionAdapter;
import com.diskree.achievetodo.adapters.EquipmentAdapter;
import com.diskree.achievetodo.adapters.FluidAdapter;
import com.diskree.achievetodo.adapters.ItemUseAdapter;
import com.diskree.achievetodo.adapters.MovementAdapter;
import com.diskree.achievetodo.adapters.ProjectileAdapter;
import com.diskree.achievetodo.adapters.SleepAdapter;
import com.diskree.achievetodo.adapters.TradeAdapter;
import com.diskree.achievetodo.adapters.VehicleAdapter;
import com.diskree.achievetodo.adapters.VaultAdapter;
import com.diskree.achievetodo.landmark.LandmarkEngine;
import com.diskree.achievetodo.progression.DemystificationSink;
import com.diskree.achievetodo.restriction.RestrictionEngine;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitRestrictionRuntime {

    private final JavaPlugin plugin;
    private final BukkitRestrictionService restrictionService;

    public BukkitRestrictionRuntime(
        JavaPlugin plugin,
        LandmarkEngine landmarkEngine,
        PlayerProgressionLookup progressionLookup,
        DemystificationSink demystificationSink
    ) {
        this.plugin = plugin;
        this.restrictionService = new BukkitRestrictionService(
            plugin,
            new RestrictionEngine(landmarkEngine),
            progressionLookup,
            demystificationSink
        );
    }

    public void start() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(new BlockInteractionAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new BlockBreakAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new ItemUseAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new EquipmentAdapter(plugin, restrictionService), plugin);
        pluginManager.registerEvents(new BundleAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new AttachedEntityAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new EntityInteractionAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new TradeAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new VehicleAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new ProjectileAdapter(plugin, restrictionService), plugin);
        // S4K portal enforcement runs at Entity.setAsInsidePortal HEAD in BukkitGameplayRuntime.
        pluginManager.registerEvents(new MovementAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new FluidAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new SleepAdapter(restrictionService), plugin);
        pluginManager.registerEvents(new VaultAdapter(restrictionService), plugin);
        plugin.getLogger().info("Restriction adapters are running against the BACAP progression runtime.");
    }
}
