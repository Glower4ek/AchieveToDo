# PORT STATUS

## Stage

Stage 10B UI backend and navigation is complete as of August 18, 2026:

- a server-driven vanilla-client UI runtime now exists for AchieveToDo under `/atd` and `/achievetodo`
- the Stage 10A architecture boundary was preserved:
  - pure Java UI view-model/state/layout mapping
  - Bukkit inventory/session rendering and click routing
  - no gameplay decisions moved into UI listeners
- active UI sessions now refresh from authoritative progression updates instead of becoming a second progression engine
- hidden, demystified, unlocked, initially-unlocked, and permanently-locked states render through explicit server-side view models
- owned UI inventories are excluded from gameplay restriction adapters so browsing the UI does not trigger false restriction denies
- live client validation confirmed:
  - `/atd` opens on a real vanilla client
  - category navigation works
  - `UNKNOWN_LOCKED` preserves privacy
  - `DEMYSTIFIED_LOCKED` shows real name/description/progress
  - `Landmark` mystery slots are visibly rendered after the fallback-material fix
  - already-open screens refresh in place when progression changes
  - inactive next-page navigation is labeled honestly instead of looking like a broken active button
- no NMS was introduced

This is not a claim that Stage 10 UI reconstruction is complete. Stage 10B closes the backend/session/navigation pass only. The next planned step is Stage 10C resource-pack visuals and higher-fidelity presentation work.

Stage 10A original UI audit and visual contract is complete as of August 18, 2026:

- original Fabric UI surface was fully re-audited READ ONLY across client state, advancements-screen mixins, world-creation helpers, notifications, and in-world overlays
- the original main gameplay UI is confirmed to be a modified vanilla `AdvancementsScreen`, not a separate standalone custom screen tree
- no dedicated landmark screen exists in the original; landmark presentation is folded into ability entries plus in-world overlay feedback
- no custom font assets exist in the original UI; AchieveToDo visual identity comes from layout, state masking, toast skinning, color choices, and icon selection
- Stage 10A created implementation-independent audit/design docs:
  - `UI_AUDIT.md`
  - `UI_VISUAL_CONTRACT.md`
  - `UI_RESOURCE_PACK_PLAN.md`
  - `UI_STAGE_10B_PLAN.md`
- Stage 10A recommends an inventory-first, server-driven Stage 10B architecture with chat/actionbar support and a required server resource pack for intended fidelity
- exact Fabric-style advancements-screen injection, fullscreen blackout, and animated landmark-border rendering are confirmed impossible to reproduce exactly for vanilla clients without a client mod
- no runtime UI implementation was started during Stage 10A itself
- no NMS was introduced

This is not a claim that Stage 10 UI reconstruction is complete. Stage 10A closes the audit/design pass only and is now superseded by the completed Stage 10B backend/session implementation.

## Stage 10B UI Backend & Navigation Report

Stage 10B is complete as a backend/session/navigation pass and does not include Stage 10C resource-pack visuals, Stage 11 performance work, or Stage 12 parity audit work.

### Stage 10B summary

- `/atd` and `/achievetodo` open a 54-slot AchieveToDo browser for vanilla clients
- UI state is derived only from the existing progression and ability backends
- per-player UI sessions track route, locale, last rendered view, and safe reopen/update behavior
- progression-runtime listener callbacks mark active sessions dirty and refresh them on the next scheduled UI tick
- hidden abilities render as `UNKNOWN_LOCKED` without leaking true ability identity
- demystified locked abilities render the real title/description plus progress and remaining count
- owned UI inventories are ignored by restriction, equipment, and bundle adapters to avoid gameplay false positives
- inactive page navigation remains visible but now advertises its disabled state honestly

### Implementation

- pure Java UI domain:
  - `UiRoute`
  - `UiAbilityGroup`
  - `UiAbilityLayoutResolver`
  - `UiAbilityStateResolver`
  - `UiAbilityBrowserService`
  - view-model records for tabs, abilities, screens, and actions
- Bukkit runtime:
  - `BukkitUiRuntime`
  - `BukkitUiSessionManager`
  - `BukkitUiItemFactory`
  - `BukkitUiInventoryHolder`
  - `PlayerUiSession`
- plugin/runtime integration:
  - `/atd` and `/achievetodo` command registration
  - progression listener registration through `BukkitProgressionRuntime`
  - UI cleanup on close, quit, and plugin disable
- localization:
  - Stage 10B fallback catalogs in `src/main/resources/ui/lang`
  - imported original READ ONLY catalogs under `src/main/resources/ui/lang/original`

### Tests

- `UiAbilityStateResolverTest`
- `UiAbilityLayoutResolverTest`
- `UiAbilityBrowserServiceTest`
- `UiTextServiceTest`
- `Stage2CoreTestMain`

Added coverage verifies:

- hidden abilities do not expose identity
- relock returns to demystified locked rendering
- unavailable progression does not fabricate a screen
- route transitions reset page correctly
- single-page routes do not expose a next-page action
- UI text parsing and locale fallback work
- every ability appears exactly once in deterministic group layout

### Live validation

- `/atd` open path - `PASS`
- unknown locked privacy - `PASS`
- demystified locked rendering - `PASS`
- `Landmark` category rendering - initial result `FAIL`, final result `PASS`
- live refresh while screen stays open - `PASS`
- inactive next-page navigation messaging - initial result `FAIL`, final result `PASS`
- Stage 10B closure-check acceptance criteria - initial result `FAIL`, final result `PASS`

Observed live details:

- hovering a hidden action ability exposed only the mystery placeholder and did not leak the real ability identity
- `USE_WATER_BUCKET` displayed the real localized name/description plus obtained/remaining progress while still locked
- the initial `Landmark` screen looked empty because `UNKNOWN_LOCKED` used `GRAY_STAINED_GLASS_PANE`, visually blending into filler panes
- changing the mystery placeholder material to `TINTED_GLASS` made the slots visibly distinct and the client then confirmed two visible rows of mystery placeholders
- with `/atd` open on `Actions`, a controlled scoreboard progression change updated `USE_WATER_BUCKET` from `42 / 134` to `44 / 134` without closing or visibly reopening the screen
- on single-page `Actions`, slot `53` correctly had no click action; Stage 10B now labels that disabled state as `Больше страниц нет` instead of presenting it as a misleading active `Следующая страница`
- closure-check findings on the final JAR:
  - `/atd` still opens while `OPEN_INVENTORY` is locked
  - an ordinary nearby chest still remains denied in that same locked state, so the UI command does not create a gameplay inventory bypass
  - with BACAP objective removed from `SIDEBAR`, `/atd` does not fabricate a screen and reports that BACAP progression is not ready
  - normal `Esc` close did not self-reopen the UI
  - kick/disconnect while `/atd` was open cleaned the active UI session, and reconnect created a fresh session on the initial tab with no stale route state
  - owned UI inventories intentionally freeze lower-inventory mutation paths while open, preventing representative shift-click and drag leakage
  - representative `NUMBER_KEY` hotbar-swap originally kept items safe but still misrouted as a semantic UI click; this was fixed and live-retested

### Bugs found and fixed

1. `Landmark` unknown slots looked visually empty
   - fix:
   - changed unknown placeholder material from `GRAY_STAINED_GLASS_PANE` to `TINTED_GLASS`
   - retest:
   - `PASS`
2. inactive next-page arrow looked like a broken active button
   - fix:
   - added dedicated localized disabled titles for previous/next buttons
   - added regression coverage for single-page routes
   - retest:
   - `PASS`
3. `NUMBER_KEY` / hotbar-swap clicks on owned UI slots still triggered semantic UI actions
   - fix:
   - added `BukkitUiClickPolicy` so only direct top-inventory left/right clicks can route as semantic UI actions
   - added regression coverage for `NUMBER_KEY`, shift-click, offhand-swap, and lower-inventory clicks
   - retest:
   - `PASS`

### Current Stage 10 status after 10B

- Stage 10A audit/design - `PASS`
- Stage 10B backend/session/navigation - `PASS`
- Stage 10C resource-pack visuals and fidelity work - `NOT STARTED`

## Stage 10A Original UI Audit & Visual Contract Report

Stage 10A is complete as an audit/design pass and does not include Stage 10B implementation, Stage 10C resource-pack production assets, or Stage 11 work.

### Stage 10A summary

- original UI source map completed
- main screen identified as the modified vanilla advancements screen
- single-player-only auxiliary UI identified separately from runtime player UI
- original assets, text, and payload contract inventoried
- vanilla feasibility matrix documented
- resource-pack requirement recommendation documented
- Stage 10B architecture recommendation documented

Stage 9C feature-landmark finalization is complete as of August 18, 2026:

- `desert_well` and `monster_room` original Fabric semantics were re-audited READ ONLY from the generation hook and chunk-persistence paths
- supported Paper/Purpur API was re-audited and does not expose exact feature-generation provenance equivalent to the original `Feature.generateIfValid(...)` tracking path
- Stage 9C replaced repeated post-load feature rescanning with a one-time feature-scan lifecycle plus persisted scan markers
- already persisted feature landmarks are now preserved across reload/restart without being recomputed from later block edits
- later hand-built replicas inside already scanned chunks no longer become falsely persisted feature landmarks on reload
- exact generation provenance, exact generated-write bounding boxes, and exact command-generation parity still remain unsupported through current non-NMS API
- no NMS was introduced

This is still not a claim of full behavioral parity. Stage 9C closes the feature-landmark finalization pass honestly as an accepted supported-API approximation, and the remaining work now moves to Stage 10 UI reconstruction plus later performance/release hardening.

## Stage 9C Feature Landmark Finalization Report

Stage 9C is complete as a feature-landmark finalization pass and does not include Stage 10, Stage 11, or Stage 12 work.

### Checklist audit

- `1. desert_well original generation semantics` - `PASS`
- `2. monster_room original generation semantics` - `PASS`
- `3. supported Paper/Purpur provenance feasibility` - `PASS`
- `4. desert_well natural-generation live evidence` - `INCONCLUSIVE`
- `5. monster_room natural-generation live evidence` - `NOT TESTED`
- `6. command-generation evidence` - `INCONCLUSIVE`
- `7. hand-built replica resistance after Stage 9C patch` - `PASS`
- `8. persisted reload/restart preservation` - `PASS`
- `9. exact original provenance parity` - `SEMANTIC DIFFERENCE - ACCEPTED`

### Original semantics established from `./original` READ ONLY

