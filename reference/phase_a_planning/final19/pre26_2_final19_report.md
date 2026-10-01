# PRE_26_2_FINAL19_AUDIT_AND_MASTER_MAP

Date: 2026-09-30. Planning complete; implementation has not begun. Historical Phase A remains `PHASE_A_ACTIONABLE_FRONTIER_EXHAUSTED`.

**Result:** a concrete, conditional path to real 1152/1152 is defined. Exactly 19 targets, 108 criteria and 79 requirement groups were audited. Current certification remains **1133/1152**; no new GREEN or status promotion occurred.

**Proven defect:** `minecraft:adventure/voluntary_exile` loses captain semantics because production conversion emits `isCaptain`, while actual 26.2 codec reads `is_captain` and defaults it to false. Parsing succeeds despite incorrect semantics. Later narrow production migration and runtime-pack freshness repair is required.

**Baseline integrity gate:** the same converter emits `hasRaid` for already `STATIC_CERTIFIED` `blazeandcave:adventure/feeling_ill`; all five intended `has_raid=true` conditions decode false. This is not a twentieth unresolved target and contributes zero certified gain. It must be repaired/revalidated as the raider fix regression surface before claiming real 1152. `dungeon_crawler` explicit false remains false by coincidence and also requires regression validation. Canonical statuses/history are untouched.

The other 17 static failures comprise one already-runtime-GREEN jungle (classification A) and 16 valid-current-data static context failures (B). The shield control has correct current predicates and a missing native runtime receipt (D). Thus 18 target definitions have viable current native paths; only jungle is actually proven complete by accepted evidence. The other 17 viable paths are likely working, not certified by this audit. No production gate defect or explicit product/design incompatibility was proved.

## Verified starting state

1152 total; 1133 certified; 958 STATIC_CERTIFIED; 175 RUNTIME_CERTIFIED; 17 STATIC_FAIL_UNRESOLVED; 2 RUNTIME_DEFERRED. Runtime strengthened 175; runtime partial 0; unique runtime touched 182. Of the 79 target groups, jungle accounts for the one already satisfied group; 78 remain unsatisfied.

- `src/test/resources/phase_a_certification/phase_a_advancement_rollup.json`: `439f2aa1c577ffbee275525765ac9229bf5633f0f5e1da498c0adc5702bd2810`
- `reference/phase_a_planning/phase_a_frontier_map.json`: `2cb3d3f835a1d8f1eda97ec782725a72936f346dd8cfeed75da61fc4d9f36662`
- `.agents/skills/achievetodo-phase-a/SKILL.md`: `22448f14632d290cee8b0639c88d51f1ee992cafd504231a57cb77384aa9a594`
- `reference/phase_a_preservation/files/final/bacap.zip`: `8c72314535c5df7b4416bf0f38310371ec8aec537fde1445bdc820a3c9aada70`
- `reference/phase_a_planning/phase_a_terminal_reconciliation_20260930.json`: `b08e001dd797e783484c3e338122286b98a17276cf289fc50270067b2a4aefb0`
- Frozen BACAP SHA-1: `45b8bb0076bbf5b92fde7dc9590c6686937abbc0`.

## Actual families and order

| Order | Root cause | Targets | Criteria / groups | Predicted certified |
|---|---|---|---|---|
| 1 | Biome HolderSet scalar/list validator context | 2 | 37 / 37 | 1133 + 2 = 1135 |
| 2 | Cat/frog/wolf data-driven component registry context | 3 | 23 / 23 | 1135 + 3 = 1138 |
| 3 | Item-used-on-block simultaneous item/block tags | 4 | 6 / 6 | 1138 + 4 = 1142 |
| 4 | Inventory item tag plus enchantment HolderSet context | 6 | 35 / 9 | 1142 + 6 = 1148 |
| 5 | Worldgen HolderSet combined with another predicate context | 2 | 5 / 2 | 1148 + 2 = 1150 |
| 6 | Raider snake_case field semantics lost by migration | 1 | 1 / 1 | 1150 + 1 = 1151 |
| 7 | Native shield block of skeleton-owned projectile | 1 | 1 / 1 | 1151 + 1 = 1152 |

The progression is **1133 → 1135 → 1138 → 1142 → 1148 → 1150 → 1151 → 1152**. It predicts gains after the gates pass; it is not a claim that the current build is compatible for every entry. Final real 1152 additionally requires the zero-gain certified raider integrity gate. No explicit decision blocker was found; no target qualifies as genuinely unsupported/impossible.

### Biome HolderSet scalar/list validator context

PhaseACertification starts with BuiltInRegistries only. findLocationHolderFieldMatches/containsOnlyLocationHolderSetDiagnostics account for scalar Not-a-json-array errors, but not list-shaped missing-biome-registry errors. Jungle is a biome list; bard mixes 19 list and 17 scalar biome selectors across 36 criteria. Scalar HolderSets are accepted by real 26.2 HolderSetCodec; they are not malformed data.

