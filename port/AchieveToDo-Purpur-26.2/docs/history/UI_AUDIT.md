# UI Audit

Stage: `10A - Original UI Audit & Visual Contract`

Scope of this document:

- audit original Fabric UI surfaces READ ONLY
- identify entry points, states, assets, networking, and rendering dependencies
- map original UI/UX to vanilla-compatible reconstruction options
- recommend a Stage 10B direction without implementing production UI

Out of scope:

- no production GUI classes
- no resource-pack assets
- no runtime/server implementation
- no changes to gameplay semantics

## 1. Original UI Source Map

### 1.1 Main client-state source

- `original/src/main/java/com/diskree/achievetodo/client/AchieveToDoClient.java`
  - client-side authoritative UI cache in the original mod
  - stores:
    - abilities configuration
    - obtained advancements count
    - tracked score/stat progress
    - locked landmark boxes
    - derived ability row layout
  - owns:
    - actionbar lock feedback
    - client-side demystify packet send
    - nearby-entity count calculation for UI progress

### 1.2 Advancements UI integration

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementsScreenMixin.java`
  - injects locked placeholder tabs
  - removes placeholder when real tab unlocks
  - forces `ABILITIES` tab open by default
  - blocks clicking locked tabs
  - renders locked-tab mask sprite

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementTabMixin.java`
  - remaps advancement roots to custom tab positions
  - restructures ability advancement graph into custom rows
  - special-cases the `ABILITIES` tab layout

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementWidgetMixin.java`
  - resolves whether a widget is:
    - tracked score
    - tracked nearby entity target
    - tracked statistics target
    - ability
  - overrides progress widths/fractions
  - adds special description lines for initially unlocked / permanently locked
  - renders mystified mask over unknown locked abilities
  - hides icon and tooltip for mystified abilities

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementProgressMixin.java`
  - overrides obtained count / required count / percentage / fraction text
  - drives live progress display for:
    - abilities
    - tracked score objectives
    - tracked statistics
    - nearby entities

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/ClientAdvancementManagerMixin.java`
  - tags client advancement progress objects with their advancement id
  - enables type-specific UI overrides above

### 1.3 Notification and tutorial UI

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/AdvancementToastMixin.java`
  - custom background for ability unlock toasts
  - custom title by unlock category
  - custom title/subtitle colors
  - custom sound

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/MovementTutorialStepHandlerMixin.java`
  - delays tutorial progression until advancements were opened
  - shows custom tutorial toast: "Open advancements"
  - shortens vanilla toast timing

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/MinecraftClientMixin.java`
  - blocks opening advancements when client is not ready
  - blocks opening inventory when `OPEN_INVENTORY` is locked
  - marks the advancements tutorial as completed when the screen opens

### 1.4 World creation and datapack-helper UI

- `original/src/main/java/com/diskree/achievetodo/client/gui/WorldCreationTab.java`
  - custom tab embedded into vanilla `CreateWorldScreen`
  - progression config selector
  - cooperative mode toggle
  - rewards section
  - custom generation section
  - custom background framing

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/CreateWorldScreenMixin.java`
  - inserts the AchieveToDo tab into world creation
  - persists/reloads AchieveToDo world options
  - intercepts world creation to require/copy/enable datapacks
  - reroutes to downloader screen when required external packs are absent

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/WorldCreatorMixin.java`
  - stores tab state inside `WorldCreator`

- `original/src/main/java/com/diskree/achievetodo/client/gui/ExternalPackDownloader.java`
  - confirm-style download/select-file screen for missing external datapacks

- `original/src/main/java/com/diskree/achievetodo/client/gui/ErrorScreen.java`
  - notice screen for integrity and file errors

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/WorldListWidgetMixin.java`
  - intercepts "Play" on a world
  - validates datapack integrity
  - can redirect to downloader/error screens

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/PackScreenMixin.java`
  - hides AchieveToDo-managed internal/external packs from vanilla pack list

### 1.5 Other UI-adjacent client surfaces

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/DeathScreenMixin.java`
  - in hardcore, replaces score line with obtained advancements count

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/GameRendererMixin.java`
  - renders fullscreen black overlay while `VISION` is locked

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/client/WorldRendererMixin.java`
  - renders animated in-world forcefield-like landmark border while inside a locked landmark

- `original/src/main/java/com/diskree/achievetodo/injection/mixin/main/CommandManagerMixin.java`
  - if the user tries `/advancementssearch ...` without the companion mod, sends a clickable install suggestion

## 2. UI Entry Points

### EP-01 Vanilla advancements key

- trigger:
  - vanilla `advancements` keybinding
- side:
  - client
- path:
  - `MinecraftClientMixin#setScreenInject`