- `LandmarkType` maps both landmarks to real feature identities:
  - `DESERT_WELL(Feature.DESERT_WELL)`
  - `MONSTER_ROOM(Feature.MONSTER_ROOM)`
- `FeatureMixin` wraps `Feature.generateIfValid(...)` / `Feature.generate(...)` and enables `LandmarkGenerationTracker` only for mapped feature types
- while tracking is enabled, mixins around `setBlockState(...)` collect the real worldgen write envelope into a `BlockBox`
- after generation completes, the tracked box is stored as a feature landmark in the chunk associated with the feature generation position
- feature landmarks are serialized into chunk NBT, reloaded from chunk NBT, and copied through the ProtoChunk -> WorldChunk path
- original runtime therefore depends on feature identity and generation provenance rather than only on the final visible block pattern
- a hand-built visual replica would not trigger the original by itself
- later block edits do not erase the already persisted original feature landmark automatically

### Supported API findings

- current supported Paper/Purpur API exposes chunk lifecycle hooks such as `ChunkLoadEvent.isNewChunk()` and `ChunkPopulateEvent`
- current supported API does not expose a feature-specific generate callback or generated-write `BlockBox` equivalent for `desert_well` / `monster_room`
- structure-generation APIs do not cover these feature landmarks as an exact substitute
- because of that API gap, exact original provenance parity is not available without unsupported internals or NMS

### Final implementation

- `BukkitLandmarkRuntime` now distinguishes three feature-scan modes:
  - `NONE` for newly loaded chunks that should wait for populate
  - `POPULATION` for first-generation population scanning
  - `COMPATIBILITY` for already generated chunks that still need a one-time compatibility scan
- `FeatureLandmarkScanPolicy` decides whether a chunk should:
  - scan and mark
  - mark only
  - skip
- `LandmarkPersistence` now records a sidecar `.feature-scan` marker per chunk
- once a chunk is marked as scanned, ordinary later reloads do not recompute feature landmarks from current blocks
- if a persisted feature landmark record already exists, the chunk is marked and preserved without unnecessary re-detection

### DESERT_WELL

- original semantics:
  - identified only from real `Feature.DESERT_WELL` generation
  - bounding box is the actual generated-write envelope captured during generation
  - source chunk comes from the feature-generation position
  - persisted through chunk serialization and survives later block edits
- current pre-Stage 9C behavior:
  - heuristic post-load pattern scan based on visible block layout
  - later block edits or replicas could become persisted landmarks when a chunk was rescanned
- natural generation evidence:
  - `INCONCLUSIVE`
  - nearby desert search was performed without broad destructive world churn, but no natural registered candidate was confirmed in this test world during Stage 9C
- hand-built replica evidence:
  - `PASS` for the Stage 9C patched lifecycle goal
  - after an isolated chunk was first scanned and marked with no landmark file, building a desert-well-style replica later in that same chunk and reloading it did not create a persisted landmark
- command generation:
  - `INCONCLUSIVE`
  - original hook strongly suggests true feature-generation paths would count, but current supported API still does not expose exact command-path provenance and the attempted live `/place feature` path did not produce a usable confirmation
- bounding box:
  - final Purpur behavior remains heuristic rather than the original exact generated-write box
- source chunk:
  - final Purpur behavior uses the scanning/source chunk contract already hardened in Stage 8B, but provenance remains approximate rather than exact original generation provenance
- persistence:
  - persisted feature records continue to load
  - scan markers now prevent later ordinary reloads from re-promoting block edits into fake feature landmarks
- false positives:
  - materially reduced after first scan because later edits in already scanned chunks are ignored for feature registration
  - still possible on the first compatibility scan of a previously generated unscanned chunk if a replica already exists there
- false negatives:
  - still possible because the detector remains heuristic and does not see the original worldgen identity directly
- final classification:
  - `SEMANTIC DIFFERENCE - ACCEPTED`

### MONSTER_ROOM

- original semantics:
  - identified only from real `Feature.MONSTER_ROOM` generation
  - bounding box is the actual generated-write envelope captured during generation
  - source chunk comes from the feature-generation position
  - persisted through chunk serialization and survives later block edits
- current pre-Stage 9C behavior:
  - heuristic post-load spawner-room pattern scan
  - later block edits or replicas could become persisted landmarks when a chunk was rescanned
- natural generation evidence:
  - `NOT TESTED`
  - no safe naturally generated Stage 9C monster-room candidate was confirmed in the existing world without escalating into broader discovery churn
- hand-built replica evidence:
  - `PASS` for the Stage 9C patched lifecycle goal
  - after an isolated chunk was first scanned and marked with no landmark file, building a monster-room-style spawner room later in that same chunk and reloading it did not create a persisted landmark
- command generation:
  - `NOT TESTED`
  - exact command-path equivalence was not proven from supported API or live evidence
- bounding box:
  - final Purpur behavior remains heuristic rather than the original exact generated-write box
- source chunk:
  - final Purpur behavior uses the Stage 8B source-chunk invariant, but provenance remains approximate rather than exact original generation provenance
- persistence:
  - persisted feature records continue to load
  - scan markers now prevent later ordinary reloads from re-promoting block edits into fake feature landmarks
- false positives:
  - materially reduced after first scan because later edits in already scanned chunks are ignored for feature registration
  - still possible on the first compatibility scan of a previously generated unscanned chunk if a replica already exists there
- false negatives:
  - still possible because the detector remains heuristic and does not see the original worldgen identity directly
- final classification:
  - `SEMANTIC DIFFERENCE - ACCEPTED`

### Live and persistence evidence

- baseline discovery on the pre-patch JAR confirmed that broad forceload-based natural search was a poor method and could stall while worldgen completed
- the Stage 9C patch was then validated on one fresh JAR / one clean restart cycle
- isolated fresh chunks at `[376, 375]` and `[377, 375]` produced only `.feature-scan` markers with no `.landmarks` files after the initial empty scan
- later hand-built desert-well and monster-room replicas in those already scanned chunks still did not create `.landmarks` files after unload/reload
- this confirms the intended new lifecycle:
  - first scan can register or clear the chunk once
  - later block edits do not become persisted feature landmarks through ordinary reloads

### Existing-world compatibility

- existing `.landmarks` files remain readable
- Stage 9C does not mass rewrite or delete prior persisted feature-landmark files
- previously generated unscanned chunks still rely on a one-time compatibility scan, so exact original provenance is not retroactively reconstructable
- plugin upgrades from the Stage 9B.2 JAR preserve prior persisted feature landmarks and add scan markers lazily as chunks are seen

### Performance sanity

- Stage 9C did not reintroduce synchronous eager structure scans on ordinary chunk load
- structure landmarks remain on their separate event-driven path from earlier stages
- one broad forceload discovery attempt stalled while new terrain generated, but this was discovery-method churn rather than evidence of the old Stage 9A structure-scan freeze class
- dense or broad worldgen search remains a deferred Stage 11 performance concern

Stage 9B.2 remaining gameplay restriction parity is complete as of August 18, 2026:

- portal restriction ordering is now live-verified for:
  - unmounted player portal denial
  - controlling mounted rider denial
  - non-controlling minecart passenger separation behavior
- merchant open suppression is now live-verified on real client screens for both villager and wandering-trader paths
- water-bucket and waterlogging behavior is now live-verified for:
  - denied water placement into a waterloggable target while `use_water_bucket` is locked
  - allowed empty-bucket drain of open waterlogged stairs outside a locked landmark
- nearby world-change and spectator carried-forward scenarios are now live-verified after a safe-destination retest
- direct equipment, offhand swap, and dispenser armor denial are now live-verified
- live testing found and fixed three runtime defects:
  - vault shared-state deny feedback could repeat passively through `VaultChangeStateEvent`
  - direct right-click armor equip could commit and leave the item stuck equipped
  - bow launch denial did not intercept the real bow firing path
- live testing also established two honest parity limits without introducing NMS:
  - projectile landmark parity remains post-hit rather than original pre-hit `canHit(...)`
  - bundle handling now matches the tested insert-and-close path, but exact non-insert bundle mutation parity is still broader than the original source hook
- no NMS was introduced

This is still not a claim of full behavioral parity. The remaining open work is now concentrated in Stage 9C feature-landmark finalization and later performance/release hardening.

## Stage 9B.2 Remaining Gameplay Restriction Parity Report

Stage 9B.2 is complete as a gameplay-path parity pass and does not include Stage 9C, Stage 10, or Stage 11 work.

### Checklist audit

- `1. Portal / passenger / mounted ordering` - `PASS`
- `2. Vault shared-state behavior` - `INCONCLUSIVE`
- `3. Merchant / trade UI server semantics` - `PASS`
- `4. Merchant / trade UI client semantics` - `PASS`
- `5. Bundle mutation paths` - `SEMANTIC DIFFERENCE`
- `6. Waterlogging parity` - `PASS`
- `7. Projectile exact semantics` - `SEMANTIC DIFFERENCE`
- `8. Equipment / offhand player paths` - `PASS`
- `9. Dispenser / automation equipment path` - `PASS`
- `10. Movement / swimming carried-forward audit` - `CLOSED / SUPERSEDED`
- `11. Nearby teleport / world-change / spectator audit` - `PASS`
- `12. Synthetic advancement timing audit` - `CLOSED / SUPERSEDED`
- `13. Deprecated SMITHING_NEW warning follow-up` - `INCONCLUSIVE`

### 9B.2A Portal / passenger / mounted ordering

- original behavior:
  - direct player portal use is denied when the required portal ability is locked
  - if a vehicle's controlling passenger is the locked player, original code cancels the whole transport portal use
  - if the locked player is only a non-controlling passenger, original code removes that passenger while allowing the transport to continue
- current port behavior before changes:
  - direct player denial already worked
  - mounted vehicle denial could recurse through passenger collection and later split rider from mount
  - minecart passenger denial canceled the whole transport instead of matching original non-controlling-passenger semantics
- Paper/Purpur integration path used:
  - `PlayerPortalEvent`
  - `PlayerTeleportEvent`
  - `EntityPortalEnterEvent`
  - `EntityPortalEvent`
- live test setup:
  - plain survival player portal
  - saddled horse with `Glower4ek` as controlling rider
  - minecart with `Glower4ek` as passenger and portal plane aligned perpendicular to rails
- actual server observation:
  - unmounted player portal denial fired immediately and no teleport happened
  - mounted horse path initially reproduced a rider/mount split defect, then after fix denied the whole transport use with no split
  - minecart passenger path after fix left `Glower4ek` in the overworld with no `RootVehicle` while the minecart could continue the portal path
