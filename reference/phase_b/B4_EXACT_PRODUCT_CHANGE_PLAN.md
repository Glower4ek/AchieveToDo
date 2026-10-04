# B4 — Exact Product Change Plan

**PHASE_B_B4_PLANNED.** This is a planning record. B5 has not started.

The machine-readable implementation authority is [b4_product_change_manifest.json](b4_product_change_manifest.json), supported by [converter decisions](b4_converter_decision.json), [companion decisions](b4_companion_matrix.json), and [localization handoff](b4_localization_handoff.json). Every planned production path has an action, stage, reason, evidence, architectural role, semantic contract, maintenance method, validation and dependencies.

## A. Exact B5 changes

Plan **174 ADD, 65 MODIFY, 9 DELETE**. These are distinct production paths, not advancement counts. KEEP decisions explicitly account for 1678 production files within the reviewed surfaces. Seven later certification artifacts and three B6 authoring/provenance artifacts are separate from these production totals.

| Subsystem | ADD | MODIFY | DELETE | KEEP |
| --- | ---: | ---: | ---: | ---: |
| companion_advancement_merges | 14 | 0 | 0 | 0 |
| companion_contracts | 2 | 0 | 0 | 0 |
| companion_messages | 6 | 1 | 0 | 38 |
| companion_wrappers | 24 | 0 | 0 | 0 |
| converter | 0 | 1 | 0 | 0 |
| integration | 0 | 0 | 0 | 15 |
| pack_metadata | 0 | 9 | 0 | 0 |
| pins | 0 | 1 | 0 | 0 |
| predicate_tags | 0 | 0 | 0 | 2 |
| reward_messages | 116 | 31 | 9 | 1108 |
| root_wrappers | 0 | 17 | 0 | 0 |
| setup | 0 | 1 | 0 | 9 |
| trackers_gui | 0 | 4 | 0 | 0 |
| trophy_shims | 12 | 0 | 0 | 0 |
| unchanged_product | 0 | 0 | 0 | 506 |

Main BACAP pin: official version **1.21 / Y2zZ5eSs**, source SHA1 `14da3f07b5467e8b59ffc0253fd8212c938cd739`, wrapper SHA1 null, filename `bacap.zip`, automatic download true. Preserve the original enum → verified global ZIP → world-copy architecture and downloader behavior. Exact page/download URLs are in `mainPin`; no upstream archive is added to Git.

All seven removed IDs follow their target references: adventure/spear_fishing → adventure/seafood_skewer; animal/birdkeeper and animal/chatterbox → biomes; the two nether lodestone advancements → mining; statistics/two_by_two → statistics/overpopulation; minecraft:nether/use_lodestone → minecraft:adventure/use_lodestone. Preserve B3's four confirmed and three high-confidence classifications. Exact target display, criteria, reward and tree identities justify reference maintenance for the latter three without asserting additional historical rename certainty. No progress migration.

B3 facts remain unchanged: 1222 common, seven old-only, 110 target-only; 594 byte-identical, four formatting-only, 624 semantically changed common IDs. Canonical arithmetic is **1152 − 7 + 97 = 1242**, with no same-ID canonical membership transitions.

## B. No-change decisions

Keep generic acquisition, world selection and freshness mechanisms, the existing Search bridge, all 17 categories and the accepted unknown-category fallback. Keep all ability thresholds, presets, pacing and the 1152 total clamp. Keep all six raw progression objectives; points do not become ATD ability progression.

Keep historical conversion implementations and the runtime LegacyChatText bridge for retained bundled messages. Keep upstream objective creation, global_install, number-format behavior and update_score delegation. Keep optional sparse fanpack/reward resources and local predicate/tag contracts. The removed pack-owned `minecraft:shulker_boxes` item tag exists in Minecraft 26.2 itself; the jar entry/hash is recorded. Keep ATD's historical nether_fungus/nether_roots aliases.

## C. Converter and marker

All 34 families are resolved in the converter authority:

- 29 migration families: **KEEP_HISTORICAL_SOURCE_ONLY**.
- Two item-SNBT probe families: **NOOP_FOR_TARGET**.
- Announcement suppression: **KEEP_TARGET_AS_INTENTIONAL_ATD_TRANSFORM**.
- Two runtime chat parse/diagnostic families: **KEEP_ALL_SOURCES**.

Compute the actual source ZIP SHA1 once before traversal. Only the verified main and Hardcore 1.21 identities use native handling. Their functions pass through unchanged; JSON receives only existing reward-announcement suppression. Preserve original historical conversion methods and their String-signature regression entry points. Do not trust filename, enum metadata alone, or a broad pack-format heuristic.

The real Minecraft 26.2 holder codec accepts the scalar enchantment tag `#blazeandcave:fishing_rod` but rejects r16's array containing that tag. Target Treasure Hunter contains this form. Native bypass fixes a demonstrated incompatibility, also avoiding unnecessary direct-holder, Unit-tooltip and serialization-only rewrites. Isolated codec probes are evidence for this decision, not a claim that the entire future integrated pack has passed runtime certification.

