package com.diskree.achievetodo.gameplay;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.progression.PlayerProgressionState;
import com.diskree.achievetodo.restriction.PlayerProgressionLookup;
import org.bukkit.GameMode;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Cancellable native transactions: let Minecraft produce the result only after server permission. */
public final class BukkitGameplayRuntime implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final PlayerProgressionLookup progression;
    private final BukkitExpandedGameplayListener expanded;
    private BukkitFoodGameplayListener food;
    private NativeTransactionBindings nativeTransactions;
    private NativeActionBindings nativeActions;
    private NativeToolBindings nativeTools;
    private NativeComplexBindings nativeComplex;
    private NativePortalBindings nativePortals;
    private NativeMovementBindings nativeMovement;
    private com.diskree.achievetodo.landmark.bukkit.BukkitExactLandmarkRuntime exactLandmarks;

    public BukkitGameplayRuntime(JavaPlugin plugin, PlayerProgressionLookup progression) {
        this.plugin = plugin;
        this.progression = progression;
        this.expanded = new BukkitExpandedGameplayListener(progression);
    }

    public void start() {
        var foods = new NativeFoodAbilities();
        nativeTransactions = new NativeTransactionBindings(progression, foods);
        nativeActions = new NativeActionBindings(progression);
        nativeTools = new NativeToolBindings(progression);
        nativeComplex = new NativeComplexBindings(progression);
        nativePortals = new NativePortalBindings(progression);
        nativeMovement = new NativeMovementBindings(progression);
        if (Boolean.getBoolean("achievetodo.landmarks.exactPrototype")) {
            exactLandmarks = new com.diskree.achievetodo.landmark.bukkit.BukkitExactLandmarkRuntime(plugin, progression);
            exactLandmarks.start();
        }
        food = new BukkitFoodGameplayListener(progression, foods);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(expanded, plugin);
        plugin.getServer().getPluginManager().registerEvents(food, plugin);
        plugin.getLogger().info("S4L gameplay implementation gates active: " + GameplayGate.IMPLEMENTED.size() + " exact IDs; native certification separate; PRODUCT_GAMEPLAY_READY=false");
    }

    public com.diskree.achievetodo.bridge.BridgeLandmarks.Snapshot landmarkPresentation(java.util.UUID actor) {
        return exactLandmarks==null ? com.diskree.achievetodo.bridge.BridgeLandmarks.Snapshot.disabled() : exactLandmarks.presentation(actor);
    }

    private static boolean bypass(Player p) {
        return p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR;
    }

    private PlayerProgressionState state(Player p) {
        return progression.getProgressionState(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void damage(BlockDamageEvent e) {
        if (!GameplayGate.allowsBreak(state(e.getPlayer()), e.getPlayer().getUniqueId(), bypass(e.getPlayer()), e.getBlock().getY()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void destroy(BlockBreakEvent e) {
        // Recheck at completion: permission can be revoked while the player is mining.
        if (!GameplayGate.allowsBreak(state(e.getPlayer()), e.getPlayer().getUniqueId(), bypass(e.getPlayer()), e.getBlock().getY()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void sleep(PlayerBedEnterEvent e) {
        if (!GameplayGate.allows(state(e.getPlayer()), e.getPlayer().getUniqueId(), bypass(e.getPlayer()), AbilityType.SLEEP))
            e.setUseBed(org.bukkit.event.Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void enter(VehicleEnterEvent e) {
        AbilityType ability = mountAbility(e.getVehicle());
        if (ability != null && e.getEntered() instanceof Player p
            && !GameplayGate.allows(state(p), p.getUniqueId(), bypass(p), ability))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void mount(EntityMountEvent e) {
        // Paper also exposes the generic passenger path (including addPassenger).
        AbilityType ability = mountAbility(e.getMount());
        if (ability != null && e.getEntity() instanceof Player p
            && !GameplayGate.allows(state(p), p.getUniqueId(), bypass(p), ability))
            e.setCancelled(true);
    }

    private static AbilityType mountAbility(org.bukkit.entity.Entity entity) {
        // Fabric checks vanilla Minecart, not AbstractMinecart/container carts.
        return entity instanceof Boat ? AbilityType.GET_INTO_BOAT
            : entity instanceof RideableMinecart ? AbilityType.GET_INTO_MINECART : null;
    }

    @Override public void close() {
        if (exactLandmarks != null) exactLandmarks.close();
        if (nativeTransactions != null) nativeTransactions.close();
        if (nativeActions != null) nativeActions.close();
        if (nativePortals != null) nativePortals.close();
        if (nativeMovement != null) nativeMovement.close();
        if (nativeComplex != null) nativeComplex.close();
        if (nativeTools != null) nativeTools.close();
        if (food != null) HandlerList.unregisterAll(food);
        HandlerList.unregisterAll(expanded);
        HandlerList.unregisterAll(this);
    }
}