- actual client observation:
  - unmounted: no teleport
  - mounted horse after retest: deny message appeared immediately, no portal effect delay, rider stayed mounted, neither player nor horse teleported
  - minecart passenger after retest: the minecart teleported while `Glower4ek` was left beside the portal and did not teleport
- classification:
  - `PASS`
- bugs found:
  - recursive passenger collection could blow the mounted path
  - controlling-passenger deny originally allowed rider/mount separation
  - non-controlling passenger path originally canceled the whole transport
- fixes made:
  - added a cycle guard for portal transport passenger collection
  - added short-lived denied-transport suppression so mounted denial blocks follow-up transport events cleanly
  - split controlling-passenger and non-controlling-passenger behavior to match original semantics
- live retest result:
  - horse controlling-rider denial - `PASS`
  - minecart non-controlling-passenger separation - `PASS`

### 9B.2B Vault shared state

- original behavior:
  - vault connected-player shared state silently filters out players who lack `unlock_vault`
  - actual unlock / loot transition is separately denied in the vault server logic
- current port behavior before changes:
  - connected-player filtering denied loudly through the generic feedback path
  - standing near an interactable vault could continuously resend the deny actionbar without a click
- Paper/Purpur integration path used:
  - `VaultChangeStateEvent`
  - player interaction path through the existing restriction service
- live test setup:
  - prepared a vault reachable in survival
  - kept `Glower4ek` near the vault without interacting to isolate passive shared-state callbacks
- actual server observation:
  - before fix, passive state changes could retrigger deny feedback without `PlayerInteract`
  - after fix, the shared-state filter remained enforced but did not emit cyclical actionbar spam while idle
- actual client observation:
  - before fix, `Locked ability: unlock_vault` reappeared repeatedly while simply standing nearby
  - after retest, idle actionbar stayed empty
- classification:
  - `INCONCLUSIVE`
- rationale:
  - the live single-player connected-player path and the spam bug fix are validated
  - full multi-player shared-state observation was not claimed without a second real client
- bug found:
  - passive shared-state filtering used loud deny feedback and could spam the actionbar
- fix made:
  - shared vault filtering now uses a silent deny path
- live retest result:
  - idle shared-state spam retest - `PASS`

### 9B.2C Merchant / trade UI semantics

- original behavior:
  - source suppresses merchant opening by making the offer list effectively unavailable when the relevant trade ability is locked
  - this is closer to "screen never opens" than to "screen opens and purchase later fails"
- current port behavior before changes:
  - adapter already denied merchant open/select/execute paths through Bukkit-visible merchant events
- Paper/Purpur integration path used:
  - merchant open and trade-related Bukkit events through `TradeAdapter`
- live test setup:
  - prepared both a villager and a wandering trader for locked trade abilities
- actual server observation:
  - no trade transaction proceeded
  - no exception or repeated feedback loop was observed
- actual client observation:
  - villager window did not open
  - wandering-trader window did not open
- classification:
  - `PASS`
- bug found:
  - none in Stage 9B.2
- fix made:
  - none
- live retest result:
  - villager open suppression - `PASS`
  - wandering-trader open suppression - `PASS`

### 9B.2D Bundle mutation paths

- original behavior:
  - original hooks specifically target inserting items into a bundle
  - on deny, the handled screen is closed immediately
- current port behavior before changes:
  - deny already covered inventory-visible bundle interactions, but the client screen stayed open on the tested insert path
  - the adapter currently keys off any click touching a bundle item, which is broader than the original source hooks
- Paper/Purpur integration path used:
  - `InventoryClickEvent`
  - `InventoryDragEvent`
- live test setup:
  - locked `put_in_bundle`
  - bundle in player inventory with cobblestone prepared for insertion
- actual server observation:
  - insert attempts were denied
  - after fix, the inventory screen was closed on deny
- actual client observation:
  - before fix: item did not insert, but the inventory remained open
  - after retest: item still did not insert and the inventory closed immediately
- classification:
  - `SEMANTIC DIFFERENCE`
- rationale:
  - the tested insert-and-close path now matches the original observable behavior
  - exact non-insert bundle mutation parity is still not identical because the current adapter is broader than the original insertion-specific hooks
- bug found:
  - denied insert path did not close the inventory screen
- fix made:
  - bundle denial now closes the inventory immediately
- live retest result:
  - insert-path close-on-deny retest - `PASS`

### 9B.2E Waterlogging parity

- original behavior:
  - locked `use_water_bucket` prevents relevant bucket-use transitions
  - `WaterloggableMixin` separately blocks `tryDrainFluid` for locked landmarks
  - empty-bucket draining of open waterlogged stairs outside a locked landmark is not denied by that original landmark hook
- current port behavior before changes:
  - current fluid adapter already handled the tested bucket and waterloggable paths through Bukkit-visible events
- Paper/Purpur integration path used:
  - water bucket / empty bucket interaction events through `FluidAdapter`
- live test setup:
  - locked `use_water_bucket`
  - open waterloggable stairs with direct face access
  - tested both placing water into the stairs and draining existing water from waterlogged stairs
- actual server observation:
  - water placement into the target was denied and final stairs state stayed `waterlogged=false`
  - draining open waterlogged stairs outside a locked landmark was allowed
- actual client observation:
  - placement deny: water did not appear and the water bucket remained unchanged
  - drain allow: empty bucket became a water bucket
- classification:
  - `PASS`
- bug found:
  - none in Stage 9B.2
- fix made:
  - none
- live retest result:
  - place-water deny path - `PASS`
  - open-stairs drain path outside locked landmark - `PASS`

### 9B.2F Projectile exact semantics

- original behavior:
  - original landmark restriction changes `ProjectileEntity.canHit(...)` before impact
  - when denied, the projectile should fail hit eligibility rather than merely being noticed after collision
- current port behavior before changes:
  - bow firing was not denied on the real bow path
  - landmark collision parity was implemented from `ProjectileHitEvent`, which is later than original `canHit(...)`
- Paper/Purpur integration path used:
  - `PlayerLaunchProjectileEvent`
  - `EntityShootBowEvent`
  - `ProjectileHitEvent`
- live test setup:
  - locked projectile-use path with a bow
  - controlled survival shooting lane toward an armor stand target
- actual server observation:
  - before fix, bow use still launched
  - after fix, locked bow use did not launch at all
  - block/entity landmark-hit parity remains post-hit, because Bukkit does not expose the original pre-hit `canHit(...)` hook here without NMS
- actual client observation:
  - after retest, the arrow did not fire
- classification:
  - `SEMANTIC DIFFERENCE`
- rationale:
  - launch denial is now correct for the tested player-fired path
  - exact landmark hit-eligibility parity remains later than the original hook
- bug found:
  - bow launch denial missed the real bow event path
- fix made:
  - added `EntityShootBowEvent` handling
- live retest result:
  - locked bow launch retest - `PASS`

### 9B.2G Equipment / dispenser / offhand automation

- original behavior:
  - direct equip is blocked before commit
  - armor-slot insert/take denial closes the screen
  - dispenser armor equip returns `canEquipFromDispenser=false`
  - no special separate offhand-only semantic was found beyond ordinary equipment/inventory restriction handling
- current port behavior before changes:
  - right-click leather armor equip could still commit and then become stuck equipped
  - dispenser armor denial already behaved correctly in the tested path
  - offhand swap path was implemented but not yet live-validated
- Paper/Purpur integration path used:
  - `PlayerInteractEvent`
  - `PlayerInventorySlotChangeEvent`
  - `InventoryClickEvent`
  - `InventoryDragEvent`
  - `PlayerSwapHandItemsEvent`
  - `BlockDispenseArmorEvent`
  - `PlayerArmorStandManipulateEvent`
- live test setup:
  - locked `equip_leather_armor`
  - direct right-click leather cap equip
  - `F` offhand swap with leather cap in hand
  - dispenser firing a leather helmet at the player
- actual server observation:
  - before fix, right-click equip could commit
  - after fix, direct equip rolled back cleanly
  - offhand swap did not move the helmet and triggered deny feedback
  - dispenser armor path did not equip at all
- actual client observation:
  - before fix, the cap equipped and then could not be removed
  - after retest, the cap did not equip
  - offhand swap was blocked immediately with actionbar deny feedback
  - dispenser armor did not equip
- classification:
  - player equipment / offhand - `PASS`
  - dispenser / automation - `PASS`
- bug found:
  - direct equip could commit and leave the armor stuck equipped
- fix made:
  - added slot-change rollback handling for committed direct equip
- live retest result:
  - direct right-click equip retest - `PASS`
  - offhand `F` swap retest - `PASS`
  - dispenser armor retest - `PASS`

### 9B.2H Movement / swimming carried-forward audit

- audit result:
  - `CLOSED / SUPERSEDED`
- rationale:
  - the remaining Stage 9B.2 work did not uncover a separate unresolved movement-system semantic gap beyond the existing `MovementAdapter` paths
  - no new live defect surfaced in jump, sprint, sneak, glide, or swimming handling while running Stage 9B.2 scenarios
  - the earlier carried-forward wording is superseded by the fact that Stage 9B.2 focused on other gameplay parity gaps and did not produce a distinct remaining movement bug

### 9B.2I Nearby teleport / world-change / spectator audit

- original behavior:
  - nearby-entity progression is per-player runtime state and should converge after movement or world changes
  - spectator mode should not keep ordinary nearby progression active
- current port behavior before changes:
  - movement in/out, reconnect, restart, and `/reload` were already validated
  - teleport/world-change and spectator semantics were still carried forward as open
- Paper/Purpur integration path used:
  - existing nearby scan task
  - `PlayerChangedWorldEvent`
  - spectator handling in `BukkitNearbyEntityScanner`
- live test setup:
  - prepared a force-loaded safe overworld platform with nearby cows and `landmarks=none`
  - prepared a safe force-loaded Nether platform after an initial invalid destination setup
- actual server observation:
  - safe overworld platform:
    - `nearby animal_kingdom=3`
    - `landmarks=none`
  - after switching to spectator and waiting one reconciliation tick:
    - nearby snapshot converged to `0`
  - after switching back to survival on the same platform:
    - nearby snapshot converged back to the expected non-zero value
  - first Nether destination attempt was invalid and put the player into blocks, so that attempt is not counted
  - after safe-destination retest:
    - Nether snapshot showed `world=world_nether`, `nearby animal_kingdom=0`, `landmarks=none`
    - returning to the overworld restored the nearby snapshot to `3`