- result:
  - opens modified vanilla `AdvancementsScreen`
- unavailable state:
  - if AchieveToDo client cache is not ready, screen does not open
  - player gets red actionbar/message: `achievetodo.error.not_ready_yet`

### EP-02 Tutorial prompt to open advancements

- trigger:
  - early vanilla movement tutorial progression
- side:
  - client
- path:
  - `MovementTutorialStepHandlerMixin`
- result:
  - custom tutorial toast appears telling the player to open advancements
- completion:
  - toast is dismissed once the advancements screen opens

### EP-03 Unlock chat message click-through

- trigger:
  - ability unlock chat messages generated by data/resources
- side:
  - client chat interaction
- result:
  - click runs `/advancementssearch highlight ...`
- note:
  - this is not a built-in AchieveToDo screen opener
  - it assumes optional companion mod support

### EP-04 Create world flow

- trigger:
  - open vanilla `CreateWorldScreen`
- side:
  - client
- result:
  - AchieveToDo tab appears inside world creation

### EP-05 Existing world launch integrity flow

- trigger:
  - select and play an existing world
- side:
  - client
- result:
  - if external datapacks are missing/outdated:
    - downloader screen opens
    - or error notice opens

### EP-06 Automatic notifications

- trigger:
  - ability unlock advancement completion
- side:
  - client
- result:
  - modified advancement toast

### EP-07 Passive world overlays

- trigger:
  - being inside locked landmark
  - having `VISION` locked
- side:
  - client render
- result:
  - border overlay
  - fullscreen blackout overlay

There is no original custom command or custom keybind that directly opens a dedicated AchieveToDo standalone screen.

## 3. Screen Inventory

The original mod uses a small number of screens, but several of them are deep vanilla-screen modifications rather than new top-level classes.

### UI-01 Modified Advancements Screen

- original class surface:
  - vanilla `AdvancementsScreen`
  - modified by:
    - `AdvancementsScreenMixin`
    - `AdvancementTabMixin`
    - `AdvancementWidgetMixin`
    - `AdvancementProgressMixin`
    - `ClientAdvancementManagerMixin`
- purpose:
  - main AchieveToDo player UI
  - browse abilities and progression categories
  - surface hidden vs revealed vs unlocked state
  - show live counts and requirements
- title:
  - vanilla advancements screen title
- navigation:
  - vanilla tab navigation with AchieveToDo-managed tabs
- logical structure:
  - left tabs:
    - `ABILITIES`
    - `BACAP`
    - `STATISTICS`
  - top tabs:
    - `BUILDING`
    - `FARMING`
    - `HUSBANDRY`
    - `BIOMES`
    - `ADVENTURE`
    - `MONSTERS`
    - `WEAPONRY`
  - bottom tabs:
    - `MINING`
    - `REDSTONE`
    - `ENCHANTING`
    - `NETHER`
    - `POTION`
    - `END`
    - `CHALLENGES`
- default-open behavior:
  - `ABILITIES` tab only
- locked-tab behavior:
  - hidden real tab replaced by placeholder root
  - placeholder has no icon
  - placeholder is visible but not clickable
  - placeholder uses gray italic unlock-hint tooltip
  - placeholder renders a custom 16x16 mask sprite over the tab icon area
- `ABILITIES` tab layout:
  - not vanilla tree layout
  - rows are rebuilt from `AbilityType` hierarchy and required counts
  - categories:
    - `MAIN`
    - `ACTIONS`
    - `BLOCKS`
    - `UPGRADE`
    - `FOOD`
    - `TRADING`
    - `LANDMARK`
  - row counts:
    - `MAIN`: 1
    - `ACTIONS`: 2
    - `BLOCKS`: 2
    - `UPGRADE`: 2
    - `FOOD`: 4
    - `TRADING`: 2
    - `LANDMARK`: 2