Targets:

- `blazeandcave:biomes/the_mighty_jungle`
- `blazeandcave:redstone/travelling_bard`

**Repair:** Add source-path-preserving typed biome HolderSet context/error accounting for both scalar and list selectors. Validate only fields actually requiring context; do not migrate valid scalars. Parse all 37 criteria. Preserve accepted jungle receipt; native empty-hand note-block default use in each of the 36 biome selector groups completes bard.

**Order rationale:** Jungle provides a pre-existing real runtime control for the validator/accounting foundation; bard amortizes the same worldgen repair and avoids 36 data patches.

**Canary:** First reuse jungle GREEN with full static proof; then bard plains + snowy_plains (list), pale_garden (scalar), nether_wastes and the_end. Exact stage covers all 36 bard groups.

**Regression gates:**

- Unknown scalar/list/mixed biome IDs must fail; do not substitute a successful parse after deleting a predicate.
- Jungle must keep its three alternatives and spectator exclusion; normal rollup STATIC_FAIL precedence stays intact.
- Bard retains minecraft:default_block_use, note_block, all 36 AND groups, source biome aliases and exact dimension/player predicates. No jukebox/music-disc substitution.
- Travelling-bard protected static control reopens only after all 36 exact biome predicates parse/resolve and unknown/scalar/list/mixed-biome fail-closed controls replace the old no-generic-migration guard; exact native note-block completion still required.

### Cat/frog/wolf data-driven component registry context

Converted legacy type_specific variant predicates are correctly represented as minecraft:cat/variant, frog/variant and wolf/variant component predicates. Limited static RegistryAccess excludes these three data-driven registries; current deferred classifier has no variant-registry family. All 23 IDs resolve in actual VanillaRegistries 26.2 and all 23 criteria parse.

Targets:

- `minecraft:husbandry/complete_catalogue`
- `minecraft:husbandry/leash_all_frog_variants`
- `minecraft:husbandry/whole_pack`

**Repair:** Provide typed data-driven cat_variant/frog_variant/wolf_variant context, check direct IDs/components against the real registry, and retain frozen criteria unchanged. Feed untamed cats/wolves through normal packets using finite fish/bones and bounded retries; lead each live frog normally. Native taming/leash attachment must cause the receipt.

**Order rationale:** One typed registry foundation resolves three independent targets; finite native interaction fixtures are already available.

**Canary:** One cat, warm frog and pale wolf, with wrong-variant negative controls; execute all 11+3+9 AND groups afterward.

**Regression gates:**

- Reject unknown variant/component ID and wrong species/variant; default variant may not stand in for all entries.
- No direct tame calls or setTame proof. Count consumed food/lead state and actual owner/leash holder.
- All 23 frozen AND groups remain required; compare the five current vanilla controls without replacing BACAP definitions.

### Item-used-on-block simultaneous item/block tags

Both match_tool item tag and location_check block tag are present. collapseTagComponents returns null when components differ; classifyMultiContextDeferredComponent requires an equipment-enchantment dimension, so two tag-only dimensions are rejected. All source tags exist and resolve with full source-backed context. No missing-tag conversion is required.

Targets:

- `blazeandcave:building/happy_birthday`
- `blazeandcave:building/setting_up_the_mood`
- `blazeandcave:building/washing_machine`
- `minecraft:husbandry/wax_off`

**Repair:** Compose only actual typed item+block tag dependencies with complete diagnostic coverage. Reuse item/block/contextual GameTests. Assert candle insertion, lighting, each cleaning mutation and native axe unwaxing. Wax-off uses AxeItem native pre-setBlock trigger; do not move predicate onto the already unwaxed post-state.

**Order rationale:** One two-tag composer improvement unlocks four IDs with six small, existing native-action fixtures.

**Canary:** Happy birthday candle insertion plus wax_off pre-mutation trigger canary; exact four advancements/six groups next.

**Regression gates:**

- Missing nested tag or unknown member remains a hard failure; retain custom waxed_copper_blocks frozen membership (36 entries), not a broader new vanilla tag.
- Test no-op interactions, wrong item/block, unlit/undyeable cases and locked abilities; correct interaction success must be accompanied by actual mutation.
- RECIPE_CRAFTED_WAX_ON is a different trigger and cannot prove wax_off. Successful single-tag families must keep exact classified dependencies.

### Inventory item tag plus enchantment HolderSet context

The converter already performs the intended direct enchantment scalar -> singleton list migration under item predicates. Each master combines item tag and enchantment registry dependencies in conditions.items[0].predicates.enchantments. Multi-context classification accepts enchantments only under conditions.player[0].predicate.equipment; inventory item predicates are excluded. Pure tag/enchantment classifiers cannot account for the other dimension. All 35 branches parse and have a legal supported-item/max-level/nonexclusive witness.

