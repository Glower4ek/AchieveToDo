# Stage 10B Plan

Stage to follow: `10B - UI Backend & Navigation`

This plan is design-only. No classes are implemented in Stage 10A.

## 1. Recommended Direction

Recommended architecture:

- server-driven UI session model
- inventory-first main browser
- chat/actionbar as supporting surfaces
- server resource pack assumed for intended fidelity

Why:

- best balance of:
  - vanilla compatibility
  - dense icon browsing
  - hover/click affordances
  - closeness to the original advancements-based UX

## 2. Scope for Stage 10B

In scope:

- UI backend/session architecture
- open/navigation flow
- state derivation from existing backend
- refresh/update policy
- session cleanup
- tests for session/navigation/state mapping

Out of scope:

- final resource-pack assets
- Stage 10C art/font implementation
- unrelated gameplay runtime changes

## 3. Proposed Modules

### 3.1 UI domain model

Likely classes/interfaces:

- `UiScreenId`
- `UiRoute`
- `UiEntryViewModel`
- `UiCategoryViewModel`
- `UiScreenViewModel`
- `UiFeedbackViewModel`

Purpose:

- pure-Java representation of what the player should see
- no Bukkit inventory dependency in the core mapping layer

### 3.2 UI state resolvers

Likely classes/interfaces:

- `UiAbilityStateResolver`
- `UiProgressViewResolver`
- `UiLandmarkStateResolver`
- `UiCategoryLayoutResolver`

Consumes existing backend:

- progression runtime state
- ability definitions
- synthetic advancement/demystification state
- landmark lock state if exposed

### 3.3 Bukkit/Purpur UI adapter layer

Likely classes/interfaces:

- `BukkitUiSessionManager`
- `BukkitUiInventoryRenderer`
- `BukkitUiOpenService`
- `BukkitUiClickRouter`

Purpose:

- render route/view-models into vanilla inventory containers
- translate clicks/close/open into navigation actions

### 3.4 Session lifecycle

Likely classes/interfaces:

- `PlayerUiSession`
- `PlayerUiSessionStore`
- `PlayerUiSessionRefresher`

Responsibilities:

- current route
- selected category/page
- selected detail target if any
- pending refresh flag
- disconnect cleanup

## 4. Opening Strategy

Recommended Stage 10B opening mechanism:

- add one explicit plugin command for players

Suggested command:

- `/atd`

Suggested behavior:

- opens the main UI browser

Suggested aliases:

- `/achievetodo`

Permissions:

- default player-accessible

Why:

- vanilla-compatible
- short
- easy to teach
- does not rely on client mods or hidden items

Optional later complements:

- clickable chat component in unlock feedback
- contextual reminder/help prompt

## 5. Navigation Model

Recommended graph:

- `ENTRY`
  - command/chat click
- `HOME`
  - main abilities overview
- `CATEGORY`
  - specific category slice if separated from home
- `DETAIL`
  - optional focused ability detail if needed
- `BACK`
  - returns to previous logical route
- `CLOSE`
  - closes inventory cleanly

Guidelines:

- keep navigation shallow
- do not create deep menus just because inventory pagination is easy
- preserve the original “overview-first” feeling

## 6. Refresh / Update Model

Rules:

- UI must never become a second progression engine
- all view state is derived from authoritative backend state

Refresh triggers to support:

- obtained progression count changes
- demystification
- unlock
- relock
- landmark state changes if surfaced in UI
- objective readiness changes

Recommended implementation:

- mark active UI sessions dirty on relevant backend events
- refresh on next safe server tick
- reopen inventory only when necessary
- prefer in-place updates where Bukkit container semantics allow it cleanly

## 7. Event / Listener Model

Expected Stage 10B listeners:

- inventory click
- inventory close
- player quit
- plugin disable cleanup
- optional join reminder hook if later approved

Guidelines:

- listeners should route to the UI session manager only
- no progression logic inside listeners

## 8. Resource-Pack Boundary

Stage 10B assumes:

- logical slot layout and icon ids can be defined now
- physical textures/glyphs arrive in Stage 10C

Therefore Stage 10B should separate:

- semantic icon id
- displayed item/material fallback
- optional resource-pack-backed presentation id

## 9. Test Plan

Recommended tests for 10B:

- route transitions
- session open/close cleanup
- ability state -> view-model mapping
- category layout mapping
- hidden vs demystified vs unlocked rendering decisions
- fallback rendering decisions when pack-specific presentation is unavailable

Avoid:

- fake tests that only mirror implementation branches

## 10. Explicit Non-Goals for 10B

- no world-creation-tab recreation
- no datapack downloader recreation
- no client-side overlay parity hacks
- no NMS

## 11. Success Criteria

Stage 10B should be considered successful if it delivers:

- a player-openable main AchieveToDo browser on vanilla clients
- state derived from existing backend only
- correct navigation/session cleanup
- distinct hidden/revealed/unlocked states
- no runtime gameplay regressions
