# PURPUR INTEGRATION TEST PLAN

Prepared for real Purpur runtime validation on August 17, 2026.

Unit tests and compile success do not close the following scenarios by themselves.

## Landmark Structure Scenarios

1. Generate or locate a real vanilla structure and confirm detection exists on live Purpur.
2. Compare detected structure bounds against actual vanilla generated bounds.
3. Validate woodland mansion detection and bounding box shape.
4. Validate mineshaft detection and piece aggregation.
5. Validate a structure whose relevant pieces cross a chunk border.
6. Restart the server and confirm landmark persistence after unload/reload.

## Landmark Feature Scenarios

1. Validate a real vanilla `desert_well`.
2. Validate a real vanilla `monster_room`.
3. Validate multiple `monster_room` instances within the same chunk.
4. Validate a feature near a chunk border.
5. Validate absence of false positives on a similar player-built structure.
6. Measure feature scan cost on chunk load and chunk populate.

## Restriction Scenarios

1. Landmark denial blocks action for a locked player.
2. Unlocking removes the denial without restart.
3. Two players with different unlock states observe different results against the same landmark or mechanic.
4. Offhand path is denied where main-hand denial already exists.
5. Dispenser and other automation paths do not bypass player-only checks.
6. Secondary paths such as inventory mutation, merchant selection, or projectile collision are still denied.
7. Nether portal, End portal, End gateway, and Ender Pearl all use the intended ability mapping.
8. Vehicles and passengers with mixed unlock states do not bypass portal restrictions.
9. Projectile collision into a locked landmark or protected target produces no damage or side effect.
10. Equipment restrictions cover normal equip, shift-click, drag, hotbar swap, dispenser equip, and armor stand manipulation.

## Progression Tracking Scenarios

1. BACAP objective detection succeeds from `SIDEBAR`.
2. BACAP objective detection succeeds from `PLAYER_LIST` when sidebar is absent.
3. BACAP objective detection succeeds from `BELOW_NAME` when both higher-priority slots are absent.
4. Join is denied when no BACAP objective is present.
5. Runtime recovers after a BACAP objective appears without server restart.
6. Personal objective mode updates obtained count after earning an advancement.
7. Team objective mode sums teammates correctly.
8. Team objective mode updates correctly after adding a player to a scoreboard team.
9. Team objective mode behavior after removing a player from a scoreboard team is documented and verified.
10. Demystified ability criterion is granted after first deny.
11. Reconnect preserves demystified state.
12. Unlock grants synthetic advancement criteria when threshold is reached.
13. Relock revokes only the unlocked criterion when progression becomes unavailable again.
14. World progression selection file is created on first boot and reused on restart.
15. Generated progression config file remains stable across restart with same world seed.

## BACAP Tracking Data Scenarios

1. `bac_underwater` updates both `FREE_DIVER` and `SLEEP_WITH_THE_FISHES` progress.
2. `ON_A_RAIL` uses eligible/current axis semantics and reports absolute distance.
3. `LOSER` normalizes score according to BACAP threshold behavior.
4. Custom statistics such as `WALK_ONE_CM`, `TIME_SINCE_REST`, and `JUMP` are read correctly.
5. Item-used statistics such as `HONEY_BOTTLE` are read correctly.
6. Block-mined statistics such as `STONE` are read correctly.
7. Entity-killed statistics such as `GLOW_SQUID` and `ENDER_DRAGON` are read correctly.
8. Percentage-tracked statistics clamp to `0..100`.
9. Nearby-entity semantics remain intentionally unbound in Stage 6 and should not be marked complete by live tests yet.

## Mechanic-Specific Live Cases

### Block breaking

1. Break a normal block with a normal tool in survival.
2. Start break in `BlockDamageEvent` path, then complete in `BlockBreakEvent`.
3. Break below `Y < 0`.
4. Break with shears.
5. Break with unusual tools and swapped tools.
6. Repeat inside and outside a locked landmark.
7. Repeat in creative and spectator.

### Item use

1. Bow
2. Crossbow
3. Egg
4. Snowball
5. Ender Pearl
6. Eye of Ender
7. Firework rocket
8. Fishing rod
9. Shield
10. Flint and steel
11. Shears
12. Brush
13. Bucket
14. End crystal
15. Lead
16. Boat and minecart placement
17. Armor stand
18. Consumables

### Equipment

1. Normal right-click equip
2. Shift-click equip
3. Drag equip
4. Number-key hotbar swap
5. swap-with-slot path
6. dispenser equip
7. armor stand manipulation
8. entity equipment interaction path if applicable

### Projectiles

1. Bow and arrow against player, mob, and block target
2. Crossbow projectile against player, mob, and block target
3. Trident collision behavior
4. Snowball and egg collision behavior
5. Fire charge or fireball side effects
6. Ender Pearl teleport denial behavior
7. Firework projectile behavior
8. Confirm `ProjectileCollideEvent` prevents damage or effect before `ProjectileHitEvent` fallback is needed

### Trade

1. Villager open denied before visible trade use
2. Wandering trader open denied correctly
3. Trade selection path denied
4. Actual execution path denied
5. Confirm whether blocked trades are hidden, emptied, or merely cancelled

### Fluid / waterlogging

1. Bucket fill
2. Bucket empty
3. Cauldron interaction
4. Powder snow bucket path
5. Bubble column relevant behavior
6. Waterlogged drain or mutation path

### Vault

1. Direct use or unlock path
2. Nearby-player shared-state participation
3. Mixed unlock state among nearby players
4. Document exact API limitation if shared-state parity cannot be observed

### Bundle

1. Inventory UI mutation path
2. Drag path
3. Any observed non-UI mutation path
4. Document exact bypass if direct component mutation is not observable

## Pass / Fail Rule

Do not mark Stage 5 as parity-complete only because offline unit tests pass.

A scenario stays open until it is validated on a real Purpur runtime or explicitly documented as an integration-only limitation.
