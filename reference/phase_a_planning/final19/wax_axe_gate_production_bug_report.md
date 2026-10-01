# FINAL19 new production bug: locked axe wax-off bypass

Product remains **1138 / 1152**, historical Phase A **1133 / 1152**. WORLDGEN_HOLDERSET_CONTEXT and ENTITY_VARIANT_COMPONENT_CONTEXT remain accepted. DUAL_ITEM_BLOCK_TAG_CONTEXT is unfinished; nothing from its failed canary or diagnostic was promoted. Families 4–7 have not started.

## Observed proof

CompileTestJava, compileGametestJava and both Final19DualTagsStaticTest tests passed after fixing only draft 26.2 harness API names. All six criteria and six requirement groups retain both tag dimensions.

Fresh canary `c01abdba-e443-46d3-93d0-e021716c72f1` selected one test and failed: locked diamond axe unwaxed copper, consumed one durability and completed wax_off. Its TEMP run-state remains RUNNING because complete() was not reached; it is failed diagnostic output, never promotable. Birthday's partial receipt does not certify the family.

Independent minimized native packet diagnostic `0937d889-b634-4397-b591-96e25f4307ee` selected exactly one test, exited 0 with BUILD SUCCESSFUL, and **confirmed the bug**. This successful diagnostic is not a successful certification gate. It used a joined client-loaded SURVIVAL player, registered connection and finite diamond axe. Live threshold was 401; score after action 1 remained below it, with the ability locked both before and after. Locked oak-log stripping was blocked. On the same actor, native ServerboundUseItemOnPacket handling changed waxed_copper_block to copper_block, durability 0 -> 1, and criterion false -> true. Owned player/connection/channel/block cleanup passed. Native diagnostic invocation through shutdown has zero WARN/ERROR/network debt. Startup Windows performance-counter/tag warnings are outside the owned actor window and remain in the preserved full log.

## Root cause

`src/main/java/com/diskree/achievetodo/injection/mixin/main/AxeItemMixin.java` injects in evaluateNewBlockState immediately before its direct Level.playSound invocation. Official loaded 26.2 AxeItem bytecode places that call only in the stripping branch. Scraping and wax removal invoke spawnSoundAndParticle, whose playSound lies in a separate helper and cannot match the injection in evaluateNewBlockState. Therefore the material and landmark checks are skipped for those branches. The packet reproduction excludes a direct gameMode harness bypass; the stripping control proves the material gate can run on this same axe/player.

## Reviewable proposal — not applied

Move the existing evaluateNewBlockState injection to HEAD. Retain the existing material/landmark checks, Optional.empty return and all method signatures. This covers stripping, scraping and wax-off before any side effects or native criterion trigger. No converter or datapack freshness migration is needed for this Java gate repair; runtime classes must be rebuilt after authorization. The raider proposal remains a later separate boundary.

Patch: `reference/phase_a_planning/final19/wax_axe_gate_production_fix_proposal.patch`

SHA-256: `8aa346920a42de51dc8ce8756d6fd212d5d055709c633e07ce8e223e7a49f1a3`

Current production file SHA-256: `e19c4a65da4df917424a44ff00248220f20dcab57d0668b9ebfbebd01fabbb46`

## Required regressions after authorization

1. Native packet with locked diamond axe: oak log stays unstripped; exposed copper stays exposed; waxed copper stays waxed; durability unchanged and wax_off false.
2. Native packet with the legitimate live-score gate unlocked: each corresponding native stripping/scraping/unwaxing mutation occurs; correct durability cost and false -> true wax_off.
3. Locked landmark with otherwise unlocked material: all three paths blocked, no mutation/progress/durability cost; outside-landmark controls succeed.
4. Wrong item, no-op native use, exact pre-mutation waxed block tag and actual held axe tag remain strict. Preserve other five dual-tag groups and independent observed receipts.
5. Fresh compile/static gates, fresh canary, actual TEMP/run-state audit, fresh exact all-six, independent validation, promotion, local ledger/state delta, persistent regression, seal and checkpoint. Do not reuse failed or diagnostic run IDs.

## Preservation and continuation

The patch has not been applied. No production bytes, historical Phase A artifacts, accepted family evidence, Git HEAD/branch/index, or product attribution changed. The prior dirty production changes predate this recovery. Checkpoint re-verifies these bytes against implementation_preservation.json. Diagnostic logs, original failed TEMP/run-state, exact observations and official bytecode are preserved under diagnosed_production_bugs/wax_axe_gate with a hash inventory.

Stop is required by user request section 18, allowed stop 2: **newly proven production bug OUTSIDE the mapped raider issue**. This is not credit exhaustion, an ordinary fixture failure, or the planned raider boundary. First unfinished transaction remains dual-tags; explicit user authorization is needed before applying this additional production fix.

Recovery persistent regression: 5 tests, zero failures/errors/skipped, BUILD SUCCESSFUL, exit 0. Existing accepted worldgen/variant proofs and ledger pass artifact-local validation. Proposal passes git apply --check; no patch was applied. Final checkpoint passes at productCertified=1138.