Targets:

- `blazeandcave:enchanting/master_armorer`
- `blazeandcave:enchanting/master_axeman`
- `blazeandcave:enchanting/master_digger`
- `blazeandcave:enchanting/master_farmer`
- `blazeandcave:enchanting/master_knight`
- `blazeandcave:enchanting/master_miner`

**Repair:** Extend typed diagnostic composition to exact inventory item predicate paths, retaining each enchantment conjunction. Audit all 35 branch definitions against live supported_items, max_level and exclusive_set. Reuse existing inventory pickup lifecycle. Nine true group witnesses suffice for completion across six advancements; optional other OR-branch runtime coverage must not inflate gain.

**Order rationale:** Highest certified gain (six) per shared repair; existing pickup executors and static legality checks minimize semantic changes.

**Canary:** Diamond helmet protection branch plus diamond axe sharpness/fortune branch; exact stage includes four armor groups and one group for each of five tools/sword.

**Regression gates:**

- Retain successful PURE_ENCHANTMENT_INVENTORY_CHANGED, ENCHANTMENT_HOLDERSET_INVENTORY_CHANGED and ENCHANTMENT_INVENTORY_CHANGED_EXPANSION_DIRECT16 behavior and source hashes.
- Fortune/Silk Touch, armor protection alternatives, Sharpness/Smite/Bane and Depth Strider/Frost Walker are alternatives across criteria, never simultaneous requirements. All 35 selected branch witnesses are valid.
- Reject missing item tag, enchantment registry/ID, one missing/underlevel enchantment or unsupported item. Full registry lookup does not become a generic enchantment fallback.

### Worldgen HolderSet combined with another predicate context

The scalar structure/biome selectors are valid HolderSetCodec forms. Existing pure location classifier requires only location errors; it cannot compose boat entity-tag+structure or boots-enchantment+deep-ocean contexts. Existing multi-context classifier combines tags with equipment enchantments but omits worldgen. Both complete definitions and all five criteria parse with exact source-backed contexts.

Targets:

- `blazeandcave:biomes/boatception`
- `blazeandcave:enchanting/let_it_go`

**Repair:** Compose worldgen/structure or biome dependencies with actual tag/enchantment paths; no generic scalar/list migration. Boat proof uses actual registered/generated shipwreck OR beached-shipwreck start and native mounting. Let-it-go proof equips a legal Frost Walker boot, walks on water at a qualifying deep-ocean biome and observes native frosted-ice and LOCATION tick. One of four OR branches satisfies that advancement.

**Order rationale:** Depends on working typed worldgen foundation and explicit protected-control replacement; two distinct native fixtures deserve isolation.

**Canary:** Boat tag member outside shipwreck negative then legitimate shipwreck mount; Frost Walker on ordinary/deep-ocean ice negative then native water-to-frosted-ice movement.

**Regression gates:**

- Boat mounted outside structure/nonboat remains false; structure predicate evaluates vehicle location, not a display-name or player biome proxy.
- Let-it-go protected control is reopened only after typed semantic proof and replacement wrong-biome/boots/ice/enchantment/spectator guards.
- Both shipwreck branches and all four deep-ocean branches must parse unchanged. Existing ENTITY_TYPE_TAG_STARTED_RIDING and worldgen family evidence remains historical.

### Raider snake_case field semantics lost by migration

ExternalPackCompatibility.transformRaiderTypeSpecificObject rewrites is_captain -> isCaptain and has_raid -> hasRaid. Actual 26.2 RaiderPredicate.CODEC reads is_captain/has_raid, both default false. Current voluntary_exile decodes hasRaid=false,isCaptain=false although frozen is_captain=true. Historical official 1.21.4 has the same defaults, so preserving omitted has_raid=false needs no product choice. This is a proven gameplay contract bug, not just a missing receipt.

Targets:

- `minecraft:adventure/voluntary_exile`

**Repair:** Later preserve has_raid/is_captain exactly within the existing raider type-specific transform; keep migrated outer type_specific/raider field and type discriminator as expected by current codec. Regenerate current compatible copy with an explicit freshness marker/fingerprint so old r15 copies cannot silently survive. Verify all three affected frozen definitions. Native player kill proves the corrected voluntary_exile predicate.

**Order rationale:** Real semantic repair has a wider integrity gate despite only one target; do it after analyzer/lifecycle baselines and before any claim of real 1152.

**Canary:** Decode converted predicate and assert actual boolean fields; normal noncaptain negative then true no-raid captain native kill positive. Additionally feeling_ill raid-vs-nonraid semantic canary is mandatory before final target claim.

**Regression gates:**