- widget states:
  - unknown locked ability:
    - icon hidden
    - 26x26 mystified mask rendered
    - tooltip suppressed
  - demystified locked ability:
    - icon visible
    - tooltip visible
    - progress visible
  - unlocked ability:
    - vanilla-complete advancement state
  - initially unlocked ability:
    - special italic gray note appended to description
    - progress width forced to `0`
    - fraction text suppressed
  - permanently locked ability:
    - special italic red note appended to description
    - progress width forced to `0`
    - fraction text suppressed
- progress data:
  - abilities:
    - required count from synchronized config
    - current count from synchronized obtained-advancements count
  - scoreboard/stat/nearby widgets:
    - required count from tracked type metadata
    - current count from synchronized score/stat payloads or local nearby scan
- close/back behavior:
  - vanilla close behavior
- keyboard:
  - vanilla advancements key opens it
  - vanilla close key closes it
- mouse:
  - vanilla tab click/hover, except locked tabs cannot be clicked
- client-only dependencies:
  - Fabric mixins
  - custom networking payload cache
  - custom rendering hooks

### UI-02 World Creation Tab

- original class:
  - `WorldCreationTab`
  - inserted into vanilla `CreateWorldScreen`
- purpose:
  - configure progression mode
  - cooperative mode
  - reward packs
  - optional custom world-generation packs
- title:
  - literal `AchieveToDo`
- layout:
  - tab embedded into vanilla tab navigation
  - top row:
    - progression config selector
    - cooperative mode toggle
  - section 1:
    - `Rewards`
    - Items / Experience / Trophies toggles
  - section 2:
    - `Custom generation`
    - Overworld / Nether / End toggles
- visual structure:
  - uses vanilla list background texture as framed panel interior
  - uses vanilla header/footer separator textures
  - yellow section titles
- interactions:
  - cycling selector for built-in/custom config
  - toggles with tooltips
- data dependencies:
  - local config directory scan for custom `.toml` configs
  - `WorldCreatorExtension`
- note for Purpur port:
  - this is original UI, but it is tied to local single-player world creation and does not directly map to vanilla clients joining a dedicated server

### UI-03 External Pack Downloader

- original class:
  - `ExternalPackDownloader`
- base:
  - `ConfirmScreen`
- purpose:
  - help user obtain required external datapacks
- title:
  - localized prefix + pack title
- interactions:
  - `Download`
  - `Select File` when direct in-game download is not supported
  - `Back` / `Cancel`
  - `Learn More…`
  - file drag-and-drop
  - OS browser/file-dialog integration
- states:
  - idle
  - auto-download progress
  - manual-file selection
  - cancel
  - success
  - wrong file -> error screen
- note for Purpur port:
  - this is not a gameplay UI surface for connected vanilla clients

### UI-04 Error Notice

- original class:
  - `ErrorScreen`
- base:
  - `NoticeScreen`
- purpose:
  - show integrity/file errors in the single-player datapack helper flow
- states:
  - unknown error
  - wrong datapack file
  - integrity check failed

### UI-05 Modified Death Screen State

- original base:
  - vanilla `DeathScreen`
- modification:
  - in hardcore, score line becomes obtained advancements count
- scope:
  - minor state variation, not a standalone new screen

## 4. Auxiliary Non-Screen UI Surfaces

### UO-01 Ability unlock toast

- source:
  - `AdvancementToastMixin`
  - `AbilityUnlockedToastType`
- behavior:
  - custom background
  - custom toast title by unlock category
  - custom colors
  - custom level-up-like sound

### UO-02 Open-advancements tutorial toast

- source:
  - `MovementTutorialStepHandlerMixin`
- behavior:
  - custom title/description
  - shown early in tutorial
  - persists until player opens advancements

### UO-03 Actionbar lock feedback

- source:
  - `AchieveToDoClient#isAbilityLocked`
- behavior:
  - on denied use, sends player-facing lock/unlock-progress message
  - also sends `DemystifyAbilityPayload`

### UO-04 Landmark border overlay

- source:
  - `WorldRendererMixin`
- behavior:
  - animated yellow forcefield-style border
  - only while player camera is inside a locked landmark box
  - updates when lock state or box geometry changes

### UO-05 Vision blackout overlay

- source:
  - `GameRendererMixin`
- behavior:
  - full black overlay while `VISION` is locked
  - gradual fade-out when unlocked

### UO-06 Pack list filtering

- source:
  - `PackScreenMixin`
- behavior:
  - hides AchieveToDo-managed packs from vanilla pack list