- actual client observation:
  - on the invalid first attempt, the player suffocated in blocks and the scenario was discarded
  - on the safe retest, the player stood normally on the open Nether platform without suffocation
- classification:
  - `PASS`
- bugs found:
  - no runtime bug in AchieveToDo was confirmed
  - one unsafe temporary live setup was discarded as invalid test preparation
- fixes made:
  - none in plugin code
- live retest result:
  - spectator suppression - `PASS`
  - safe world-change convergence retest - `PASS`

### 9B.2J Synthetic advancement timing audit

- audit result:
  - `CLOSED / SUPERSEDED`
- rationale:
  - reconnect, restart, and active-player `/reload` timing were already validated in Stage 9A and Stage 9B.1
  - Stage 9B.2 gameplay-path tests repeatedly exercised demystified/locked and unlocked transitions without exposing a new timing defect
  - no separate synthetic-timing-only bug remained after the live gameplay-path pass

### Bugs found

- vault shared-state deny feedback could spam the actionbar passively
- direct right-click leather armor equip could commit and then remain stuck equipped
- locked bow use could still fire because the real bow event path was not intercepted
- mounted portal deny and non-controlling passenger portal deny both diverged from original transport semantics
- bundle deny did not close the inventory screen on the tested insert path

### Fixes made

- added a silent shared-state deny path for vault connected-player filtering
- added direct-equip rollback through `PlayerInventorySlotChangeEvent`
- added `EntityShootBowEvent` handling for the real bow launch path
- corrected mounted-versus-non-controlling passenger portal ordering and guarded passenger collection
- closed the inventory immediately on denied bundle insertion

### Retest results

- passive vault actionbar spam idle retest - `PASS`
- horse controlling-rider portal retest - `PASS`
- minecart non-controlling passenger retest - `PASS`
- locked bow launch retest - `PASS`
- direct leather armor equip retest - `PASS`
- bundle insert close-on-deny retest - `PASS`
- spectator suppression and safe world-change convergence retest - `PASS`

### Remaining gaps after Stage 9B.2

#### Remaining gaps for Stage 9C

- feature-landmark natural-generation parity for:
  - `desert_well`
  - `monster_room`

#### Remaining Stage 9B semantic differences or non-claims

- vault shared-state parity is still not claimed beyond the validated single-real-player connected-state path
- bundle parity outside the tested insert-and-close path remains broader than the original insertion-specific hooks
- projectile landmark parity remains post-hit instead of original pre-hit `canHit(...)`
- deprecated `InventoryType.SMITHING_NEW` warning is still documented but not changed speculatively

#### Deferred Stage 11 performance items

- dense-entity nearby-scan behavior remains a performance concern rather than a Stage 9B.2 semantic bug claim

Stage 9B.1 live edge-case validation is complete as of August 18, 2026:

- exact landmark boundary inclusion is now live-verified on a persisted village structure with a known `BlockBox`
- chunk-border traversal inside the same locked village landmark is now live-verified without a transient `ALLOW`
- active-player `/reload` continuity is now live-verified with the player kept online through a real reload
- team BACAP progression is now live-verified for online assignment, reassignment, removal, and reconnect
- personal-versus-team progression identity is now live-verified through objective-slot switching and team switching
- no Stage 9B.1 runtime defect requiring a code fix was confirmed
- no NMS was introduced

This is still not a claim of full behavioral parity. The remaining open work is now concentrated in Stage 9B.2 gameplay-path parity and Stage 9C feature-landmark finalization.

## Stage 9B.1 Live Edge Cases Report

Stage 9B.1 is complete as a live edge-case validation pass and does not include Stage 9B.2, Stage 9C, or Stage 10 work.

### Checklist audit

- `1. Landmark exact boundary` - `PASS`
- `2. Chunk-border traversal` - `PASS`
- `3. Active-player /reload continuity` - `PASS`
- `4. Team assignment` - `PASS`
- `5. Team reassignment` - `PASS`
- `6. Team removal` - `PASS`
- `7. Personal vs team identity` - `PASS`
- `8. Reconnect with active team` - `PASS`
- `9. Sanity / regression observations` - `PASS`
- `10. Deprecated SMITHING_NEW warning follow-up` - `INCONCLUSIVE`

### 9B.1.1 Landmark exact boundary

- setup:
  - reused the persisted village landmark already validated in Stage 9A
  - live persisted record:
    - `village@source=75,78@box=1206,53,1250..1348,76,1342`
  - compared the adjacent X positions `1348` and `1349` at the same Z coordinate
- expected original semantics:
  - original landmark checks use `BlockBox.intersects(...)`
  - the max-X edge of the stored box is inclusive, so `x=1348` should still be inside and `x=1349` should be outside
- actual server observation:
  - at `1348,72,1290`:
    - `engine.query` matched the village
    - `landmarkDecision kind=deny_landmark`
  - at `1349,72,1290`:
    - `engine.query` returned `none`
    - `landmarkDecision kind=allow`
- actual client observation:
  - the player was moved across the boundary without disconnect or freeze
- result:
  - `PASS`
- conclusion:
  - no off-by-one error was observed at the tested max-X landmark boundary

### 9B.1.2 Chunk-border traversal

- setup:
  - stayed inside the same persisted village landmark
  - crossed the chunk boundary from `x=1279` to `x=1280` at `z=1290`
- expected original semantics:
  - the player should remain inside the same landmark while crossing the chunk edge
  - no transient `ALLOW` should appear
- actual server observation:
  - before the step:
    - `pos=1279,72,1290`
    - village landmark matched
    - `landmarkDecision kind=deny_landmark`
  - after the step:
    - `pos=1280,67,1290`
    - `targetBox=BlockBox[minX=1279, ..., maxX=1280, ...]`
    - village landmark still matched
    - `landmarkDecision kind=deny_landmark`
  - no server freeze was observed
  - no repeated eager structure-scan symptom was observed in logs or behavior
- actual client observation:
  - the player crossed the chunk border normally
  - no visible disconnect or freeze occurred
- result:
  - `PASS`

### 9B.1.3 Active-player `/reload` continuity

- setup:
  - prepared a controlled pre-reload state with:
    - active locked village context
    - nearby snapshot `animal_kingdom=2`
    - synthetic advancement state `open_chest snapshot=unlocked kind=unlocked demystified=true`
    - active team BACAP objective in sidebar
  - before snapshot:
    - `pos=1348,69,1290`
    - `objective=TEAM_ADVANCEMENTS via bac_advancements_team in SIDEBAR`
    - `landmarkDecision kind=deny_landmark`
    - `nearby animal_kingdom=2`
- expected original semantics:
  - plugin runtime should recover after `/reload`
  - player should remain online
  - objective detection, landmark context, nearby runtime, and synthetic state should remain coherent
- actual server observation:
  - console accepted a real `/reload`
  - post-reload console remained responsive
  - after reload:
    - `There are 1 of a max of 20 players online: Glower4ek`
    - `progression ready=true`
    - `objective=TEAM_ADVANCEMENTS via bac_advancements_team in SIDEBAR`
    - `nearbyScannerRunning=true`
    - `landmarks=village@source=75,78@box=1206,53,1250..1348,76,1342`
    - `landmarkDecision kind=deny_landmark`
    - `open_chest snapshot=unlocked kind=unlocked demystified=true`
  - nearby count converged from `2` before reload to `1` after reload without error or duplicate spam
  - no duplicate-listener symptom, repeated advancement spam, or exception was observed
- actual client observation:
  - the player remained connected through the reload
- result:
  - `PASS`
- note:
  - an earlier uncontrolled reload attempt became non-representative because the player died to a skeleton before the measurement snapshot; the controlled retest above is the valid Stage 9B.1 result

### 9B.1.4 Team BACAP validation

- setup:
  - created `bac_advancements_team`
  - set sidebar to `bac_advancements_team`
  - assigned:
    - `Glower4ek = 41`
    - `helper_red = 1`
  - created teams `red` and `blue`
- expected original semantics from source:
  - when a team BACAP objective is active, effective obtained count is the sum of the active team members' scores
  - if no team is assigned, original code warns and leaves the previous obtained count unchanged
- actual server observation:
  - objective switch to team mode was detected on the next reconciliation pass, not immediately in the same tick
  - this timing matched the port's existing reconciliation model and was not treated as a defect by itself

#### Assignment

- action:
  - `Glower4ek` joined `red`
  - `helper_red` already contributed `1`
- observation after reconciliation:
  - `obtained=42`
  - `open_chest snapshot=unlocked kind=unlocked demystified=true`
- result:
  - `PASS`

#### Reassignment

- action:
  - moved `Glower4ek` from `red` to `blue`
- observation after reconciliation:
  - `obtained=41`
  - `open_chest snapshot=demystified kind=locked demystified=true`
  - old `red` helper score no longer affected the player
- result:
  - `PASS`

#### Removal

- action:
  - removed `Glower4ek` from any team while team objective remained active
- observation after reconciliation:
  - runtime logged:
    - `BACAP team objective is active, but player Glower4ek is not assigned to any scoreboard team`
  - the player's obtained count remained at the last resolved value
    - from a `41` state, it stayed `41`
    - from a `42` state, it stayed `42`
- classification:
  - `PASS`
- rationale:
  - this matches the current original-source semantics more closely than forcing a synthetic fallback to personal mode or zero

### 9B.1.5 Personal vs team identity

- setup:
  - used the same live team-mode scenario and then switched sidebar objective back and forth between:
    - `bac_advancements`
    - `bac_advancements_team`
- expected semantics:
  - personal mode should use the player's personal score
  - team mode should use the active team sum
  - switching team or objective should not permanently mix identities
- actual server observation:
  - in stable team mode:
    - `obtained=42`
    - `open_chest unlocked`
  - after switching sidebar back to personal mode and waiting for reconciliation:
    - `objective=DEFAULT via bac_advancements in SIDEBAR`
    - `obtained=41`
    - `open_chest demystified/locked`
  - after switching back to team mode and waiting for reconciliation:
    - `objective=TEAM_ADVANCEMENTS via bac_advancements_team in SIDEBAR`
    - `obtained=42`
    - `open_chest unlocked`
  - after reconnect with active team mode:
    - `obtained=42`
    - `open_chest unlocked`
- result:
  - `PASS`

### 9B.1.6 Sanity / regression observations