- All and only three frozen definitions contain raider boolean predicates: voluntary_exile is_captain=true; feeling_ill five has_raid=true criteria; dungeon_crawler three has_raid=false criteria. Other advancement criteria stay byte/semantically unchanged.
- Already STATIC_CERTIFIED feeling_ill currently decodes all five required raid booleans as false. Repair/revalidate this as certified regression debt with zero counted gain; do not add it as a twentieth unresolved target or blindly trust 1133 baseline gameplay correctness.
- Negative ordinary raider, correct banner without patrol-leader flag, active-raid captain, outsider entity tag; after fix none may satisfy voluntary_exile. Assert omitted fields keep false and explicit false remains false.
- Reject cached pre-fix packs even with compat_26_2_r15; retain source SHA, root override and llamaCarpetNbtMapping=equipment.body guarantees. Existing llama data remains exact.

### Native shield block of skeleton-owned projectile

Current source-backed definition correctly requires blocked=true plus damage type projectile tag and skeleton source-entity tag. Its historical RUNTIME_DEFERRED status comes from missing skeleton tag context after display shield/banner registry fallback. No current migration or gameplay bug was found; there is no accepted native shield/projectile false-to-true receipt. Existing damage/using-item/packet lifecycle is reusable, but exact ranged skeleton shield coordination is new test orchestration.

Targets:

- `minecraft:story/deflect_arrow`

**Repair:** Reuse joined-player/packet lifecycle and damage-source provenance infrastructure; add a finite skeleton AI ranged attack/shield coordinator. Arrow must have actual skeleton owner, projectile tag and native blocked amount. Observe LivingEntity native hurt/BlocksAttacks -> ENTITY_HURT_PLAYER and target receipt. Do not copy the relaxed 26.2 vanilla any-blocked-projectile predicate.

**Order rationale:** Timing-sensitive last proof follows all reusable context and lifecycle gates; excludes a known source of fake GREEN.

**Canary:** Confirm using shield active with live unlocked gate; real skeleton shot block false->true. Paired unlocked/unblocked and wrong-source negatives precede exact evidence.

**Regression gates:**

- No-block skeleton projectile, melee/nonprojectile, non-skeleton projectile owner and locked USE_SHIELD remain false.
- Display shield banner-pattern parsing is distinct from gameplay damage predicate; do not delete icon components to make parsing pass.
- Protected runtime control replacement preserves deferred-tag classifier and stale/missing runtime receipt rejection.

## Per-advancement dispositions

| Advancement | Primary class / repair layer | Current proof and later requirement |
|---|---|---|
| `blazeandcave:biomes/boatception` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:biomes/the_mighty_jungle` | ALREADY_WORKS_RUNTIME_STATIC_CERTIFICATION_BUG / STATIC_ANALYZER | Accepted jungle GREEN retained; fix biome-list static context, then normal accounting. |
| `blazeandcave:building/happy_birthday` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:building/setting_up_the_mood` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:building/washing_machine` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 3 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:enchanting/let_it_go` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:enchanting/master_armorer` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 4 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:enchanting/master_axeman` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:enchanting/master_digger` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:enchanting/master_farmer` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:enchanting/master_knight` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:enchanting/master_miner` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `blazeandcave:redstone/travelling_bard` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 36 exact AND-of-OR groups need mapped native witnesses. |
| `minecraft:adventure/voluntary_exile` | COMPAT_MIGRATION_BUG / COMPAT_MIGRATION | Proven captain-field loss; narrow conversion, fresh pack and native valid captain kill. |
| `minecraft:husbandry/complete_catalogue` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 11 exact AND-of-OR groups need mapped native witnesses. |
| `minecraft:husbandry/leash_all_frog_variants` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 3 exact AND-of-OR groups need mapped native witnesses. |
| `minecraft:husbandry/wax_off` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 1 exact AND-of-OR groups need mapped native witnesses. |
| `minecraft:husbandry/whole_pack` | STATIC_ANALYZER_CONTEXT_BUG / STATIC_ANALYZER | Full context parses; 9 exact AND-of-OR groups need mapped native witnesses. |
| `minecraft:story/deflect_arrow` | RUNTIME_CERTIFICATION_GAP / RUNTIME_EVIDENCE_ONLY | Correct skeleton projectile+blocked predicates; new native shield proof. |

The audit JSON contains full frozen/current/fresh-converter definitions, raw member hashes, exact criteria and requirement arrays, all changed field paths, source-backed tag membership/definitions, required registries/components/NBT/locations, reward function bodies/hashes, exact limited-codec failures and native boundary/gate/harness references for each of the 19. Missing `requirements` is materialized as one singleton AND group per criterion, exactly as vanilla completion semantics require.

## Important semantic conclusions