## 5. Ability / Progression UI Audit

### 5.1 Distinct ability visual states

- unknown locked ability:
  - trigger:
    - ability locked
    - demystified criterion not obtained
  - visible result:
    - mask shown
    - icon hidden
    - tooltip hidden

- demystified locked ability:
  - trigger:
    - player attempted locked ability
    - client sent `DemystifyAbilityPayload`
    - server granted demystified criterion
  - visible result:
    - icon visible again
    - tooltip visible
    - progress fraction/count visible
    - still locked in gameplay

- unlocked ability:
  - trigger:
    - obtained count reaches required count
  - visible result:
    - advancement completion
    - unlock toast
    - unlock chat message

- initially unlocked ability:
  - visible result:
    - no progress fraction
    - description gets italic gray "unlocked by default" note

- permanently locked ability:
  - visible result:
    - no progress fraction
    - description gets italic red "locked permanently" note

### 5.2 Data sources used by original UI

- synchronized from server:
  - ability -> required advancements count map
  - obtained advancements count
  - tracked score progress
  - tracked statistics progress
  - locked landmark add/remove
  - unlocked landmark types
  - resized landmark boxes
- locally derived on client:
  - nearby-entity progress counts
  - ability rows ordering/grouping
  - lock/demystify/unlock display state from advancement progress plus config/count state

### 5.3 Live update behavior

Original client UI is not a separate snapshot model. It reuses advancement widgets/progress objects that call into live `AchieveToDoClient` state, so these parts are effectively live-updating while the screen remains open:

- obtained advancements count
- required counts
- tracked score/stat counters
- nearby-entity counters
- demystified vs mystified display
- unlock/relock completion state

## 6. Landmark UI Audit

There is no dedicated landmark screen in the original mod.

Landmark-related UI is split across three places:

- landmark abilities inside the `ABILITIES` advancements tab
- landmark unlock toast category (`LANDMARK`)
- in-world locked landmark border overlay

Original does not expose:

- separate landmark list screen
- landmark browser
- map-like landmark directory
- dedicated landmark tooltip catalog outside the advancement cards

## 7. Visual Asset Audit

### 7.1 AchieveToDo-owned UI assets

- `original/src/main/resources/assets/achievetodo/textures/gui/sprites/ability_mystified_mask.png`
  - `26x26`
  - used by `AdvancementWidgetMixin`
  - state-dependent
  - purpose:
    - hide unknown locked ability icon

- `original/src/main/resources/assets/achievetodo/textures/gui/sprites/advancements_tab_mystified_mask.png`
  - `16x16`
  - used by `AdvancementsScreenMixin`
  - state-dependent
  - purpose:
    - hide locked tab icon area

- `original/src/main/resources/assets/achievetodo/textures/gui/sprites/ability_unlocked_notification_background.png`
  - `160x32`
  - used by `AdvancementToastMixin`
  - state-dependent
  - purpose:
    - custom ability-unlock toast skin

- `original/src/main/resources/assets/achievetodo/icon.png`
  - project icon, not an in-game UI surface

### 7.2 Vanilla textures reused by original

- `textures/gui/menu_list_background.png`
  - world-creation panel background

- `Screen.HEADER_SEPARATOR_TEXTURE`
- `Screen.FOOTER_SEPARATOR_TEXTURE`
  - world-creation panel framing

- `textures/misc/forcefield.png`
  - landmark border overlay animation

### 7.3 Fonts

- no custom font JSON found
- no custom glyph providers found
- no icon font assets found

Conclusion:

- original uses vanilla font
- AchieveToDo visual identity comes mostly from:
  - icon selection
  - custom masking sprites
  - toast background sprite
  - color choices
  - tab layout / information hierarchy

## 8. Text / Localization Audit

Primary localization files:

- `original/src/main/resources/assets/achievetodo/lang/en_us.json`
- `original/src/main/resources/assets/achievetodo/lang/ru_ru.json`

Important key groups:

- world creation tab:
  - `achievetodo.world_creation_tab.*`
- downloader / errors:
  - `achievetodo.downloader.*`
  - `achievetodo.error.*`
- tutorial:
  - `achievetodo.tutorial.open_advancements.*`
- locked tab hints:
  - `achievetodo.advancements_tab_mystified_tooltip.*`
- ability names / descriptions / locked messages:
  - `achievetodo.ability.<id>.*`
