package com.diskree.achievetodo.adapters;

import com.diskree.achievetodo.ability.AbilityType;
import com.diskree.achievetodo.restriction.RestrictionActionKind;
import com.diskree.achievetodo.restriction.RestrictionAutomationSource;
import com.diskree.achievetodo.restriction.RestrictionDecision;
import com.diskree.achievetodo.restriction.RestrictionFlag;
import com.diskree.achievetodo.restriction.RestrictionHand;
import com.diskree.achievetodo.restriction.RestrictionTarget;
import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import io.papermc.paper.event.entity.EntityPortalReadyEvent;
import org.bukkit.Location;
import org.bukkit.PortalType;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Steerable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PortalAdapter implements Listener {

    private static final long DENIED_TRANSPORT_TTL_MILLIS = 5_000L;

    private final BukkitRestrictionService restrictionService;
    private final Map<UUID, Long> deniedTransportUntilByEntityId = new HashMap<>();

    public PortalAdapter(BukkitRestrictionService restrictionService) {
        this.restrictionService = restrictionService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerPortal(PlayerPortalEvent event) {
        denyTransport(
            event.getPlayer(),
            BukkitRestrictionResolver.resolvePortalAbility(event.getCause()),
            portalTarget(event.getTo(), event.getPlayer(), portalKey(event.getCause())),
            Set.of(),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event instanceof PlayerPortalEvent || event instanceof PlayerTeleportEndGatewayEvent) {
            return;
        }
        Optional<AbilityType> ability = BukkitRestrictionResolver.resolvePortalAbility(event.getCause());
        if (ability.isEmpty()) {
            return;
        }
        denyTransport(
            event.getPlayer(),
            ability,
            portalTarget(event.getTo(), event.getPlayer(), portalKey(event.getCause())),
            Set.of(RestrictionFlag.SECONDARY_PATH),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerTeleportEndGateway(PlayerTeleportEndGatewayEvent event) {
        denyTransport(
            event.getPlayer(),
            Optional.of(AbilityType.TELEPORT_OUTER_ISLANDS),
            portalTarget(event.getTo(), event.getPlayer(), "end_gateway"),
            Set.of(),
            event
        );
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityPortal(EntityPortalEvent event) {
        if (cancelIfRecentlyDeniedTransport(event.getEntity(), event)) {
            return;
        }
        Optional<AbilityType> ability = resolvePortalAbility(event.getPortalType());
        RestrictionTarget target = portalTarget(event.getTo(), event.getEntity(), event.getPortalType().name().toLowerCase());
        if (handleEntityPortalTransport(event.getEntity(), ability, target, event)) {
            return;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityPortalReady(EntityPortalReadyEvent event) {
        if (cancelIfRecentlyDeniedTransport(event.getEntity(), event)) {
            return;
        }
        Optional<AbilityType> ability = resolvePortalAbility(event.getPortalType());
        RestrictionTarget target = BukkitRestrictionTargets.fromBoundingBox(
            event.getEntity().getWorld(),
            event.getEntity().getBoundingBox(),
            com.diskree.achievetodo.restriction.RestrictionTargetKind.PORTAL,
            event.getPortalType().name().toLowerCase()
        ).orElse(RestrictionTarget.none());
        if (handleEntityPortalTransport(event.getEntity(), ability, target, event)) {
            return;
        }
    }

    private void denyTransport(
        Player actor,
        Optional<AbilityType> ability,
        RestrictionTarget target,
        Set<RestrictionFlag> flags,
        Cancellable event
    ) {
        Set<Player> players = collectPlayers(actor);
        players.add(actor);
        for (Player player : players) {
            RestrictionDecision decision = restrictionService.check(
                player,
                RestrictionActionKind.PORTAL_USE,
                restrictionService.abilityList(ability.orElse(null)),
                target,
                RestrictionHand.NONE,
                RestrictionAutomationSource.PORTAL,
                player == actor ? flags : mergeFlags(flags, RestrictionFlag.PASSENGER_PATH)
            );
            if (!decision.isAllowed()) {
                event.setCancelled(true);
                markDeniedTransport(actor);
                restrictionService.sendFeedback(player, decision);
                return;
            }
        }
    }

    private void markDeniedTransport(Entity anchor) {
        long expiresAt = System.currentTimeMillis() + DENIED_TRANSPORT_TTL_MILLIS;
        Set<UUID> relatedEntities = new LinkedHashSet<>();
        collectTransportEntityIds(anchor, relatedEntities, new LinkedHashSet<>());
        for (UUID entityId : relatedEntities) {
            deniedTransportUntilByEntityId.put(entityId, expiresAt);
        }
    }

    private boolean cancelIfRecentlyDeniedTransport(Entity entity, Cancellable event) {
        purgeExpiredDeniedTransport();
        Long expiresAt = deniedTransportUntilByEntityId.get(entity.getUniqueId());
        if (expiresAt == null || expiresAt < System.currentTimeMillis()) {
            return false;
        }
        event.setCancelled(true);
        return true;
    }

    private void purgeExpiredDeniedTransport() {
        long now = System.currentTimeMillis();
        deniedTransportUntilByEntityId.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private Set<RestrictionFlag> mergeFlags(Set<RestrictionFlag> flags, RestrictionFlag extra) {
        Set<RestrictionFlag> merged = new LinkedHashSet<>(flags);
        merged.add(extra);
        return Set.copyOf(merged);
    }

    private RestrictionTarget portalTarget(Location to, Entity fallbackEntity, String key) {
        if (to != null) {
            BoundingBox box = new BoundingBox(to.getX(), to.getY(), to.getZ(), to.getX() + 1, to.getY() + 1, to.getZ() + 1);
            return BukkitRestrictionTargets.fromBoundingBox(to.getWorld(), box, com.diskree.achievetodo.restriction.RestrictionTargetKind.PORTAL, key)
                .orElseGet(() -> BukkitRestrictionTargets.fromEntity(fallbackEntity).orElse(RestrictionTarget.none()));
        }
        return BukkitRestrictionTargets.fromEntity(fallbackEntity).orElse(RestrictionTarget.none());
    }

    private String portalKey(PlayerTeleportEvent.TeleportCause cause) {
        return cause == null ? "portal" : cause.name().toLowerCase();
    }

    private Optional<AbilityType> resolvePortalAbility(PortalType portalType) {
        if (portalType == PortalType.NETHER) {
            return Optional.of(AbilityType.ENTER_NETHER);
        }
        if (portalType == PortalType.ENDER) {
            return Optional.of(AbilityType.ENTER_END);
        }
        if (portalType == PortalType.END_GATEWAY) {
            return Optional.of(AbilityType.TELEPORT_OUTER_ISLANDS);
        }
        return Optional.empty();
    }

    private Set<Player> collectPlayers(Entity root) {
        Set<Player> players = new LinkedHashSet<>();
        collectPlayersRecursive(root, players, new LinkedHashSet<>());
        return players;
    }

    private boolean handleEntityPortalTransport(
        Entity transport,
        Optional<AbilityType> ability,
        RestrictionTarget target,
        Cancellable event
    ) {
        Set<Player> players = collectPlayers(transport);
        if (players.isEmpty()) {
            return false;
        }
        for (Player player : players) {
            RestrictionDecision decision = restrictionService.check(
                player,
                RestrictionActionKind.PORTAL_USE,
                restrictionService.abilityList(ability.orElse(null)),
                target,
                RestrictionHand.NONE,
                RestrictionAutomationSource.PORTAL,
                Set.of(RestrictionFlag.PASSENGER_PATH, RestrictionFlag.SECONDARY_PATH)
            );
            if (decision.isAllowed()) {
                continue;
            }
            if (isControllingPassenger(transport, player)) {
                event.setCancelled(true);
                restrictionService.sendFeedback(player, decision);
                return true;
            }
            player.leaveVehicle();
        }
        return false;
    }

    private boolean isControllingPassenger(Entity transport, Player player) {
        if (!transport.getPassengers().contains(player)) {
            return false;
        }
        if (transport instanceof Minecart) {
            return false;
        }
        if (transport instanceof Boat || transport instanceof AbstractHorse || transport instanceof Steerable) {
            return !transport.getPassengers().isEmpty() && transport.getPassengers().getFirst().getUniqueId().equals(player.getUniqueId());
        }
        return !transport.getPassengers().isEmpty() && transport.getPassengers().getFirst().getUniqueId().equals(player.getUniqueId());
    }

    private void collectPlayersRecursive(Entity entity, Set<Player> players, Set<UUID> visited) {
        if (!visited.add(entity.getUniqueId())) {
            return;
        }
        if (entity instanceof Player player) {
            players.add(player);
        }
        if (entity.getVehicle() != null) {
            collectPlayersRecursive(entity.getVehicle(), players, visited);
        }
        for (Entity passenger : entity.getPassengers()) {
            collectPlayersRecursive(passenger, players, visited);
        }
    }

    private void collectTransportEntityIds(Entity entity, Set<UUID> entityIds, Set<UUID> visited) {
        if (!visited.add(entity.getUniqueId())) {
            return;
        }
        entityIds.add(entity.getUniqueId());
        if (entity.getVehicle() != null) {
            collectTransportEntityIds(entity.getVehicle(), entityIds, visited);
        }
        for (Entity passenger : entity.getPassengers()) {
            collectTransportEntityIds(passenger, entityIds, visited);
        }
    }
}