- Jungle uses a three-biome list. Static lookup omits the biome registry; the location classifier recognizes scalar diagnostics only. Accepted `jungle` GREEN satisfies the group, but unconditional STATIC_FAIL precedence keeps it unresolved. Preserve that fail-closed rule and repair valid static context; do not discard the receipt or special-case a GREEN override.
- Let-it-go retains valid scalar deep-ocean HolderSets and scoped Frost Walker enchantment conversion. There is no demonstrated data incompatibility. It needs worldgen-plus-enchantment context and native freezing/LOCATION proof; one of four OR branches suffices.
- Masters use `conditions.items[0].predicates.enchantments`, not player equipment paths. Existing multi-context acceptance is equipment-only. All 35 master branches and four let-it-go branches have a real supported-item, within-max-level, nonexclusive enchantment witness. Protection, damage, Fortune/Silk Touch and boot alternatives remain OR, never illegal combined requirements. Six master advancements need nine group witnesses, not 35 compulsory runtime branches.
- Single-tag ITEM_TAG_ITEM_USED_ON_BLOCK and BLOCK_TAG_ITEM_USED_ON_BLOCK families succeeded because each diagnostic had one component. The four unresolved block-use targets combine both types without enchantments; their source tags are present. Current multi-context logic rejects that composition. Wax-off native axe triggering is before unwax mutation; wax-on crafting evidence cannot substitute.
- Travelling bard uses `minecraft:default_block_use` on a note block, with 36 AND groups (19 biome lists, 17 scalar selectors), including Nether, End, caves, cherry grove and pale garden. It has no jukebox/disc predicate. One common typed context repair covers all 36; every group still needs its own native note-block witness.
- Boatception checks the vehicle location inside shipwreck OR shipwreck_beached and the actual boats tag. Riding elsewhere does not qualify. Current started-riding infrastructure supplies lifecycle/mounting but not this exact combined structure proof.
- Cat/frog/wolf entries are present in three actual data-driven variant registries, and current variant component conversion agrees with 26.2 vanilla controls. No removed registry or missing variant entry was found. Native taming/leashing executors must cover 11+3+9 AND groups.
- Voluntary exile needs no active raid because historical and current boolean defaults match. The real bug is ignored camelCase keys. Exact native captain state is patrol leader plus the native ominous head banner. Bad Omen acquisition/effect is not this frozen criterion.
- Deflect arrow is narrower than the 26.2 vanilla any-projectile control: BACAP requires a skeleton-tag source. Preserve projectile tag, `blocked=true`, source owner and shield display components; no weaker vanilla replacement.

## Foundations and independent proof nodes

The master JSON provides an acyclic graph: typed context foundation; separate new-stage accounting; native SURVIVAL/network lifecycle; narrow raider migration/freshness; zero-gain certified raider integrity; four protected-control replacements; nineteen independent CERT nodes; final real-1152 product gate. Only the nineteen unique CERT nodes carry gain 1. Shared foundations do not count as advancements.

Runtime gates require legitimate native actions, exact player/item/entity/world state, false-to-true receipts, finite resources/ticks and loaded live production gate thresholds. No grants, direct triggers, forced tame/block results, missing predicate fields or invented tag members. Fresh fingerprint-bound exact evidence and zero fixture/network cleanup debt are mandatory. Fixture setup is not an action receipt.

For every native fixture, inspect gate config after player-loaded handling: GET_INTO_BOAT/shipwreck landmark for boat; boot equipment/movement gates for Frost Walker; cauldron and material abilities for washing/wax-off; applicable ignition/landmark gates for candles; cat/wolf/lead landmark restrictions; USE_SHIELD for shield. Masters need native pickup, not wearing/using the equipment. Source inspection identified these gates and found no proven gate defect; later canary verifies actual loaded behavior.

## Protected control replacement