- special ability state text:
  - `achievetodo.ability.left_to_unlock`
  - `achievetodo.ability.permanently_locked`
  - `achievetodo.ability.initially_unlocked`
- unlock toast category text:
  - `achievetodo.ability_unlocked_toast.*`
- unlock chat text:
  - `achievetodo.tab_unlocked_chat_message`
  - `achievetodo.ability_unlocked_chat_message`

Text styling patterns observed:

- yellow highlight for AchieveToDo UI labels
- gray italic for hints / locked-tab guidance / default-unlocked notes
- red for permanent-lock or error emphasis
- black + brown-tinted subtitle on custom unlock toast
- standard vanilla text shadow behavior through vanilla draw calls

## 9. Networking Relevant to UI

### S2C payloads

- `SyncAbilitiesConfigurationPayload`
  - data:
    - `Map<AbilityType, Integer>`
  - purpose:
    - required counts / lock flags / progression mode settings

- `SyncObtainedAdvancementsCountPayload`
  - data:
    - `int obtainedAdvancementsCount`
  - purpose:
    - overall progress count for ability unlocking

- `ScoreProgressChangedPayload`
  - data:
    - `TrackedScoreType`
    - `int progress`
  - purpose:
    - live scoreboard-based UI progress

- `StatisticsDataProgressChangedPayload`
  - data:
    - `TrackedStatisticsDataType`
    - `int progress`
  - purpose:
    - live stat-based UI progress

- `LandmarksLockedStatusChangedPayload`
  - data:
    - `Map<LandmarkType, Set<DimensionalBlockBox>>`
    - `boolean isLocked`
  - purpose:
    - add/remove locked landmark boxes on client

- `LandmarkTypesUnlockedPayload`
  - data:
    - `Set<LandmarkType>`
  - purpose:
    - clear now-unlocked landmark types from client lock overlay state

- `LockedLandmarkResizedPayload`
  - data:
    - `LandmarkType`
    - `DimensionType`
    - old `BlockBox`
    - new `BlockBox`
  - purpose:
    - resize in-world border without full reload

- `CheckTargetInLockedLandmarkPayload`
  - data:
    - `DimensionType`
    - `Box`
  - purpose:
    - client-side recheck path that ends in lock feedback when target intersects a locked landmark

### C2S payloads

- `DemystifyAbilityPayload`
  - data:
    - `AbilityType`
  - purpose:
    - tell server that the player attempted a locked ability and the UI should reveal it

### Original networking conclusion

Original UI is not passive. It depends on custom client-side state synchronization and one client->server intent packet. This exact contract cannot be reused with vanilla clients and must be replaced by server-driven state derivation over already-ported backend services.

## 10. State Transition Matrix

### Obtained advancements count changes

User-visible result:

- affected ability progress bars/fractions update
- unlock threshold crossing can complete advancement
- unlock toast/chat can occur

### Locked ability first attempted

User action:

- perform locked gameplay action

Original response:

- actionbar lock message
- `DemystifyAbilityPayload` sent
- server grants demystified criterion
- UI card changes from masked/hidden-tooltip to revealed/progress-visible

### Unlock

User-visible result:

- advancement becomes complete
- ability unlock toast appears
- unlock chat message appears
- gameplay restriction is removed

### Relock

User-visible result:

- unlocked criterion revoked
- card becomes incomplete again
- if still demystified, it stays revealed rather than returning to full mystery

### Objective/config not ready

User-visible result:

- advancements screen cannot open
- red "not ready yet" feedback instead

### Locked landmark add/remove/resize

User-visible result:

- in-world landmark border appears/disappears/resizes live

### Vision unlock

User-visible result:

- black overlay fades out gradually rather than disappearing instantly

## 11. Vanilla Reconstruction Capability Matrix

### Exact

- actionbar lock feedback
- clickable chat messages with hover text
- vanilla advancement completion state as a concept
- inventory click navigation semantics
- simple localized text states

### Near-exact

- main ability browser as a server-driven inventory UI with resource-pack-assisted iconography
- locked vs demystified vs unlocked card identity
- tab/category navigation
- item/icon-based hover tooltips
- unlock notifications using vanilla toast-like substitutes plus existing synthetic advancement lifecycle

### Approximate

- original advancements-tab geometry and path graph
- progress bars/fractions inside a vanilla inventory UI
- locked landmark visibility as server-driven indicators
- world-creation tab semantics as a future admin/server configuration surface