Plan **compat_26_2_r17** in B5. A new source hash invalidates old-source copies but cannot invalidate a same-target-source copy already processed under r16. The marker changes because conversion semantics change. B4 leaves the production marker at r16. Source SHA1 and root-override freshness fields remain intact.

## D. Trackers, GUI and Search

All 86 tracker bindings have final dispositions: **80 KEEP, six content updates**. Target bindings are 33 score, 50 statistics and three nearby trackers.

- TWO_BY_TWO: change only the advancement ID; retain animals_bred and 2500.
- Honey: move from item-use statistics at 200 to bac_consume_honey_bottle at 100.
- Pupil Poppers: move from item-use statistics to bac_consume_spider_eye; retain 1000.
- Animal Kingdom: add nautilus, zombie_nautilus, zombie_horse and camel_husk; retain radius 32.
- Family Reunion: radius 5 → 10, matching the changed upstream selector.
- Bone-to-Party: add parched and radius 5 → 10, matching the changed selector.

These changes synchronize actual advancement contracts; ability values do not change. ON_A_RAIL retains 1000 and its existing X/Z displacement handling. HALF_HEART_LIFE retains bac_hh_life, 60 and health-reset semantics. Removed bac_apple_eaten/bac_1000th_item objectives have no direct ATD tracker bindings. Do not treat the unused bac_apple_today dummy as an active replacement; the current per-day completion flag is technical/consume_apple.

All 28 ordered children are resolved: retain 27 and remove Constellation from the obsolete challenges/root list. Its new parent uses the existing generic ordering. Seven stale Search IDs follow the target IDs in nine moved message paths. Nullscape's invalid data:advancement/root link becomes nullscape:root. No separate Search index or GUI redesign.

All nine builtin pack metadata files remove pack_format 61 and declare min_format/max_format `[107,1]`, retaining descriptions. The actual SERVER_DATA PackFormat codec accepted that range; bare pack_format 107 was rejected. The cached version declares data format 107.1.

## E. Configuration, macros and rewards

All **1187 current message files** have final decisions. Main: 1108 KEEP, 31 MODIFY and nine path moves represented as DELETE plus target ADD. All 117 target paths without an existing main message override are resolved: 101 ordinary messages get original ATD localization/Search treatment; 15 root messages become comment-only duplicate suppressions; the excluded hidden bacap/root retains its upstream message.

The final main message override count is 1255. Preserve original root wrappers' unconditional tab-unlock tellraw, then delegate bookkeeping once to the native macro. Empty msg/root overrides prevent duplicate macro announcements. The legend wrapper delegates native bookkeeping while retaining its localized message. All 17 root/milestone wrappers are explicitly resolved, including Nullscape.

All ten setup/config files are resolved: nine KEEP; new_world adds native point defaults task=1, goal=2, challenge=5, super_challenge=20, milestone=50, hidden=0, advancement_legend=500. Preserve original raw sidebar, four option hooks and intro-message policy. Upstream start_timers creates 110 objectives and initializes hidden bac_dont_count=1. Other absent exclusion scores intentionally satisfy the upstream unless guard; do not invent additional settings. Keep upstream raw/point reconstruction and its retained old update_score delegate.

Resolve all 14 direct missing callees: 12 one-line trophy-category forwarding aliases, the Hardcore-supplied loser_hurt hook, and the shared Terralith/Nullscape initialization hook. Aliases repair the reachable grant-all operation without copying the full configuration function. The shared hook preserves explicit settings and supplies the original missing-setting default for Nullscape-only selection.

The 1075 sparse optional effect paths comprise 1023 trophy, 28 item and 24 experience paths. They have activation guards, **not existence guards**. If an enabled effect is absent, native execution catches the command error and continues later count/point/first/coop commands. Advancement execution suppresses command output. Preserve this upstream convention and certify continuation in B10; do not claim that explicit tracing has zero diagnostic failures. Do not create fake effect files or 1258 fake fanpack extension tags.

## F. Companion policy

| Integration | Decision | Official candidate |
| --- | --- | --- |
| BACAP_HARDCORE | UPDATE_PIN_REQUIRED | 1.21 |
| TERRALITH | UPDATE_PIN_REQUIRED | 2.6.4 |
| AMPLIFIED_NETHER | UPDATE_PIN_REQUIRED | 1.2.15 |
| NULLSCAPE | UPDATE_PIN_REQUIRED | 1.2.20 |
| BACAP_TERRALITH | COMPATIBILITY_SHIM_REQUIRED | 1.18.3 |
| BACAP_AMPLIFIED_NETHER | COMPATIBILITY_SHIM_REQUIRED | 1.18.3 |
| BACAP_NULLSCAPE | COMPATIBILITY_SHIM_REQUIRED | 1.18.3 |

Verified native candidates: Hardcore 1.21/ZHHHw5wF, Terralith 2.6.4/CzijfXJQ, Amplified Nether 1.2.15/xIayvf8F and Nullscape 1.2.20/prWWpjSv. All four primary downloads matched official size/SHA1/SHA512. The companion matrix records official URLs, metadata, hashes and current/future pins.