- **`blazeandcave:enchanting/let_it_go`:** Guarded against broad enchantment/worldgen scalar migration and runtime-only override of malformed/static-invalid input. Existing test explicitly asserts remaining deep-biome scalar errors and no frost_walker scalar error. Final certification need not retain indefinite protection. Full typed biome+enchantment parse and frozen semantic equality; all four OR branches valid; replacement fail-closed controls plus native Frost Walker/LOCATION proof. Replacement: Unknown biome/holder, wrong boots, absent Frost Walker, ordinary ice, shallow/wrong ocean, spectator negatives; preserve valid deep-ocean scalar predicates and reject broad fallback/migration.
- **`blazeandcave:redstone/travelling_bard`:** Protected static negative control against generic biome migration; existing regression explicitly expects STATIC_FAIL with pale_garden diagnostics. No runtime evidence could replace its unresolved static semantic boundary in Phase A. Final certification need not retain indefinite protection. New-stage typed worldgen context accounts for every diagnostic and resolves all 36 exact biome definitions without a generic rewrite; replacement fail-closed controls accepted, followed by native note-block use for each of the 36 AND groups. Replacement: Unknown biome scalar/list/mixed selectors, missing biome registry/entry, wrong biome, wrong block or non-default block use, deleted AND group and stale receipt negatives. Retain all valid frozen scalar/list forms and all 36 predicates.
- **`minecraft:adventure/voluntary_exile`:** Deferred entity-tag control after bounded banner-pattern fallback; deliberately excluded from runtime promotion until explicit invalidation. Old guard did not prove captain boolean semantics. Final certification need not retain indefinite protection. Proven current field loss justifies new-stage reopening only after narrow correction, fresh copy marker, certified raider zero-gain integrity gates and native valid captain kill proof. Replacement: Noncaptain, banner/leader mismatch, active raid captain, outsider entity tag; decode snake_case booleans and reject missing/stale receipts.
- **`minecraft:story/deflect_arrow`:** Deferred skeleton entity-tag control after shield display/banner fallback; retained as negative accounting/runtime-promotion guard, never accepted gameplay evidence. Final certification need not retain indefinite protection. Exact source-backed schema/tag proof and replacement controls, then skeleton-owned native projectile block with loaded gate, finite resources and cleanup. Replacement: No block, melee, non-skeleton projectile, locked shield, missing skeleton tag; stale/absent native receipts rejected.

Until replacement guards and the exact reopening gate pass, the old protection still applies to promotion. Record invalidation only as a new FINAL19-stage action; history remains true. No protection was invalidated by this planning task.

## Current copy and evidence provenance

Frozen `run/datapacks/bacap.zip` is source, not a migrated runtime proof. The effective `build/run/gameTest/world/datapacks/bacap.zip` has compat_26_2_r15 plus `llamaCarpetNbtMapping=equipment.body`; all 19 criterion definitions and requirements match freshly invoking the actual current converter. All 19 fresh compact UTF-8 hashes match the checked-in llama-authorized converted fingerprint map. The one whole-definition difference is voluntary-exile display punctuation already encoded as replacement characters in the effective archive; its criteria are identical. It is not the captain predicate failure.

Eighteen archive copies were inspected. Older save-world r15 packs lack the required llama mapping marker; ten target criterion definitions are stale there. They cannot be accepted as current compatible proof. They were not edited. A later raider fix needs a distinct freshness marker/revision plus converter/source fingerprints so old r15 output is rejected even when llama freshness passes.

The accepted jungle receipt is retained from runtime_execution_evidence.json, family LOCATION_MOVEMENT, source PhaseALocationMovementGameTest, criterion jungle, GREEN/AUTOMATED_GREEN. The audit stores its entire existing envelope/runId; no new runtime result replaces it.

## Diagnostic validation and limits

No GameTest/gameplay run was needed to distinguish the proven root causes. One scratch JUnit diagnostic invoked actual production conversion and real 26.2 codecs with complete vanilla registry context plus recursively resolved actual source tags. It verified 19 complete definitions and all 108 individual criteria parse, and decoded raider boolean loss. Thirty-nine enchantment branches were independently checked against actual 26.2 enchantment definitions, supported-item tags, level limits and exclusive sets; all have legal witnesses.

The focused final run passed 18 tests: FINAL19 diagnostic (1), existing PhaseACertificationTest (15), PhaseATerminalReconciliationTest (2). The first scratch compile had a generic delegate typing error; scratch was corrected and the final focused command passed. Gradle/JUnit logs contain existing native-access/Unsafe deprecation warnings. This is not a warning-free GameTest/network gameplay receipt and no such claim is made.

The diagnostic registry providers cannot encode decoded holder objects back due to registry-owner identity validation. No successful encoding roundtrip is claimed. This diagnostic limitation is distinct from the positive parse result and exact independently decoded raider boolean evidence. It does not justify changing gameplay definitions.