### Not possible without client mod

- injecting directly into vanilla `AdvancementsScreen`
- true custom tabs inside the client’s native advancements UI
- exact mask-over-tab and mask-over-widget rendering in that screen
- direct reuse of Fabric custom payload UI contract
- fullscreen smooth black vision overlay
- animated in-world forcefield border identical to original
- custom client-side world-creation tab inside vanilla create-world flow on a dedicated-server-only vanilla client target

## 12. Known Impossibilities / Honest Deviations

- the original main UI is a modified client-side advancements screen; vanilla clients cannot receive that screen mutation from a server plugin
- the original client computes nearby-entity UI progress locally; the Purpur port must instead use the already authoritative server runtime state
- original in-world overlays require custom client rendering hooks unavailable to vanilla clients
- the world-creation tab is fundamentally a local single-player client workflow and has no direct runtime counterpart for friends joining a dedicated server

## 13. Resource-Pack Feasibility

Resource pack is justified.

What it can meaningfully improve:

- custom icon identity for ability states
- custom masked/locked icon appearance
- closer toast/background styling
- decorative panel framing through glyph/item-model presentation
- stronger category/tab visual identity

What it still cannot solve alone:

- true client-side advancement screen injection
- exact fullscreen fade overlay logic
- exact animated landmark border renderer

Recommendation from this audit:

- for intended fidelity, a server resource pack should be treated as `REQUIRED`
- a no-pack fallback should still exist, but it will be materially lower fidelity

## 14. Architecture Options for Stage 10B

### Option A - Inventory-first, resource-pack-assisted browser

- vanilla mechanism:
  - custom container UI
  - custom item icons/models
  - lore/tooltips/actionbar/chat for detail
- fidelity:
  - highest realistic fidelity for vanilla clients
- strengths:
  - dense grid possible
  - hover semantics possible
  - click navigation possible
  - backend consumption straightforward
- weaknesses:
  - not the native advancements screen
  - progress bar rendering is approximate

### Option B - Book/chat-driven browser

- vanilla mechanism:
  - written book pages
  - clickable chat links
  - actionbar/title feedback
- fidelity:
  - low
- strengths:
  - no inventory state machine complexity
- weaknesses:
  - poor icon density
  - poor hover affordance
  - poor resemblance to original

### Option C - Hybrid inventory + chat detail

- vanilla mechanism:
  - inventory for overview
  - chat/components for drill-down and commands
  - optional book pages for help/explanations
- fidelity:
  - medium-high
- strengths:
  - separates dense browsing from verbose explanation
  - reduces lore clutter
- weaknesses:
  - split attention between surfaces
  - more navigation complexity than Option A

### Recommended Option

Recommend `Option C` with an `inventory-first main browser`.

Interpretation:

- main screen is an inventory UI
- chat/actionbar are supplementary, not primary
- resource pack carries iconography and decorative identity
- server-side session state controls navigation and live refresh

This keeps the original “browse icons, hover for meaning, click for navigation, watch state evolve live” contract much better than book/chat-only approaches.

## 15. Recommended Stage 10B Direction

- focus only on in-game player UI, not on reproducing single-player world-creation helper screens
- implement a server-driven UI session layer that consumes:
  - current progression runtime
  - ability definitions
  - demystified/unlocked state
  - landmark state when relevant
- use an inventory-backed main browser with resource-pack iconography
- keep:
  - distinct unknown locked / demystified locked / unlocked / initially unlocked / permanently locked states
  - category navigation
  - hover detail
  - progress visibility
- preserve original chat feedback paths where they already exist naturally

## 16. Stage 10A Conclusions

- meaningful original screen surfaces found:
  - `UI-01` Modified Advancements Screen
  - `UI-02` World Creation Tab
  - `UI-03` External Pack Downloader
  - `UI-04` Error Notice
  - `UI-05` Modified Death Screen State
- primary gameplay UI:
  - `UI-01` Modified Advancements Screen
- dedicated landmark screen:
  - none
- custom font:
  - none
- custom textures:
  - three AchieveToDo GUI sprites only
- recommended resource-pack mode:
  - `REQUIRED` for intended fidelity
- recommended Stage 10B direction:
  - inventory-first browser plus chat/actionbar supplements, driven entirely from existing server backend state
