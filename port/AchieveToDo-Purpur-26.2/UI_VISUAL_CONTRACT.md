# UI Visual Contract

Stage: `10A - Original UI Audit & Visual Contract`

Purpose:

- define what future Stage 10 implementations must preserve
- stay implementation-independent
- prioritize behavior/data/navigation first, but keep visual identity explicit

Priority legend:

- `P0` must preserve
- `P1` should preserve
- `P2` cosmetic approximation allowed

## VC-01 Main Ability Browser

Original source surface:

- modified vanilla `AdvancementsScreen`
- `ABILITIES` tab opens by default

Future contract:

- `P0`
  - there must be a single clear in-game "main UI" for abilities
  - it must be reachable by players on demand
  - it must display abilities as a browseable overview, not only as text messages
  - it must distinguish:
    - unknown locked
    - demystified locked
    - unlocked
    - initially unlocked
    - permanently locked
  - it must show unlock progress for relevant abilities
  - it must reflect backend state live enough that reopening is not required for every small change
- `P1`
  - icon identity should be strong and category-specific
  - the screen should feel grid-based rather than linear
  - the information hierarchy should resemble:
    - icon first
    - name
    - short description
    - progress / lock note
  - hover/focus detail should exist
- `P2`
  - exact advancement-node geometry
  - exact pixel spacing from the Fabric screen

## VC-02 Category / Tab Navigation

Original source surface:

- AchieveToDo-managed advancement tabs

Future contract:

- `P0`
  - category navigation must exist
  - player must not be forced through a flat 150-item list
  - locked categories must be visibly distinct from available categories
- `P1`
  - tabs/groups should feel like first-class navigation, not hidden admin filters
  - `ABILITIES` must remain the default landing category
- `P2`
  - exact left/above/below physical tab positions

## VC-03 Unknown vs Revealed Ability State

Original source surface:

- `AdvancementWidgetMixin`

Future contract:

- `P0`
  - an ability the player has never "discovered" must look materially different from one they have revealed but not unlocked
  - discovery must happen through gameplay denial, not by default
- `P1`
  - unknown locked state should suppress or obscure detail
  - revealed locked state should expose name/description/progress
- `P2`
  - exact mask sprite shape

## VC-04 Progress Presentation

Original source surface:

- advancement progress overrides

Future contract:

- `P0`
  - current progress and required progress must be visible for non-special abilities
  - initially unlocked and permanently locked states must not look like ordinary progress bars
- `P1`
  - percentages for percentage-tracked goals should remain understandable at a glance
  - progress should update without forcing the user into a different UI concept
- `P2`
  - exact vanilla advancement bar fraction styling

## VC-05 Ability Detail Content

Original source surface:

- localized ability title/description keys
- special state notes

Future contract:

- `P0`
  - every ability entry must preserve:
    - name
    - description
    - special locked/default state note where relevant
- `P1`
  - icon should match the original item/block/theme identity where feasible
- `P2`
  - exact tooltip wrapping width

## VC-06 Landmark Presence

Original source surface:

- landmark abilities inside the same main browser
- in-world border overlay

Future contract:

- `P0`
  - landmark-related abilities remain part of the same primary progression UI
  - do not invent a standalone landmark browser as the main experience
- `P1`
  - landmark-specific abilities should preserve landmark identity in icon/text naming
- `P2`
  - exact in-world forcefield look

## VC-07 Unlock Feedback

Original source surface:

- custom ability unlock toast
- unlock chat message

Future contract:

- `P0`
  - unlocking an ability must create explicit celebratory feedback
  - the feedback must clearly name what became available
- `P1`
  - unlock category flavor should remain visible where practical
  - the main browser should make it easy to find the newly unlocked entry
- `P2`
  - exact toast background art and sound pitch

## VC-08 Entry Guidance

Original source surface:

- tutorial toast asking the player to open advancements

Future contract:

- `P0`
  - first-time players need an explicit hint that the UI exists and how to open it
- `P1`
  - this hint should feel like onboarding, not a noisy command manual
- `P2`
  - exact tutorial-toast form

## VC-09 Objective Not Ready State

Original source surface:

- advancements screen refuses to open when state is not loaded

Future contract:

- `P0`
  - if backend state is unavailable, the UI must fail clearly instead of showing stale or fabricated data
- `P1`
  - failure messaging should be immediate and short

## VC-10 World-Creation / Configuration UX

Original source surface:

- `WorldCreationTab`
- downloader/error helper screens

Future contract:

- `P0`
  - preserve the semantics of the options, not the exact local single-player client screen
- `P1`
  - if a future server/admin UX is built, it should keep:
    - progression mode choice
    - cooperative toggle semantics
    - reward toggles
    - optional generation-pack toggles as configuration concepts
- `P2`
  - exact screen layout of the Fabric create-world tab

Note:

- this contract is historical and informative for Stage 10+
- it is not the core player-facing runtime target for vanilla clients on a dedicated server

## VC-11 Visual Identity Summary

- `P0`
  - strong icon-first browsing
  - distinct hidden vs revealed vs unlocked states
  - category navigation
  - explicit unlock feedback
- `P1`
  - warm yellow/gold AchieveToDo palette cues
  - decorative panel framing
  - recognizable tab/icon identity
- `P2`
  - exact pixel skin parity
  - exact animation timing

## Recommended Fidelity Target

- realistic target:
  - `near-exact` for information hierarchy and interaction contract
  - `approximate` for native-advancements-screen rendering parity
- not realistic without client mod:
  - exact Fabric-style advancement-screen injection
  - exact custom world overlays