Actual loaded Mojang-named 26.2 jar: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-84afe0508c/26.2/minecraft-merged-84afe0508c-26.2.jar`, SHA-256 `4beaad36aebac56874c1feef75a680638fed2d733387b5977481e5b21406d1ef`. Its embedded version is 26.2. Bytecode citations use that loaded Loom jar. Source tag resources came from the local official deobfuscated 26.2 jar; class hashes for RaiderPredicate and HolderSetCodec match loaded classes. Official mapped historical 1.21.4 RaiderPredicate was consulted only to verify the frozen omitted-boolean defaults.

The authoritative focused Gradle command used accepted Java 25.0.4.7, Gradle home and TEMP/TMP UDS-probe paths directly through the approval-capable route. No sacrificial restricted Gradle attempt or GameTest process termination occurred.

## Created TEMP diagnostics

No diagnostic file is accepted durable evidence. The two essential diagnostic code files are:

- `build/tmp/final19_audit/java/com/diskree/achievetodo/certification/Final19DiagnosticTest.java`
- `build/tmp/final19_audit/final19.init.gradle`

Scratch source-analysis/planning scripts: `collect_facts.py` and `build_outputs.py`. The complete created scratch inventory (including raw diagnostics, generated current definitions, bytecode, logs, JUnit reports and baseline hashes) follows. Gradle-managed compiled/report/cache output is ordinary TEMP build output, not a new durable planning artifact.

- `build/tmp/final19_audit/baseline_hashes.json`
- `build/tmp/final19_audit/build_outputs.py`
- `build/tmp/final19_audit/bytecode/AbstractArrow.txt`
- `build/tmp/final19_audit/bytecode/Advancement.txt`
- `build/tmp/final19_audit/bytecode/AxeItem.txt`
- `build/tmp/final19_audit/bytecode/CakeBlock.txt`
- `build/tmp/final19_audit/bytecode/Cat.txt`
- `build/tmp/final19_audit/bytecode/CauldronInteraction.txt`
- `build/tmp/final19_audit/bytecode/DamagePredicate.txt`
- `build/tmp/final19_audit/bytecode/EnchantmentHelper.txt`
- `build/tmp/final19_audit/bytecode/EntityHurtPlayerTrigger.txt`
- `build/tmp/final19_audit/bytecode/EntityPredicate.txt`
- `build/tmp/final19_audit/bytecode/EntitySubPredicates.txt`
- `build/tmp/final19_audit/bytecode/HISTORICAL_1_21_4_RaiderPredicate.txt`
- `build/tmp/final19_audit/bytecode/HolderSetCodec.txt`
- `build/tmp/final19_audit/bytecode/ItemUsedOnLocationTrigger.txt`
- `build/tmp/final19_audit/bytecode/Leashable.txt`
- `build/tmp/final19_audit/bytecode/LivingEntity.txt`
- `build/tmp/final19_audit/bytecode/LocationPredicate.txt`
- `build/tmp/final19_audit/bytecode/Mob.txt`
- `build/tmp/final19_audit/bytecode/NoteBlock.txt`
- `build/tmp/final19_audit/bytecode/PlayerTrigger.txt`
- `build/tmp/final19_audit/bytecode/Raider.txt`
- `build/tmp/final19_audit/bytecode/RaiderPredicate.txt`
- `build/tmp/final19_audit/bytecode/ServerPlayer.txt`
- `build/tmp/final19_audit/bytecode/ServerPlayerGameMode.txt`
- `build/tmp/final19_audit/bytecode/StartRidingTrigger.txt`
- `build/tmp/final19_audit/bytecode/Wolf.txt`
- `build/tmp/final19_audit/canonical19.json`
- `build/tmp/final19_audit/collect_facts.py`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/biomes/boatception.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/biomes/the_mighty_jungle.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/building/happy_birthday.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/building/setting_up_the_mood.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/building/washing_machine.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/enchanting/let_it_go.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/enchanting/master_armorer.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/enchanting/master_axeman.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/enchanting/master_digger.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/enchanting/master_farmer.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/enchanting/master_knight.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/enchanting/master_miner.json`
- `build/tmp/final19_audit/current_converted/data/blazeandcave/advancement/redstone/travelling_bard.json`
- `build/tmp/final19_audit/current_converted/data/minecraft/advancement/adventure/voluntary_exile.json`
- `build/tmp/final19_audit/current_converted/data/minecraft/advancement/husbandry/complete_catalogue.json`
- `build/tmp/final19_audit/current_converted/data/minecraft/advancement/husbandry/leash_all_frog_variants.json`
- `build/tmp/final19_audit/current_converted/data/minecraft/advancement/husbandry/wax_off.json`
- `build/tmp/final19_audit/current_converted/data/minecraft/advancement/husbandry/whole_pack.json`
- `build/tmp/final19_audit/current_converted/data/minecraft/advancement/story/deflect_arrow.json`
- `build/tmp/final19_audit/diagnostic.gradle.log`
- `build/tmp/final19_audit/diagnostic_final.gradle.log`
- `build/tmp/final19_audit/diagnostic_provenance.gradle.log`
- `build/tmp/final19_audit/diagnostic_retry.gradle.log`
- `build/tmp/final19_audit/diagnostic_semantic.gradle.log`
- `build/tmp/final19_audit/diagnostic_verified.gradle.log`
- `build/tmp/final19_audit/extracted.json`
- `build/tmp/final19_audit/final19.init.gradle`
- `build/tmp/final19_audit/fresh_context_diagnostic.json`
- `build/tmp/final19_audit/java/com/diskree/achievetodo/certification/Final19DiagnosticTest.java`
- `build/tmp/final19_audit/junit/TEST-com.diskree.achievetodo.certification.Final19DiagnosticTest.xml`
- `build/tmp/final19_audit/junit/TEST-com.diskree.achievetodo.certification.PhaseACertificationTest.xml`
- `build/tmp/final19_audit/junit/TEST-com.diskree.achievetodo.certification.PhaseATerminalReconciliationTest.xml`
- `build/tmp/final19_audit/junit-html/css/base-style.css`
- `build/tmp/final19_audit/junit-html/css/style.css`
- `build/tmp/final19_audit/junit-html/Gng69gOvgeY/5OMYYsB3mdE.html`
- `build/tmp/final19_audit/junit-html/Gng69gOvgeY/index.html`
- `build/tmp/final19_audit/junit-html/index.html`
- `build/tmp/final19_audit/junit-html/js/report.js`
- `build/tmp/final19_audit/junit-html/nmttUJZU5Qg/index.html`
- `build/tmp/final19_audit/junit-html/syh8LruQH48/index.html`
- `build/tmp/final19_audit/loaded_bytecode/Advancement.txt`
- `build/tmp/final19_audit/loaded_bytecode/AxeItem.txt`
- `build/tmp/final19_audit/loaded_bytecode/CakeBlock.txt`
- `build/tmp/final19_audit/loaded_bytecode/Cat.txt`
- `build/tmp/final19_audit/loaded_bytecode/CauldronInteraction.txt`
- `build/tmp/final19_audit/loaded_bytecode/DefaultBlockInteractionTrigger.txt`
- `build/tmp/final19_audit/loaded_bytecode/EnchantmentHelper.txt`
- `build/tmp/final19_audit/loaded_bytecode/Entity.txt`
- `build/tmp/final19_audit/loaded_bytecode/EntityHurtPlayerTrigger.txt`
- `build/tmp/final19_audit/loaded_bytecode/EntityPredicate.txt`
- `build/tmp/final19_audit/loaded_bytecode/HolderSetCodec.txt`
- `build/tmp/final19_audit/loaded_bytecode/ItemUsedOnLocationTrigger.txt`
- `build/tmp/final19_audit/loaded_bytecode/KilledTrigger.txt`
- `build/tmp/final19_audit/loaded_bytecode/LivingEntity.txt`
- `build/tmp/final19_audit/loaded_bytecode/LocationPredicate.txt`
- `build/tmp/final19_audit/loaded_bytecode/NoteBlock.txt`
- `build/tmp/final19_audit/loaded_bytecode/PlayerPredicate.txt`
- `build/tmp/final19_audit/loaded_bytecode/Raider.txt`
- `build/tmp/final19_audit/loaded_bytecode/RaiderPredicate.txt`
- `build/tmp/final19_audit/loaded_bytecode/ServerPlayer.txt`
- `build/tmp/final19_audit/loaded_bytecode/ServerPlayerGameMode.txt`
- `build/tmp/final19_audit/loaded_bytecode/TamableAnimal.txt`
- `build/tmp/final19_audit/loaded_bytecode/TameAnimalTrigger.txt`
- `build/tmp/final19_audit/loaded_bytecode/Wolf.txt`
- `build/tmp/final19_audit/source_facts.json`
- `build/classes/java/test/com/diskree/achievetodo/certification/Final19DiagnosticTest$1.class` (Gradle-managed compiled diagnostic class)
- `build/classes/java/test/com/diskree/achievetodo/certification/Final19DiagnosticTest.class` (Gradle-managed compiled diagnostic class)

