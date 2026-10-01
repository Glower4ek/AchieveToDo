# FINAL19 new production bug: locked cauldron falsely awards cleaning criteria

Product remains **1138 / 1152**; historical Phase A remains **1133 / 1152**. Accepted FINAL19 families WORLDGEN_HOLDERSET_CONTEXT and ENTITY_VARIANT_COMPONENT_CONTEXT are unchanged. DUAL_ITEM_BLOCK_TAG_CONTEXT is unfinished and unpromoted. Families 4–7 have not started. Raider authorization is still required at its later boundary.

## Authorized wax/axe fix completed

Both user-specified pre-application SHA-256 values matched exactly. Only the reviewed AxeItemMixin evaluateNewBlockState injection moved to HEAD. All bytes outside that replacement were preserved. Production post-fix SHA-256: `4247ef44ce7e331f43820d889ecef541f8cbb0a200a31ede2764c8ecbdd46a13`.

Fresh native packet regression `566ab631-e69f-43b4-b4a6-690a83c312f6` passed all 14 controls: locked material, legitimate live-score unlock, locked landmark with unlocked material, equivalent outside-landmark cases for stripping/scraping/wax-off, wrong item and no-op. Block/durability/progress/gates are observed before and after; actual axe/waxed-block tag membership and actor/connection/channel/landmark/block cleanup passed. Landmark testing uses an owned production loaded-landmark metadata fixture, not a forced gate predicate. Source bytes, every tuple, unique players and missing/stale/fabricated witness rejection were independently validated. Persistent proof is `src/test/resources/final19_certification/axe_gate_postfix_regression.json`, gain zero.

## Dual-tags resumed from post-fix state

Fresh canary `5c40ea07-d9ad-45c6-b1ad-c267fe43740d` passed both candle-on-cake and wax-off groups. ACTUAL TEMP/run-state audit checked exact keys, unique players, current fingerprint, runtime pack hash, exit 0, selector count 1 and zero native warning debt. Canary remains diagnostic, never product evidence.

Fresh exact `62c46b0a-8e9f-4f26-be99-6299336d8ac3` selected exactly one all-six test, then failed at locked clean_leather_armor. Criterion false -> true occurred while water_cauldron level=3 and dyed chestplate bytes stayed identical. Failed exact contains only two partial receipts; run-state remains RUNNING because completion was not reached. It is not promotable. The flame no-op fixture was narrowly corrected to obstruct fire placement above an already lit candle; negative controls were retained.

## Independent production-equivalent diagnosis

Native packet diagnostic `30c8e9d9-4710-476a-852f-30e3f4baa96f` used three distinct joined, client-loaded SURVIVAL actors and finite items. Wrong-item controls did not award progress. For each actual leather_armor/banners/shulker_boxes tag member on an actual cauldrons tag member, USE_CAULDRON was locked before and after, live threshold **513**, score after action **0**. Each named cleaning criterion changed false -> true despite unchanged water level, unchanged held item bytes and no cleaning mutation. Packet path is ServerboundUseItemOnPacket.handle -> normal server interaction. All three cleanup witnesses passed; exact one-test selector exited 0 with BUILD SUCCESSFUL, confirming the bug. This success is diagnostic only. Native invocation through shutdown has zero WARN/ERROR/network debt; full startup diagnostics are preserved.

`AbstractCauldronBlockMixin.lockCauldron` returns InteractionResult.SUCCESS when its gate blocks CauldronInteraction.interact. Official 26.2 ServerPlayerGameMode.useItemOn emits ITEM_USED_ON_BLOCK whenever the block callback consumes the action. Thus the blocked callback still awards criteria. The original cauldron source hash remains `c9473d4918c8d31c7215dcb98a388f3a24660b36940940a598861e9d90985394`.

## Review-only proposal — not applied

Patch: `reference/phase_a_planning/final19/cauldron_gate_production_fix_proposal.patch`

SHA-256: `ec33a350ef1ae9185ac99e1c2425a1cec38ca0d64244819058c3cc610a5a07fe`

Exact scope is three production files:

- Add AbstractCauldronBlockAccessor.java for the live dispatcher.
- Add ServerPlayerGameModeCauldronGateMixin.java: at useItemOn HEAD, when the target is a cauldron and its actual dispatcher recognizes a non-default interaction, apply the existing material-independent cauldron/landmark gate and terminate the entire server interaction with FAIL if closed. Preserve vanilla intentional secondary item use, default/unregistered item interactions, unlocked uses and unrelated targets.
- Register only those two mixins in achievetodo.mixins.json; pre-fix SHA-256 `ed9368d4cf34f18e2d72b47f764ee1c4674418eff82b7d57fe4a1d6825380339`.

The existing AbstractCauldronBlockMixin client/server wrapper stays byte-identical. A return-value-only change inside that block callback is insufficiently bounded: vanilla can fall through to ItemStack.useOn, allowing placeable banner/shulker items to produce another consuming interaction and trigger. The earlier server boundary avoids both award emission and item fallback while preserving existing dispatch and gate policy.

Exact proposed Java sources compiled separately against current production classes and official 26.2 APIs: compileFinal19CauldronProposal, BUILD SUCCESSFUL, exit 0. Output is isolated under build/tmp and the proposal is not registered or loaded at runtime. Compilation is not post-fix native proof. git apply --check passed without applying the patch. Both added production files are absent, and current mixin configuration remains at its pre-fix hash.

## Required runtime regressions after separate authorization

1. Each of the three cleaning actions with USE_CAULDRON locked: native packet, item/block unchanged, no water cost and no criterion/progress.
2. Legitimate live-score unlock: native dyed-armor color removal, banner pattern removal, shulker uncoloring; exact water cost, finite consumption and false -> true named criteria.
3. Otherwise unlocked cauldron ability inside a closed landmark: all three cleaning paths blocked; equivalent outside controls succeed. Restore owned landmark metadata and world fixtures.
4. Wrong-item and already-clean no-op controls, real item/block tags, default cauldron dispatch, empty hand and deliberate secondary item placement behavior remain source-equivalent. Include placement-capable banner/shulker fallback controls.
5. Preserve all 14 authorized axe controls and the two existing accepted FINAL19 families. Then run fresh dual-tags canary and all-six exact, actual TEMP/run-state audits, independent evidence validation, persistent promotion, local ledger/state delta, persistent regression, transaction sealing and checkpoint.

## Durable preservation and stop reason

Nine focused tests passed with zero failures/errors/skipped: artifact-local wax proof (2), dual-tag static semantics (2), accepted worldgen/variants (4) and ledger (1). No closed native family was rerun. The only changed production file is the exact authorized AxeItemMixin patch; converter/datapack/markers and all other preserved production files remain unchanged. Historical Phase A, accepted catalog/evidence bytes, ledger attribution, Git HEAD/branch/index remain preserved.

Failed exact, successful diagnostic and all gate logs/receipts/source bytecode are hash-inventoried under diagnosed_production_bugs/cauldron_gate. The first unfinished transaction is recorded in execution_state.json. No sealing or family promotion was attempted after the failed exact.

Stop follows the current user STOP POLICY item 2: **another newly proven production bug outside this wax fix**. The wax authorization does not cover the cauldron proposal. Separate explicit authorization is needed before any of its production bytes change.