- no AchieveToDo exception was observed
- no server freeze or tick-stall symptom was observed during boundary, chunk-border, team, reconnect, or reload scenarios
- no repeated structure-scan freeze symptom reappeared
- no duplicate advancement grant/revoke spam was observed
- no duplicate scheduler/listener symptom was confirmed from runtime behavior or logs
- the known upstream `org.joml` `sun.misc.Unsafe` warning still appears on startup and is not emitted by AchieveToDo

### Bugs found

- no Stage 9B.1 port runtime defect was confirmed

### Fixes made

- none

### Retest results

- controlled `/reload` retest in a live locked-landmark + nearby + synthetic-unlocked state - `PASS`
- team removal retest from both `41` and `42` steady states - `PASS`

### Verification results in this workspace

- `gradlew.bat clean coreTest` - `PASS` on August 18, 2026
- `gradlew.bat build` - `PASS` on August 18, 2026
- fresh plugin jar regenerated via wrapper and redeployed to:
  - `test-server/plugins/AchieveToDo-Purpur-26.2-0.1.5-SNAPSHOT.jar`

### Deprecated `SMITHING_NEW` warning status

- investigated during Stage 9B.1
- current warning:
  - `InventoryType.SMITHING_NEW` is deprecated and marked for removal
- result:
  - `INCONCLUSIVE`
- rationale:
  - a trivially safe semantic replacement was not proven in-session from available primary evidence
  - no gameplay bug was tied to this warning during Stage 9B.1
  - warning is documented for a later minimal compatibility cleanup rather than changed speculatively

### Remaining gaps after Stage 9B.1

#### Remaining gaps for Stage 9B.2

- historical note:
  - this list is superseded by the completed Stage 9B.2 report above
  - Stage 9B.2 outcomes are now:
    - portal / passenger / mounted ordering - `PASS`
    - vault shared-state behavior - `INCONCLUSIVE`
    - merchant / trade UI semantics - `PASS`
    - bundle mutation paths - `SEMANTIC DIFFERENCE`
    - waterlogging parity - `PASS`
    - projectile exact semantics - `SEMANTIC DIFFERENCE`
    - equipment / dispenser / offhand automation paths - `PASS`

#### Remaining gaps for Stage 9C

- feature-landmark natural-generation parity for:
  - `desert_well`
  - `monster_room`

#### Remaining non-claims

- no claim of full Fabric-to-Purpur parity yet
- feature-landmark generation-hook identity is still unresolved for Stage 9C
- no multiplayer stress or performance-benchmark claims are made from Stage 9B.1

Stage 9A live-player validation and closure is complete as of August 17, 2026:

- baseline live player session is now validated on real Purpur with a controllable vanilla client
- landmark-driven restriction transitions are now live-verified for village enter, exit, and re-entry
- nearby-entity progression is now live-verified for movement, unique-type semantics, entity removal, reconnect, and full restart
- synthetic advancement state is now live-verified across the full `LOCKED -> DEMYSTIFIED -> UNLOCKED -> RELOCKED` lifecycle
- demystified/relocked synthetic state is now confirmed to persist across reconnect and full server restart
- landmark restrictions are now live-confirmed to remain independent from ability unlock state
- the previously found structure-scan freeze is fixed by removing eager structure queries from the ordinary chunk refresh path
- the `sourceChunk` landmark ownership invariant is fixed and covered by regression tests
- Stage 9A closes the highest-value live validation gap from Stage 8D without expanding scope into Stage 9B or Stage 9C

This is still not a claim of full behavioral parity. The remaining gaps are now concentrated in dedicated boundary/reload/team validation work and in older feature-landmark parity gaps that belong to later stages.

Stage 9A.1 reproducible build tooling closure is complete as of August 18, 2026:

- standard Gradle Wrapper has been added to the port project
- wrapper is pinned to `Gradle 9.5.0`
- `gradlew.bat --version` succeeds in this workspace
- `gradlew.bat clean coreTest` succeeds in this workspace
- `gradlew.bat build` succeeds in this workspace
- reproducible build execution no longer depends on a preinstalled system `gradle`

## Stage 9A Closure Report

Stage 9A is complete as a live-player validation and closure pass and does not include Stage 9B work.

### Checklist audit

- `1. Baseline player session` - `PASS`
- `2. Landmark enter / exit / boundary / chunk-border` - `INCONCLUSIVE`
- `3. Representative RestrictionEngine ALLOW / DENY transitions` - `PASS`
- `4. Nearby Entity Progression` - `PASS`
  - movement in/out radius - `PASS`
  - unique-type semantics - `PASS`
  - entity removal - `PASS`
  - reconnect - `PASS`
  - full restart - `PASS`
- `5. Synthetic advancement lifecycle` - `PASS`
  - `LOCKED` - `PASS`
  - `DEMYSTIFIED` - `PASS`
  - `UNLOCKED` - `PASS`
  - `RELOCKED` - `PASS`
  - reconnect - `PASS`
  - full restart - `PASS`
- `6. Player reconnect/lifecycle state` - `PASS`
- `7. Team/scoreboard player-specific validation` - `NOT TESTED`
- `8. Performance sanity observations` - `PASS`

### What Stage 9A live validation proved

- `interact_inside_village` correctly denied in `SURVIVAL` inside a locked village landmark
- leaving the village restored `landmarkDecision kind=allow`
- returning into the same village restored `landmarkDecision kind=deny_landmark`
- denied chest interaction demystified `open_chest`
- raising `bac_advancements` to `42` unlocked `open_chest`
- lowering `bac_advancements` back to `41` re-locked `open_chest` while preserving demystification
- outside landmark coverage, an actually opened vanilla chest succeeded with `open_chest` unlocked
- inside locked landmark coverage, landmark denial remained active even when `open_chest` itself was unlocked
- `animal_kingdom` updated from movement out of range, from tracked-entity removal, after reconnect, and after a clean full restart
- duplicate nearby entities of the same tracked type did not inflate `animal_kingdom`, confirming unique-type semantics on live runtime

### Stage 9A-relevant fixes recorded in this port

- structure-scan freeze fix:
  - ordinary chunk refresh in `BukkitLandmarkRuntime` no longer forces eager `chunk.getStructures()` scanning
  - structure refresh is now limited to the dedicated structure event path
- `sourceChunk` invariant fix:
  - structure landmarks now retain the descriptor-provided source chunk instead of rebinding ownership to the caller chunk

### Stage 9A-relevant modified files in this port

- `src/main/java/com/diskree/achievetodo/landmark/bukkit/BukkitLandmarkRuntime.java`
- `src/main/java/com/diskree/achievetodo/landmark/StructureLandmarkDetector.java`
- `src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitNearbyEntityProgressService.java`
- `src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitBacapProgressionSource.java`
- `src/main/java/com/diskree/achievetodo/tracking/ProgressionCountResolver.java`
- `src/main/java/com/diskree/achievetodo/tracking/ProgressionStateSynchronizer.java`
- `src/main/java/com/diskree/achievetodo/progression/PlayerProgressionState.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionEngine.java`
- `src/main/java/com/diskree/achievetodo/adapters/BukkitRestrictionTargets.java`
- `src/main/java/com/diskree/achievetodo/ability/AbilityResolvers.java`
- `src/test/java/com/diskree/achievetodo/landmark/LandmarkEngineTest.java`
- `src/test/java/com/diskree/achievetodo/tracking/TrackingCoreTest.java`

### Regression tests to keep

- `LandmarkEngineTest.verifyFeatureOnlySyncPreservesStructureRegistration`
- `LandmarkEngineTest.run`
- `TrackingCoreTest.testNearbyEntityModel`
- `TrackingCoreTest.testProgressionSynchronization`
- `TrackingCoreTest.run`
- existing `RestrictionEngineTest` live-mirrored ability/landmark restriction cases

### Verification results in this workspace

- live Purpur + vanilla-client Stage 9A validation - `PASS`
- clean full restart validation - `PASS`
- historical Stage 9A note: `coreTest` via `gradlew.bat` initially failed because `gradlew.bat` was absent
- `coreTest` via system `gradle` - `FAIL` in this workspace because `gradle` is not installed
- historical Stage 9A note: `jar` via `gradlew.bat` initially failed because `gradlew.bat` was absent
- `jar` via system `gradle` - `FAIL` in this workspace because `gradle` is not installed
- Stage 9A.1 closure: `gradlew.bat --version` - `PASS` on pinned `Gradle 9.5.0`
- Stage 9A.1 closure: `gradlew.bat clean coreTest` - `PASS`
- Stage 9A.1 closure: `gradlew.bat build` - `PASS`
- existing jar artifact present - `build/libs/AchieveToDo-Purpur-26.2-0.1.5-SNAPSHOT.jar`
- Stage 9A.1 generated jar path - `build/libs/AchieveToDo-Purpur-26.2-0.1.5-SNAPSHOT.jar`
- direct `Stage2CoreTestMain` bootstrap from compiled classes remains environment-incomplete:
  - first missing dependency path hit `com.google.gson.JsonParser`
  - after adding cached `gson`, the next missing dependency path hit `net.kyori.adventure.key.Namespaced`
  - these failures indicate an incomplete manual classpath in this workspace, not a confirmed Stage 9A code regression

### Gaps closed in Stage 9A

- real live player/client validation is no longer absent
- nearby-entity runtime is no longer only indirectly validated
- synthetic advancement demystify/unlock/relock behavior is no longer only model-level
- reconnect persistence is now live-confirmed for nearby, landmark, progression, and demystified ability state
- full restart persistence is now live-confirmed for nearby, landmark, progression, and demystified ability state
- structure-scan freeze is recorded as fixed in the port status
- the `sourceChunk` ownership invariant fix is recorded together with its regression coverage

### Remaining gaps after Stage 9A

#### Still open before Stage 9A can claim nothing more

- dedicated landmark boundary-edge traversal remains unproven
- dedicated chunk-border landmark traversal remains unproven
- team/scoreboard player-specific validation remains untested

#### Remaining gaps for Stage 9B

- superseded by Stage 9B.1:
  - active-player `/reload` validation for landmark, nearby, and synthetic state continuity
  - team and scoreboard/player identity scenarios covered in Stage 9B.1 scope
  - dedicated boundary and chunk-border traversal cases with a live player

#### Remaining gaps for Stage 9C

- feature-landmark natural-generation parity for:
  - `desert_well`
  - `monster_room`
- any broader gameplay-parity audit beyond Stage 9A critical restriction/progression flows

## Stage

Stage 8D stabilization and remaining gap closure is complete as of August 17, 2026:

- server-independent `RestrictionEngine` exists in pure Java
- Purpur gameplay adapters are wired into the runtime
- landmark and restriction layers now compose through a shared decision model
- BACAP progression tracking runtime is now active in the normal Purpur startup path
- synthetic advancement unlock/relock and demystification are synchronized from runtime state
- embedded datapack metadata is validated against live Purpur `26.2`
- deprecated projectile listener registration has been removed from the runtime
- nearby-entity progression is no longer abstraction-only
- nearby progression data now flows through the existing `ProgressionSource` path
- landmark source-chunk ownership now matches the runtime chunk that discovered the landmark
- landmark overlap queries now resolve in deterministic order
- structure landmark bounds now prefer structure-piece unions over coarse fallback boxes
- live Purpur validation now includes strict BACAP objective disappearance/reappearance after startup, after `/reload`, and across full restart
- `/place structure minecraft:ruined_portal` is now confirmed to persist as a structure landmark on live Purpur
- Stage 8D audited remaining landmark and runtime gaps without introducing new large subsystems

This is still not a claim of full behavioral parity. The remaining gaps are now narrower and better classified: the largest open landmark issue is feature-landmark generation semantics for `desert_well` and `monster_room`, and the largest open validation issue is the absence of a controllable live player/client in this workspace.

## Stage 8D Stabilization Report

Stage 8D is complete as a stabilization pass and does not include Stage 9 work.

### Scope actually handled

- landmark engine remaining-gap audit
- live Purpur runtime validation
- nearby-entity runtime validation audit
- cheap rechecks of previously deferred validation items
- startup/runtime code-quality audit

### What was actually run

1. offline core verification from existing compiled artifacts
   - executed `com.diskree.achievetodo.tests.Stage2CoreTestMain` against:
     - `build/classes/java/main`
     - `build/classes/java/test`
     - `build/resources/main`
     - `build/resources/test`
     - `build/manual-libs/*.jar`
2. unsandboxed live Purpur startup
   - started `purpur-26.2-2620.jar` from:
     - `D:/ВАЙБКОД/AchieveToDo 26.2/test-server`
   - confirmed AchieveToDo startup and captured `logs/latest.log`
3. live BACAP objective transition checks
   - `scoreboard objectives setdisplay sidebar bac_advancements`
   - waited for runtime reconciliation
   - `scoreboard objectives setdisplay sidebar`
   - repeated the same objective reappearance check after `/reload`
4. live landmark persistence checks
   - `place structure minecraft:ruined_portal 15 70 15`
   - `save-all flush`
   - inspected:
     - `world/dimensions/minecraft/overworld/data/achievetodo-landmarks/overworld/*.landmarks`
5. live reload and restart checks
   - ran `/reload`
   - verified the server still accepted commands afterward
   - clean `stop`
   - full restart
   - verified persisted scoreboard/objective state and AchieveToDo startup again
   - clean `stop`

### Confirmed results

- offline verification:
  - `Stage 2 core, Stage 3 datapack, Stage 4 landmark, Stage 5 restriction-core, and Stage 6 tracking-core tests passed.`
- live Purpur startup:
  - AchieveToDo enabled successfully on real Purpur
  - embedded datapack registered successfully
  - `Loaded 1840 advancements` still appeared on live startup
  - no AchieveToDo bootstrap exception appeared
  - no AchieveToDo deprecated-listener warning appeared
- BACAP objective disappearance and reappearance:
  - missing-objective strict-runtime error still appears when no BACAP objective is displayed
  - after `setdisplay sidebar bac_advancements`, live runtime logged:
    - `Detected active BACAP objective: DEFAULT via bac_advancements in SIDEBAR`
  - after clearing the sidebar, live runtime returned to unavailable state and logged the missing-objective error again
  - the same objective-detection path still worked after `/reload`
  - the same objective-detection path still worked after a full restart because the sidebar objective persisted and was detected again on next startup
- landmark runtime:
  - command-placed `ruined_portal` is now confirmed to persist as a structure landmark on live Purpur
  - `14_1.landmarks` contains:
    - `record=ruined_portal|STRUCTURE|...|14|1|224|42|16|231|52|23`
  - previously validated natural-structure and multi-slice persistence evidence remained present
- reload/restart lifecycle:
  - `/reload` completed without breaking command handling
  - AchieveToDo continued to respond to scoreboard-objective changes after `/reload`
  - clean shutdown still logged:
    - `AchieveToDo Purpur landmark, BACAP tracking, and restriction runtimes disabled.`
  - full restart completed successfully and AchieveToDo re-enabled normally

### Gaps closed in Stage 8D

- `/place structure` is no longer an unproven landmark-runtime caveat for at least `minecraft:ruined_portal`
- scoreboard objective disappearance/reappearance is now validated on live Purpur
- scoreboard objective re-detection after `/reload` is now validated on live Purpur
- scoreboard/objective state surviving full restart is now validated on live Purpur
- startup warning audit did not reveal new AchieveToDo-specific deprecation noise

### Remaining gaps after Stage 8D

#### Implementation gaps

- feature-landmark parity remains incomplete for:
  - `desert_well`
  - `monster_room`
- the current Purpur implementation detects those feature landmarks by chunk block-pattern scan, not by generation-time worldgen hooks
- because of that, natural-generation-only semantics are not proven and may be wider than Fabric semantics for:
  - hand-built replicas
  - `/place feature`
  - other non-natural block arrangements that match the detector

#### Validation gaps

- Landmark runtime still lacks live player validation for:
  - entering a locked landmark
  - leaving a locked landmark
  - standing on a landmark boundary
  - crossing a chunk border while restrictions depend on landmark context
  - overlap primary selection as experienced by an actual player
- Nearby Entity Progression still lacks live runtime validation for:
  - real nearby-entity counter updates while a player moves
  - decrement behavior after entity removal
  - reconnect/resync behavior
  - reload behavior with active players
- old deferred validation gaps still remain open:
  - synthetic advancement timing under real player actions
  - portal passenger ordering
  - vault shared-state behavior
  - merchant UI restriction behavior

#### Environment limits during Stage 8D

- player-driven live validation was not completed because this workspace had no controllable Minecraft client or bot available
- initial sandboxed Purpur startup could not reach online state because Netty selector loopback creation was blocked by the sandbox
- unsandboxed startup resolved that environment issue, so server-side live validation was completed, but this did not create a substitute for a real player session

#### Semantic differences

- progression config selection remains file-backed rather than world-data backed
- synthetic advancement synchronization remains Bukkit-API based
- `desert_well` and `monster_room` remain approximation-based in Purpur because Fabric uses generation-time hooks plus chunk serialization for feature landmarks
- exact natural-generation-only parity for feature landmarks would require a separate stage with a different integration strategy; it was not expanded inside Stage 8D

## Stage 8C Live Validation Report

Stage 8C is complete as a dedicated validation pass and does not include Stage 9 work.

### Previously implemented scope being validated

- Stage 8A:
  - nearby-entity progression runtime wiring
  - dirty-player reconciliation path integration
  - `ProgressionSource` nearby count exposure
- Stage 8B:
  - landmark source-chunk ownership hardening
  - deterministic overlap ordering
  - structure-piece union bounds
  - persisted intersecting-slice reload behavior

### Runtime environment

- validation date:
  - August 17, 2026
- OS:
  - Windows 11 `10.0` amd64
- Java:
  - Temurin OpenJDK `25.0.4+7-LTS`
- Purpur:
  - `26.2-2620-HEAD@8762b2f`
- Minecraft:
  - `26.2`
- live server path:
  - `D:/ВАЙБКОД/AchieveToDo 26.2/test-server`

### What was actually run

1. fresh plugin build and deploy
   - built `AchieveToDo-Purpur-26.2-0.1.5-SNAPSHOT.jar`
   - verified the plugin JAR in `test-server/plugins/` was stale by SHA-256 before deploy
   - copied the fresh JAR into `test-server/plugins/`
2. first live startup
   - started `purpur-26.2-2620.jar`
   - captured full startup log in `test-server/logs/latest.log`
3. BACAP objective transition validation
   - `scoreboard objectives add bac_advancements dummy`
   - `scoreboard objectives setdisplay sidebar bac_advancements`
   - `scoreboard objectives setdisplay sidebar`
4. landmark runtime validation via live server commands
   - `forceload add 0 0`
   - `place structure minecraft:ruined_portal 15 70 15`
   - `place structure minecraft:shipwreck_beached 15 70 15`
   - clean shutdown
   - inspected persisted `.landmarks` files under:
     - `world/dimensions/minecraft/overworld/data/achievetodo-landmarks/overworld/`
5. restart validation
   - started the same Purpur server again
   - confirmed restart with `111 persistent chunks` in overworld
   - `locate structure minecraft:ruined_portal`
   - `forceload add 32 -384`
   - clean shutdown
   - inspected startup/shutdown log and persisted landmark files again

### Confirmed live results

- plugin startup validation:
  - `AchieveToDo` loaded successfully on real Purpur
  - embedded datapack registered successfully
  - `Loaded 1840 advancements` appeared in live startup
  - no pack metadata error appeared
  - no deprecated projectile/listener warning from AchieveToDo appeared
  - no bootstrap exception from AchieveToDo appeared
- BACAP objective disappearance and reappearance:
  - startup without BACAP objective produced the expected strict-runtime error log
  - after `setdisplay sidebar bac_advancements`, live runtime logged:
    - `Detected active BACAP objective: DEFAULT via bac_advancements in SIDEBAR`
  - after clearing the display slot, runtime returned to unavailable state and logged the missing-objective error again
- landmark persistence and chunk lifecycle:
  - persisted landmark files were written under the actual world folder path used by the runtime:
    - `world/dimensions/minecraft/overworld/data/achievetodo-landmarks/overworld/`
  - natural structure detection persisted correctly on live runtime
    - `2_-24.landmarks` contains:
      - `record=ruined_portal|STRUCTURE|...|2|-24|32|-27|-384|36|-19|-380`
  - multi-chunk persisted slices exist on live runtime
    - example:
      - `0_8.landmarks` contains `trial_chambers` with `sourceChunk=6,14`
    - multiple persisted slice files also contain the same `sourceChunk`, confirming real intersecting-slice persistence rather than unit-test-only coverage
- restart validation:
  - first shutdown completed cleanly
  - second startup completed cleanly
  - datapack registration repeated successfully on restart
  - previously persisted landmark data remained present across restart

### Problems found