## Preservation and artifact integrity

2101 protected baseline files were SHA-256 compared with the initial audit snapshot; **zero changed**. This covers production Java/resources/generated files, src/test, src/gametest, all prior planning/history/evidence plus canonical/frontier/SKILL/frozen archive/build.gradle. Pre-existing dirty work remains intact. Exactly these three durable outputs were created in the dedicated FINAL19 directory; no implementation, promotion, Phase B, cleanup, final release JAR build or Git mutation occurred.

- Audit SHA-256: `357f8cfe218ab15cfd23608dfedd15cdda70e297ab4c533fe7ac76b9f04ed408`
- Master map SHA-256: `221c0041f7668cbdcbe9e647a4a3aec2705b16a304fccfb30c4ff8881fd4b913`
- This report SHA-256 is computed after final write and returned in the final response; self-hashing text is intentionally excluded.

Map validation: 19 unique IDs exactly equal current 17 unresolved-static plus 2 deferred controls; no certified ID added as an unresolved target. Criteria/groups reconcile 108/79 with 78 unsatisfied and 1 accepted GREEN. Expected gain sums 19 without duplicates; dependency graph is acyclic and references existing nodes; final predicted row is 1152/0. All three files were re-read and checked before delivery.

Work stops at the completed audit and remediation map. Real 1152 certification remains future work gated by this plan, including the proved certified raider integrity debt.
