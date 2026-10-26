# RESTRICTION ENGINE

## Purpose

Stage 5 adds a central server-independent restriction layer that combines:

- player ability state
- landmark lock state
- adapter-provided gameplay context

The core rule is:

```text
Vanilla allows
+ AchieveToDo allows
= perform
```

## Architecture

```text
Purpur Event
-> Adapter
-> RestrictionEngine
-> Ability state lookup + LandmarkEngine query
-> ALLOW / DENY_ABILITY / DENY_LANDMARK / DENY_BOTH
```

Core classes:

- `RestrictionEngine`
- `RestrictionContext`
- `RestrictionDecision`
- `RestrictionTarget`

Purpur-facing glue:

- `BukkitRestrictionService`
- `BukkitRestrictionResolver`
- `BukkitRestrictionRuntime`

## Decision Model

`RestrictionDecisionKind`:

- `ALLOW`
- `DENY_ABILITY`
- `DENY_LANDMARK`
- `DENY_BOTH`

`RestrictionDecision` may also carry:

- denied ability
- denied landmark
- landmark-derived ability
- denied landmark set
- reason key

No Bukkit objects are stored in the pure core model.

## Context Model

`RestrictionContext` carries pure-domain semantics only:

- actor UUID
- action kind
- actor mode
- ability candidate
- target
- hand
- automation source
- flags

`RestrictionTarget` abstracts the thing being acted upon:

- world id
- block-space bounding box
- target kind
- target key

## Semantics Preserved In Core

- creative bypass
- spectator bypass
- initially unlocked
- permanently locked
- demystified but still locked
- positive-threshold unlock
- landmark lock independent from ability lock
- merged ability + landmark denial path

## Adapter Inventory

### Primary interaction paths

- block interaction
- block breaking
- item use
- equipment
- entity interaction
- trade open/select/execute
- vehicle enter
- portal use
- movement toggles
- sleep
- vault use

### Secondary and bypass-oriented hooks

- block damage before block break
- inventory mutation for armor/offhand/bundle paths
- dispenser equip
- armor stand manipulation
- item frame rotation and destroy
- projectile collide before hit
- projectile hit fallback
- merchant inventory open
- mounted/passenger portal checks

## Event Mapping Summary

### Block / item

- `PlayerInteractEvent`
- `BlockPlaceEvent`
- `PlayerItemConsumeEvent`
- `PlayerBucketEmptyEvent`
- `PlayerBucketFillEvent`
- `PlayerBucketEntityEvent`

### Break / tool paths

- `BlockDamageEvent`
- `BlockBreakEvent`

### Equipment / offhand / dispenser

- `InventoryClickEvent`
- `InventoryDragEvent`
- `PlayerSwapHandItemsEvent`
- `PlayerInteractEvent`
- `BlockDispenseArmorEvent`
- `PlayerArmorStandManipulateEvent`
- `PlayerArmorChangeEvent` as monitor-only post-path observation

### Entity / trade / vehicles

- `PlayerInteractEntityEvent`
- `PlayerShearEntityEvent`
- `PlayerLeashEntityEvent`
- `EntityBreedEvent`
- `TradeSelectEvent`
- `PlayerTradeEvent`
- `InventoryOpenEvent` for merchant UI
- `VehicleEnterEvent`
- `EntityMountEvent`

### Projectile / portal / movement

- `PlayerLaunchProjectileEvent`
- `ProjectileCollideEvent`
- `ProjectileHitEvent`
- `PlayerPortalEvent`
- `PlayerTeleportEvent`
- `PlayerTeleportEndGatewayEvent`
- `EntityPortalEvent`
- `EntityPortalReadyEvent`
- `PlayerToggleSneakEvent`
- `PlayerToggleSprintEvent`
- `PlayerJumpEvent`
- `PlayerMoveEvent`
- `EntityToggleGlideEvent`

## Performance Notes

Restriction checks are target-local and do not scan:

- every player against every landmark
- arbitrary world columns during every action

Landmark-side feature detection remains chunk-local. Stage 5 adds timing logs in `BukkitLandmarkRuntime` so live scan cost can be measured on a real Purpur server.

## Known Differences And Gaps

These are intentionally documented instead of hidden:

- runtime progression lookup is currently permissive until BACAP tracking is ported
- bundle mutation parity is partial because Bukkit does not expose every component-level mutation path
- projectile pre-hit semantics are approximated with `ProjectileCollideEvent` plus `ProjectileHitEvent`
- waterlogging semantics are not proven equivalent to original lower-level hooks
- vault nearby-player shared-state semantics are not proven equivalent yet
- merchant offer invisibility parity is not yet demonstrated

Landmark validation gaps carried from Stage 4 are listed in `PURPUR_INTEGRATION_TEST_PLAN.md`.

## NMS

- none in Stage 5
- if future mechanics require NMS, it must stay out of core and be isolated per adapter