1. command-placed structures were not proven to register as landmark records in this Stage 8C run
   - reproduction:
     - `place structure minecraft:ruined_portal 15 70 15`
     - `place structure minecraft:shipwreck_beached 15 70 15`
     - inspect persisted `.landmarks` files afterward
   - result:
     - this run did not produce a confirmed persisted record for those command-placed test structures
   - reproducibility:
     - reproduced in this validation run
   - severity:
     - low for normal survival gameplay
   - status:
     - historical Stage 8C validation caveat only
     - superseded later in Stage 8D, where `/place structure minecraft:ruined_portal` was confirmed to persist as a live structure landmark record

2. initial manual persistence inspection looked in the wrong world path
   - reproduction:
     - checking `world/data/achievetodo-landmarks/...` returned no files
   - result:
     - actual runtime path is under `world/dimensions/minecraft/overworld/data/achievetodo-landmarks/...`
   - reproducibility:
     - deterministic
   - severity:
     - none for runtime, documentation only
   - status:
     - corrected during validation

### Residual gaps

#### Implementation gaps

- feature-landmark parity is still not fully proven for:
  - `desert_well`
  - `monster_room`
- no new implementation defects were confirmed in Stage 8C for the validated natural-structure persistence path

#### Validation gaps

- Landmark runtime not fully validated with a live player for:
  - inside-landmark restriction context
  - outside-landmark restriction context
  - chunk-to-chunk movement
  - crossing a live landmark boundary
  - primary landmark selection as experienced by a player in overlapping landmarks
- Nearby Entity Progression not fully validated with a live player for:
  - scanner updates during movement
  - dirty-player flow after nearby-entity state changes
  - progression source reads under active player movement
  - empty scoreboard plus later player sync
  - resync after reconnect
- old validation gaps still open in Stage 8C:
  - synthetic advancement sync timing
  - reconnect behavior
  - portal passenger handling
  - vault shared-state behavior
  - merchant UI restriction behavior

#### Semantic differences

- progression config selection remains file-backed rather than world-data backed
- synthetic advancement synchronization remains Bukkit-API based
- `/place structure` behavior is not yet proven equivalent to naturally generated structure metadata from the landmark runtime perspective

## Live Runtime Validation

### First real Purpur startup

Validated on August 17, 2026 with:

- Java `25.0.4` Temurin
- Purpur `26.2-2620`
- Minecraft `26.2`

Confirmed on live runtime:

- plugin loaded successfully
- Paper bootstrap worked
- embedded datapack was registered
- advancement count increased from `1688` to `1840`
- restriction runtime started
- landmark runtime started
- BACAP tracking runtime started

### Stage 7.1 runtime hardening

Stage 7.1 is complete.

Issues discovered during the first live startup and now closed:

1. Embedded datapack metadata warning
   - original live warning:
     - `Error reading pack metadata, attempting fallback type`
     - `Pack declares support for version newer than 81, but is missing mandatory fields min_format and max_format`
   - cause:
     - generated `pack.mcmeta` still used legacy `pack_format`
     - real `26.2` server metadata expects `max_format: 107` and `min_format: [107, 1]`
   - status:
     - fixed in generator and materialized resource
     - revalidated on live startup on August 17, 2026
     - fallback warning no longer appears in `test-server/logs/latest.log`

2. Deprecated projectile integration warning
   - original live warning:
     - `ProjectileCollideEvent ... is Deprecated`
   - cause:
     - `ProjectileAdapter` still registered a listener for deprecated `ProjectileCollideEvent`
   - status:
     - replaced with `ProjectileHitEvent`-only restriction handling
     - live startup on August 17, 2026 no longer reports the deprecated listener warning

### Current live-startup residuals

- upstream JVM warning still appears from bundled `org.joml` use of `sun.misc.Unsafe`
  - this is not emitted by AchieveToDo code
- AchieveToDo logs an error when no BACAP objective exists in `SIDEBAR`, `LIST`, or `BELOW_NAME`
  - this is expected Stage 6 strict runtime behavior, not a startup failure
  - progression remains unavailable until a BACAP objective appears

### Stage 8A nearby entity runtime

Stage 8A is complete.

Nearby-entity tracking is now implemented in the Bukkit runtime through the existing progression architecture:

- `BukkitNearbyEntityScanner`
  - performs presence-based nearby scans for online players
  - preserves original radius and baby/adult semantics
- `BukkitNearbyEntityProgressService`
  - owns runtime snapshots
  - runs a fast scan pass every `20` ticks
  - marks players dirty when nearby progress changes
  - logs timing data only when scan cost crosses the warning threshold
- `BukkitBacapProgressionSource`
  - now exposes nearby progress through `ProgressionSource`
  - does not become a second storage layer for nearby state
- `BukkitProgressionRuntime`
  - seeds nearby snapshots on initial sync
  - keeps using the existing dirty-player reconciliation path

Stage 8A closed the largest remaining progression implementation gap from Stage 7.1:

- nearby-entity progression is no longer abstraction-only
- no separate progression subsystem was introduced
- no NMS was introduced

### Stage 8B landmark parity hardening

Stage 8B is complete.

Landmark runtime parity was hardened without changing the existing landmark/restriction architecture:

- `StructureLandmarkDetector`
  - now preserves the chunk that actually reported the structure
  - now prefers a union of structure piece boxes when available
  - falls back to the coarse structure bounding box only when piece data is unavailable
- `FeatureLandmarkDetector`
  - now assigns `sourceChunk` from the scanning chunk instead of `blockBox.minX/minZ`
  - this keeps border-spanning `desert_well` and `monster_room` records stable across refresh and reload paths
- `LandmarkIndex`
  - now returns deterministic overlap/query ordering instead of depending on load order
- landmark tests
  - now cover cross-border source-chunk ownership for feature landmarks
  - now cover structure-piece union precedence
  - now cover persisted intersecting-slice reload behavior
  - now cover deterministic overlap primary selection at restriction level

Stage 8B closed the largest remaining implementation gaps in the landmark runtime:

- chunk-border source ownership is no longer derived from `blockBox` minimum coordinates
- overlap handling no longer depends on chunk load order for primary landmark selection
- unload/reload persistence confidence is strengthened with explicit reload coverage
- exact structure bounds are tighter when Paper piece bounds are available

## Workspace Source Note

Two files requested by the Stage 5 brief were not present in this workspace on August 16, 2026:

- `research/PORT_AUDIT.md`
- `LANDMARK_IMPLEMENTATION.md`

Stage 5 was implemented against:

- current ported source tree
- previous `PORT_STATUS.md`
- `./original`

## Completed Scope

### Stages 2-4 retained

- pure Java ability model
- pure Java progression model
- embedded synthetic ability datapack
- Landmark Engine core
- Bukkit/Purpur landmark runtime and persistence wiring

### Stage 5A restriction core

- `RestrictionDecisionKind` formalized:
  - `ALLOW`
  - `DENY_ABILITY`
  - `DENY_LANDMARK`
  - `DENY_BOTH`
- pure Java `RestrictionDecision`
- pure Java `RestrictionContext`
- pure Java actor, hand, target, automation, and flag domain types
- `RestrictionEngine.checkAbility(...)`
- `RestrictionEngine.checkLandmark(...)`
- `RestrictionEngine.check(...)`
- preserved ability semantics:
  - creative bypass
  - spectator bypass
  - initially unlocked
  - permanently locked
  - demystified is not unlocked
  - positive-threshold unlock
- landmark lock separate from ability lock

### Stage 5B Purpur adapters currently wired

- `BlockInteractionAdapter`
- `BlockBreakAdapter`
- `ItemUseAdapter`
- `EquipmentAdapter`
- `BundleAdapter`
- `AttachedEntityAdapter`
- `EntityInteractionAdapter`
- `TradeAdapter`
- `VehicleAdapter`
- `ProjectileAdapter`
- `PortalAdapter`
- `MovementAdapter`
- `FluidAdapter`
- `SleepAdapter`
- `VaultAdapter`

### Stage 5B runtime composition

Runtime path now follows:

```text
Purpur Event
-> Bukkit/Purpur adapter
-> RestrictionEngine
-> Ability state lookup + LandmarkEngine
-> allow / deny
```

`RestrictionEngine` itself remains server-independent.

### Stage 6 progression tracking core

- pure Java BACAP objective detection:
  - sidebar priority over list
  - list priority over below-name
  - mode lookup by objective name
- pure Java score/stat tracking model:
  - `TrackedScoreType`
  - `TrackedStatisticsDataType`
  - `TrackedNearbyEntitiesType`
- pure Java progression count resolution for:
  - personal BACAP objectives
  - scoreboard team BACAP objectives
- pure Java progression transition diffing:
  - unlock transitions
  - relock transitions
  - unavailable / not-ready progression state
- nearby-entity semantics were analyzed and preserved as data plus abstraction.
  - Stage 6 originally stopped at `NearbyEntityProgressProvider`.
  - Stage 8A adds the live Bukkit scanner/runtime path on top of that abstraction.

### Stage 6 runtime wiring

- `BukkitProgressionRuntime` is started from `AchieveToDoPlugin`
- `BukkitRestrictionRuntime` now consumes the real progression runtime instead of the permissive lookup
- BACAP objective readiness is checked against scoreboard display slots:
  - `SIDEBAR`
  - `PLAYER_LIST`
  - `BELOW_NAME`
- if no BACAP objective is present:
  - the progression runtime is marked unavailable
  - joins are denied until an objective appears
  - ability state resolves as locked, matching the original server's strict behavior more closely than the Stage 5 permissive placeholder
- synthetic advancement state is synchronized through Bukkit advancement APIs:
  - unlock grants remaining criteria
  - relock revokes only the unlocked criterion
  - demystification is granted on deny feedback path
- progression config now has a file-backed runtime backend:
  - world selection persisted in `plugins/AchieveToDo/progression/world-selection.properties`
  - generated/resolved progression configs persisted under `plugins/AchieveToDo/progression/configs/`
  - missing selection defaults to `normal`

### Stage 8A nearby runtime wiring

- `NearbyEntityProgressCalculator` provides pure Java presence-based counting
- `BukkitNearbyEntityScanner` converts nearby Bukkit entities into normalized observations
- `BukkitNearbyEntityProgressService` stores per-player nearby progress snapshots
- fast nearby scans run every `20` ticks
- dirty players are fed back into the existing reconciliation loop instead of a parallel progression pipeline
- full reconciliation remains the existing `20` tick runtime loop with a complete pass every `5` iterations
- nearby progress is exposed through `ProgressionSource.getNearbyEntityProgress(...)`