The latest author-linked BACAP worldgen variant pages report 1.18.3 but do not establish final-26.2 support. Retain the three recognized variant pins and use exact compatibility overrides rather than claiming a new official native release. The durable companion matrix contains 14 target-based advancement merge recipes with criterion pointers, source hashes, native leaf changes, parent/display operations and announcement suppression. Do not restore old full advancement bodies over new target criteria.

Add 24 active Terralith wrapper delegates and a shared macro that gates both raw and point increments by terralith_score. First/obtained/coop behavior remains native. Two archive-only historical helpers remain unchanged; their impossible criteria have no discovered automatic completion seed, but admin and guarded cooperation grants remain possible. Do not call them absolutely unreachable.

Nullscape delegates with actual adv_id=nullscape:root. Six new Hardcore message overrides cover five newly supplied message paths plus Stink Bomb: its Hardcore display changes the goal while inheriting main's message. All 39 current companion message files have final KEEP/MODIFY decisions. All 156 referenced Terralith identifiers and four Nullscape identifiers resolve in selected worldgen archives; old/new Terralith have the same 95 biome IDs.

Support remains conditional on B8/B9/B10 certification. No optional feature is silently deleted and no user-level product decision remains.

## G. Localization handoff

The exact B6 key/path/provenance manifest records **319 added keys, 41 removed keys and 42 changed-component review keys**. Selected companion components contribute one key absent from the current ATD RU overlay. Absence from that overlay alone does not establish missing runtime coverage before checking vanilla language assets.

Main 101 new ordinary messages and six Hardcore additions need translation-key coverage. The 15 comment-only root suppressions need none. Russian values stay in the original assets/minecraft/lang/ru_ru.json overlay; native ATD 533 EN and 533 RU keys remain intact. No translated prose or translation-source research occurs in B4.

B6 extends the original offline authoring/merge approach with a Phase-B-specific tool and provenance/credits records. Research all usable existing Russian sources, preserve required permission/licenses/credits, and self-author uncovered strings. Remove an old key only after checking every retained companion and ATD consumer. There is no new runtime translation product.

## H. B5 order and certification successors

1. **B5.1 — pins_pack_metadata** (10 files): Acquire verified main/Hardcore/worldgen sources; preserve filenames/downloader and correct9builtinranges.
2. **B5.2 — native_converter** (1 files): Native tagpredicate accepted; historical migration unchanged; source/root/markerfreshnessr17/idempotence.
3. **B5.3 — setup_resource_contracts** (15 files): Sevenpointdefaults,10originaltoggles/formatfiles,sharedcompanioncountgate+default,12exactforwarders; raw progression independentpoints.
4. **B5.4 — root_reward_messages** (173 files): 16mainwrappermacrocontracts;15duplicate rootmsg suppressions;101newordinarymsgs,9oldpathsremoved;localizedSearch links exact; no RUtext authoring.
5. **B5.5 — trackers_gui** (4 files): All86contracts and28childordering; noabilityconfig orTOTAL1152 change.
6. **B5.6 — companion_merges** (45 files): 14nativecriterionmerges,24delegatingTerwrappers,2archivehelperKEEP,Nullcorrectroot,6newHardcoremessages; sixrawscores/points reconstructionconsistent.
7. **B5.7 — structural_verification** (0 files): Reconcile fullmanifest, nativecodec/tree/references/message/alltracker/sourcehash checks; nohistoricalevidence changed; awaitB6/B7-B11certification.

Each changed production path is assigned exactly once. B6 remains a separate transaction. Named successor artifacts cover B7 localization/provenance, B8 parsing all 1332 target advancements and canonical 1242 plus combined companion resources, B9 conversion/tracker/Search/tree regressions, B10 real new/reopened worlds and every supported option/companion combination, and B11 manual new/changed advancement review. Exact inputs, coverage, expected invariants and verification gates are in the manifest.

Historical Phase A/FINAL19/publication tests, frozen fixtures and evidence remain immutable. Run unchanged generic product regressions and report frozen release-snapshot outcomes separately; do not rewrite historical expectations to conceal a changed target.

## I. Blockers and deferrals

**NONE.** The plan has concrete engineering dispositions supported by source, metadata and codec evidence. Later certification is required; it is not a missing user decision. Ability thresholds, balance, pacing, the 1152 clamp and terminal progression design remain **DEFER_TO_PHASE_C_OR_D**. A failed later invariant requires evidence and a stop at that later transaction, not silent feature removal.

## J. Safety

Branch remains phase-b-bacap-26.2; HEAD remains b261b02cd03b4aeae2835c63f982c9aa53c46eed. Product Java/resources/localization/tests, both main archives, all historical archives, B0–B3 evidence, production pins and marker are unchanged. B4 adds exactly these five planning records; temporary research, probes and downloads remain under ignored build/tmp. No commit, push, tag, GitHub mutation or B5 execution.