## Key Files Added Or Updated For Stages 5-8A

### Main

- `src/main/java/com/diskree/achievetodo/restriction/RestrictionActionKind.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionActorMode.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionAutomationSource.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionContext.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionDecision.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionDecisionKind.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionEngine.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionFlag.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionHand.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionTarget.java`
- `src/main/java/com/diskree/achievetodo/restriction/RestrictionTargetKind.java`
- `src/main/java/com/diskree/achievetodo/progression/DemystificationSink.java`
- `src/main/java/com/diskree/achievetodo/restriction/bukkit/BukkitRestrictionRuntime.java`
- `src/main/java/com/diskree/achievetodo/adapters/BukkitRestrictionResolver.java`
- `src/main/java/com/diskree/achievetodo/adapters/BukkitRestrictionService.java`
- `src/main/java/com/diskree/achievetodo/adapters/BlockInteractionAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/BlockBreakAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/ItemUseAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/EquipmentAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/BundleAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/AttachedEntityAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/EntityInteractionAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/TradeAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/VehicleAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/ProjectileAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/PortalAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/MovementAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/FluidAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/SleepAdapter.java`
- `src/main/java/com/diskree/achievetodo/adapters/VaultAdapter.java`
- `src/main/java/com/diskree/achievetodo/landmark/FeatureLandmarkDetector.java`
- `src/main/java/com/diskree/achievetodo/landmark/LandmarkIndex.java`
- `src/main/java/com/diskree/achievetodo/landmark/StructureLandmarkDescriptor.java`
- `src/main/java/com/diskree/achievetodo/landmark/StructureLandmarkDetector.java`
- `src/main/java/com/diskree/achievetodo/landmark/bukkit/BukkitLandmarkRuntime.java`
- `src/main/java/com/diskree/achievetodo/tracking/AdvancementsMode.java`
- `src/main/java/com/diskree/achievetodo/tracking/BacapObjectiveDetector.java`
- `src/main/java/com/diskree/achievetodo/tracking/DetectedAdvancementsObjective.java`
- `src/main/java/com/diskree/achievetodo/tracking/NearbyEntityProgressProvider.java`
- `src/main/java/com/diskree/achievetodo/tracking/NearbyEntityProgressCalculator.java`
- `src/main/java/com/diskree/achievetodo/tracking/PlayerProgressionUpdate.java`
- `src/main/java/com/diskree/achievetodo/tracking/ProgressionCountResolver.java`
- `src/main/java/com/diskree/achievetodo/tracking/ProgressionSource.java`
- `src/main/java/com/diskree/achievetodo/tracking/ProgressionStateSynchronizer.java`
- `src/main/java/com/diskree/achievetodo/tracking/TrackedNearbyEntitiesType.java`
- `src/main/java/com/diskree/achievetodo/tracking/TrackedScoreType.java`
- `src/main/java/com/diskree/achievetodo/tracking/TrackedStatisticCategory.java`
- `src/main/java/com/diskree/achievetodo/tracking/TrackedStatisticKey.java`
- `src/main/java/com/diskree/achievetodo/tracking/TrackedStatisticsDataType.java`
- `src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitBacapProgressionSource.java`
- `src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitNearbyEntityProgressService.java`
- `src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitNearbyEntityScanner.java`
- `src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitProgressionConfigRepository.java`
- `src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitProgressionRuntime.java`
- `src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitSyntheticAdvancementSynchronizer.java`
- `src/main/java/com/diskree/achievetodo/tracking/bukkit/BukkitWorldProgressionConfigStore.java`
- `src/main/java/com/diskree/achievetodo/core/AchieveToDoPlugin.java`

### Tests

- `src/test/java/com/diskree/achievetodo/landmark/FeatureLandmarkDetectorTest.java`
- `src/test/java/com/diskree/achievetodo/landmark/LandmarkEngineTest.java`
- `src/test/java/com/diskree/achievetodo/landmark/StructureLandmarkDetectorTest.java`
- `src/test/java/com/diskree/achievetodo/restriction/RestrictionEngineTest.java`
- `src/test/java/com/diskree/achievetodo/adapters/BukkitRestrictionResolverTest.java`
- `src/test/java/com/diskree/achievetodo/tracking/TrackingCoreTest.java`

### Docs

- `RESTRICTION_ENGINE.md`
- `RESTRICTION_BYPASS_MATRIX.md`
- `PURPUR_INTEGRATION_TEST_PLAN.md`
- `PORT_STATUS.md`

## Verification Performed

### Offline compile and test loop

On August 17, 2026 the self-contained compile and test runner passed after the Stage 6 runtime integration:

```text
Stage 2 core, Stage 3 datapack, Stage 4 landmark, Stage 5 restriction-core, and Stage 6 tracking-core tests passed.
```

Verified in the loop:

- restriction decision model
- ability and landmark merge semantics
- resolver mappings for block, item, trade, vehicle, and portal paths
- adapter sources compile against Purpur/Paper 1.21.8 API surface
- BACAP objective detection priority
- personal versus team scoreboard progression counts
- tracked score fixups including `ON_A_RAIL` and `LOSER`
- tracked statistics normalization
- nearby-entity enum semantics and doubled baby-separated counts
- nearby-entity runtime counting semantics for:
  - unique-type presence
  - adult/baby split
  - per-type deduplication
- unavailable progression state and unlock/relock transition diffs

### Build artifacts

On August 17, 2026:

- `coreTest` passed
- `build` produced the plugin jar
- runtime classes compiled successfully with the Stage 6 tracking path enabled
- Stage 7.1 live-hardening jar started successfully on real Purpur after rebuild and redeploy
- Stage 8A nearby runtime integration compiled and packaged successfully
- Stage 8B landmark parity hardening compiled and packaged successfully

### Landmark scan instrumentation

Added lightweight timing logs in `BukkitLandmarkRuntime` for:

- feature scan duration per chunk
- full landmark refresh duration per chunk

This is measurement support, not proof of acceptable live-runtime performance.

## Known Gaps And Non-Claims

### Open Stage 4 landmark gaps carried forward

The following historical landmark gaps are now superseded by later live stages and Stage 9C:

1. real vanilla `desert_well` detection
   - superseded by Stage 9C classification:
   - natural-generation evidence remained `INCONCLUSIVE`, but the final accepted semantic-difference decision is now documented
2. real vanilla `monster_room` detection
   - superseded by Stage 9C classification:
   - natural-generation evidence remained `NOT TESTED`, but the final accepted semantic-difference decision is now documented
3. feature scanning performance cost
   - still open, but now explicitly deferred to Stage 11 performance work rather than treated as a current Stage 9C blocker
4. historical note: live proof for chunk-border landmark behavior on Purpur was required before Stage 9B.1 and is now superseded by Stage 9B.1 live validation
5. historical note: live proof for active-player reload continuity was required before Stage 9B.1 and is now superseded by Stage 9B.1 live validation

### Stage 6 semantic gaps or validation requirements

- Nearby-entity tracking live status:
  - movement in and out of radius - validated in Stage 9A
  - reconnect and restart behavior - validated in Stage 9A
  - active-player reload continuity - validated in Stage 9B.1
  - teleport/world-change timing and spectator behavior - validated in Stage 9B.2
  - dense-entity performance behavior remains deferred to Stage 11
- Team-mode behavior live status:
  - team assignment, reassignment, removal, reconnect, and personal-vs-team identity - validated in Stage 9B.1
  - exact scoreboard update timing relative to underlying Bukkit events is still not claimed beyond observed reconciliation behavior
- Objective disappearance/reappearance recovery live status:
  - validated on live runtime in Stage 8D and carried through later live stages
- Portal passenger and mounted-entity handling is now live-validated in Stage 9B.2.
- Bundle mutation parity is partially validated in Stage 9B.2.
  - the tested insert-and-close denial path is live-verified
  - exact non-insert bundle mutation parity remains a documented semantic difference
- Waterlogging parity is live-validated for the tested placement and drain scenarios in Stage 9B.2.
- Vault shared-state semantics are partially validated in Stage 9B.2.
  - the single-real-player connected-state path and shared-state spam fix are live-verified
  - exact multi-player nearby-participation behavior is still not claimed without a second real client
- Trade UI invisibility vs cancellation semantics are live-validated in Stage 9B.2 for suppressed merchant opening on real client screens.
- Progression config selection is file-backed rather than world-data backed.
  - semantic contract is preserved
  - physical persistence backend differs from the original mod runtime
- Synthetic advancement sync uses Bukkit advancement APIs.
  - no NMS was introduced
  - reconnect, restart, reload, and Stage 9B.2 gameplay-path timing did not expose a new criterion-timing defect
- BACAP objective absence currently logs at error level during startup.
  - runtime behavior is intentional
  - log severity may still need tuning in a later hardening pass

### Remaining real gaps grouped by next-stage relevance

Highest-priority implementation gaps:

1. no additional Stage 9C feature-landmark implementation block remains
2. exact original feature-generation provenance for `desert_well` / `monster_room` remains a documented supported-API semantic difference rather than an active implementation task
3. Stage 10 UI reconstruction is now the next planned feature stage

Highest-priority live-validation gaps:

1. no additional Stage 9B gameplay-path live-validation block remains after Stage 9B.2
2. no additional Stage 9C live-validation block remains after the accepted semantic-difference decision
3. dense-entity nearby-scan behavior is deferred to Stage 11 as a performance concern
4. exact multi-player vault shared-state observation remains unclaimed without a second real client

Known backend or semantic differences still accepted for now:

1. progression config selection is file-backed instead of world-data backed
2. synthetic advancement synchronization uses Bukkit APIs instead of mod/NMS hooks
3. `desert_well` and `monster_room` feature landmarks remain supported-API approximations:
   - exact original generation provenance is unavailable without unsupported internals or NMS
   - exact original generated-write bounding boxes are unavailable
   - first-scan compatibility on legacy chunks can still misclassify pre-existing replicas
   - Stage 9C now prevents later rescans from promoting new replicas in already scanned chunks
4. bundle, waterlogging, vault, and merchant handling are implemented through Bukkit-visible event paths rather than lower-level original hooks
5. projectile landmark-hit parity remains post-hit rather than original pre-hit `canHit(...)`

## NMS Status

- No NMS is used in Stage 5 or Stage 6.
- No NMS was introduced into progression, tracking, restriction, or landmark core classes.

## Repository Safety

- `./original` was not modified.
